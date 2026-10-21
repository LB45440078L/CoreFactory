package io.corefactory.core.turret;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TargetPolicyTest {

    private static final TurretSettings SETTINGS = TurretSettings.defaults();

    @Test
    void aNewTurretHasNoFriendlyFireAndFollowsTheConfig() {
        assertThat(TargetPolicy.DEFAULT.friendlyFire()).isFalse();
        assertThat(TargetPolicy.DEFAULT.allows("ZOMBIE", TargetKind.MONSTER, SETTINGS)).isTrue();
        assertThat(TargetPolicy.DEFAULT.allows("PHANTOM", TargetKind.PHANTOM, SETTINGS)).isFalse();
    }

    @Test
    void anEntityOverrideWinsOverItsKind() {
        TargetPolicy policy = TargetPolicy.DEFAULT.withOverride("monsters", false).withOverride("ZOMBIE", true);

        assertThat(policy.allows("ZOMBIE", TargetKind.MONSTER, SETTINGS)).isTrue();
        assertThat(policy.allows("SKELETON", TargetKind.MONSTER, SETTINGS)).isFalse();
    }

    @Test
    void aKindOverrideWinsOverTheConfig() {
        TargetPolicy policy = TargetPolicy.DEFAULT.withOverride("phantoms", true);

        assertThat(policy.allows("PHANTOM", TargetKind.PHANTOM, SETTINGS)).isTrue();
    }

    @Test
    void anUnknownEntityFollowsItsKind() {
        assertThat(TargetPolicy.DEFAULT.allows(null, TargetKind.MONSTER, SETTINGS)).isTrue();
        assertThat(TargetPolicy.DEFAULT.allows(null, TargetKind.OTHER, SETTINGS)).isFalse();
    }

    @Test
    void theBuildersLeaveTheOtherPartsAlone() {
        TargetPolicy on = TargetPolicy.DEFAULT.withFriendlyFire(true).withOverride("GHAST", false);

        assertThat(on.friendlyFire()).isTrue();
        assertThat(on.overrideOf("GHAST")).contains(false);
        assertThat(on.withFriendlyFire(false).overrideOf("GHAST")).contains(false);
        assertThat(on.overrideOf("ZOMBIE")).isEmpty();
    }

    @Test
    void aPlayerStanceIsSetAndClearedByItsId() {
        UUID alice = UUID.randomUUID();
        TargetPolicy targeted = TargetPolicy.DEFAULT.withPlayer(alice, TargetPolicy.PlayerStance.TARGET);

        assertThat(targeted.stanceOf(alice)).contains(TargetPolicy.PlayerStance.TARGET);
        assertThat(targeted.withPlayer(alice, TargetPolicy.PlayerStance.SPARE).stanceOf(alice))
                .contains(TargetPolicy.PlayerStance.SPARE);
        assertThat(targeted.withPlayer(alice, null).stanceOf(alice)).isEmpty();
        assertThat(targeted.stanceOf(UUID.randomUUID())).isEmpty();
        assertThat(targeted.stanceOf(null)).isEmpty();
    }

    @Test
    void theTeamIsSparedUnlessTheTurretSaysOtherwise() {
        assertThat(TargetPolicy.DEFAULT.teamMode()).isEqualTo(TargetPolicy.TeamMode.SPARE);
        assertThat(TargetPolicy.DEFAULT.withTeamMode(TargetPolicy.TeamMode.IGNORE).teamMode())
                .isEqualTo(TargetPolicy.TeamMode.IGNORE);
    }

    @Test
    void theBuildersKeepPlayerStancesAndTheTeamMode() {
        UUID alice = UUID.randomUUID();
        TargetPolicy policy = TargetPolicy.DEFAULT.withPlayer(alice, TargetPolicy.PlayerStance.SPARE)
                .withTeamMode(TargetPolicy.TeamMode.TARGET)
                .withFriendlyFire(true)
                .withOverride("GHAST", false);

        assertThat(policy.stanceOf(alice)).contains(TargetPolicy.PlayerStance.SPARE);
        assertThat(policy.teamMode()).isEqualTo(TargetPolicy.TeamMode.TARGET);
        assertThat(policy.withOverride("ZOMBIE", true).stanceOf(alice)).contains(TargetPolicy.PlayerStance.SPARE);
        assertThat(policy.withOverride("ZOMBIE", true).teamMode()).isEqualTo(TargetPolicy.TeamMode.TARGET);
    }

    @Test
    void theCatalogListsEachEntityOnceAndFitsTheMenu() {
        Set<String> names = new HashSet<>();
        for (TargetCatalog.Entry entry : TargetCatalog.ENTITIES) {
            assertThat(names.add(entry.entity())).as(entry.entity()).isTrue();
            assertThat(entry.kind()).isNotEqualTo(TargetKind.OTHER);
        }
        assertThat(TargetCatalog.ENTITIES.size()).isLessThanOrEqualTo(18);
        assertThat(TargetCatalog.CATEGORIES).doesNotContain(TargetKind.OTHER);
    }
}
