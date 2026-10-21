package io.corefactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SkyTest {

    private static final long SEED = 12345L;

    @Test
    void theDayTimeFoldsIntoOneDay() {
        assertThat(Sky.dayTicks(24_000)).isZero();
        assertThat(Sky.dayTicks(-1)).isEqualTo(23_999);
        assertThat(Sky.dayTicks(30_000)).isEqualTo(6_000);
    }

    @Test
    void theSunIsHighestAtNoonAndAbsentAtNight() {
        assertThat(Sky.sunSine(6_000)).isCloseTo(1.0, within(1e-12));
        assertThat(Sky.sunSine(13_000)).isZero();
        assertThat(Sky.sunSine(18_000)).isZero();
        assertThat(Sky.sunSine(22_999)).isZero();
        assertThat(Sky.sunSine(23_000)).isZero();
    }

    @Test
    void theSunIsSymmetricAroundNoonAndGivesTheSineOfItsElevation() {
        assertThat(Sky.sunSine(7_000)).isCloseTo(Sky.sunSine(5_000), within(1e-12));
        assertThat(Sky.sunSine(9_500)).isCloseTo(Math.cos(Math.PI / 4), within(1e-9));
    }

    @Test
    void theIrradianceIsAThousandWattsAtNoonAndZeroAtNight() {
        assertThat(Sky.irradianceWPerM2(6_000)).isCloseTo(1_000.0, within(1e-9));
        assertThat(Sky.irradianceWPerM2(15_000)).isZero();
        assertThat(Sky.irradianceWPerM2(6_000 + 24_000)).isCloseTo(1_000.0, within(1e-9));
    }

    @Test
    void aPanelGivesThreeHundredTwentyWattsAtFullIrradianceAndNothingAtNight() {
        assertThat(Sky.solarOutputW(1_000)).isCloseTo(320.0, within(1e-9));
        assertThat(Sky.solarOutputW(0)).isZero();
        assertThat(Sky.solarOutputW(2_000)).isCloseTo(320.0, within(1e-9));
        assertThat(Sky.solarOutputW(-5)).isZero();
    }

    @Test
    void theWindFactorStaysBetweenZeroAndOne() {
        for (long tick = 0; tick < 48_000; tick += 37) {
            double factor = Sky.windFactor(tick, SEED);
            assertThat(factor).isBetween(0.0, 1.0);
        }
    }

    @Test
    void theWindSpeedStaysBetweenThreeAndTwelveMetresPerSecond() {
        double lowest = Double.MAX_VALUE;
        double highest = 0.0;
        for (long tick = 0; tick < 48_000; tick += 37) {
            double speed = Sky.windSpeedMs(tick, SEED);
            assertThat(speed).isBetween(3.0, 12.0);
            lowest = Math.min(lowest, speed);
            highest = Math.max(highest, speed);
        }
        assertThat(highest - lowest).isGreaterThan(1.0);
    }

    @Test
    void theWindIsReplayedBySeedAndTick() {
        assertThat(Sky.windSpeedMs(1_234, SEED)).isEqualTo(Sky.windSpeedMs(1_234, SEED));
        assertThat(Sky.windSpeedMs(1_234, SEED + 1)).isNotEqualTo(Sky.windSpeedMs(1_234, SEED));
    }

    @Test
    void theWindPowerFollowsTheRotorFormula() {
        assertThat(Sky.windPowerW(3.0)).isCloseTo(72.73, within(0.01));
    }

    @Test
    void theWindPowerIsCappedAtTheRatedOutput() {
        assertThat(Sky.windPowerW(12.0)).isEqualTo(3_000.0);
        assertThat(Sky.windPowerW(25.0)).isEqualTo(3_000.0);
    }

    @Test
    void theWindGivesNothingBelowTheCutInSpeed() {
        assertThat(Sky.windPowerW(2.999)).isZero();
        assertThat(Sky.windPowerW(0.0)).isZero();
    }

    @Test
    void theWindGivesNothingAboveTheCutOutSpeed() {
        assertThat(Sky.windPowerW(25.001)).isZero();
        assertThat(Sky.windPowerW(40.0)).isZero();
    }

    @Test
    void aClosedSiteGivesNoWindAndAnOpenOneGivesTheWindPower() {
        long tick = 6_000;
        assertThat(Sky.windOutputW(new OpenAir(5, 100), tick, SEED)).isZero();
        assertThat(Sky.windOutputW(new OpenAir(6, 95), tick, SEED)).isZero();
        assertThat(Sky.windOutputW(new OpenAir(6, 96), tick, SEED))
                .isEqualTo(Sky.windPowerW(Sky.windSpeedMs(tick, SEED)));
    }

    @Test
    void noiseIsAUnitValueAndIsEqualForEqualInputs() {
        for (long cell = 0; cell < 100; cell++) {
            double noise = Sky.noise(SEED, cell);
            assertThat(noise).isBetween(0.0, 1.0);
            assertThat(noise).isEqualTo(Sky.noise(SEED, cell));
        }
    }
}
