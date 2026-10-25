package io.corefactory.paper.logistics;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PlacementAimTest {

    private static final Pos CLICKED = new Pos(0, 64, 0);
    private static final Pos EAST_FACE = new Pos(1, 0, 0);
    private static final Pos WEST_FACE = new Pos(-1, 0, 0);
    private static final Pos UP = new Pos(0, 1, 0);
    private static final Pos DOWN = new Pos(0, -1, 0);

    @Test
    void aBeltFlowsAwayFromThePlayerAndIsPlacedOnTheFace() {
        PlacementAim.Plan plan = PlacementAim.plan(LogisticsIds.Kind.BELT, CLICKED, EAST_FACE, 90.0).orElseThrow();
        assertThat(plan.target()).isEqualTo(new Pos(1, 64, 0));
        assertThat(plan.flow()).isEqualTo(Dir.EAST);
        assertThat(plan.inDir()).isNull();
    }

    @Test
    void lookingEastPlacesABeltThatFlowsWest() {
        PlacementAim.Plan plan = PlacementAim.plan(LogisticsIds.Kind.BELT, CLICKED, WEST_FACE, 270.0).orElseThrow();
        assertThat(plan.flow()).isEqualTo(Dir.WEST);
    }

    @Test
    void aBeltCanBePlacedOnTheTopOfABlock() {
        PlacementAim.Plan plan = PlacementAim.plan(LogisticsIds.Kind.BELT, CLICKED, UP, 0.0).orElseThrow();
        assertThat(plan.target()).isEqualTo(new Pos(0, 65, 0));
    }

    @Test
    void aPickerTakesItsInputFromTheClickedFace() {
        PlacementAim.Plan plan = PlacementAim.plan(LogisticsIds.Kind.PICKER, CLICKED, EAST_FACE, 0.0).orElseThrow();
        assertThat(plan.target()).isEqualTo(new Pos(1, 64, 0));
        assertThat(plan.inDir()).isEqualTo(Dir.WEST);
        assertThat(plan.inDir().step(plan.target())).isEqualTo(CLICKED);
    }

    @Test
    void aPickerOrSplitterOnATopOrBottomFaceIsRefused() {
        assertThat(PlacementAim.plan(LogisticsIds.Kind.PICKER, CLICKED, UP, 0.0)).isEmpty();
        assertThat(PlacementAim.plan(LogisticsIds.Kind.SPLITTER, CLICKED, DOWN, 0.0)).isEmpty();
    }

    @Test
    void aSplitterHasItsBackOnTheClickedBlock() {
        PlacementAim.Plan plan = PlacementAim.plan(LogisticsIds.Kind.SPLITTER, CLICKED, WEST_FACE, 0.0).orElseThrow();
        assertThat(plan.target()).isEqualTo(new Pos(-1, 64, 0));
        assertThat(plan.inDir()).isEqualTo(Dir.WEST);
        assertThat(plan.inDir().opposite().step(plan.target())).isEqualTo(CLICKED);
    }

    @Test
    void theFaceAnAimRayEntersABlockThroughIsTheFaceTheNewDeviceGoesOn() {
        assertThat(PlacementAim.entryFace(new double[] {-5, 64.5, 0.5}, new double[] {1, 0, 0}, CLICKED))
                .isEqualTo(new Pos(-1, 0, 0));
        assertThat(PlacementAim.entryFace(new double[] {0.5, 70, 0.5}, new double[] {0, -1, 0}, CLICKED))
                .isEqualTo(new Pos(0, 1, 0));
        assertThat(PlacementAim.entryFace(new double[] {0.5, 64.5, -4}, new double[] {0, 0, 1}, CLICKED))
                .isEqualTo(new Pos(0, 0, -1));
    }

    @Test
    void aRayThatMissesTheBlockGivesTheDominantFaceTowardTheEye() {
        assertThat(PlacementAim.entryFace(new double[] {5, 64.5, 2}, new double[] {0, 0, 1}, CLICKED))
                .isEqualTo(new Pos(1, 0, 0));
    }

    @ParameterizedTest
    @EnumSource(value = Dir.class)
    void aPickerOnAnySideFaceTakesFromTheAimedBlockAndGivesAwayFromIt(Dir face) {
        Pos offset = face.step(new Pos(0, 0, 0));
        PlacementAim.Plan plan = PlacementAim.plan(LogisticsIds.Kind.PICKER, CLICKED, offset, 0.0).orElseThrow();
        assertThat(plan.target()).isEqualTo(face.step(CLICKED));
        assertThat(plan.inDir()).isEqualTo(face.opposite());
        assertThat(plan.inDir().step(plan.target())).isEqualTo(CLICKED);
        assertThat(plan.inDir().opposite().step(plan.target())).isEqualTo(face.step(plan.target()));
    }

    @ParameterizedTest
    @EnumSource(value = Dir.class)
    void aSplitterOnAnySideFaceHasItsBackOnTheAimedBlockAndItsItemsGoAway(Dir face) {
        Pos offset = face.step(new Pos(0, 0, 0));
        PlacementAim.Plan plan = PlacementAim.plan(LogisticsIds.Kind.SPLITTER, CLICKED, offset, 0.0).orElseThrow();
        assertThat(plan.target()).isEqualTo(face.step(CLICKED));
        assertThat(plan.inDir()).isEqualTo(face);
        assertThat(plan.inDir().opposite().step(plan.target())).isEqualTo(CLICKED);
    }

    @ParameterizedTest
    @EnumSource(value = Dir.class)
    void aPickerTurnedByItsDisplayYawHasItsInputFacingTheAimedBlockAndItsOutputAway(Dir face) {
        Pos offset = face.step(new Pos(0, 0, 0));
        PlacementAim.Plan plan = PlacementAim.plan(LogisticsIds.Kind.PICKER, CLICKED, offset, 0.0).orElseThrow();
        double[] input = Orientation.rotate(Orientation.northYaw(plan.inDir()), 0, -1);
        double[] output = Orientation.rotate(Orientation.northYaw(plan.inDir()), 0, 1);
        assertThat(input[0]).isCloseTo(Orientation.vectorOf(face.opposite())[0], within(1e-9));
        assertThat(input[1]).isCloseTo(Orientation.vectorOf(face.opposite())[1], within(1e-9));
        assertThat(output[0]).isCloseTo(Orientation.vectorOf(face)[0], within(1e-9));
        assertThat(output[1]).isCloseTo(Orientation.vectorOf(face)[1], within(1e-9));
    }
}
