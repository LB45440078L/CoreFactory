package io.corefactory.core.artillery;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArtilleryDispersionTest {

    private static final int SAMPLES = 20_000;
    private final ArtilleryDispersion dispersion =
            new ArtilleryDispersion(new ArtilleryTable(ArtillerySettings.defaults()));
    private final GroundPoint aim = new GroundPoint(1000.0, 2000.0);

    @Test
    void theMeanOfManyLandingsIsTheAimPoint() {
        Random random = new Random(42L);
        double sumX = 0;
        double sumZ = 0;
        for (int i = 0; i < SAMPLES; i++) {
            GroundPoint p = dispersion.landing(aim, 1000.0, ArtilleryLevel.I, ChargeTier.TIER_I, random);
            sumX += p.x();
            sumZ += p.z();
        }
        // Sigma is 60 blocks, so the standard error of a mean of 20000 is 60 / 141 = 0.42; 3 blocks is about 7 standard errors.
        assertThat(sumX / SAMPLES).isCloseTo(1000.0, within(3.0));
        assertThat(sumZ / SAMPLES).isCloseTo(2000.0, within(3.0));
    }

    @Test
    void theSampleSigmaMatchesTheModelWithinFivePerCent() {
        Random random = new Random(7L);
        double[] xs = new double[SAMPLES];
        double[] zs = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            GroundPoint p = dispersion.landing(aim, 1000.0, ArtilleryLevel.I, ChargeTier.TIER_I, random);
            xs[i] = p.x() - aim.x();
            zs[i] = p.z() - aim.z();
        }
        // The model: 60 blocks at 1000 blocks of range, so 60 blocks on each axis, and 5 per cent of it is 3 blocks.
        assertThat(sampleSigma(xs)).isCloseTo(60.0, within(3.0));
        assertThat(sampleSigma(zs)).isCloseTo(60.0, within(3.0));
    }

    @Test
    void aHigherLevelLandsCloserToItsAim() {
        Random low = new Random(11L);
        Random high = new Random(11L);
        double[] levelOne = new double[SAMPLES];
        double[] levelSeven = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            levelOne[i] = dispersion.landing(aim, 1000.0, ArtilleryLevel.I, ChargeTier.TIER_I, low).x() - aim.x();
            levelSeven[i] = dispersion.landing(aim, 1000.0, ArtilleryLevel.VII, ChargeTier.TIER_I, high).x() - aim.x();
        }
        assertThat(sampleSigma(levelSeven)).isLessThan(sampleSigma(levelOne) / 5.0);
        // Level VII at 1000 blocks: 8 blocks, so 3 blocks of tolerance is more than a third of it.
        assertThat(sampleSigma(levelSeven)).isCloseTo(8.0, within(0.4));
    }

    @Test
    void theSigmaOfAShotScalesWithItsRange() {
        // Level I at 2000 blocks: 60 x 2000 / 1000 = 120 blocks.
        assertThat(dispersion.sigmaBlocks(ArtilleryLevel.I, ChargeTier.TIER_I, 2000.0)).isCloseTo(120.0, within(1e-9));
    }

    @Test
    void aSeededRandomGivesTheSameLanding() {
        GroundPoint first = dispersion.landing(aim, 500.0, ArtilleryLevel.III, ChargeTier.TIER_II, new Random(3L));
        GroundPoint second = dispersion.landing(aim, 500.0, ArtilleryLevel.III, ChargeTier.TIER_II, new Random(3L));
        assertThat(first).isEqualTo(second);
    }

    private static double sampleSigma(double[] values) {
        double mean = 0;
        for (double v : values) {
            mean += v;
        }
        mean /= values.length;
        double sum = 0;
        for (double v : values) {
            sum += (v - mean) * (v - mean);
        }
        return Math.sqrt(sum / (values.length - 1));
    }
}
