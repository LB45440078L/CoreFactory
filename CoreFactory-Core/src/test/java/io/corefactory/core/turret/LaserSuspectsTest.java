package io.corefactory.core.turret;

import io.corefactory.core.turret.TargetAssignment.TurretSlot;
import io.corefactory.core.turret.TargetRules.Candidate;
import io.corefactory.core.turret.TargetRules.Relation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * One test per suspect in the damage path of two level V lasers on a Warden. Each shows the size of its effect, in damage per second or in
 * ticks, against the expected 180 per second of {@link LaserDamageAccountingTest}. The Warden is 0.9 blocks wide, so its half width is 0.45
 * (vanilla's hitbox, assumed here), and it has no armour (assumed; the repository does not state it).
 */
class LaserSuspectsTest {

    private static final UUID ZOMBIE = new UUID(0, 1);
    private static final UUID OTHER_ZOMBIE = new UUID(0, 2);
    private static final UUID WARDEN = new UUID(0, 3);
    private static final double WARDEN_HALF_WIDTH = 0.45;
    private static final double RANGE_BLOCKS = 20.0;
    private static final double LEVEL_FIVE_TURN_DEGREES = TurretLevels.LASER.level(5).rotationDegreesOr(6.0);

    // (a) The cooldown. The cooldown counts down every tick and is set only when a cannon fires, so a dropped and re-found target does not
    // reset it: the cannon loses the ticks it has no target, and nothing more.
    @Test
    void aTargetGapCostsOnlyTheTicksWithoutATargetAndTheCooldownKeepsRunning() {
        List<Integer> withGap = LaserCannonModel.firingTicks(4, 1.0, 20, tick -> tick < 6 || tick > 10);

        // The full schedule is 1, 5, 9, 13, 17. The gap on ticks 6 to 10 removes the shot due on 9, and the cannon then fires on 11, 15, 19.
        assertThat(withGap).containsExactly(1, 5, 11, 15, 19);
        assertThat(withGap.size()).isEqualTo(5);
    }

    // (b) The coordinated assignment. A second turret takes the nearest mob that no turret holds, so with a zombie nearer than the Warden the
    // Warden gets one laser, not two.
    @Test
    void aZombieNearerThanTheWardenTakesTheSecondLaserSoTheWardenGetsOneLaser() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                new TurretSlot("a", 1, List.of(), List.of(ZOMBIE, WARDEN)),
                new TurretSlot("b", 1, List.of(), List.of(ZOMBIE, WARDEN))));

        assertThat(answer.get("a")).containsExactly(ZOMBIE);
        assertThat(answer.get("b")).containsExactly(WARDEN);
        double wardenPerSecond = LaserCannonModel.damagePerSecond(4, 1.0, 4, 18.0);
        assertThat(wardenPerSecond).isEqualTo(90.0);
        assertThat(500.0 / wardenPerSecond).isCloseTo(5.56, within(0.01));
    }

    @Test
    void aWardenBehindTwoZombiesGetsNoLaserAtAll() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                new TurretSlot("a", 1, List.of(), List.of(ZOMBIE, OTHER_ZOMBIE, WARDEN)),
                new TurretSlot("b", 1, List.of(), List.of(ZOMBIE, OTHER_ZOMBIE, WARDEN))));

        assertThat(answer.get("a")).containsExactly(ZOMBIE);
        assertThat(answer.get("b")).containsExactly(OTHER_ZOMBIE);
        assertThat(answer.values().stream().flatMap(List::stream)).doesNotContain(WARDEN);
    }

    // (c) The aim point and tolerance. The tolerance is 3 degrees, but the Warden is only 0.45 blocks either side of its centre, which at 20
    // blocks is about 1.3 degrees. A barrel 2 degrees off counts as aligned and still misses.
    @Test
    void aTwoDegreeErrorIsAlignedButTheBeamMissesAWardenTwentyBlocksAway() {
        double[] direction = TurretAim.direction(2.0, 0.0);
        double lateral = Math.abs(direction[0] / direction[2]) * RANGE_BLOCKS;

        assertThat(TurretAim.aligned(2.0, 0.0, 0.0, 0.0)).isTrue();
        assertThat(lateral).isGreaterThan(WARDEN_HALF_WIDTH);
        assertThat(lateral).isCloseTo(0.699, within(0.001));
    }

    @Test
    void aOneDegreeErrorStillHitsAWardenTwentyBlocksAway() {
        double[] direction = TurretAim.direction(1.0, 0.0);
        double lateral = Math.abs(direction[0] / direction[2]) * RANGE_BLOCKS;

        assertThat(lateral).isLessThan(WARDEN_HALF_WIDTH);
    }

    @Test
    void aWardenStandingStillIsReachedExactlyWhereTheBarrelConverges() {
        double yaw = 0.0;
        yaw = TurretAim.step(yaw, 25.0, LEVEL_FIVE_TURN_DEGREES);
        yaw = TurretAim.step(yaw, 25.0, LEVEL_FIVE_TURN_DEGREES);
        yaw = TurretAim.step(yaw, 25.0, LEVEL_FIVE_TURN_DEGREES);

        assertThat(yaw).isEqualTo(25.0);
        assertThat(TurretAim.aligned(yaw, 0.0, 25.0, 0.0)).isTrue();
    }

    @Test
    void aWardenWalkingAtItsFullSpeedStaysInAimOnEveryTick() {
        double yaw = 0.0;
        for (int tick = 1; tick <= 60; tick++) {
            double targetYaw = TurretAim.yawOf(0.3 * tick, RANGE_BLOCKS);
            yaw = TurretAim.step(yaw, targetYaw, LEVEL_FIVE_TURN_DEGREES);
            assertThat(TurretAim.aligned(yaw, 0.0, targetYaw, 0.0)).as("tick %d", tick).isTrue();
        }
    }

    // (d) The line of sight. A block on the line gives BLOCKED, the turret does not fire, and the target is dropped. That is the rule, not
    // a lost shot to a Warden in the open.
    @Test
    void aWardenBehindABlockIsBlockedAndAWardenInTheOpenIsValid() {
        TurretSettings settings = TurretSettings.defaults();
        Candidate blocked = warden(false);
        Candidate open = warden(true);

        assertThat(TargetRules.verdict(blocked, settings, TargetPolicy.DEFAULT)).isEqualTo(TargetRules.Verdict.BLOCKED);
        assertThat(TargetRules.verdict(open, settings, TargetPolicy.DEFAULT)).isEqualTo(TargetRules.Verdict.VALID);
    }

    private static Candidate warden(boolean clearShot) {
        return new Candidate(TargetKind.MONSTER, false, false, false, false, false, false, false, RANGE_BLOCKS, true,
                "WARDEN", Relation.NONE, null, false, clearShot);
    }

    // (e) Armour. A Warden with no armour takes the full 18. With the laser's bypass at 1 (the shipped default) the armour takes nothing, so
    // any armour leaves the full 18. With the bypass at 0 the armour cuts the hit by the game's formula.
    @Test
    void aWardenWithNoArmourTakesTheFullEighteen() {
        assertThat(LaserRules.damageAfterArmour(18.0, 0.0, 0.0, 1.0)).isEqualTo(18.0);
    }

    @Test
    void withTheShippedBypassArmourTwentyStillLeavesEighteen() {
        assertThat(LaserRules.damageAfterArmour(18.0, 20.0, 0.0, 1.0)).isEqualTo(18.0);
    }

    @Test
    void withoutTheBypassArmourTwentyLeavesTenPointOhEight() {
        assertThat(LaserRules.damageAfterArmour(18.0, 20.0, 0.0, 0.0)).isCloseTo(10.08, within(1e-9));
    }

    // (f) The invulnerability window. Vanilla refuses a hit no stronger than the last while the window is above 10 ticks, and a hit sets
    // the window to 20. The turret removes its hits from health with no window at all (TurretService.strike), so every shot lands. This
    // model is of vanilla as it is generally documented, not a test against a server.
    @Test
    void withTheWindowClearedEveryShotLandsAndTheWardenTakesNinetyPerSecond() {
        int accepted = acceptedHitsInFourSeconds(true);

        assertThat(accepted).isEqualTo(20);
        assertThat(accepted * 18.0 / 4.0).isEqualTo(90.0);
    }

    @Test
    void withoutClearingTheWindowOnlyOneShotInThreeLands() {
        int accepted = acceptedHitsInFourSeconds(false);

        assertThat(accepted).isEqualTo(7);
        assertThat(accepted * 18.0 / 4.0).isCloseTo(31.5, within(1e-9));
    }

    /** The shots of one level V laser over four seconds that a mob takes, with the window cleared before each hit or not. */
    private static int acceptedHitsInFourSeconds(boolean windowCleared) {
        List<Integer> shots = LaserCannonModel.firingTicks(4, 1.0, 80);
        int invulnerable = 0;
        int accepted = 0;
        for (int tick = 1; tick <= 80; tick++) {
            if (invulnerable > 0) {
                invulnerable--;
            }
            if (shots.contains(tick)) {
                if (windowCleared) {
                    invulnerable = 0;
                }
                if (invulnerable <= 10) {
                    accepted++;
                    invulnerable = 20;
                }
            }
        }
        return accepted;
    }
}
