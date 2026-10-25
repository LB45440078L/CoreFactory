package io.corefactory.paper.water;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpFuel;
import io.corefactory.core.water.WaterModels;
import io.corefactory.core.water.WaterPart;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The model a placed water block is drawn with: its orientation is in its id, so no display turn is needed. */
class WaterModelIdTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static WaterDevice device(WaterPart part, Face face, Face output, int level) {
        WaterDevice device = new WaterDevice("d", part, OWNER, WORLD, new Pos(0, 64, 0), face, level, PumpFuel.Burn.empty());
        if (output != null) {
            device.setOutput(output);
        }
        return device;
    }

    @Test
    void aPipeWithTwoFacesIsTheBendBetweenThem() {
        assertThat(WaterEntities.pipeModelId(EnumSet.of(Face.WEST, Face.SOUTH))).isEqualTo("water_pipe_bend_south_west");
        assertThat(WaterEntities.pipeModelId(EnumSet.of(Face.NORTH, Face.EAST))).isEqualTo("water_pipe_bend_north_east");
        assertThat(WaterEntities.pipeModelId(EnumSet.of(Face.NORTH, Face.DOWN))).isEqualTo("water_pipe_bend_north_down");
    }

    @Test
    void aStraightPipeIsNamedByItsRun() {
        assertThat(WaterEntities.pipeModelId(EnumSet.of(Face.EAST, Face.WEST))).isEqualTo("water_pipe");
        assertThat(WaterEntities.pipeModelId(EnumSet.of(Face.NORTH, Face.SOUTH))).isEqualTo("water_pipe_z");
        assertThat(WaterEntities.pipeModelId(EnumSet.of(Face.UP, Face.DOWN))).isEqualTo("water_pipe_y");
    }

    @Test
    void aJunctionIsTheHub() {
        assertThat(WaterEntities.pipeModelId(EnumSet.of(Face.NORTH, Face.EAST, Face.UP))).isEqualTo(WaterModels.HUB);
    }

    @Test
    void aDevicesModelIsItsOwnModelId() {
        WaterDevice pipe = device(WaterPart.PIPE, null, null, 1);
        pipe.setShown(Set.of(Face.SOUTH, Face.WEST));
        assertThat(WaterEntities.modelId(pipe)).isEqualTo("water_pipe_bend_south_west");

        assertThat(WaterEntities.modelId(device(WaterPart.PUMP, Face.EAST, null, 3))).isEqualTo("water_pump_3_east");
        assertThat(WaterEntities.topModelKey(device(WaterPart.PUMP, Face.EAST, null, 4))).endsWith("water_pump_4_east_top");
        assertThat(WaterEntities.modelId(device(WaterPart.TEE, Face.UP, Face.EAST, 1))).isEqualTo("water_splitter_t_up_east");
        assertThat(WaterEntities.modelId(device(WaterPart.CROSS, Face.EAST, Face.UP, 1))).isEqualTo("water_splitter_x_xy");
    }

    @Test
    void everyIdADeviceCanGetIsOneOfTheWaterModels() {
        Set<String> all = Set.copyOf(WaterModels.all());
        for (Face a : Face.values()) {
            for (Face b : Face.values()) {
                if (a != b && a != b.opposite()) {
                    assertThat(all).contains(WaterEntities.pipeModelId(EnumSet.of(a, b)));
                }
            }
        }
        for (int level = 1; level <= 5; level++) {
            for (Face front : new Face[] {Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST}) {
                assertThat(all).contains(WaterModels.pump(level, front));
            }
        }
    }
}
