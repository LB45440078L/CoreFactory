package io.corefactory.core.turret;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * A pure check of a volley of level 5 lasers on one Warden: the target assignment of the turrets, the cadence of their shots, and the time
 * to kill. The model follows {@code TurretService.tickOne} with the same {@link TurretAim}, {@link FireRate} and {@link LaserRules} calls,
 * so it shows what the rules give without a server.
 */
class LaserVolleyTest {

    private static final int LASERS = 10;
    private static final double WARDEN_HEALTH = 500.0;
    private static final double TARGET_YAW = 0.0;
    private static final double START_YAW = 180.0;
    private static final int LEVEL_FIVE_INTERVAL = TurretLevels.LASER.level(5).fireIntervalTicks();
    private static final double LEVEL_FIVE_HIT = TurretLevels.LASER.level(5).effectiveDamage();
    private static final int MAX_TICKS = 2000;

    @Test
    void aLevelFiveLaserHandsOverItsEighteenPerHitOnAnUnarmouredTarget() {
        assertThat(LEVEL_FIVE_HIT).isCloseTo(18.0, within(1e-9));
        assertThat(LaserRules.damageAfterArmour(LEVEL_FIVE_HIT, 0.0, 0.0, 1.0)).isCloseTo(18.0, within(1e-9));
    }

    @Test
    void tenLevelFiveLasersOnOneWardenAllHoldIt() {
        UUID warden = UUID.randomUUID();
        List<TargetAssignment.TurretSlot> slots = new ArrayList<>();
        for (int i = 0; i < LASERS; i++) {
            slots.add(new TargetAssignment.TurretSlot("laser-" + i, 1, List.of(), List.of(warden)));
        }

        Map<String, List<UUID>> answer = TargetAssignment.assign(slots);

        assertThat(answer).hasSize(LASERS);
        assertThat(answer.values()).allSatisfy(targets -> assertThat(targets).containsExactly(warden));
    }

    @Test
    void tenLevelFiveLasersAtFullPowerDealNineHundredPerSecond() {
        assertThat(damagePerSecond(LASERS, 1.0, 1200)).isCloseTo(900.0, within(1e-9));
    }

    @Test
    void halfThePowerHalvesTheDamageRate() {
        assertThat(damagePerSecond(LASERS, 0.5, 1200)).isCloseTo(450.0, within(1e-9));
    }

    @Test
    void aTwoPercentPowerShareGivesTheSeventeenPerSecondThatWasReported() {
        assertThat(damagePerSecond(LASERS, 0.02, 1200)).isBetween(15.0, 20.0);
    }

    @Test
    void aQuarterTurnTakesFifteenTicksAtSixDegreesAndEightAtTwelve() {
        assertThat(ticksToTurn(90.0, 6.0)).isEqualTo(15);
        assertThat(ticksToTurn(90.0, 12.0)).isEqualTo(8);
    }

    @Test
    void theOldRulesTakeFiftySevenTicksToKillTheWardenWithTenLasers() {
        assertThat(ticksToKill(20, 6.0)).isEqualTo(57);
    }

    @Test
    void theNewRulesTakeTwentySevenTicksToKillTheWardenWithTenLasers() {
        assertThat(ticksToKill(5, 12.0)).isEqualTo(27);
    }

    /** The damage per second of lasers that hold a target from the first tick, on a target with no armour. */
    private static double damagePerSecond(int lasers, double satisfaction, int ticks) {
        double dealt = 0.0;
        for (int laser = 0; laser < lasers; laser++) {
            int cooldown = 0;
            for (int tick = 0; tick < ticks; tick++) {
                if (cooldown > 0) {
                    cooldown--;
                }
                if (cooldown == 0) {
                    dealt += LEVEL_FIVE_HIT;
                    cooldown = FireRate.intervalTicks(LEVEL_FIVE_INTERVAL, satisfaction).getAsInt();
                }
            }
        }
        return dealt / (ticks / 20.0);
    }

    /** The ticks a turret takes to turn from one yaw to the target within the aim tolerance, at a fixed speed. */
    private static int ticksToTurn(double fromYaw, double speed) {
        double yaw = fromYaw;
        int ticks = 0;
        while (!TurretAim.aligned(yaw, 0.0, TARGET_YAW, 0.0) && ticks < MAX_TICKS) {
            yaw = TurretAim.step(yaw, TARGET_YAW, speed);
            ticks++;
        }
        return ticks;
    }

    /**
     * The tick on which ten lasers, all starting a half turn from the Warden with no target, reach its health. The coordinated pass gives
     * them the Warden on every tick that is a multiple of {@code passInterval}, and each laser turns {@code rotation} degrees a tick, then fires
     * on its cannon's interval once it is aligned.
     */
    private static int ticksToKill(int passInterval, double rotation) {
        double[] yaw = new double[LASERS];
        int[] cooldown = new int[LASERS];
        boolean[] holding = new boolean[LASERS];
        Arrays.fill(yaw, START_YAW);
        double dealt = 0.0;
        for (int tick = 1; tick <= MAX_TICKS; tick++) {
            if (tick % passInterval == 0) {
                Arrays.fill(holding, true);
            }
            for (int laser = 0; laser < LASERS; laser++) {
                if (cooldown[laser] > 0) {
                    cooldown[laser]--;
                }
            }
            for (int laser = 0; laser < LASERS; laser++) {
                if (!holding[laser]) {
                    continue;
                }
                yaw[laser] = TurretAim.step(yaw[laser], TARGET_YAW, rotation);
                if (cooldown[laser] == 0 && TurretAim.aligned(yaw[laser], 0.0, TARGET_YAW, 0.0)) {
                    cooldown[laser] = FireRate.intervalTicks(LEVEL_FIVE_INTERVAL, 1.0).getAsInt();
                    dealt += LEVEL_FIVE_HIT;
                }
            }
            if (dealt >= WARDEN_HEALTH) {
                return tick;
            }
        }
        return -1;
    }
}
