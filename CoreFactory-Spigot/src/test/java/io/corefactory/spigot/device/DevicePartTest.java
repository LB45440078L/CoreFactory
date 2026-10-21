package io.corefactory.spigot.device;

import io.corefactory.core.device.Footprint;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DevicePartTest {

    /** The devices by their anchor's coordinates, each a component id. A world with no server: only coordinates matter here. */
    private static final class Anchors {

        private final Map<String, String> ids = new HashMap<>();

        Anchors place(int x, int y, int z, String id) {
            ids.put(key(x, y, z), id);
            return this;
        }

        Optional<String> covering(int x, int y, int z) {
            Location block = new Location(null, x, y, z);
            return DevicePart.covering(block,
                    spot -> Optional.ofNullable(ids.get(key(spot.getBlockX(), spot.getBlockY(), spot.getBlockZ()))),
                    id -> Footprint.heightOf(id));
        }

        private static String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }
    }

    @Test
    void theAnchorOfATallDeviceMapsToItself() {
        assertThat(new Anchors().place(10, 64, 10, "ore_drill_4").covering(10, 64, 10)).contains("ore_drill_4");
    }

    @Test
    void theBlockAboveTheAnchorOfATallDeviceMapsToTheDevice() {
        assertThat(new Anchors().place(10, 64, 10, "nuclear_generator").covering(10, 65, 10)).contains("nuclear_generator");
    }

    @Test
    void theBlockTwoAboveATallDeviceIsNotCovered() {
        assertThat(new Anchors().place(10, 64, 10, "nuclear_generator").covering(10, 66, 10)).isEmpty();
    }

    @Test
    void theBlockBelowATallDeviceIsNotCovered() {
        assertThat(new Anchors().place(10, 64, 10, "nuclear_generator").covering(10, 63, 10)).isEmpty();
    }

    @Test
    void aShortDeviceCoversOnlyItsOwnBlock() {
        Anchors anchors = new Anchors().place(10, 64, 10, "solar_panel");
        assertThat(anchors.covering(10, 64, 10)).contains("solar_panel");
        assertThat(anchors.covering(10, 65, 10)).isEmpty();
    }

    @Test
    void theBlockAboveAShortPylonBelongsToNoDevice() {
        assertThat(new Anchors().place(10, 64, 10, "pylon_2").covering(10, 65, 10)).isEmpty();
    }

    @Test
    void anotherColumnIsNotCovered() {
        Anchors anchors = new Anchors().place(10, 64, 10, "ore_drill_5");
        assertThat(anchors.covering(11, 65, 10)).isEmpty();
        assertThat(anchors.covering(10, 65, 11)).isEmpty();
    }

    @Test
    void aDeviceAnchoredAboveATallOneIsTheDeviceOfThatBlock() {
        Anchors anchors = new Anchors().place(10, 64, 10, "ore_drill_4").place(10, 65, 10, "diesel_generator");
        assertThat(anchors.covering(10, 65, 10)).contains("diesel_generator");
    }

    @Test
    void aTallDeviceAboveAShortOneOwnsItsOwnUpperBlock() {
        Anchors anchors = new Anchors().place(10, 64, 10, "ore_drill_1").place(10, 65, 10, "charcoal_generator");
        assertThat(anchors.covering(10, 65, 10)).contains("charcoal_generator");
        assertThat(anchors.covering(10, 64, 10)).contains("ore_drill_1");
    }

    @Test
    void blocksOfATallDeviceAreTheAnchorAndTheBlockAboveIt() {
        List<Location> blocks = DevicePart.blocksOf(new Location(null, 3, 70, -4), 2);
        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0).getBlockY()).isEqualTo(70);
        assertThat(blocks.get(1).getBlockY()).isEqualTo(71);
        assertThat(blocks.get(1).getBlockX()).isEqualTo(3);
        assertThat(blocks.get(1).getBlockZ()).isEqualTo(-4);
    }

    @Test
    void blocksOfAShortDeviceIsItsAnchorOnly() {
        List<Location> blocks = DevicePart.blocksOf(new Location(null, 3, 70, -4), 1);
        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).getBlockY()).isEqualTo(70);
    }
}
