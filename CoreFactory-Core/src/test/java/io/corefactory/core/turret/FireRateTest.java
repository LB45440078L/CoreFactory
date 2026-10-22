package io.corefactory.core.turret;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class FireRateTest {

    @Test
    void aFullPowerTurretFiresAtItsConfiguredInterval() {
        assertThat(FireRate.intervalTicks(10, 1.0)).isEqualTo(OptionalInt.of(10));
    }

    @Test
    void aHalfPowerTurretFiresHalfAsOften() {
        assertThat(FireRate.intervalTicks(10, 0.5)).isEqualTo(OptionalInt.of(20));
    }

    @Test
    void theIntervalRoundsUp() {
        assertThat(FireRate.intervalTicks(10, 0.3)).isEqualTo(OptionalInt.of(34));
    }

    @Test
    void aTurretWithNoPowerDoesNotFire() {
        assertThat(FireRate.intervalTicks(10, 0.0)).isEmpty();
        assertThat(FireRate.intervalTicks(10, -1.0)).isEmpty();
    }

    @Test
    void aSatisfactionAboveOneIsTreatedAsFullPower() {
        assertThat(FireRate.intervalTicks(10, 3.0)).isEqualTo(OptionalInt.of(10));
    }

    @Test
    void aFullPowerTurretOneUlpBelowOneStillFiresAtItsConfiguredInterval() {
        // Four divided by (1 - 2^-52) is just above 4, so without the rounding slack a laser of interval 4 would fire every 5 ticks.
        assertThat(FireRate.intervalTicks(4, 1.0 - Math.ulp(1.0))).isEqualTo(OptionalInt.of(4));
    }

    @Test
    void aShareJustBelowFullPowerStepsTheIntervalUpAWholeTick() {
        // A level 5 laser at 90 per cent of its power fires every 5 ticks: the interval is rounded up, so the rate steps down in whole ticks.
        assertThat(FireRate.intervalTicks(4, 0.9)).isEqualTo(OptionalInt.of(5));
        assertThat(FireRate.intervalTicks(4, 0.8)).isEqualTo(OptionalInt.of(5));
        assertThat(FireRate.intervalTicks(4, 0.79)).isEqualTo(OptionalInt.of(6));
    }

    @Test
    void aLevelFiveLaserAtAQuarterOfItsPowerFiresEverySixteenTicks() {
        assertThat(FireRate.intervalTicks(4, 0.25)).isEqualTo(OptionalInt.of(16));
    }
}
