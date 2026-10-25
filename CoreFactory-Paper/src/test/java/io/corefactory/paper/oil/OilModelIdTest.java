package io.corefactory.paper.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.OilModels;
import io.corefactory.core.oil.OilPart;
import io.corefactory.core.water.Face;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The model an oil tube is drawn with: every shape has an oil model id, and the id is in the oil model list. */
class OilModelIdTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-00000000000d");

    private static OilDevice device(OilPart part, Face face, Face output) {
        OilDevice device = new OilDevice("d", part, OWNER, WORLD, new Pos(0, 64, 0), face, 100.0);
        if (output != null) {
            device.setOutput(output);
        }
        return device;
    }

    @Test
    void aStraightPipeIsTheOilRun() {
        assertThat(OilEntities.modelId(withShown(EnumSet.of(Face.EAST, Face.WEST)))).isEqualTo("oil_pipe");
        assertThat(OilEntities.modelId(withShown(EnumSet.of(Face.NORTH, Face.SOUTH)))).isEqualTo("oil_pipe_z");
        assertThat(OilEntities.modelId(withShown(EnumSet.of(Face.UP, Face.DOWN)))).isEqualTo("oil_pipe_y");
    }

    @Test
    void aBendIsTheOilBendBetweenItsFaces() {
        assertThat(OilEntities.modelId(withShown(EnumSet.of(Face.WEST, Face.SOUTH)))).isEqualTo("oil_pipe_bend_south_west");
        assertThat(OilEntities.modelId(withShown(EnumSet.of(Face.NORTH, Face.DOWN)))).isEqualTo("oil_pipe_bend_north_down");
    }

    @Test
    void aJunctionIsTheOilHub() {
        assertThat(OilEntities.modelId(withShown(EnumSet.of(Face.NORTH, Face.EAST, Face.UP)))).isEqualTo("oil_pipe_hub");
    }

    @Test
    void aTeeIsTheOilTeeOfItsInputAndOutput() {
        assertThat(OilEntities.modelId(device(OilPart.TEE, Face.UP, Face.EAST))).isEqualTo("oil_splitter_t_up_east");
        assertThat(OilEntities.modelId(device(OilPart.TEE, Face.WEST, Face.NORTH))).isEqualTo("oil_splitter_t_west_north");
    }

    @Test
    void aCrossIsTheOilCrossOfItsPlane() {
        assertThat(OilEntities.modelId(device(OilPart.CROSS, Face.EAST, Face.UP))).isEqualTo("oil_splitter_x_xy");
        assertThat(OilEntities.modelId(device(OilPart.CROSS, Face.EAST, Face.SOUTH))).isEqualTo("oil_splitter_x_xz");
        assertThat(OilEntities.modelId(device(OilPart.CROSS, Face.NORTH, Face.UP))).isEqualTo("oil_splitter_x_zy");
    }

    @Test
    void everyShapeIsAnOilModelTheCodeNames() {
        Set<String> ids = Set.copyOf(OilModels.all());

        assertThat(ids).contains(
                OilEntities.modelId(withShown(EnumSet.of(Face.EAST, Face.WEST))),
                OilEntities.modelId(withShown(EnumSet.of(Face.WEST, Face.SOUTH))),
                OilEntities.modelId(withShown(EnumSet.of(Face.NORTH, Face.EAST, Face.UP))),
                OilEntities.modelId(device(OilPart.TEE, Face.UP, Face.EAST)),
                OilEntities.modelId(device(OilPart.CROSS, Face.EAST, Face.UP)));
    }

    @Test
    void everyOilModelIdIsTheWaterIdWithTheOilPrefix() {
        assertThat(OilModels.all()).allSatisfy(id -> assertThat(id).startsWith("oil_").doesNotStartWith("oil_pump"));
    }

    private static OilDevice withShown(Set<Face> faces) {
        OilDevice pipe = device(OilPart.PIPE, null, null);
        pipe.setShown(Set.copyOf(faces));
        return pipe;
    }
}
