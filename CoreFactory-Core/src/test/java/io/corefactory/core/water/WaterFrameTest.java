package io.corefactory.core.water;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaterFrameTest {

    @Test
    void theIdentityMapsEveryFaceToItself() {
        WaterFrame identity = WaterFrame.identity();

        for (Face face : Face.values()) {
            assertThat(identity.map(face)).isEqualTo(face);
        }
    }

    @Test
    void aFrameIsRightHanded() {
        assertThat(WaterFrame.of(Face.EAST, Face.SOUTH).y()).isEqualTo(Face.UP);
        assertThat(WaterFrame.of(Face.SOUTH, Face.EAST).y()).isEqualTo(Face.DOWN);
    }

    @Test
    void aMirroredFrameIsRefused() {
        assertThatThrownBy(() -> new WaterFrame(Face.EAST, Face.DOWN, Face.SOUTH)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theAxesMustBeThreeLines() {
        assertThatThrownBy(() -> new WaterFrame(Face.EAST, Face.WEST, Face.SOUTH)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WaterFrame.of(Face.EAST, Face.WEST)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aStraightRunAlongAVerticalFaceMapsItsXToThatFace() {
        assertThat(WaterFrame.straight(Face.UP).x()).isEqualTo(Face.UP);
        assertThat(WaterFrame.straight(Face.UP).map(Face.WEST)).isEqualTo(Face.DOWN);
        assertThat(WaterFrame.straight(Face.DOWN).x()).isEqualTo(Face.DOWN);
    }

    @Test
    void aBendMapsItsEndsToTheFacesItJoins() {
        WaterFrame bend = WaterFrame.bend(Face.EAST, Face.UP);

        assertThat(bend.map(Face.WEST)).isIn(Face.EAST, Face.UP);
        assertThat(bend.map(Face.SOUTH)).isIn(Face.EAST, Face.UP);
        assertThat(bend.map(Face.WEST)).isNotEqualTo(bend.map(Face.SOUTH));
    }

    @Test
    void aBendOfOppositeFacesIsRefused() {
        assertThatThrownBy(() -> WaterFrame.bend(Face.EAST, Face.WEST)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aVerticalTeeTakesItsInputFromBelowAndSendsWaterAcrossTheOutput() {
        WaterFrame tee = WaterFrame.tee(Face.DOWN, Face.EAST);

        assertThat(tee.map(Face.WEST)).isEqualTo(Face.DOWN);
        assertThat(tee.map(Face.EAST)).isEqualTo(Face.UP);
        assertThat(tee.map(Face.SOUTH)).isEqualTo(Face.EAST);
        assertThat(tee.map(Face.NORTH)).isEqualTo(Face.WEST);
    }

    @Test
    void aTeeWithTheInputOnTheOutputsLineIsRefused() {
        assertThatThrownBy(() -> WaterFrame.tee(Face.EAST, Face.WEST)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aCrossMapsItsFourArmsToTheFacesOfItsPlane() {
        WaterFrame cross = WaterFrame.cross(Face.UP, Face.EAST);

        assertThat(cross.map(Face.WEST)).isEqualTo(Face.DOWN);
        assertThat(cross.map(Face.SOUTH)).isEqualTo(Face.EAST);
        assertThat(cross.map(Face.NORTH)).isEqualTo(Face.WEST);
    }
}
