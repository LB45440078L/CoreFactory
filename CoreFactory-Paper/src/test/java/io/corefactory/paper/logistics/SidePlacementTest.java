package io.corefactory.paper.logistics;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.logistics.PlacementReason;
import io.corefactory.paper.logistics.SidePlacement.Input;
import io.corefactory.paper.logistics.SidePlacement.Space;
import io.corefactory.paper.logistics.SidePlacement.Verdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SidePlacementTest {

    private static final Pos CLICKED = new Pos(0, 64, 0);
    private static final Pos EAST = new Pos(1, 0, 0);
    private static final Pos WEST = new Pos(-1, 0, 0);
    private static final Pos UP = new Pos(0, 1, 0);
    private static final Pos DOWN = new Pos(0, -1, 0);

    private static Verdict decide(LogisticsIds.Kind kind, Pos face, Input input, boolean researched, Space space,
                                  PlacementReason network) {
        return SidePlacement.decide(kind, face, input, researched, space, () -> network);
    }

    @Test
    void aPickerOrSplitterOnATopOrBottomFaceIsRefusedAsNotASide() {
        assertThat(decide(LogisticsIds.Kind.PICKER, UP, Input.PORT, true, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.NOT_SIDE);
        assertThat(decide(LogisticsIds.Kind.SPLITTER, DOWN, Input.PORT, true, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.NOT_SIDE);
    }

    @Test
    void aBeltMayGoOnATopFace() {
        assertThat(decide(LogisticsIds.Kind.BELT, UP, Input.OTHER, true, Space.FREE, PlacementReason.OK)).isEqualTo(Verdict.OK);
    }

    @Test
    void aPickerOnASideFaceOfAMachineIsOk() {
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.PORT, true, Space.FREE, PlacementReason.OK)).isEqualTo(Verdict.OK);
        assertThat(decide(LogisticsIds.Kind.SPLITTER, WEST, Input.PORT, true, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.OK);
    }

    @Test
    void aBeltFlowingIntoTheSpotIsAnAcceptedInput() {
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.BELT_INTO, true, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.OK);
    }

    @Test
    void aPickerBesideAStraightBeltIsAnAcceptedInput() {
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.BELT_SIDE, true, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.OK);
    }

    @Test
    void aSplitterBesideABeltIsRefusedBecauseItTakesOnlyFromTheBeltBehindIt() {
        assertThat(decide(LogisticsIds.Kind.SPLITTER, EAST, Input.BELT_SIDE, true, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.NO_INPUT);
    }

    @Test
    void aChestOnThePickersInputSideIsAnAcceptedInput() {
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.STORAGE, true, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.OK);
    }

    @Test
    void aChestOnTheSplittersSideIsRefusedBecauseASplitterTakesOnlyFromItsBelt() {
        assertThat(decide(LogisticsIds.Kind.SPLITTER, EAST, Input.STORAGE, true, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.NO_INPUT);
    }

    @Test
    void aChestOnTheInputSideNeedsTheResearchAndAFreeSpotLikeAnyPicker() {
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.STORAGE, false, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.LOCKED);
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.STORAGE, true, Space.SOLID, PlacementReason.OK))
                .isEqualTo(Verdict.SOLID);
    }

    @Test
    void aBeltNextToThePickerSpotIsAnInputOnlyWhenItFlowsIntoOrPastTheSpot() {
        assertThat(SidePlacement.beltInput(Dir.WEST, Dir.WEST, true)).isEqualTo(Input.BELT_INTO);
        assertThat(SidePlacement.beltInput(Dir.WEST, Dir.WEST, false)).isEqualTo(Input.BELT_INTO);
        assertThat(SidePlacement.beltInput(Dir.EAST, Dir.WEST, true)).isEqualTo(Input.OTHER);
        assertThat(SidePlacement.beltInput(Dir.NORTH, Dir.WEST, true)).isEqualTo(Input.BELT_SIDE);
        assertThat(SidePlacement.beltInput(Dir.SOUTH, Dir.WEST, true)).isEqualTo(Input.BELT_SIDE);
    }

    @Test
    void aCornerBesideThePickerSpotIsNotAnInput() {
        assertThat(SidePlacement.beltInput(Dir.NORTH, Dir.WEST, false)).isEqualTo(Input.OTHER);
        assertThat(SidePlacement.beltInput(Dir.SOUTH, Dir.WEST, false)).isEqualTo(Input.OTHER);
    }

    @Test
    void anOrdinaryBlockOrABeltFlowingElsewhereCannotFeedAPickerOrSplitter() {
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.OTHER, true, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.NO_INPUT);
        assertThat(decide(LogisticsIds.Kind.SPLITTER, EAST, Input.OTHER, true, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.NO_INPUT);
    }

    @Test
    void aBeltNeedsNoInput() {
        assertThat(decide(LogisticsIds.Kind.BELT, EAST, Input.OTHER, true, Space.FREE, PlacementReason.OK)).isEqualTo(Verdict.OK);
    }

    @Test
    void aSideFaceNeedsTheResearch() {
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.PORT, false, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.LOCKED);
    }

    @Test
    void theSideIsCheckedBeforeTheResearch() {
        assertThat(decide(LogisticsIds.Kind.PICKER, UP, Input.PORT, false, Space.FREE, PlacementReason.OK))
                .isEqualTo(Verdict.NOT_SIDE);
    }

    @Test
    void aSolidSpotIsRefusedAsNotAir() {
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.PORT, true, Space.SOLID, PlacementReason.OK))
                .isEqualTo(Verdict.SOLID);
    }

    @Test
    void anEntityInTheSpotIsRefused() {
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.PORT, true, Space.ENTITY, PlacementReason.OK))
                .isEqualTo(Verdict.ENTITY);
    }

    @Test
    void aDeviceOrMachineInTheSpotIsOccupied() {
        assertThat(decide(LogisticsIds.Kind.SPLITTER, EAST, Input.PORT, true, Space.DEVICE, PlacementReason.OK))
                .isEqualTo(Verdict.OCCUPIED);
    }

    @Test
    void theNetworkRefusalsAreKeptForAFreeSpot() {
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.PORT, true, Space.FREE, PlacementReason.BLOCKED))
                .isEqualTo(Verdict.BLOCKED);
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.PORT, true, Space.FREE, PlacementReason.OCCUPIED))
                .isEqualTo(Verdict.OCCUPIED);
        assertThat(decide(LogisticsIds.Kind.BELT, EAST, Input.OTHER, true, Space.FREE, PlacementReason.MERGE))
                .isEqualTo(Verdict.MERGE);
    }

    @Test
    void theNetworkIsNotAskedAboutASpotThatFailsEarlier() {
        Verdict verdict = SidePlacement.decide(LogisticsIds.Kind.PICKER, EAST, Input.OTHER, true, Space.FREE,
                () -> {
                    throw new AssertionError("the network must not be asked");
                });
        assertThat(verdict).isEqualTo(Verdict.NO_INPUT);
    }

    @Test
    void aLeftClickBreaksADeviceOnlyWhenTheHandHoldsNoLogisticsItem() {
        assertThat(SidePlacement.breaksOnLeftClick(false)).isTrue();
        assertThat(SidePlacement.breaksOnLeftClick(true)).isFalse();
    }

    /**
     * The reproduced cause. A client that sees air over a concealed machine sends its click to the block behind it. Its face toward
     * the player is the machine's own block, so a picker placed on that face lands inside the machine: the old placement refused
     * this with "That spot is taken". The spot is a solid block and the verdict is SOLID, never a placement.
     */
    @Test
    void aClickOnTheBlockBehindAMachineAimsIntoTheMachineAndIsRefusedAsSolid() {
        Pos behind = new Pos(-1, 64, 0);
        PlacementAim.Plan plan = PlacementAim.plan(LogisticsIds.Kind.PICKER, behind, EAST, 0.0).orElseThrow();
        assertThat(plan.target()).isEqualTo(CLICKED);
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.OTHER, true, Space.SOLID, PlacementReason.OK))
                .isEqualTo(Verdict.NO_INPUT);
        assertThat(decide(LogisticsIds.Kind.PICKER, EAST, Input.PORT, true, Space.SOLID, PlacementReason.OK))
                .isEqualTo(Verdict.SOLID);
    }
}
