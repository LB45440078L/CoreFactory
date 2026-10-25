package io.corefactory.paper.water;

import io.corefactory.core.water.Face;
import io.corefactory.core.water.WaterPart;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** A splitter's input faces the way the player points, so it connects in front of the player, not behind. */
class WaterPlacementOrientationTest {

    @Test
    void aSplitterPlacedLookingSouthTakesItsInputFromTheSouth() {
        Face[] orientation = WaterPlacement.orientation(WaterPart.TEE, null, 0f);

        assertThat(orientation[0]).isEqualTo(Face.SOUTH);
    }

    @Test
    void aPumpStillFacesThePlayer() {
        Face[] orientation = WaterPlacement.orientation(WaterPart.PUMP, null, 0f);

        assertThat(orientation[0]).isEqualTo(Face.NORTH);
    }
}
