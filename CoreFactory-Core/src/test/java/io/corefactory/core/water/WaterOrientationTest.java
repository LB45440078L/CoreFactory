package io.corefactory.core.water;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaterOrientationTest {

    @Test
    void aPipeWithNoConnectionIsStraightAlongX() {
        assertThat(WaterOrientation.pipe(Set.of())).isEqualTo(new WaterOrientation.PipeModel(WaterOrientation.PipeShape.STRAIGHT_X, 0, 0));
    }

    @Test
    void aPipeOnOppositeEastWestFacesIsTheModelAsDrawn() {
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.EAST, Face.WEST)))
                .isEqualTo(new WaterOrientation.PipeModel(WaterOrientation.PipeShape.STRAIGHT_X, 0, 0));
    }

    @Test
    void aPipeOnNorthSouthFacesIsStraightAlongZWithAYawOfNinety() {
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.NORTH, Face.SOUTH)))
                .isEqualTo(new WaterOrientation.PipeModel(WaterOrientation.PipeShape.STRAIGHT_Z, 90, 0));
    }

    @Test
    void aPipeOnUpAndDownFacesIsVerticalWithARollOfNinety() {
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.UP, Face.DOWN)))
                .isEqualTo(new WaterOrientation.PipeModel(WaterOrientation.PipeShape.STRAIGHT_Y, 0, 90));
    }

    @Test
    void aCornerOfTwoHorizontalFacesIsABendTurnedToItsOpenEnds() {
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.WEST, Face.SOUTH)))
                .isEqualTo(new WaterOrientation.PipeModel(WaterOrientation.PipeShape.BEND, 0, 0));
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.SOUTH, Face.EAST)))
                .isEqualTo(new WaterOrientation.PipeModel(WaterOrientation.PipeShape.BEND, 90, 0));
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.EAST, Face.NORTH)))
                .isEqualTo(new WaterOrientation.PipeModel(WaterOrientation.PipeShape.BEND, 180, 0));
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.NORTH, Face.WEST)))
                .isEqualTo(new WaterOrientation.PipeModel(WaterOrientation.PipeShape.BEND, 270, 0));
    }

    @Test
    void aBendNeedsItsCornerWithNoVerticalConnection() {
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.WEST, Face.SOUTH, Face.UP)).shape())
                .isEqualTo(WaterOrientation.PipeShape.STRAIGHT_X);
    }

    @Test
    void aPipeWithAThirdConnectionTakesTheAxisWithTheMostOfThem() {
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.EAST, Face.WEST, Face.NORTH)).shape())
                .isEqualTo(WaterOrientation.PipeShape.STRAIGHT_X);
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.EAST, Face.NORTH, Face.SOUTH)).shape())
                .isEqualTo(WaterOrientation.PipeShape.STRAIGHT_Z);
    }

    @Test
    void aTieOfAxesPrefersEastWestThenNorthSouth() {
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.EAST, Face.UP)).shape())
                .isEqualTo(WaterOrientation.PipeShape.STRAIGHT_X);
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.NORTH, Face.UP)).shape())
                .isEqualTo(WaterOrientation.PipeShape.STRAIGHT_Z);
    }

    @Test
    void aYawTurnsTheWestModelFaceToTheTarget() {
        assertThat(WaterOrientation.yawOf(Face.WEST, Face.WEST)).isZero();
        assertThat(WaterOrientation.yawOf(Face.WEST, Face.SOUTH)).isEqualTo(90);
        assertThat(WaterOrientation.yawOf(Face.WEST, Face.EAST)).isEqualTo(180);
        assertThat(WaterOrientation.yawOf(Face.WEST, Face.NORTH)).isEqualTo(270);
    }

    @Test
    void aYawOfAVerticalFaceIsRefused() {
        assertThatThrownBy(() -> WaterOrientation.yawOf(Face.WEST, Face.UP)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aBendYawFindsTheQuarterTurnOfItsEnds() {
        assertThat(WaterOrientation.bendYaw(EnumSet.of(Face.WEST, Face.SOUTH))).isZero();
        assertThat(WaterOrientation.bendYaw(EnumSet.of(Face.SOUTH, Face.EAST))).isEqualTo(90);
        assertThat(WaterOrientation.bendYaw(EnumSet.of(Face.EAST, Face.NORTH))).isEqualTo(180);
        assertThat(WaterOrientation.bendYaw(EnumSet.of(Face.NORTH, Face.WEST))).isEqualTo(270);
    }

    @Test
    void aBendRefusesOppositeOrVerticalEnds() {
        assertThatThrownBy(() -> WaterOrientation.bendYaw(EnumSet.of(Face.EAST, Face.WEST)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WaterOrientation.bendYaw(EnumSet.of(Face.UP, Face.EAST)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theInputOfASplitterFacesThePlayerWhoPlacesIt() {
        assertThat(WaterOrientation.inputFromLook(0)).isEqualTo(Face.NORTH);
        assertThat(WaterOrientation.inputFromLook(90)).isEqualTo(Face.EAST);
        assertThat(WaterOrientation.inputFromLook(180)).isEqualTo(Face.SOUTH);
        assertThat(WaterOrientation.inputFromLook(-90)).isEqualTo(Face.WEST);
    }

    @Test
    void theDominantFaceFollowsTheLargestComponent() {
        assertThat(WaterOrientation.dominantFace(0, 1, 0)).isEqualTo(Face.UP);
        assertThat(WaterOrientation.dominantFace(0, -1, 0)).isEqualTo(Face.DOWN);
        assertThat(WaterOrientation.dominantFace(0.2, 0, -0.9)).isEqualTo(Face.NORTH);
        assertThat(WaterOrientation.dominantFace(-0.9, 0, 0.2)).isEqualTo(Face.WEST);
    }

    @Test
    void turningAVerticalFaceLeavesItAlone() {
        assertThat(WaterOrientation.turn(Face.UP, 90)).isEqualTo(Face.UP);
        assertThat(WaterOrientation.turn(Face.WEST, -90)).isEqualTo(Face.NORTH);
        assertThat(WaterOrientation.turn(Face.NORTH, 450)).isEqualTo(Face.WEST);
    }
}
