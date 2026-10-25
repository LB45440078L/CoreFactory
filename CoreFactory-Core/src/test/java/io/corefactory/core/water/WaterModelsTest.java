package io.corefactory.core.water;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaterModelsTest {

    @Test
    void theIdListHasOneModelForEveryOrientation() {
        assertThat(WaterModels.all()).hasSize(65);
        assertThat(new HashSet<>(WaterModels.all())).hasSize(65);
    }

    @Test
    void aBendIsNamedByItsTwoFacesWhicheverOrderTheyAreGiven() {
        assertThat(WaterModels.bend(Face.SOUTH, Face.WEST)).isEqualTo("water_pipe_bend_south_west");
        assertThat(WaterModels.bend(Face.WEST, Face.SOUTH)).isEqualTo("water_pipe_bend_south_west");
        assertThat(WaterModels.bend(Face.NORTH, Face.EAST)).isEqualTo("water_pipe_bend_north_east");
        assertThat(WaterModels.bend(Face.DOWN, Face.NORTH)).isEqualTo("water_pipe_bend_north_down");
        assertThatThrownBy(() -> WaterModels.bend(Face.EAST, Face.WEST)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTeeIsNamedByItsInputAndTheEarlierOfItsOutputs() {
        assertThat(WaterModels.tee(Face.UP, Face.EAST)).isEqualTo("water_splitter_t_up_east");
        assertThat(WaterModels.tee(Face.UP, Face.WEST)).isEqualTo("water_splitter_t_up_east");
        assertThat(WaterModels.tee(Face.WEST, Face.NORTH)).isEqualTo("water_splitter_t_west_north");
        assertThatThrownBy(() -> WaterModels.tee(Face.EAST, Face.WEST)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aCrossIsNamedByItsPlane() {
        assertThat(WaterModels.cross(Face.EAST, Face.SOUTH)).isEqualTo("water_splitter_x_xz");
        assertThat(WaterModels.cross(Face.EAST, Face.UP)).isEqualTo("water_splitter_x_xy");
        assertThat(WaterModels.cross(Face.NORTH, Face.UP)).isEqualTo("water_splitter_x_zy");
    }

    @Test
    void aStraightPipeIsNamedByItsAxis() {
        assertThat(WaterModels.straight(Face.WEST)).isEqualTo("water_pipe");
        assertThat(WaterModels.straight(Face.NORTH)).isEqualTo("water_pipe_z");
        assertThat(WaterModels.straight(Face.DOWN)).isEqualTo("water_pipe_y");
    }

    @Test
    void aPumpAndItsTopAreNamedByLevelAndFront() {
        assertThat(WaterModels.pump(3, Face.NORTH)).isEqualTo("water_pump_3_north");
        assertThat(WaterModels.pumpTop(4, Face.EAST)).isEqualTo("water_pump_4_east_top");
        assertThatThrownBy(() -> WaterModels.pump(3, Face.UP)).isInstanceOf(IllegalArgumentException.class);
    }
}
