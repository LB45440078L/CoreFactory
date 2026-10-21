package io.corefactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class WirePathTest {

    private static final double[] A = {0, 64, 0};
    private static final double[] B = {24, 64, 0};

    @Test
    void theSagIsZeroAtBothEnds() {
        double sag = WirePath.sag(24);
        assertThat(WirePath.drop(0, sag)).isCloseTo(0, within(1e-12));
        assertThat(WirePath.drop(1, sag)).isCloseTo(0, within(1e-12));
        assertThat(WirePath.point(A, B, 0)).containsExactly(A[0], A[1], A[2]);
        assertThat(WirePath.point(A, B, 1)[1]).isCloseTo(B[1], within(1e-12));
    }

    @Test
    void theSagIsMaximalAtTheMiddleAndNeverNegative() {
        double sag = WirePath.sag(24);
        assertThat(WirePath.drop(0.5, sag)).isCloseTo(sag, within(1e-12));
        for (int i = 0; i <= 100; i++) {
            double drop = WirePath.drop(i / 100.0, sag);
            assertThat(drop).isBetween(0.0, sag + 1e-12);
        }
    }

    @Test
    void theSagOfAWireOfTwentyFourBlocksIsAboutAHalfToAWholeBlock() {
        assertThat(WirePath.sag(24)).isBetween(0.5, 1.0);
        assertThat(WirePath.sag(24)).isCloseTo(0.72, within(1e-9));
    }

    @Test
    void theSagGrowsWithTheLengthAndStopsAtTheCap() {
        assertThat(WirePath.sag(12) * 2).isCloseTo(WirePath.sag(24), within(1e-12));
        assertThat(WirePath.sag(0)).isZero();
        assertThat(WirePath.sag(-3)).isZero();
        assertThat(WirePath.sag(500)).isEqualTo(WirePath.MAX_SAG);
    }

    @Test
    void aWireSagsBelowItsStraightLine() {
        List<double[]> points = WirePath.path(A, B, 0.5);
        double[] middle = points.get(points.size() / 2);
        assertThat(middle[0]).isCloseTo(12, within(1e-9));
        assertThat(middle[1]).isCloseTo(64 - WirePath.sag(24), within(1e-9));
    }

    @Test
    void thePointCountIsBounded() {
        assertThat(WirePath.pointCount(0, 0.5)).isEqualTo(WirePath.MIN_POINTS);
        assertThat(WirePath.pointCount(24, 0.5)).isEqualTo(49);
        assertThat(WirePath.pointCount(1000, 0.5)).isEqualTo(WirePath.MAX_POINTS);
        for (double length = 0; length <= 200; length += 0.7) {
            assertThat(WirePath.pointCount(length, 0.5))
                    .isBetween(WirePath.MIN_POINTS, WirePath.MAX_POINTS);
        }
    }

    @Test
    void aPathKeepsItsEndsAndHasTheCountOfItsLength() {
        List<double[]> points = WirePath.path(A, B, 0.5);
        assertThat(points).hasSize(49);
        assertThat(points.get(0)).containsExactly(A[0], A[1], A[2]);
        assertThat(points.get(48)[0]).isCloseTo(24, within(1e-9));
        assertThat(points.get(48)[1]).isCloseTo(64, within(1e-9));
    }

    @Test
    void aPathOfZeroLengthIsItsTwoEndsAtOnePlace() {
        List<double[]> points = WirePath.path(A, A, 0.5);
        assertThat(points).hasSize(2);
        assertThat(points).allSatisfy(p -> assertThat(p).containsExactly(A[0], A[1], A[2]));
    }

    @Test
    void thePulseWrapsIntoTheWire() {
        double length = 24;
        double speed = 4;
        for (double seconds = -20; seconds <= 40; seconds += 0.37) {
            double t = WirePath.pulseFraction(seconds, speed, length, 0, 1);
            assertThat(t).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
        }
        double period = length / speed;
        assertThat(WirePath.pulseFraction(1.3 + period, speed, length, 0, 1))
                .isCloseTo(WirePath.pulseFraction(1.3, speed, length, 0, 1), within(1e-9));
    }

    @Test
    void thePulseTravelsFromTheStartTowardTheEnd() {
        double speed = 3;
        double length = 24;
        assertThat(WirePath.pulseFraction(0, speed, length, 0, 1)).isZero();
        assertThat(WirePath.pulseFraction(4, speed, length, 0, 1)).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void thePulsesOfAWireAreSpacedEvenly() {
        double first = WirePath.pulseFraction(2, 3, 24, 0, 3);
        double second = WirePath.pulseFraction(2, 3, 24, 1, 3);
        double third = WirePath.pulseFraction(2, 3, 24, 2, 3);
        assertThat(second - first).isCloseTo(1.0 / 3, within(1e-9));
        assertThat(third - second).isCloseTo(1.0 / 3, within(1e-9));
    }

    @Test
    void aWireWithNoPowerHasNoPulse() {
        assertThat(WirePath.flow(0.9, 0, 100)).isZero();
        assertThat(WirePath.flow(0.9, 10, 0)).isZero();
        assertThat(WirePath.flow(0.9, -1, 10)).isZero();
        assertThat(WirePath.pulseCount(0)).isZero();
        assertThat(WirePath.pulseCount(0.01)).isZero();
    }

    @Test
    void theFlowFollowsTheSatisfactionOfALoadedGrid() {
        assertThat(WirePath.flow(0.75, 100, 75)).isCloseTo(0.75, within(1e-12));
        assertThat(WirePath.flow(1.5, 100, 100)).isEqualTo(1.0);
        assertThat(WirePath.flow(Double.NaN, 100, 100)).isZero();
    }

    @Test
    void thePulsesGrowWithTheFlowAndStayBounded() {
        assertThat(WirePath.pulseCount(1.0)).isEqualTo(WirePath.MAX_PULSES);
        assertThat(WirePath.pulseCount(0.5)).isEqualTo(2);
        assertThat(WirePath.pulseCount(0.05)).isEqualTo(1);
        assertThat(WirePath.pulseSpeed(0)).isEqualTo(WirePath.SLOWEST_BLOCKS_PER_SECOND);
        assertThat(WirePath.pulseSpeed(1)).isEqualTo(WirePath.FASTEST_BLOCKS_PER_SECOND);
        assertThat(WirePath.pulseSpeed(0.5)).isBetween(WirePath.SLOWEST_BLOCKS_PER_SECOND, WirePath.FASTEST_BLOCKS_PER_SECOND);
    }

    @Test
    void theBudgetNeverSendsMoreThanTheCap() {
        int[] costs = {49, 52, 20, 33, 49, 7, 60, 2, 49};
        for (int cap = 0; cap <= 300; cap++) {
            boolean[] drawn = WirePath.fit(costs, cap);
            int sent = 0;
            for (int i = 0; i < costs.length; i++) {
                if (drawn[i]) {
                    sent += costs[i];
                }
            }
            assertThat(sent).isLessThanOrEqualTo(cap);
        }
    }

    @Test
    void theBudgetTakesTheNearestWiresAndSkipsOnesThatDoNotFit() {
        boolean[] drawn = WirePath.fit(new int[] {49, 60, 20}, 80);
        assertThat(drawn).containsExactly(true, false, true);
    }

    @Test
    void aRoomyBudgetDrawsEveryWireAndNoCapDrawsNone() {
        int[] costs = {49, 49, 49};
        assertThat(WirePath.fit(costs, 1000)).containsExactly(true, true, true);
        assertThat(WirePath.fit(costs, 0)).containsExactly(false, false, false);
        assertThat(WirePath.fit(costs, -5)).containsExactly(false, false, false);
    }

    @Test
    void aColourBrightensTowardWhite() {
        int colour = 0x3CB44B;
        assertThat(WirePath.brighten(colour, 0)).isEqualTo(colour);
        assertThat(WirePath.brighten(colour, 1)).isEqualTo(0xFFFFFF);
        assertThat(WirePath.brighten(0x000000, 0.5)).isEqualTo(0x808080);
    }

    @Test
    void aNonPositiveSpacingIsRefused() {
        assertThatThrownBy(() -> WirePath.pointCount(10, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
