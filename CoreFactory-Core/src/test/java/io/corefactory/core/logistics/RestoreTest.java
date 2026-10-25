package io.corefactory.core.logistics;

import io.corefactory.core.process.ItemRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.corefactory.core.logistics.Fixtures.A;
import static io.corefactory.core.logistics.Fixtures.B;
import static io.corefactory.core.logistics.Fixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

class RestoreTest {

    private static final LogisticsLevel L = LogisticsLevel.I;

    @Test
    void anItemRestoredAtAPositionKeepsItsPlaceAndMovesOn() {
        LogisticsNetwork network = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L).belt(at(1, 0), Dir.EAST, L).build();
        assertThat(network.insertAt(at(0, 0), A, 0.5)).isTrue();
        assertThat(network.itemsOn(at(0, 0)).get(0).position()).isEqualTo(0.5);
        assertThat(network.itemCount()).isEqualTo(1);
    }

    @Test
    void aRestoredItemTooCloseToAnotherIsRefused() {
        LogisticsNetwork network = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L).build();
        assertThat(network.insertAt(at(0, 0), A, 0.5)).isTrue();
        assertThat(network.insertAt(at(0, 0), B, 0.6)).isFalse();
        assertThat(network.insertAt(at(0, 0), B, 0.25)).isTrue();
        assertThat(network.itemsOn(at(0, 0))).hasSize(2);
        assertThat(network.itemsOn(at(0, 0)).get(0).position()).isEqualTo(0.5);
    }

    @Test
    void aRestoredItemOutsideTheBeltIsRefused() {
        LogisticsNetwork network = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L).build();
        assertThat(network.insertAt(at(0, 0), A, 1.5)).isFalse();
        assertThat(network.insertAt(at(0, 0), A, -0.1)).isFalse();
    }

    @Test
    void theSerialOfAnItemStaysTheSameAsItMoves() {
        LogisticsNetwork network = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L).belt(at(1, 0), Dir.EAST, L).build();
        network.insertAtEntry(at(0, 0), A);
        List<Long> before = network.serialsOn(at(0, 0));
        for (int t = 0; t < 5; t++) {
            network.tick(0.05, Map.of(), Map.of());
        }
        assertThat(network.serialsOn(at(0, 0))).isEqualTo(before);
        assertThat(network.serialsOn(at(0, 0))).hasSize(1);
    }

    @Test
    void aSplitterHoldsAnItemOnlyWhenItHoldsNone() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L).build();
        assertThat(network.heldBy("s")).isEmpty();
        assertThat(network.holdAt("s", A)).isTrue();
        assertThat(network.heldBy("s")).contains(A);
        assertThat(network.holdAt("s", B)).isFalse();
        assertThat(network.itemCount()).isEqualTo(1);
    }

    @Test
    void aPickerPlacementInAnOccupiedCellIsRefused() {
        Picker held = new Picker("p", at(0, 0), Dir.WEST, Dir.EAST, L);
        Endpoints endpoints = new Endpoints(List.of(held), List.of(), Set.of());
        Picker same = new Picker("q", at(0, 0), Dir.WEST, Dir.EAST, L);
        assertThat(Placement.canPlacePicker(same, List.of(), endpoints)).isEqualTo(PlacementReason.OCCUPIED);
    }

    @Test
    void aPickerFedFromTheWrongSideIsBlocked() {
        Picker candidate = new Picker("p", at(0, 0), Dir.WEST, Dir.EAST, L);
        List<BeltUnit> units = List.of(new BeltUnit(at(0, 1), Dir.NORTH, L));
        assertThat(Placement.canPlacePicker(candidate, units, Endpoints.none())).isEqualTo(PlacementReason.BLOCKED);
    }

    @Test
    void aPickerWithABeltAtItsInputIsOk() {
        Picker candidate = new Picker("p", at(0, 0), Dir.WEST, Dir.EAST, L);
        List<BeltUnit> units = List.of(new BeltUnit(at(-1, 0), Dir.EAST, L));
        assertThat(Placement.canPlacePicker(candidate, units, Endpoints.none())).isEqualTo(PlacementReason.OK);
    }

    @Test
    void aSplitterFedFromItsBackIsOkAndFromTheSideIsBlocked() {
        Splitter candidate = new Splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L);
        assertThat(Placement.canPlaceSplitter(candidate, List.of(new BeltUnit(at(-1, 0), Dir.EAST, L)), Endpoints.none()))
                .isEqualTo(PlacementReason.OK);
        assertThat(Placement.canPlaceSplitter(candidate, List.of(new BeltUnit(at(0, 1), Dir.NORTH, L)), Endpoints.none()))
                .isEqualTo(PlacementReason.BLOCKED);
    }

    @Test
    void aPortCellIsOccupiedForAPicker() {
        Picker candidate = new Picker("p", at(0, 0), Dir.WEST, Dir.EAST, L);
        Endpoints endpoints = new Endpoints(List.of(), List.of(), Set.of(at(0, 0)));
        assertThat(Placement.canPlacePicker(candidate, List.of(), endpoints)).isEqualTo(PlacementReason.OCCUPIED);
    }

    @Test
    void aMaterialItemMapsToAMaterialRefAndBack() {
        assertThat(PortItems.refOf(Item.material("CHARCOAL"))).isEqualTo(ItemRef.material("CHARCOAL"));
        assertThat(PortItems.itemOf(ItemRef.material("CHARCOAL"))).isEqualTo(Item.material("CHARCOAL"));
    }

    @Test
    void aComponentItemMapsToAComponentRefAndBack() {
        assertThat(PortItems.refOf(Item.component("raw_galena"))).isEqualTo(ItemRef.item("raw_galena"));
        assertThat(PortItems.itemOf(ItemRef.item("raw_galena"))).isEqualTo(Item.component("raw_galena"));
    }
}
