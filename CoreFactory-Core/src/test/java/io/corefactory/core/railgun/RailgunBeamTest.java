package io.corefactory.core.railgun;

import io.corefactory.core.config.TestConfig;
import io.corefactory.core.railgun.RailgunBeam.Kind;
import io.corefactory.core.railgun.RailgunBeam.Sample;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RailgunBeamTest {

    /** The frontier's distance in one tick at level I, in blocks: 2 blocks a second over 20 ticks. */
    private static final double LEVEL_ONE_STEP = 0.1;

    @Test
    void theFrontierMovesAtTheLevelsSpeedEachTick() {
        RailgunBeam beam = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.I, 0, 0, 0);

        beam.advance(0, 0, 100);
        assertThat(beam.frontierZ()).isCloseTo(LEVEL_ONE_STEP, within(1e-9));
        for (int tick = 2; tick <= 20; tick++) {
            beam.advance(0, 0, 100);
        }

        assertThat(beam.ticks()).isEqualTo(20);
        assertThat(beam.frontierZ()).isCloseTo(2.0, within(LEVEL_ONE_STEP / 10));
        assertThat(beam.pathLength()).isCloseTo(2.0, within(LEVEL_ONE_STEP / 10));
    }

    @Test
    void theFrontierOfLevelThreeMovesAtNineBlocksASecond() {
        RailgunBeam beam = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.III, 0, 0, 0);

        for (int tick = 0; tick < 20; tick++) {
            beam.advance(0, 0, 100);
        }

        assertThat(beam.frontierZ()).isCloseTo(9.0, within(1e-9));
    }

    @Test
    void atTheStartTheFrontierAndFourStrandsAreDrawnAroundThePath() {
        RailgunBeam beam = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.I, 0, 0, 0);

        beam.advance(0, 0, 100);
        List<Sample> points = beam.points();
        List<Sample> red = ofKind(points, Kind.RED_STRAND);

        assertThat(ofKind(points, Kind.FLAME_FRONTIER)).containsExactly(new Sample(Kind.FLAME_FRONTIER, 0, 0, beam.frontierZ()));
        assertThat(ofKind(points, Kind.BLUE_SPIRAL)).isEmpty();
        // A tenth of a block along the path is inside one sample spacing, so each strand has one point, at the barrel.
        assertThat(red).hasSize(4);

        double radius = RailgunSettings.defaults().strandRadiusBlocks();
        for (Sample s : red) {
            assertThat(Math.hypot(s.x(), s.y())).isCloseTo(radius, within(1e-9));
            assertThat(s.z()).isCloseTo(red.get(0).z(), within(1e-9));
        }
        assertThat(distance(red.get(0), red.get(2))).isCloseTo(2 * radius, within(1e-9));
        assertThat(distance(red.get(1), red.get(3))).isCloseTo(2 * radius, within(1e-9));
    }

    @Test
    void theStrandsAreSampledAlongTheWholePathNotOnlyAtTheFrontier() {
        RailgunBeam beam = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.I, 0, 0, 0);
        for (int tick = 0; tick < 200; tick++) {
            beam.advance(0, 0, 1000);
        }

        List<Sample> red = ofKind(beam.points(), Kind.RED_STRAND);
        double head = beam.frontierZ();
        double farthest = red.stream().mapToDouble(Sample::z).max().orElseThrow();
        double nearest = red.stream().mapToDouble(Sample::z).min().orElseThrow();

        // The last sample lies within one spacing of the frontier, and the first sits at the barrel.
        assertThat(farthest).isCloseTo(head, within(1.0));
        assertThat(nearest).isLessThan(head * 0.2);
    }

    @Test
    void theBlueSpiralStartsAtTheDelayAndNotBefore() {
        RailgunBeam beam = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.I, 0, 0, 0);

        for (int tick = 1; tick <= 9; tick++) {
            beam.advance(0, 0, 100);
        }
        assertThat(beam.spiralStarted()).isFalse();
        assertThat(ofKind(beam.points(), Kind.BLUE_SPIRAL)).isEmpty();

        beam.advance(0, 0, 100);
        assertThat(beam.spiralStarted()).isTrue();
        List<Sample> blue = ofKind(beam.points(), Kind.BLUE_SPIRAL);
        assertThat(blue).isNotEmpty();
        // The spiral begins at the frontier's point at the delay, which is one second in: 10 ticks of 0.1 blocks.
        assertThat(blue.get(0).z()).isCloseTo(1.0, within(1e-6));
        assertThat(Math.hypot(blue.get(0).x(), blue.get(0).y()))
                .isCloseTo(RailgunSettings.defaults().spiralRadiusBlocks(), within(1e-9));
        assertThat(blue.get(blue.size() - 1).z()).isCloseTo(beam.frontierZ(), within(0.25));
    }

    @Test
    void theSpiralIsWiderThanTheStrandsAndWrapsThemAsOneHelix() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThat(s.spiralRadiusBlocks()).isGreaterThanOrEqualTo(2 * s.strandRadiusBlocks());
        assertThat(s.strands()).isEqualTo(4);
    }

    @Test
    void theBeamCurvesTowardATargetThatMovesSideways() {
        RailgunBeam beam = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.I, 0, 0, 0);
        List<double[]> path = new ArrayList<>();

        int tick = 0;
        while (!beam.arrived() && tick < 3000) {
            tick++;
            // The target starts 40 blocks ahead and slides 0.1 blocks east a tick for 100 ticks, then stops at 10 blocks east.
            double east = Math.min(10.0, 0.1 * tick);
            beam.advance(east, 0, 40);
            path.add(new double[] {beam.frontierX(), beam.frontierZ()});
        }

        assertThat(beam.arrived()).isTrue();
        double[] end = path.get(path.size() - 1);
        assertThat(end[0]).isCloseTo(10.0, within(1.0));
        double[] mid = path.get(path.size() / 2);
        double chord = Math.hypot(end[0], end[1]);
        double offChord = Math.abs(mid[0] * end[1] - mid[1] * end[0]) / chord;
        assertThat(offChord).isGreaterThan(0.5);
    }

    @Test
    void arrivalIsReportedOnceAndThenTheStrandsAndTheSpiralStop() {
        RailgunBeam beam = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.I, 0, 0, 0);

        int arrivals = 0;
        for (int tick = 1; tick <= 1000 && !beam.arrived(); tick++) {
            if (beam.advance(0, 0, 5)) {
                arrivals++;
            }
        }
        assertThat(arrivals).isEqualTo(1);
        assertThat(beam.arrived()).isTrue();
        // The target is 5 blocks away, the hit distance is half a block, and the frontier covers 0.1 blocks a tick.
        assertThat(beam.ticks()).isBetween(45, 46);
        assertThat(beam.spiralStarted()).isTrue();

        assertThat(beam.points()).isEmpty();
        assertThat(beam.advance(0, 0, 5)).isFalse();
        assertThat(beam.advance(0, 0, 5)).isFalse();
        assertThat(beam.points()).isEmpty();
    }

    @Test
    void thePointsOfATickNeverExceedTheParticleBudget() {
        for (int budget : new int[] {0, 1, 2, 5, 20, 48, 128}) {
            RailgunBeam beam = new RailgunBeam(settings(budget), RailgunLevel.III, 0, 0, 0);
            for (int tick = 1; tick <= 300; tick++) {
                beam.advance(0, 0, 1000);
                assertThat(beam.points().size()).isLessThanOrEqualTo(budget);
            }
        }
    }

    @Test
    void aSmallBudgetDrawsTheStrandsBeforeTheSpiral() {
        RailgunBeam small = new RailgunBeam(settings(5), RailgunLevel.I, 0, 0, 0);
        RailgunBeam wider = new RailgunBeam(settings(6), RailgunLevel.I, 0, 0, 0);
        for (int tick = 1; tick <= 20; tick++) {
            small.advance(0, 0, 1000);
            wider.advance(0, 0, 1000);
        }
        assertThat(small.spiralStarted()).isTrue();

        // Five points: the frontier and one point on each strand, and no spiral, since the strands take all the rest.
        List<Sample> five = small.points();
        assertThat(five).hasSize(5);
        assertThat(ofKind(five, Kind.RED_STRAND)).hasSize(4);
        assertThat(ofKind(five, Kind.BLUE_SPIRAL)).isEmpty();

        // Six points: the frontier, one point on each strand, and one of the spiral.
        List<Sample> six = wider.points();
        assertThat(six).hasSize(6);
        assertThat(ofKind(six, Kind.RED_STRAND)).hasSize(4);
        assertThat(ofKind(six, Kind.BLUE_SPIRAL)).hasSize(1);
    }

    @Test
    void aBudgetOfZeroDrawsNothingAndTheDefaultBudgetIsNeverExceeded() {
        RailgunBeam none = new RailgunBeam(settings(0), RailgunLevel.I, 0, 0, 0);
        none.advance(0, 0, 100);
        assertThat(none.points()).isEmpty();

        RailgunBeam full = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.I, 0, 0, 0);
        for (int tick = 1; tick <= 30; tick++) {
            full.advance(0, 0, 100);
        }
        // Three blocks of path at a quarter-block spacing: 13 points on each of the four strands, and nine of the spiral, which started
        // at one block. The count moves by one with the float sum of the path, so it is checked as a range.
        assertThat(ofKind(full.points(), Kind.RED_STRAND)).hasSizeBetween(48, 52);
        assertThat(full.points().size()).isLessThanOrEqualTo(RailgunSettings.defaults().particleBudget());
    }

    @Test
    void anAllowanceLimitsThePointsButNeverRaisesTheBudget() {
        RailgunBeam beam = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.I, 0, 0, 0);
        for (int tick = 1; tick <= 30; tick++) {
            beam.advance(0, 0, 100);
        }

        assertThat(beam.points(5)).hasSize(5);
        assertThat(beam.points(0)).isEmpty();
        assertThat(beam.points(1000)).hasSizeLessThanOrEqualTo(RailgunSettings.defaults().particleBudget());
        assertThat(beam.points(5).get(0).kind()).isEqualTo(Kind.FLAME_FRONTIER);
    }

    @Test
    void aLongFlightDoesNotGrowTheWorkOfATick() {
        RailgunBeam beam = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.III, 0, 0, 0);
        int budget = RailgunSettings.defaults().particleBudget();
        for (int tick = 1; tick <= 3000; tick++) {
            beam.advance(0, 0, 100_000);
            assertThat(beam.points().size()).isLessThanOrEqualTo(budget);
        }

        assertThat(beam.historySize()).isLessThanOrEqualTo(RailgunBeam.CAPACITY);
        assertThat(beam.pathLength()).isCloseTo(3000 * 9.0 / 20, within(1e-6));
    }

    @Test
    void theRecordedPathIsBoundedWhenTheFlightIsLong() {
        RailgunBeam beam = new RailgunBeam(RailgunSettings.defaults(), RailgunLevel.III, 0, 0, 0);
        for (int tick = 1; tick <= 20_000; tick++) {
            beam.advance(0, 0, 1_000_000);
        }

        assertThat(beam.historySize()).isBetween(RailgunBeam.CAPACITY / 2, RailgunBeam.CAPACITY);
        List<Sample> red = ofKind(beam.points(), Kind.RED_STRAND);
        assertThat(red).isNotEmpty();
        // A long path is drawn in full, its last sample within the wide spacing of the frontier.
        assertThat(red.stream().mapToDouble(Sample::z).max().orElseThrow()).isGreaterThan(beam.frontierZ() * 0.9);
    }

    private static RailgunSettings settings(int budget) {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "railgun.particle-budget", budget);
        return RailgunSettings.from(config);
    }

    private static List<Sample> ofKind(List<Sample> points, Kind kind) {
        return points.stream().filter(p -> p.kind() == kind).toList();
    }

    private static double distance(Sample a, Sample b) {
        return Math.sqrt(Math.pow(a.x() - b.x(), 2) + Math.pow(a.y() - b.y(), 2) + Math.pow(a.z() - b.z(), 2));
    }
}
