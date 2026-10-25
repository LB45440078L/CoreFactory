package io.corefactory.core.turret;

import io.corefactory.core.config.TestConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TurretAimTest {

    @Test
    void anAngleWrapsIntoPlusOrMinusOneEighty() {
        assertThat(TurretAim.wrap(190.0)).isCloseTo(-170.0, within(1e-9));
        assertThat(TurretAim.wrap(-190.0)).isCloseTo(170.0, within(1e-9));
        assertThat(TurretAim.wrap(180.0)).isCloseTo(-180.0, within(1e-9));
        assertThat(TurretAim.wrap(45.0)).isCloseTo(45.0, within(1e-9));
    }

    @Test
    void theYawOfAnOffsetFollowsMinecraftsConvention() {
        assertThat(TurretAim.yawOf(0, 1)).isCloseTo(0.0, within(1e-9));
        assertThat(TurretAim.yawOf(-1, 0)).isCloseTo(90.0, within(1e-9));
        assertThat(Math.abs(TurretAim.yawOf(0, -1))).isCloseTo(180.0, within(1e-9));
    }

    @Test
    void aPitchOfADownwardOffsetIsPositive() {
        assertThat(TurretAim.pitchOf(0, -1, 1)).isCloseTo(45.0, within(1e-9));
        assertThat(TurretAim.pitchOf(0, 1, 1)).isCloseTo(-45.0, within(1e-9));
        assertThat(TurretAim.pitchOf(3, 0, 0)).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void aDirectionIsTheUnitVectorOfItsYawAndPitch() {
        double[] south = TurretAim.direction(0, 0);
        assertThat(south[0]).isCloseTo(0.0, within(1e-9));
        assertThat(south[2]).isCloseTo(1.0, within(1e-9));

        double[] west = TurretAim.direction(90, 0);
        assertThat(west[0]).isCloseTo(-1.0, within(1e-9));
        assertThat(west[2]).isCloseTo(0.0, within(1e-9));

        double[] offset = {3.0, -4.0, 12.0};
        double[] back = TurretAim.direction(TurretAim.yawOf(offset[0], offset[2]),
                TurretAim.pitchOf(offset[0], offset[1], offset[2]));
        double length = Math.sqrt(offset[0] * offset[0] + offset[1] * offset[1] + offset[2] * offset[2]);
        assertThat(back[0]).isCloseTo(offset[0] / length, within(1e-9));
        assertThat(back[1]).isCloseTo(offset[1] / length, within(1e-9));
        assertThat(back[2]).isCloseTo(offset[2] / length, within(1e-9));
    }

    @Test
    void aYawInsideHalfTheArcOnEitherSideIsWithinTheArc() {
        assertThat(TurretAim.withinArc(80.0, 0.0, 180.0)).isTrue();
        assertThat(TurretAim.withinArc(-90.0, 0.0, 180.0)).isTrue();
        assertThat(TurretAim.withinArc(91.0, 0.0, 180.0)).isFalse();
        assertThat(TurretAim.withinArc(-95.0, 0.0, 180.0)).isFalse();
    }

    @Test
    void theArcWrapsAcrossTheSeam() {
        assertThat(TurretAim.withinArc(170.0, 175.0, 60.0)).isTrue();
        assertThat(TurretAim.withinArc(-170.0, 175.0, 60.0)).isTrue();
        assertThat(TurretAim.withinArc(-150.0, 175.0, 60.0)).isFalse();
    }

    @Test
    void aTargetAboveTheTiltLimitIsOutOfReach() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "turrets.pitch-limit-degrees", 30);
        TestConfig.set(config, "turrets.arc-degrees", 180);
        TurretSettings settings = TurretSettings.from(config);

        assertThat(TurretAim.inReach(0.0, 25.0, 0.0, settings)).isTrue();
        assertThat(TurretAim.inReach(0.0, -25.0, 0.0, settings)).isTrue();
        assertThat(TurretAim.inReach(0.0, 40.0, 0.0, settings)).isFalse();
        assertThat(TurretAim.inReach(120.0, 0.0, 0.0, settings)).isFalse();
    }

    @Test
    void theDefaultTiltReachesStraightUpAndDown() {
        TurretSettings settings = TurretSettings.defaults();

        assertThat(TurretAim.inReach(0.0, 90.0, 0.0, settings)).isTrue();
        assertThat(TurretAim.inReach(0.0, -90.0, 0.0, settings)).isTrue();
        assertThat(TurretAim.inReach(0.0, 61.0, 0.0, settings)).isTrue();
    }

    @Test
    void aStepTurnsByNoMoreThanTheMaximumAndTakesTheShorterWay() {
        assertThat(TurretAim.step(0.0, 30.0, 6.0)).isCloseTo(6.0, within(1e-9));
        assertThat(TurretAim.step(0.0, -30.0, 6.0)).isCloseTo(-6.0, within(1e-9));
        assertThat(TurretAim.step(170.0, -170.0, 6.0)).isCloseTo(176.0, within(1e-9));
    }

    @Test
    void aStepReachesTheTargetExactlyWhenItIsClose() {
        assertThat(TurretAim.step(10.0, 12.0, 6.0)).isCloseTo(12.0, within(1e-9));
    }

    @Test
    void alignmentNeedsBothAxesWithinTheTolerance() {
        assertThat(TurretAim.aligned(10.0, 5.0, 12.0, 6.0)).isTrue();
        assertThat(TurretAim.aligned(10.0, 5.0, 16.0, 5.0)).isFalse();
        assertThat(TurretAim.aligned(10.0, 5.0, 10.0, 12.0)).isFalse();
    }

    @Test
    void anIdleSweepTurnsBackAtEachEndOfTheArc() {
        TurretAim.Idle state = new TurretAim.Idle(0.0, true);
        for (int tick = 0; tick < 100 && state.forward(); tick++) {
            state = TurretAim.idle(state.yaw(), 0.0, 180.0, 6.0, state.forward());
        }
        assertThat(state.yaw()).isCloseTo(90.0, within(1e-9));
        assertThat(state.forward()).isFalse();

        for (int tick = 0; tick < 100 && !state.forward(); tick++) {
            state = TurretAim.idle(state.yaw(), 0.0, 180.0, 6.0, state.forward());
        }
        assertThat(state.yaw()).isCloseTo(-90.0, within(1e-9));
        assertThat(state.forward()).isTrue();
    }

    @Test
    void aFullTurnArcHoldsEveryYawWhateverTheFacing() {
        for (double home : new double[] {0.0, 90.0, -135.0, 179.0}) {
            for (int step = 0; step < 24; step++) {
                double yaw = step * 15.0 - 180.0;
                assertThat(TurretAim.withinArc(yaw, home, 360.0)).as("yaw %s facing %s", yaw, home).isTrue();
            }
        }
    }

    @Test
    void aHalfTurnArcStillRefusesTheBackHalf() {
        assertThat(TurretAim.withinArc(180.0, 0.0, 180.0)).isFalse();
        assertThat(TurretAim.withinArc(120.0, 0.0, 180.0)).isFalse();
    }

    @Test
    void aFullTurnIdleSweepKeepsTurningInOneDirectionAndComesRoundAgain() {
        double step = 1.5;
        TurretAim.Idle state = new TurretAim.Idle(0.0, true);
        for (int tick = 0; tick < 120; tick++) {
            state = TurretAim.idle(state.yaw(), 0.0, 360.0, step, state.forward());
        }
        assertThat(TurretAim.wrap(state.yaw())).isCloseTo(-180.0, within(1e-6));
        assertThat(state.forward()).isTrue();

        for (int tick = 0; tick < 120; tick++) {
            state = TurretAim.idle(state.yaw(), 0.0, 360.0, step, state.forward());
        }
        assertThat(TurretAim.wrap(state.yaw())).isCloseTo(0.0, within(1e-6));
        assertThat(state.forward()).isTrue();
    }

    @Test
    void theRotationSoundPlaysOnlyWhileTurningTowardADetectedTarget() {
        assertThat(TurretAim.playsRotationSound(true, true)).isTrue();
        assertThat(TurretAim.playsRotationSound(false, true)).isFalse();
        assertThat(TurretAim.playsRotationSound(true, false)).isFalse();
        assertThat(TurretAim.playsRotationSound(false, false)).isFalse();
    }

    @Test
    void aSpreadOffsetStaysWithinTheSpread() {
        assertThat(TurretAim.spreadOffset(0.5, 4.0)).isCloseTo(2.0, within(1e-9));
        assertThat(TurretAim.spreadOffset(-1.0, 4.0)).isCloseTo(-4.0, within(1e-9));
        assertThat(TurretAim.spreadOffset(3.0, 4.0)).isCloseTo(4.0, within(1e-9));
    }
}
