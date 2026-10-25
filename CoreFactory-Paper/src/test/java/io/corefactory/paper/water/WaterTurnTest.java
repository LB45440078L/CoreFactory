package io.corefactory.paper.water;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpFuel;
import io.corefactory.core.water.WaterPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Every water display is turned by a half turn about the vertical axis, so a bend's ends meet the faces they name. */
class WaterTurnTest {

    @Test
    void everyWaterDisplayIsTurnedHalfWayAboutTheVerticalAxis() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-00000000000c");
        WaterDevice pipe = new WaterDevice("p", WaterPart.PIPE, id, id, new Pos(0, 64, 0), null, 1, PumpFuel.Burn.empty());
        WaterDevice pump = new WaterDevice("q", WaterPart.PUMP, id, id, new Pos(0, 64, 0), Face.EAST, 3, PumpFuel.Burn.empty());
        for (WaterDevice device : new WaterDevice[] {pipe, pump}) {
            Quaternionf turn = WaterEntities.transformOf(device).getLeftRotation();
            Vector3f east = turn.transform(new Vector3f(1, 0, 0));
            assertThat(east.x).isCloseTo(-1f, org.assertj.core.data.Offset.offset(1e-5f));
            assertThat(east.z).isCloseTo(0f, org.assertj.core.data.Offset.offset(1e-5f));
        }
    }
}
