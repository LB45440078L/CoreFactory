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
}
