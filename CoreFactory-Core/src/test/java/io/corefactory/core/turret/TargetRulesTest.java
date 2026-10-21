package io.corefactory.core.turret;

import io.corefactory.core.turret.TargetRules.Candidate;
import io.corefactory.core.turret.TargetRules.Verdict;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TargetRulesTest {

    private static final TurretSettings SETTINGS = TurretSettings.defaults();

    private static Candidate zombie(double distance) {
        return new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, distance, true);
    }

    @Test
    void aHostileMonsterInRangeAndInArcIsValid() {
        assertThat(TargetRules.verdict(zombie(10.0), SETTINGS)).isEqualTo(Verdict.VALID);
        assertThat(TargetRules.isValid(zombie(10.0), SETTINGS)).isTrue();
    }

    @Test
    void aMonsterExactlyAtTheRangeIsStillValid() {
        assertThat(TargetRules.verdict(zombie(SETTINGS.range()), SETTINGS)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aPlayerIsNeverATarget() {
        Candidate player = new Candidate(TargetKind.MONSTER, true, false, false, false, false, false, false, 5.0, true);

        assertThat(TargetRules.verdict(player, SETTINGS)).isEqualTo(Verdict.PLAYER);
    }

    @Test
    void aPlayerIsRefusedEvenWhenEveryOtherSettingWouldAllowIt() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.targets", List.of("monsters", "phantoms", "slimes", "ghasts"));
        config.set("turrets.target-named", true);
        config.set("turrets.target-tamed", true);
        TurretSettings permissive = TurretSettings.from(config);
        Candidate player = new Candidate(TargetKind.MONSTER, true, false, false, false, true, true, false, 5.0, true);

        assertThat(TargetRules.verdict(player, permissive)).isEqualTo(Verdict.PLAYER);
    }

    @Test
    void aVillagerIsNeverATarget() {
        Candidate villager = new Candidate(TargetKind.OTHER, false, true, false, false, false, false, false, 5.0, true);

        assertThat(TargetRules.verdict(villager, SETTINGS)).isEqualTo(Verdict.VILLAGER);
    }

    @Test
    void aGolemIsNeverATarget() {
        Candidate golem = new Candidate(TargetKind.OTHER, false, false, true, false, false, false, false, 5.0, true);

        assertThat(TargetRules.verdict(golem, SETTINGS)).isEqualTo(Verdict.GOLEM);
    }

    @Test
    void anAnimalIsNeverATarget() {
        Candidate cow = new Candidate(TargetKind.OTHER, false, false, false, true, false, false, false, 5.0, true);

        assertThat(TargetRules.verdict(cow, SETTINGS)).isEqualTo(Verdict.ANIMAL);
    }

    @Test
    void aMobInAProtectedRegionIsNeverATarget() {
        Candidate protectedMob = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, true, 5.0, true);

        assertThat(TargetRules.verdict(protectedMob, SETTINGS)).isEqualTo(Verdict.PROTECTED);
    }

    @Test
    void aTamedMobIsRefusedByDefault() {
        Candidate tamed = new Candidate(TargetKind.MONSTER, false, false, false, false, true, false, false, 5.0, true);

        assertThat(TargetRules.verdict(tamed, SETTINGS)).isEqualTo(Verdict.TAMED);
    }

    @Test
    void aNamedMobIsRefusedByDefault() {
        Candidate named = new Candidate(TargetKind.MONSTER, false, false, false, false, false, true, false, 5.0, true);

        assertThat(TargetRules.verdict(named, SETTINGS)).isEqualTo(Verdict.NAMED);
    }

    @Test
    void taggedMobsBecomeTargetsOnlyWhenTheSettingsAllowThem() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.target-named", true);
        config.set("turrets.target-tamed", true);
        TurretSettings allowing = TurretSettings.from(config);
        Candidate named = new Candidate(TargetKind.MONSTER, false, false, false, false, true, true, false, 5.0, true);

        assertThat(TargetRules.verdict(named, allowing)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aPhantomIsATargetOnlyWhenTheListNamesPhantoms() {
        Candidate phantom = new Candidate(TargetKind.PHANTOM, false, false, false, false, false, false, false, 5.0, true);
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.targets", List.of("monsters", "phantoms"));

        assertThat(TargetRules.verdict(phantom, SETTINGS)).isEqualTo(Verdict.NOT_A_TARGET_KIND);
        assertThat(TargetRules.verdict(phantom, TurretSettings.from(config))).isEqualTo(Verdict.VALID);
    }

    @Test
    void aHostileOfAnotherKindIsNotHostileToTheTurret() {
        Candidate other = new Candidate(TargetKind.OTHER, false, false, false, false, false, false, false, 5.0, true);

        assertThat(TargetRules.verdict(other, SETTINGS)).isEqualTo(Verdict.NOT_HOSTILE);
    }

    @Test
    void aMobBeyondTheRangeIsOutOfRange() {
        assertThat(TargetRules.verdict(zombie(SETTINGS.range() + 0.5), SETTINGS)).isEqualTo(Verdict.OUT_OF_RANGE);
    }

    @Test
    void aMobOutsideTheArcIsOutOfArc() {
        Candidate behind = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 5.0, false);

        assertThat(TargetRules.verdict(behind, SETTINGS)).isEqualTo(Verdict.OUT_OF_ARC);
    }

    @Test
    void theRulesRunInOrderSoAPlayerOutOfRangeIsStillAPlayer() {
        Candidate player = new Candidate(TargetKind.MONSTER, true, false, false, false, false, false, false, 99.0, false);

        assertThat(TargetRules.verdict(player, SETTINGS)).isEqualTo(Verdict.PLAYER);
    }

    private static final TargetPolicy FRIENDLY_FIRE = new TargetPolicy(true, java.util.Map.of());

    private static Candidate player(TargetRules.Relation relation, double distance) {
        return new Candidate(TargetKind.OTHER, true, false, false, false, false, false, false, distance, true, null, relation);
    }

    @Test
    void theOwnerIsNeverATargetEvenWithFriendlyFireOn() {
        assertThat(TargetRules.verdict(player(TargetRules.Relation.OWNER, 5.0), SETTINGS, FRIENDLY_FIRE))
                .isEqualTo(Verdict.OWNER);
    }

    @Test
    void aFriendIsNeverATargetWithFriendlyFireOn() {
        assertThat(TargetRules.verdict(player(TargetRules.Relation.FRIEND, 5.0), SETTINGS, FRIENDLY_FIRE))
                .isEqualTo(Verdict.FRIEND);
    }

    @Test
    void aFriendIsSparedWhileFriendlyFireIsOff() {
        assertThat(TargetRules.verdict(player(TargetRules.Relation.FRIEND, 5.0), SETTINGS, TargetPolicy.DEFAULT))
                .isEqualTo(Verdict.FRIEND);
    }

    @Test
    void aStrangerIsATargetOnlyWhileFriendlyFireIsOn() {
        assertThat(TargetRules.verdict(player(TargetRules.Relation.OTHER, 5.0), SETTINGS, TargetPolicy.DEFAULT))
                .isEqualTo(Verdict.PLAYER);
        assertThat(TargetRules.verdict(player(TargetRules.Relation.OTHER, 5.0), SETTINGS, FRIENDLY_FIRE))
                .isEqualTo(Verdict.VALID);
    }

    @Test
    void aStrangerInsideAClaimIsStillProtected() {
        Candidate inClaim = new Candidate(TargetKind.OTHER, true, false, false, false, false, false, true, 5.0, true, null,
                TargetRules.Relation.OTHER);

        assertThat(TargetRules.verdict(inClaim, SETTINGS, FRIENDLY_FIRE)).isEqualTo(Verdict.PROTECTED);
    }

    @Test
    void aStrangerOutOfRangeOrArcIsStillRefused() {
        assertThat(TargetRules.verdict(player(TargetRules.Relation.OTHER, 99.0), SETTINGS, FRIENDLY_FIRE))
                .isEqualTo(Verdict.OUT_OF_RANGE);
        Candidate behind = new Candidate(TargetKind.OTHER, true, false, false, false, false, false, false, 5.0, false, null,
                TargetRules.Relation.OTHER);
        assertThat(TargetRules.verdict(behind, SETTINGS, FRIENDLY_FIRE)).isEqualTo(Verdict.OUT_OF_ARC);
    }

    @Test
    void aTurretWithAnEntityOverrideSparesThatTypeOnly() {
        TargetPolicy noZombies = TargetPolicy.DEFAULT.withOverride("ZOMBIE", false);
        Candidate zombie = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 5.0, true, "ZOMBIE",
                TargetRules.Relation.NONE);
        Candidate skeleton = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 5.0, true, "SKELETON",
                TargetRules.Relation.NONE);

        assertThat(TargetRules.verdict(zombie, SETTINGS, noZombies)).isEqualTo(Verdict.NOT_A_TARGET_KIND);
        assertThat(TargetRules.verdict(skeleton, SETTINGS, noZombies)).isEqualTo(Verdict.VALID);
    }

    @Test
    void anEntityOverrideCanTargetAKindTheConfigLeavesOut() {
        TurretSettings none = TurretSettings.from(configWithTargets(List.of()));
        TargetPolicy blazes = TargetPolicy.DEFAULT.withOverride("BLAZE", true);
        Candidate blaze = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 5.0, true, "BLAZE",
                TargetRules.Relation.NONE);
        Candidate zombie = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 5.0, true, "ZOMBIE",
                TargetRules.Relation.NONE);

        assertThat(TargetRules.verdict(blaze, none, blazes)).isEqualTo(Verdict.VALID);
        assertThat(TargetRules.verdict(zombie, none, blazes)).isEqualTo(Verdict.NOT_A_TARGET_KIND);
    }

    @Test
    void aKindOverrideAppliesToEveryTypeOfThatKindWithoutItsOwnOverride() {
        TargetPolicy noMonsters = TargetPolicy.DEFAULT.withOverride("monsters", false);
        Candidate witch = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 5.0, true, "WITCH",
                TargetRules.Relation.NONE);
        Candidate slime = new Candidate(TargetKind.SLIME, false, false, false, false, false, false, false, 5.0, true, "SLIME",
                TargetRules.Relation.NONE);

        assertThat(TargetRules.verdict(witch, SETTINGS, noMonsters)).isEqualTo(Verdict.NOT_A_TARGET_KIND);
        assertThat(TargetRules.verdict(slime, SETTINGS, noMonsters)).isEqualTo(Verdict.NOT_A_TARGET_KIND);
        assertThat(TargetRules.verdict(slime, SETTINGS, noMonsters.withOverride("SLIME", true))).isEqualTo(Verdict.VALID);
    }

    private static MemoryConfiguration configWithTargets(List<String> targets) {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.targets", targets);
        return config;
    }
}
