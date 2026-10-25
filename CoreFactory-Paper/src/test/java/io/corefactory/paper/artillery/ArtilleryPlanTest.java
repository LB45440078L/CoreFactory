package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryBallistics;
import io.corefactory.core.artillery.ArtillerySettings;
import io.corefactory.core.artillery.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Checks that a planned flight starts at the barrel, lands on its aim, and arcs above the ground between. */
class ArtilleryPlanTest {

    private static final Position LAUNCH = new Position(0.5, 65.0, 0.5);

    @Test
    void theShellLandsOnItsAimAtTheElevation() {
        ArtilleryPlan plan = ArtilleryPlan.of(ArtillerySettings.defaults(), LAUNCH, 1000.5, 0.5, 64.0, 45.0);

        assertThat(plan.distance()).isCloseTo(1000.0, within(1e-9));
        assertThat(plan.dirX()).isCloseTo(1.0, within(1e-9));
        assertThat(plan.dirZ()).isCloseTo(0.0, within(1e-9));
        assertThat(plan.trajectory().landed()).isTrue();
        assertThat(plan.speed()).isPositive();
        assertThat(plan.trajectory().apex()).isGreaterThan(0.0);

        Position start = plan.positionAt(0);
        assertThat(start.x()).isCloseTo(LAUNCH.x(), within(1e-9));
        assertThat(start.y()).isCloseTo(LAUNCH.y(), within(1e-9));

        Position landing = plan.positionAt(plan.lastTick());
        assertThat(landing.x()).isCloseTo(1000.5, within(0.01));
        assertThat(landing.y()).isCloseTo(64.0, within(0.01));
    }

    @Test
    void aTickPastTheLandingStaysOnTheLandingPoint() {
        ArtilleryPlan plan = ArtilleryPlan.of(ArtillerySettings.defaults(), LAUNCH, 500.5, 0.5, 64.0, 30.0);
        assertThat(plan.positionAt(plan.lastTick() + 100)).isEqualTo(plan.positionAt(plan.lastTick()));
    }

    @Test
    void aFarShotAtTheLowestElevationStillLands() {
        ArtilleryPlan plan = ArtilleryPlan.of(ArtillerySettings.defaults(), LAUNCH, 9000.5, 0.5, 64.0, 5.0);
        assertThat(plan.trajectory().landed()).isTrue();
        assertThat(plan.trajectory().range()).isCloseTo(9000.0, within(0.5));
    }

    @Test
    void anAimOnTheLaunchPointIsRefused() {
        assertThatThrownBy(() -> ArtilleryPlan.of(ArtillerySettings.defaults(), LAUNCH, 0.5, 0.5, 64.0, 45.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * The flight of a level I shell at its default range and the default elevation of a Command Center, 45 degrees, from a barrel 1.5 blocks
     * above flat ground. The simulation gives about 17.6 seconds, which is the time the shell is in the air before it lands.
     */
    @Test
    void aLevelOneShellAtFortyFiveDegreesFliesAboutSeventeenSeconds() {
        Position launch = new Position(0.5, 65.5, 0.5);
        ArtilleryPlan plan = ArtilleryPlan.of(ArtillerySettings.defaults(), launch, 1500.5, 0.5, 64.0, 45.0);

        assertThat(plan.speed()).isCloseTo(6.13, within(0.01));
        assertThat(plan.trajectory().flightTicks() * ArtilleryBallistics.SECONDS_PER_TICK).isCloseTo(17.6, within(0.1));
        assertThat(plan.trajectory().apex()).isCloseTo(379.0, within(1.0));
    }

    /**
     * The flight of a level VII shell at its default range and 45 degrees, from a barrel 3.75 blocks above flat ground. The simulation gives about
     * 43.5 seconds, and the apex is about 2300 blocks up, well above the build height.
     */
    @Test
    void aLevelSevenShellAtFortyFiveDegreesFliesAboutFortyThreeSeconds() {
        Position launch = new Position(0.5, 67.75, 0.5);
        ArtilleryPlan plan = ArtilleryPlan.of(ArtillerySettings.defaults(), launch, 9000.5, 0.5, 64.0, 45.0);

        assertThat(plan.speed()).isCloseTo(15.28, within(0.01));
        assertThat(plan.trajectory().flightTicks() * ArtilleryBallistics.SECONDS_PER_TICK).isCloseTo(43.5, within(0.2));
        assertThat(plan.trajectory().apex()).isCloseTo(2314.0, within(5.0));
    }
}
