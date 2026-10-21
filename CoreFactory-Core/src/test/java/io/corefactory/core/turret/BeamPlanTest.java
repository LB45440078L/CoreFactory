package io.corefactory.core.turret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BeamPlanTest {

    @Test
    void aShortBeamDrawsAboutOneParticleEverySixTenthsOfABlock() {
        BeamPlan beam = BeamPlan.of(3.0, 24);

        assertThat(beam.particles()).isEqualTo(5);
        assertThat(beam.length()).isEqualTo(3.0);
    }

    @Test
    void aLongBeamIsCappedAndItsCapSpreadsAlongTheWholeLength() {
        BeamPlan beam = BeamPlan.of(40.0, 24);

        assertThat(beam.particles()).isEqualTo(24);
        assertThat(beam.distanceOf(0)).isCloseTo(40.0 * 0.5 / 24, within(1e-9));
        assertThat(beam.distanceOf(23)).isCloseTo(40.0 * 23.5 / 24, within(1e-9));
        assertThat(beam.distanceOf(23)).isLessThan(40.0);
    }

    @Test
    void theParticleDistancesIncreaseAndStayInsideTheBeam() {
        BeamPlan beam = BeamPlan.of(20.0, 32);

        double previous = 0.0;
        for (int index = 0; index < beam.particles(); index++) {
            double distance = beam.distanceOf(index);
            assertThat(distance).isGreaterThan(previous).isLessThan(beam.length());
            previous = distance;
        }
    }

    @Test
    void theCapIsClampedToTheMaximumOfTheBeam() {
        assertThat(BeamPlan.of(40.0, 1000).particles()).isEqualTo(BeamPlan.MAX_BEAM_PARTICLES);
        assertThat(BeamPlan.of(40.0, -4).particles()).isZero();
    }

    @Test
    void aShotWithAHitStaysUnderTheParticleLimit() {
        BeamPlan beam = BeamPlan.of(40.0, 1000);

        assertThat(beam.totalParticles(true)).isEqualTo(BeamPlan.MAX_BEAM_PARTICLES + BeamPlan.IMPACT_PARTICLES);
        assertThat(beam.totalParticles(true)).isLessThan(BeamPlan.MAX_PARTICLES_PER_SHOT);
        assertThat(beam.totalParticles(false)).isEqualTo(BeamPlan.MAX_BEAM_PARTICLES);
    }

    @Test
    void theDefaultCapAndAHitStayUnderTheLimit() {
        BeamPlan beam = BeamPlan.of(40.0, 24);

        assertThat(beam.totalParticles(true)).isEqualTo(30).isLessThan(BeamPlan.MAX_PARTICLES_PER_SHOT);
    }

    @Test
    void aBeamWithNoLengthOrNoCapDrawsNothing() {
        assertThat(BeamPlan.of(0.0, 24).particles()).isZero();
        assertThat(BeamPlan.of(Double.NaN, 24).length()).isZero();
        assertThat(BeamPlan.of(10.0, 0).particles()).isZero();
        assertThat(BeamPlan.of(0.0, 24).distanceOf(0)).isZero();
    }
}
