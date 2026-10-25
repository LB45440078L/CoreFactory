package io.corefactory.core.item;

import io.corefactory.core.item.PlaceableItems.Kind;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** The placeable plugin items. Nothing here starts a server. */
class PlaceableItemsTest {

    private static final Set<String> MACHINES = Set.of("crusher", "converter", "petroleum_drill_1", "ore_drill_1");

    private static boolean machine(String id) {
        return MACHINES.contains(id);
    }

    @Test
    void aTntTypeAndAnOreBlockItemPlaceThroughTheirOwnPlacement() {
        assertThat(PlaceableItems.allowed(Kind.TNT, "nuke_little_boy", PlaceableItemsTest::machine)).isTrue();
        assertThat(PlaceableItems.allowed(Kind.ORE, "uraninite_ore", PlaceableItemsTest::machine)).isTrue();
    }

    @Test
    void aMachineComponentPlacesItsHost() {
        assertThat(PlaceableItems.allowed(Kind.COMPONENT, "crusher", PlaceableItemsTest::machine)).isTrue();
        assertThat(PlaceableItems.allowed(Kind.COMPONENT, "petroleum_drill_1", PlaceableItemsTest::machine)).isTrue();
    }

    @Test
    void theSolarPanelIsAnEnergyDeviceAndNotADaylightDetector() {
        assertThat(PlaceableItems.allowed(Kind.COMPONENT, PlaceableItems.SOLAR_PANEL, PlaceableItemsTest::machine)).isTrue();
        assertThat(PlaceableItems.placedByHand(true, Kind.COMPONENT)).isFalse();
    }

    @Test
    void thePylonsAccumulatorsAndGeneratorsArePlaceableAndTheWireIsNot() {
        for (String id : PlaceableItems.ENERGY_DEVICES) {
            assertThat(PlaceableItems.allowed(Kind.COMPONENT, id, PlaceableItemsTest::machine)).as(id).isTrue();
        }
        assertThat(PlaceableItems.ENERGY_DEVICES).contains("wind_generator", "charcoal_generator", "petroleum_generator",
                "diesel_generator", "nuclear_generator");
        assertThat(PlaceableItems.ENERGY_DEVICES).contains("pylon_4", "pylon_5", "accumulator_4", "accumulator_5");
        assertThat(PlaceableItems.allowed(Kind.COMPONENT, "energy_wire", PlaceableItemsTest::machine)).isFalse();
    }

    @Test
    void aComponentThatIsNotAMachineIsRefused() {
        assertThat(PlaceableItems.allowed(Kind.COMPONENT, "neutron_initiator", PlaceableItemsTest::machine)).isFalse();
        assertThat(PlaceableItems.allowed(Kind.COMPONENT, "pulse_capacitor", PlaceableItemsTest::machine)).isFalse();
        assertThat(PlaceableItems.allowed(Kind.COMPONENT, "fuse_linker", PlaceableItemsTest::machine)).isFalse();
    }

    @Test
    void aPlainItemIsNotRestrictedHere() {
        assertThat(PlaceableItems.allowed(null, null, PlaceableItemsTest::machine)).isTrue();
    }

    @Test
    void aPlainBlockCanBePlacedByHandButNoPluginBlock() {
        assertThat(PlaceableItems.placedByHand(true, null)).isTrue();
        assertThat(PlaceableItems.placedByHand(true, Kind.COMPONENT)).isFalse();
        assertThat(PlaceableItems.placedByHand(true, Kind.TNT)).isFalse();
        assertThat(PlaceableItems.placedByHand(true, Kind.ORE)).isFalse();
    }

    @Test
    void aNonBlockIsNeverPlacedByHand() {
        assertThat(PlaceableItems.placedByHand(false, null)).isFalse();
        assertThat(PlaceableItems.placedByHand(false, Kind.COMPONENT)).isFalse();
    }
}
