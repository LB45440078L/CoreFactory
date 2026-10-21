package io.corefactory.spigot.energy;

import io.corefactory.core.energy.OutlineTracer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EnergyGeometryTest {

    private static boolean has(List<double[]> points, double x, double z) {
        return points.stream().anyMatch(p -> Math.abs(p[0] - x) < 1e-9 && Math.abs(p[2] - z) < 1e-9);
    }

    @Test
    void aSquareOfThreeBlocksIsOneLoopOfFourCorners() {
        List<List<OutlineTracer.Corner>> loops = OutlineTracer.trace(OutlineTracer.square(0, 0, 1));
        assertThat(loops).hasSize(1);
        assertThat(loops.get(0)).hasSize(4);
    }

    @Test
    void theLoopsOfASquareHalfABlockApartGiveTwentyFourPointsAtTheHeight() {
        List<double[]> points = EnergyGeometry.loops(OutlineTracer.trace(OutlineTracer.square(0, 0, 1)), 64.5, 0.5);
        assertThat(points).hasSize(4 * 3 * 2);
        assertThat(points).allSatisfy(p -> assertThat(p[1]).isEqualTo(64.5));
        assertThat(has(points, -1, -1)).isTrue();
        assertThat(has(points, 2, 2)).isTrue();
        assertThat(has(points, 0, 0)).isFalse();
    }

    @Test
    void noPointOfTheLoopsIsListedTwice() {
        List<double[]> points = EnergyGeometry.loops(OutlineTracer.trace(OutlineTracer.square(0, 0, 1)), 64.5, 0.5);
        long distinct = points.stream().map(p -> p[0] + ":" + p[2]).distinct().count();
        assertThat(distinct).isEqualTo(points.size());
    }

    @Test
    void aLineHasAPointEveryHalfBlockFromItsStartToItsEnd() {
        double[] a = {0, 0, 0};
        double[] b = {3, 0, 4};
        List<double[]> points = EnergyGeometry.line(a, b, 0.5);
        assertThat(points).hasSize(11);
        assertThat(points.get(0)).containsExactly(0, 0, 0);
        double[] last = points.get(points.size() - 1);
        assertThat(last[0]).isCloseTo(3, within(1e-9));
        assertThat(last[2]).isCloseTo(4, within(1e-9));
    }

    @Test
    void aZeroLengthLineIsASinglePoint() {
        assertThat(EnergyGeometry.line(new double[] {1, 2, 3}, new double[] {1, 2, 3}, 0.5)).hasSize(1);
    }

    @Test
    void aRingHasItsPointsOnTheCircle() {
        List<double[]> ring = EnergyGeometry.ring(0, 0, 0, 1.0, 4);
        assertThat(ring).hasSize(4);
        assertThat(ring.get(0)[0]).isCloseTo(1.0, within(1e-9));
        assertThat(ring.get(1)[2]).isCloseTo(1.0, within(1e-9));
        assertThat(ring.get(2)[0]).isCloseTo(-1.0, within(1e-9));
    }
}
