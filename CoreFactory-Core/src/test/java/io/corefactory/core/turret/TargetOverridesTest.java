package io.corefactory.core.turret;

import io.corefactory.core.turret.TargetPolicy.PlayerStance;
import io.corefactory.core.turret.TargetPolicy.TeamMode;
import io.corefactory.core.turret.TargetRules.Candidate;
import io.corefactory.core.turret.TargetRules.Relation;
import io.corefactory.core.turret.TargetRules.Verdict;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The verdict order for players and the per-player and team overrides that feed it: owner, then a SPARE stance, then a TARGET stance,
 * then the team rule, then the friends rule, then friendly fire, then the claim protection and the rest.
 */
class TargetOverridesTest {

    private static final TurretSettings SETTINGS = TurretSettings.defaults();
    private static final TargetPolicy FRIENDLY_FIRE = TargetPolicy.DEFAULT.withFriendlyFire(true);
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static Candidate player(UUID id, Relation relation, boolean teamMate, boolean protectedHere) {
        return new Candidate(TargetKind.OTHER, true, false, false, false, false, false, protectedHere, 5.0, true, null, relation, id,
                teamMate);
    }

    private static Candidate player(Relation relation) {
        return player(ALICE, relation, false, false);
    }

    private static Verdict verdict(Candidate candidate, TargetPolicy policy) {
        return TargetRules.verdict(candidate, SETTINGS, policy);
    }

    @Test
    void theOwnerIsNeverATargetWhateverItsStance() {
        TargetPolicy targeted = TargetPolicy.DEFAULT.withFriendlyFire(true).withPlayer(ALICE, PlayerStance.TARGET);

        assertThat(verdict(player(ALICE, Relation.OWNER, false, false), targeted)).isEqualTo(Verdict.OWNER);
        assertThat(verdict(player(ALICE, Relation.OWNER, false, false), FRIENDLY_FIRE.withPlayer(ALICE, PlayerStance.TARGET)))
                .isEqualTo(Verdict.OWNER);
    }

    @Test
    void aSparedPlayerIsNeverATargetWhateverElseHolds() {
        TargetPolicy spared = FRIENDLY_FIRE.withPlayer(ALICE, PlayerStance.SPARE);

        assertThat(verdict(player(Relation.OTHER), spared)).isEqualTo(Verdict.SPARED);
        assertThat(verdict(player(ALICE, Relation.OTHER, false, true), spared)).isEqualTo(Verdict.SPARED);
    }

    @Test
    void aSparedPlayerBeatsTheClaimAndTheRange() {
        TargetPolicy spared = FRIENDLY_FIRE.withPlayer(ALICE, PlayerStance.SPARE);
        Candidate far = new Candidate(TargetKind.OTHER, true, false, false, false, false, false, true, 99.0, false, null,
                Relation.OTHER, ALICE, false);

        assertThat(verdict(far, spared)).isEqualTo(Verdict.SPARED);
    }

