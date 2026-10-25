package io.corefactory.paper.oil;

import io.corefactory.core.oil.OilPart;
import io.corefactory.core.water.Face;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** An oil tube goes on air only, and a splitter's input faces the way the player points, as a water splitter's does. */
class OilPlacementTest {

    @Test
    void anOilTubeGoesOnAir() {
        assertThat(OilPlacement.onAir(Material.AIR)).isTrue();
        assertThat(OilPlacement.onAir(Material.CAVE_AIR)).isTrue();
    }

    @Test
    void anOilTubeIsRefusedInWaterAndOnSolidBlocks() {
        assertThat(OilPlacement.onAir(Material.WATER)).isFalse();
        assertThat(OilPlacement.onAir(Material.STONE)).isFalse();
        assertThat(OilPlacement.onAir(Material.COPPER_GRATE)).isFalse();
    }

    @Test
    void aPipeHasNoInputOrOutput() {
        Face[] orientation = OilPlacement.orientation(OilPart.PIPE, null, 0f);

        assertThat(orientation).containsExactly(null, null);
    }

    @Test
    void aSplitterPlacedLookingSouthTakesItsInputFromTheSouth() {
        Face[] orientation = OilPlacement.orientation(OilPart.TEE, null, 0f);

        assertThat(orientation[0]).isEqualTo(Face.SOUTH);
        assertThat(orientation[1]).isEqualTo(Face.EAST);
    }

    @Test
    void aVerticalSplitterOnTopOfABlockTakesItsInputFromThatBlock() {
        Face[] orientation = OilPlacement.orientation(OilPart.CROSS, Face.UP, 0f);

        assertThat(orientation[0]).isEqualTo(Face.DOWN);
        assertThat(orientation[1]).isEqualTo(Face.EAST);
    }
}
