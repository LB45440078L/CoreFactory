package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static io.corefactory.core.logistics.Fixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

class PlacementTest {

    private static final LogisticsLevel L = LogisticsLevel.I;

    private static PlacementReason check(int x, int z, Dir dir, List<BeltUnit> units) {
        return Placement.canPlace(at(x, z), dir, units, Endpoints.none());
    }

    private static BeltUnit belt(int x, int z, Dir dir) {
        return new BeltUnit(at(x, z), dir, L);
    }

    @Test
    void aFreeCellWithNoNeighboursIsOk() {
        assertThat(check(0, 0, Dir.EAST, List.of())).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aBeltBehindAStraightBeltFeedingItIsOk() {
        assertThat(check(-1, 0, Dir.EAST, List.of(belt(0, 0, Dir.EAST)))).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aBeltContinuingAnExistingBeltIsOk() {
        assertThat(check(1, 0, Dir.EAST, List.of(belt(0, 0, Dir.EAST)))).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aCellHoldingABeltIsOccupied() {
        assertThat(check(0, 0, Dir.NORTH, List.of(belt(0, 0, Dir.EAST)))).isEqualTo(PlacementReason.OCCUPIED);
    }

    @Test
    void aCellHoldingAPickerIsOccupied() {
        Endpoints endpoints = new Endpoints(
                List.of(new Picker("p", at(0, 0), Dir.WEST, Dir.EAST, L)), List.of(), Set.of());
        assertThat(Placement.canPlace(at(0, 0), Dir.EAST, List.of(), endpoints)).isEqualTo(PlacementReason.OCCUPIED);
    }

    @Test
    void aCellHoldingASplitterIsOccupied() {
        Endpoints endpoints = new Endpoints(
                List.of(), List.of(new Splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)), Set.of());
        assertThat(Placement.canPlace(at(0, 0), Dir.EAST, List.of(), endpoints)).isEqualTo(PlacementReason.OCCUPIED);
    }

    @Test
    void aCellHoldingAPortIsOccupied() {
        Endpoints endpoints = new Endpoints(List.of(), List.of(), Set.of(at(0, 0)));
        assertThat(Placement.canPlace(at(0, 0), Dir.EAST, List.of(), endpoints)).isEqualTo(PlacementReason.OCCUPIED);
    }

    @Test
    void aUnitThatWouldGiveAnExistingUnitASecondInputIsAMerge() {
        List<BeltUnit> units = List.of(belt(0, 0, Dir.WEST), belt(1, 0, Dir.WEST));
        assertThat(check(0, -1, Dir.SOUTH, units)).isEqualTo(PlacementReason.MERGE);
    }

    @Test
    void aUnitThatWouldTakeTwoInputsIsAMerge() {
        List<BeltUnit> units = List.of(belt(1, 0, Dir.WEST), belt(0, -1, Dir.SOUTH));
        assertThat(check(0, 0, Dir.WEST, units)).isEqualTo(PlacementReason.MERGE);
    }

    @Test
    void aUnitInFrontOfAnExistingUnitFlowingAtItIsBlocked() {
        List<BeltUnit> units = List.of(belt(0, 0, Dir.WEST));
        assertThat(check(-1, 0, Dir.EAST, units)).isEqualTo(PlacementReason.BLOCKED);
    }

    @Test
    void aUnitThatFlowsBackOntoItsOwnFeederIsBlocked() {
        List<BeltUnit> units = List.of(belt(1, 0, Dir.WEST));
        assertThat(check(0, 0, Dir.EAST, units)).isEqualTo(PlacementReason.BLOCKED);
    }

    @Test
    void aBeltFeedingIntoAPickersOutputCellIsBlocked() {
        Endpoints endpoints = new Endpoints(
                List.of(new Picker("p", at(0, 0), Dir.WEST, Dir.EAST, L)), List.of(), Set.of());
        assertThat(Placement.canPlace(at(1, 0), Dir.WEST, List.of(), endpoints)).isEqualTo(PlacementReason.BLOCKED);
    }

    @Test
    void aBeltFeedingAPickerFromANonInputSideIsBlocked() {
        Endpoints endpoints = new Endpoints(
                List.of(new Picker("p", at(0, 0), Dir.WEST, Dir.EAST, L)), List.of(), Set.of());
        assertThat(Placement.canPlace(at(0, 1), Dir.NORTH, List.of(), endpoints)).isEqualTo(PlacementReason.BLOCKED);
    }

    @Test
    void aBeltFeedingAPickersInputCellIsOk() {
        Endpoints endpoints = new Endpoints(
                List.of(new Picker("p", at(0, 0), Dir.WEST, Dir.EAST, L)), List.of(), Set.of());
        assertThat(Placement.canPlace(at(-1, 0), Dir.EAST, List.of(), endpoints)).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aBeltBehindAPickerFlowingAwayIsOk() {
        Endpoints endpoints = new Endpoints(
                List.of(new Picker("p", at(0, 0), Dir.WEST, Dir.EAST, L)), List.of(), Set.of());
        assertThat(Placement.canPlace(at(1, 0), Dir.EAST, List.of(), endpoints)).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aBeltFeedingASplitterFromTheSideIsBlocked() {
        Endpoints endpoints = new Endpoints(
                List.of(), List.of(new Splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)), Set.of());
        assertThat(Placement.canPlace(at(0, 1), Dir.NORTH, List.of(), endpoints)).isEqualTo(PlacementReason.BLOCKED);
    }

    @Test
    void aBeltFeedingASplitterFromItsBackIsOk() {
        Endpoints endpoints = new Endpoints(
                List.of(), List.of(new Splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)), Set.of());
        assertThat(Placement.canPlace(at(-1, 0), Dir.EAST, List.of(), endpoints)).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aBeltLeavingASplitterStraightIsOk() {
        Endpoints endpoints = new Endpoints(
                List.of(), List.of(new Splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)), Set.of());
        assertThat(Placement.canPlace(at(1, 0), Dir.EAST, List.of(), endpoints)).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aBeltLeavingASplitterLeftIsOk() {
        Endpoints endpoints = new Endpoints(
                List.of(), List.of(new Splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)), Set.of());
        assertThat(Placement.canPlace(at(0, -1), Dir.NORTH, List.of(), endpoints)).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aSecondFeederOfABeltFromTheSideIsAMergeEvenWhenTheFirstIsASplitter() {
        Endpoints endpoints = new Endpoints(
                List.of(), List.of(new Splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)), Set.of());
        List<BeltUnit> units = List.of(belt(1, 0, Dir.EAST));
        assertThat(Placement.canPlace(at(1, 1), Dir.NORTH, units, endpoints)).isEqualTo(PlacementReason.MERGE);
    }

    @Test
    void aBeltPointingIntoAPortIsOk() {
        Endpoints endpoints = new Endpoints(List.of(), List.of(), Set.of(at(1, 0)));
        assertThat(Placement.canPlace(at(0, 0), Dir.EAST, List.of(), endpoints)).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aBeltNextToABeltThatDoesNotFlowIntoItIsOk() {
        assertThat(check(0, 1, Dir.EAST, List.of(belt(0, 0, Dir.EAST)))).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aSecondInputAtAnExistingCornerIsAMergeAndNotAHeadOnFeed() {
        List<BeltUnit> units = List.of(belt(0, 0, Dir.EAST), belt(0, 1, Dir.NORTH));
        assertThat(check(1, 0, Dir.WEST, units)).isEqualTo(PlacementReason.MERGE);
    }

    @Test
    void aPickerBesideAStraightBeltFlowingPastItIsOk() {
        Picker picker = new Picker("p", at(0, 1), Dir.NORTH, Dir.SOUTH, L);
        assertThat(Placement.canPlacePicker(picker, List.of(belt(0, 0, Dir.EAST)), Endpoints.none()))
                .isEqualTo(PlacementReason.OK);
    }

    @Test
    void theReasonIsOkForAnEmptyNetworkAtAnyPosition() {
        assertThat(Placement.canPlace(new Pos(100, 60, -40), Dir.SOUTH, List.of(), Endpoints.none()))
                .isEqualTo(PlacementReason.OK);
    }
}
