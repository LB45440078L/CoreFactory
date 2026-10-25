package io.corefactory.core.railgun;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The cycle of a rail gun on the server clock: the charge, the reload, the light, a lost target and a switched-off rail gun. */
class RailgunCycleTest {

    private static final RailgunSettings SETTINGS = RailgunSettings.defaults();

    /** Level I charges for 6 s (120 ticks) and reloads for 10 s (200 ticks) in the defaults. */
    private static final int CHARGE_TICKS = 120;
    private static final int RELOAD_TICKS = 200;

    @Test
    void aChargedShotFiresAfterTheLevelsChargeTime() {
        RailgunCycle cycle = new RailgunCycle();

        int fired = 0;
        for (int tick = 1; tick <= 200 && fired == 0; tick++) {
            if (cycle.tick(SETTINGS, RailgunLevel.I, true, true, true, 1.0).fired()) {
                fired = tick;
            }
        }

        assertThat(fired).isEqualTo(CHARGE_TICKS);
    }

    @Test
    void theNextChargeStartsAfterTheReload() {
        RailgunCycle cycle = new RailgunCycle();
        int first = 0;
        for (int tick = 1; first == 0; tick++) {
            if (cycle.tick(SETTINGS, RailgunLevel.I, true, true, true, 1.0).fired()) {
                first = tick;
            }
        }

        int second = 0;
        RailgunCycle.Phase beforeReloadEnds = null;
        for (int tick = 1; second == 0 && tick < 1000; tick++) {
            RailgunCycle.Step step = cycle.tick(SETTINGS, RailgunLevel.I, true, true, true, 1.0);
            if (tick == RELOAD_TICKS - 1) {
                beforeReloadEnds = step.phase();
            }
            if (step.fired()) {
                second = tick;
            }
        }

        assertThat(first).isEqualTo(CHARGE_TICKS);
        assertThat(beforeReloadEnds).isEqualTo(RailgunCycle.Phase.RELOADING);
        assertThat(second).isEqualTo(RELOAD_TICKS + CHARGE_TICKS);
    }

    @Test
    void theLightRampsFromZeroToTheMaximumAsTheChargeCompletes() {
        RailgunCycle cycle = new RailgunCycle();

        RailgunCycle.Step firstTick = cycle.tick(SETTINGS, RailgunLevel.I, true, true, false, 1.0);
        assertThat(firstTick.light()).isZero();

        int lightAtHalf = 0;
        for (int tick = 2; tick <= 60; tick++) {
            lightAtHalf = cycle.tick(SETTINGS, RailgunLevel.I, true, true, false, 1.0).light();
        }
        assertThat(lightAtHalf).isEqualTo(7);

        RailgunCycle.Step ready = null;
        for (int tick = 61; tick <= CHARGE_TICKS; tick++) {
            ready = cycle.tick(SETTINGS, RailgunLevel.I, true, true, false, 1.0);
        }
        assertThat(ready.light()).isEqualTo(15);
        assertThat(ready.charge()).isEqualTo(1.0);
        assertThat(ready.fired()).isFalse();
    }

    @Test
    void theLightIsOutWhenTheRailGunFiresAndWhileItReloads() {
        RailgunCycle cycle = new RailgunCycle();
        RailgunCycle.Step last = null;
        for (int tick = 1; tick <= CHARGE_TICKS; tick++) {
            last = cycle.tick(SETTINGS, RailgunLevel.I, true, true, true, 1.0);
        }

        assertThat(last.fired()).isTrue();
        assertThat(last.light()).isZero();
        assertThat(last.phase()).isEqualTo(RailgunCycle.Phase.RELOADING);
        for (int tick = 0; tick < 50; tick++) {
            assertThat(cycle.tick(SETTINGS, RailgunLevel.I, true, true, true, 1.0).light()).isZero();
        }
    }