    @Test
    void aTargetedPlayerIsATargetWhileFriendlyFireIsOff() {
        TargetPolicy targeted = TargetPolicy.DEFAULT.withPlayer(ALICE, PlayerStance.TARGET);

        assertThat(verdict(player(Relation.OTHER), targeted)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aTargetedPlayerBeatsTheFriendRule() {
        TargetPolicy targeted = TargetPolicy.DEFAULT.withPlayer(ALICE, PlayerStance.TARGET);

        assertThat(verdict(player(Relation.FRIEND), targeted)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aTargetedPlayerBeatsTheTeamRule() {
        TargetPolicy targeted = TargetPolicy.DEFAULT.withPlayer(ALICE, PlayerStance.TARGET);

        assertThat(verdict(player(ALICE, Relation.OTHER, true, false), targeted)).isEqualTo(Verdict.VALID);
    }

    @Test
    void aTargetedPlayerStillFacesTheClaimProtection() {
        TargetPolicy targeted = TargetPolicy.DEFAULT.withPlayer(ALICE, PlayerStance.TARGET);

        assertThat(verdict(player(ALICE, Relation.OTHER, false, true), targeted)).isEqualTo(Verdict.PROTECTED);
    }

    @Test
    void aTargetedPlayerStillFacesTheRange() {
        TargetPolicy targeted = TargetPolicy.DEFAULT.withPlayer(ALICE, PlayerStance.TARGET);
        Candidate far = new Candidate(TargetKind.OTHER, true, false, false, false, false, false, false, 99.0, true, null,
                Relation.OTHER, ALICE, false);

        assertThat(verdict(far, targeted)).isEqualTo(Verdict.OUT_OF_RANGE);
    }

    @Test
    void aStanceIsKeyedByThePlayersIdOnly() {
        TargetPolicy targetedAlice = TargetPolicy.DEFAULT.withPlayer(ALICE, PlayerStance.TARGET);

        assertThat(verdict(player(BOB, Relation.OTHER, false, false), targetedAlice)).isEqualTo(Verdict.PLAYER);
    }

    @Test
    void aPlayerWithNoIdCannotHaveAStance() {
        TargetPolicy targeted = TargetPolicy.DEFAULT.withPlayer(ALICE, PlayerStance.TARGET);
        Candidate unknown = new Candidate(TargetKind.OTHER, true, false, false, false, false, false, false, 5.0, true, null,
                Relation.OTHER);

        assertThat(verdict(unknown, targeted)).isEqualTo(Verdict.PLAYER);
    }

    @Test
    void aTeamMateIsSparedByDefault() {
        assertThat(TargetPolicy.DEFAULT.teamMode()).isEqualTo(TeamMode.SPARE);
        assertThat(verdict(player(ALICE, Relation.OTHER, true, false), TargetPolicy.DEFAULT)).isEqualTo(Verdict.TEAM);
    }

    @Test
    void theTeamSpareHoldsWithFriendlyFireOn() {
        assertThat(verdict(player(ALICE, Relation.OTHER, true, false), FRIENDLY_FIRE)).isEqualTo(Verdict.TEAM);
    }

    @Test
    void theTeamRuleBeatsTheFriendRuleAndFriendlyFire() {
        assertThat(verdict(player(ALICE, Relation.FRIEND, true, false), FRIENDLY_FIRE)).isEqualTo(Verdict.TEAM);
        assertThat(verdict(player(ALICE, Relation.FRIEND, true, false), TargetPolicy.DEFAULT)).isEqualTo(Verdict.TEAM);
    }

    @Test
    void aTeamMateInTheTargetModeIsAStrangerUnderFriendlyFire() {
        TargetPolicy targetMode = TargetPolicy.DEFAULT.withTeamMode(TeamMode.TARGET);

        assertThat(verdict(player(ALICE, Relation.OTHER, true, false), targetMode)).isEqualTo(Verdict.PLAYER);
        assertThat(verdict(player(ALICE, Relation.OTHER, true, false), targetMode.withFriendlyFire(true))).isEqualTo(Verdict.VALID);
    }

    @Test
    void aFriendOnTheTeamInTheTargetModeIsAStranger() {
        TargetPolicy targetMode = TargetPolicy.DEFAULT.withTeamMode(TeamMode.TARGET);

        assertThat(verdict(player(ALICE, Relation.FRIEND, true, false), targetMode)).isEqualTo(Verdict.PLAYER);
        assertThat(verdict(player(ALICE, Relation.FRIEND, true, false), targetMode.withFriendlyFire(true))).isEqualTo(Verdict.VALID);
    }

    @Test
    void theIgnoredTeamLeavesTheFriendRulesAlone() {
        TargetPolicy ignored = TargetPolicy.DEFAULT.withTeamMode(TeamMode.IGNORE);

        assertThat(verdict(player(ALICE, Relation.FRIEND, true, false), ignored)).isEqualTo(Verdict.FRIEND);
        assertThat(verdict(player(ALICE, Relation.OTHER, true, false), ignored)).isEqualTo(Verdict.PLAYER);
    }

    @Test
    void theFriendRuleSparesAFriendEvenWhileFriendlyFireIsOff() {
        assertThat(verdict(player(Relation.FRIEND), TargetPolicy.DEFAULT)).isEqualTo(Verdict.FRIEND);
    }

    @Test
    void theFriendlyFireRuleComesLastAmongThePlayerRules() {
        assertThat(verdict(player(Relation.OTHER), TargetPolicy.DEFAULT)).isEqualTo(Verdict.PLAYER);
        assertThat(verdict(player(Relation.OTHER), FRIENDLY_FIRE)).isEqualTo(Verdict.VALID);
        assertThat(verdict(player(Relation.FRIEND), FRIENDLY_FIRE)).isEqualTo(Verdict.FRIEND);
    }

    @Test
    void theTeamRuleIsForPlayersOnly() {
        Candidate teamMob = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 5.0, true, "ZOMBIE",
                Relation.NONE, null, true);

        assertThat(verdict(teamMob, TargetPolicy.DEFAULT)).isEqualTo(Verdict.VALID);
    }

    @Test
    void theMobOverridesStillApply() {
        TargetPolicy noZombies = TargetPolicy.DEFAULT.withOverride("ZOMBIE", false);
        Candidate zombie = new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, 5.0, true, "ZOMBIE",
                Relation.NONE);

        assertThat(verdict(zombie, noZombies)).isEqualTo(Verdict.NOT_A_TARGET_KIND);
    }
}
