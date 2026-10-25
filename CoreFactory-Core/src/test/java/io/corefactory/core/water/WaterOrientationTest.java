package io.corefactory.core.water;

import io.corefactory.core.water.WaterOrientation.PipeShape;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaterOrientationTest {

    @Test
    void aPipeWithNoConnectionIsStraightAlongX() {
        WaterOrientation.PipeModel model = WaterOrientation.pipe(Set.of());

        assertThat(model.shape()).isEqualTo(PipeShape.STRAIGHT);
        assertThat(model.frame().x()).isEqualTo(Face.EAST);
    }

    @Test
    void aStraightRunFollowsTheAxisWithTheMostConnections() {
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.EAST, Face.WEST)).frame().x()).isEqualTo(Face.EAST);
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.NORTH, Face.SOUTH)).frame().x()).isEqualTo(Face.SOUTH);
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.UP, Face.DOWN)).frame().x()).isEqualTo(Face.UP);
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.EAST, Face.NORTH, Face.SOUTH)).shape()).isEqualTo(PipeShape.JUNCTION);
        assertThat(WaterOrientation.pipe(EnumSet.of(Face.WEST, Face.SOUTH, Face.UP)).shape()).isEqualTo(PipeShape.JUNCTION);
    }

    @Test
    void everyConnectionOfAJunctionIsKept() {
        for (int mask = 0; mask < 64; mask++) {
            EnumSet<Face> faces = EnumSet.noneOf(Face.class);
            for (Face face : Face.values()) {
                if ((mask & (1 << face.ordinal())) != 0) {
                    faces.add(face);
                }
            }
            if (faces.size() >= 3) {
                assertThat(WaterOrientation.pipe(faces).shape()).as("%s", faces).isEqualTo(PipeShape.JUNCTION);
            }
        }
    }

    @Test
    void aStraightRunOnAHorizontalAxisHasItsUpAtTheTop() {
        for (Face axis : new Face[] {Face.EAST, Face.SOUTH}) {
            WaterFrame frame = WaterOrientation.pipe(EnumSet.of(axis, axis.opposite())).frame();
            assertThat(frame.y()).isEqualTo(Face.UP);
        }
    }

    @Test
    void aBendJoiningTwoHorizontalFacesLiesFlatWithItsUpAtTheTop() {
        WaterOrientation.PipeModel model = WaterOrientation.pipe(EnumSet.of(Face.WEST, Face.SOUTH));

        assertThat(model.shape()).isEqualTo(PipeShape.BEND);
        assertThat(model.frame().map(Face.WEST)).isEqualTo(Face.WEST);
        assertThat(model.frame().map(Face.SOUTH)).isEqualTo(Face.SOUTH);
        assertThat(model.frame().y()).isEqualTo(Face.UP);
    }

    @Test
    void everyPairOfHorizontalFacesIsABendOnItsEnds() {
        for (Face a : Face.values()) {
            for (Face b : Face.values()) {
                if (!a.horizontal() || !b.horizontal() || a == b || a == b.opposite()) {
                    continue;
                }
                WaterOrientation.PipeModel model = WaterOrientation.pipe(EnumSet.of(a, b));
                assertThat(model.shape()).as("%s and %s", a, b).isEqualTo(PipeShape.BEND);
                assertThat(model.frame().map(Face.WEST)).as("%s and %s", a, b).isIn(a, b);
                assertThat(model.frame().map(Face.SOUTH)).as("%s and %s", a, b).isIn(a, b);
                assertThat(model.frame().y()).as("%s and %s", a, b).isEqualTo(Face.UP);
            }
        }
    }

    @Test
    void aBendReachesAVerticalFace() {
        WaterOrientation.PipeModel model = WaterOrientation.pipe(EnumSet.of(Face.EAST, Face.UP));

        assertThat(model.shape()).isEqualTo(PipeShape.BEND);
        assertThat(model.frame().map(Face.WEST)).isIn(Face.EAST, Face.UP);
        assertThat(model.frame().map(Face.SOUTH)).isIn(Face.EAST, Face.UP);
        assertThat(model.frame().map(Face.WEST)).isNotEqualTo(model.frame().map(Face.SOUTH));
    }

    @Test
    void aBendReachesADownFace() {
        WaterOrientation.PipeModel model = WaterOrientation.pipe(EnumSet.of(Face.NORTH, Face.DOWN));

        assertThat(model.shape()).isEqualTo(PipeShape.BEND);
        assertThat(model.frame().map(Face.WEST)).isIn(Face.NORTH, Face.DOWN);
        assertThat(model.frame().map(Face.SOUTH)).isIn(Face.NORTH, Face.DOWN);
    }

    @Test
    void defaultOutputsLieAcrossTheInput() {
        assertThat(WaterOrientation.defaultOutput(Face.WEST)).isEqualTo(Face.SOUTH);
        assertThat(WaterOrientation.defaultOutput(Face.EAST)).isEqualTo(Face.SOUTH);
        assertThat(WaterOrientation.defaultOutput(Face.NORTH)).isEqualTo(Face.EAST);
        assertThat(WaterOrientation.defaultOutput(Face.SOUTH)).isEqualTo(Face.EAST);
        assertThatThrownBy(() -> WaterOrientation.defaultOutput(Face.UP)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void yawTurnsAHorizontalFaceToItsTarget() {
        assertThat(WaterOrientation.yawOf(Face.WEST, Face.WEST)).isZero();
        assertThat(WaterOrientation.yawOf(Face.WEST, Face.SOUTH)).isEqualTo(90);
        assertThat(WaterOrientation.yawOf(Face.WEST, Face.EAST)).isEqualTo(180);
        assertThat(WaterOrientation.yawOf(Face.WEST, Face.NORTH)).isEqualTo(270);
    }

    @Test
    void aYawNeedsHorizontalFaces() {
        assertThatThrownBy(() -> WaterOrientation.yawOf(Face.WEST, Face.UP)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theInputOfASplitterIsTheSideTheLookComesFrom() {
        assertThat(WaterOrientation.inputFromLook(0)).isEqualTo(Face.NORTH);
        assertThat(WaterOrientation.inputFromLook(90)).isEqualTo(Face.EAST);
        assertThat(WaterOrientation.inputFromLook(180)).isEqualTo(Face.SOUTH);
        assertThat(WaterOrientation.inputFromLook(-90)).isEqualTo(Face.WEST);
    }

    @Test
    void theDominantFaceIsTheLargestAxis() {
        assertThat(WaterOrientation.dominantFace(0, 1, 0)).isEqualTo(Face.UP);
        assertThat(WaterOrientation.dominantFace(0, -1, 0)).isEqualTo(Face.DOWN);
        assertThat(WaterOrientation.dominantFace(0.2, 0, -0.9)).isEqualTo(Face.NORTH);
        assertThat(WaterOrientation.dominantFace(-0.9, 0, 0.2)).isEqualTo(Face.WEST);
    }

    @Test
    void turnsAboutTheVerticalAxisKeepVerticalFaces() {
        assertThat(WaterOrientation.turn(Face.UP, 90)).isEqualTo(Face.UP);
        assertThat(WaterOrientation.turn(Face.WEST, -90)).isEqualTo(Face.NORTH);
        assertThat(WaterOrientation.turn(Face.NORTH, 450)).isEqualTo(Face.WEST);
    }
}