    @Test
    void aLostTargetDropsTheChargeAndTheLightAtOnce() {
        RailgunCycle cycle = new RailgunCycle();
        for (int tick = 0; tick < 60; tick++) {
            cycle.tick(SETTINGS, RailgunLevel.I, true, true, false, 1.0);
        }

        RailgunCycle.Step lost = cycle.tick(SETTINGS, RailgunLevel.I, true, false, false, 1.0);

        assertThat(lost.phase()).isEqualTo(RailgunCycle.Phase.IDLE);
        assertThat(lost.light()).isZero();
        assertThat(lost.fired()).isFalse();

        RailgunCycle.Step again = cycle.tick(SETTINGS, RailgunLevel.I, true, true, false, 1.0);
        assertThat(again.phase()).isEqualTo(RailgunCycle.Phase.CHARGING);
        assertThat(again.light()).isZero();
    }

    @Test
    void aSwitchedOffRailGunStaysIdleAndNeverFires() {
        RailgunCycle cycle = new RailgunCycle();

        for (int tick = 0; tick < 400; tick++) {
            RailgunCycle.Step step = cycle.tick(SETTINGS, RailgunLevel.I, false, true, true, 1.0);
            assertThat(step.phase()).isEqualTo(RailgunCycle.Phase.IDLE);
            assertThat(step.light()).isZero();
            assertThat(step.fired()).isFalse();
        }
    }

    @Test
    void switchingOffDuringACharge() {
        RailgunCycle cycle = new RailgunCycle();
        for (int tick = 0; tick < 60; tick++) {
            cycle.tick(SETTINGS, RailgunLevel.I, true, true, false, 1.0);
        }

        RailgunCycle.Step off = cycle.tick(SETTINGS, RailgunLevel.I, false, true, false, 1.0);

        assertThat(off.phase()).isEqualTo(RailgunCycle.Phase.IDLE);
        assertThat(off.light()).isZero();
    }

    @Test
    void aReloadRunsOnWhileTheRailGunIsSwitchedOff_soItCannotSkipIt() {
        RailgunCycle cycle = new RailgunCycle();
        for (int tick = 0; tick < CHARGE_TICKS; tick++) {
            cycle.tick(SETTINGS, RailgunLevel.I, true, true, true, 1.0);
        }
        for (int tick = 0; tick < RELOAD_TICKS; tick++) {
            cycle.tick(SETTINGS, RailgunLevel.I, false, true, true, 1.0);
        }

        assertThat(cycle.phase()).isEqualTo(RailgunCycle.Phase.IDLE);
        RailgunCycle.Step on = cycle.tick(SETTINGS, RailgunLevel.I, true, true, true, 1.0);
        assertThat(on.phase()).isEqualTo(RailgunCycle.Phase.CHARGING);
        assertThat(on.light()).isZero();
    }

    @Test
    void aShortOfPowerChargeTakesLongerAtTheShareOfPowerItGets() {
        RailgunCycle cycle = new RailgunCycle();

        int fired = 0;
        for (int tick = 1; tick <= 400 && fired == 0; tick++) {
            if (cycle.tick(SETTINGS, RailgunLevel.I, true, true, true, 0.5).fired()) {
                fired = tick;
            }
        }

        assertThat(fired).isEqualTo(CHARGE_TICKS * 2);
    }

    @Test
    void levelThreeChargesAndReloadsForItsOwnTimes() {
        RailgunCycle cycle = new RailgunCycle();

        int fired = 0;
        for (int tick = 1; tick <= 200 && fired == 0; tick++) {
            if (cycle.tick(SETTINGS, RailgunLevel.III, true, true, true, 1.0).fired()) {
                fired = tick;
            }
        }

        assertThat(fired).isEqualTo(80);
    }

    @Test
    void aChargingRailGunDrawsItsPowerAndAReloadingOneDoesNot() {
        RailgunCycle cycle = new RailgunCycle();
        assertThat(cycle.charging()).isFalse();

        cycle.tick(SETTINGS, RailgunLevel.I, true, true, false, 1.0);
        assertThat(cycle.charging()).isTrue();

        for (int tick = 0; tick < CHARGE_TICKS; tick++) {
            cycle.tick(SETTINGS, RailgunLevel.I, true, true, true, 1.0);
        }
        assertThat(cycle.charging()).isFalse();
        assertThat(cycle.phase()).isEqualTo(RailgunCycle.Phase.RELOADING);
    }
}
