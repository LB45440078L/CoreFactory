package io.corefactory.core.railgun;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RailgunHelixTest {

    private static final double RADIUS = 0.3;

    @Test
    void anOffsetIsAsLongAsTheRadiusAndLiesAcrossThePath() {
        double[][] tangents = {{1, 0, 0}, {0, 0, 1}, {0.6, 0.0, 0.8}, {0, 1, 0}, {0, -1, 0}, {0.36, 0.48, -0.8}};
        double[] out = new double[3];

        for (double[] t : tangents) {
            for (int k = 0; k < 16; k++) {
                RailgunHelix.offset(t[0], t[1], t[2], k * Math.PI / 8, RADIUS, out);

                assertThat(Math.sqrt(out[0] * out[0] + out[1] * out[1] + out[2] * out[2])).isCloseTo(RADIUS, within(1e-9));
                assertThat(out[0] * t[0] + out[1] * t[1] + out[2] * t[2]).isCloseTo(0.0, within(1e-9));
            }
        }
    }

    @Test
    void fourStrandsAQuarterTurnApartAreAtRightAnglesAroundThePath() {
        double[] a = new double[3];
        double[] b = new double[3];
        RailgunHelix.offset(0, 0, 1, 0, RADIUS, a);
        RailgunHelix.offset(0, 0, 1, Math.PI / 2, RADIUS, b);

        assertThat(a[0] * b[0] + a[1] * b[1] + a[2] * b[2]).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void theOppositeStrandsAreTheDiameterApart() {
        double[] a = new double[3];
        double[] b = new double[3];
        RailgunHelix.offset(0, 0, 1, 0.7, RADIUS, a);
        RailgunHelix.offset(0, 0, 1, 0.7 + Math.PI, RADIUS, b);

        double across = Math.sqrt(Math.pow(a[0] - b[0], 2) + Math.pow(a[1] - b[1], 2) + Math.pow(a[2] - b[2], 2));
        assertThat(across).isCloseTo(2 * RADIUS, within(1e-9));
    }

    @Test
    void aPathAlongTheZAxisHasItsOffsetsInTheXYPlane() {
        double[] out = new double[3];

        RailgunHelix.offset(0, 0, 1, 0, RADIUS, out);

        assertThat(out[2]).isCloseTo(0.0, within(1e-9));
        assertThat(Math.abs(out[0])).isCloseTo(RADIUS, within(1e-9));
    }

    @Test
    void aPathThatRunsStraightUpHasAFrameToo() {
        double[] out = new double[3];

        RailgunHelix.offset(0, 1, 0, Math.PI / 2, RADIUS, out);

        assertThat(out[1]).isCloseTo(0.0, within(1e-9));
        assertThat(Math.sqrt(out[0] * out[0] + out[2] * out[2])).isCloseTo(RADIUS, within(1e-9));
    }

    @Test
    void thePhaseIsAFullTurnForEachTurnOfTheHelix() {
        assertThat(RailgunHelix.phase(2.0, 0.5)).isCloseTo(2 * Math.PI, within(1e-9));
        assertThat(RailgunHelix.phase(0.0, 0.5)).isZero();
        assertThat(RailgunHelix.phase(1.0, 1.0)).isCloseTo(2 * Math.PI, within(1e-9));
    }
}
