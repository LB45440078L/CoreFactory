package io.corefactory.core.artillery;

import io.corefactory.core.artillery.ArtilleryBallistics.ArcSample;
import io.corefactory.core.artillery.ArtilleryBallistics.Trajectory;
import org.junit.jupiter.api.Test;

import static io.corefactory.core.artillery.ArtilleryBallistics.GRAVITY_PER_TICK2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ArtilleryBallisticsTest {

    private static final double G = GRAVITY_PER_TICK2;

    @Test
    void gravityIsNinePointEightMetresPerSecondSquaredInBlocksPerTick() {
        // 9.8 x 0.05 x 0.05 = 0.0245 blocks per tick squared.
        assertThat(G).isCloseTo(0.0245, within(1e-12));
        assertThat(ArtilleryBallistics.dragPerTick(0.002)).isCloseTo(0.0001, within(1e-12));
    }

    @Test
    void theRangeWithoutDragMatchesTheClosedFormWithinATenthOfAPercent() {
        // v = 0.5 blocks per tick at 30 degrees: v^2 sin 60 / g = 0.25 x 0.8660254 / 0.0245 = 8.836994.
        Trajectory t = ArtilleryBallistics.fly(30, 0.5, 0, G, 0);
        assertThat(t.landed()).isTrue();
        assertThat(t.range()).isCloseTo(8.836994, within(8.836994 * 0.001));
    }

    @Test
    void theApexWithoutDragMatchesTheClosedForm() {
        // Apex = (v sin 30)^2 / (2g) = 0.0625 / 0.049 = 1.275510.
        Trajectory t = ArtilleryBallistics.fly(30, 0.5, 0, G, 0);
        assertThat(t.apex()).isCloseTo(1.275510, within(1.275510 * 0.001));
    }

    @Test
    void theFlightTimeWithoutDragIsTwoVerticalSpeedsOverGravity() {
        // 2 v sin 30 / g = 0.5 / 0.0245 = 20.408163 ticks, about 1.02 seconds.
        Trajectory t = ArtilleryBallistics.fly(30, 0.5, 0, G, 0);
        assertThat(t.flightTicks()).isCloseTo(20.408163, within(0.001));
    }

    @Test
    void aSpeedSolvedForARangeWithoutDragIsTheClosedFormSpeed() {
        // 100 blocks at 45 degrees: v = sqrt(100 g) = sqrt(2.45) = 1.565248 blocks per tick.
        assertThat(ArtilleryBallistics.speedFor(45, 100, 0, G, 0)).isCloseTo(1.565248, within(1e-6));
    }

    @Test
    void aSolvedSpeedLandsAtTheRequestedRangeWithDragAndAHeight() {
        double drag = ArtilleryBallistics.dragPerTick(0.002);
        double flat = ArtilleryBallistics.speedFor(60, 500, 0, G, drag);
        assertThat(ArtilleryBallistics.fly(60, flat, 0, G, drag).range()).isCloseTo(500, within(0.01));

        double raised = ArtilleryBallistics.speedFor(45, 200, 10, G, drag);
        Trajectory t = ArtilleryBallistics.fly(45, raised, 10, G, drag);
        assertThat(t.landed()).isTrue();
        assertThat(t.range()).isCloseTo(200, within(0.01));
    }

    @Test
    void aFortyFiveDegreeShotHasTheLongestRangeForAFixedSpeedWithoutDrag() {
        double at45 = ArtilleryBallistics.fly(45, 1.0, 0, G, 0).range();
        // The closed form is v^2 / g = 1 / 0.0245 = 40.816327 at 45 degrees.
        assertThat(at45).isCloseTo(40.816327, within(0.001));
        for (double angle : new double[] {20, 30, 40, 50, 60, 70}) {
            assertThat(ArtilleryBallistics.fly(angle, 1.0, 0, G, 0).range()).isLessThan(at45);
        }
    }

    @Test
    void dragShortensTheRangeByAboutFivePerCentAtNineThousandBlocks() {
        double speed = Math.sqrt(9000 * G);
        double vacuum = ArtilleryBallistics.fly(45, speed, 0, G, 0).range();
        assertThat(vacuum).isCloseTo(9000, within(9.0));

        double withDrag = ArtilleryBallistics.fly(45, speed, 0, G, ArtilleryBallistics.dragPerTick(0.002)).range();
        assertThat(withDrag).isLessThan(vacuum).isGreaterThan(vacuum * 0.9);
    }

    @Test
    void theArcStartsAtTheLaunchAndEndsAtTheLanding() {
        Trajectory t = ArtilleryBallistics.fly(30, 0.5, 0, G, 0);
        ArcSample first = t.arc().get(0);
        ArcSample last = t.arc().get(t.arc().size() - 1);
        assertThat(first.distance()).isZero();
        assertThat(first.height()).isZero();
        assertThat(last.distance()).isCloseTo(t.range(), within(1e-9));
        assertThat(last.height()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void theHighestSampleOfTheArcIsTheApex() {
        Trajectory t = ArtilleryBallistics.fly(30, 0.5, 0, G, 0);
        double highest = t.arc().stream().mapToDouble(ArcSample::height).max().orElseThrow();
        assertThat(highest).isLessThanOrEqualTo(t.apex() + 1e-9);
        assertThat(highest).isCloseTo(t.apex(), within(0.01));
    }

    @Test
    void aTargetAboveTheReachOfTheArcIsNotLanded() {
        Trajectory t = ArtilleryBallistics.fly(30, 0.5, 5.0, G, 0);
        assertThat(t.landed()).isFalse();
        assertThat(t.range()).isZero();
    }

    @Test
    void theLandingPointLiesAlongTheDirectionFromTheLaunch() {
        Position launch = new Position(10, 64, 20);
        GroundPoint diagonal = ArtilleryBallistics.landing(launch, 3, 4, 5);
        assertThat(diagonal.x()).isCloseTo(13.0, within(1e-9));
        assertThat(diagonal.z()).isCloseTo(24.0, within(1e-9));
        GroundPoint east = ArtilleryBallistics.landing(launch, 1, 0, 100);
        assertThat(east.x()).isCloseTo(110.0, within(1e-9));
        assertThat(east.z()).isCloseTo(20.0, within(1e-9));
    }

    @Test
    void aZeroDirectionAndAnElevationOutsideTheRangeAreRefused() {
        Position launch = new Position(0, 64, 0);
        assertThatThrownBy(() -> ArtilleryBallistics.landing(launch, 0, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArtilleryBallistics.fly(0, 0.5, 0, G, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArtilleryBallistics.fly(90, 0.5, 0, G, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
