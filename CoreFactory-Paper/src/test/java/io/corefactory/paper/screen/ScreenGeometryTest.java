package io.corefactory.paper.screen;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.water.Face;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ScreenGeometryTest {

    private static final Pos ANCHOR = new Pos(10, 64, 20);
    private static final double EPS = 1e-9;
    private static final double HALF_PI = Math.PI / 2;

    @Test
    void aNorthFrontCentresThePanelBeyondTheNorthFace() {
        ScreenGeometry.Layout layout = ScreenGeometry.of(ANCHOR, Face.NORTH, 1);

        assertThat(layout.panel().x()).isCloseTo(10.5, within(EPS));
        assertThat(layout.panel().y()).isCloseTo(64.5, within(EPS));
        assertThat(layout.panel().z()).isCloseTo(20.5 - 0.525, within(EPS));
        assertThat(layout.yaw()).isCloseTo(Math.PI, within(EPS));
        assertThat(layout.text().z()).isCloseTo(20.5 - 0.54, within(EPS));
        assertThat(layout.text().x()).isCloseTo(10.5, within(EPS));
    }

    @Test
    void aSouthFrontCentresThePanelBeyondTheSouthFace() {
        ScreenGeometry.Layout layout = ScreenGeometry.of(ANCHOR, Face.SOUTH, 1);

        assertThat(layout.panel().x()).isCloseTo(10.5, within(EPS));
        assertThat(layout.panel().y()).isCloseTo(64.5, within(EPS));
        assertThat(layout.panel().z()).isCloseTo(20.5 + 0.525, within(EPS));
        assertThat(layout.yaw()).isCloseTo(0.0, within(EPS));
        assertThat(layout.text().z()).isCloseTo(20.5 + 0.54, within(EPS));
    }

    @Test
    void anEastFrontCentresThePanelBeyondTheEastFace() {
        ScreenGeometry.Layout layout = ScreenGeometry.of(ANCHOR, Face.EAST, 1);

        assertThat(layout.panel().x()).isCloseTo(10.5 + 0.525, within(EPS));
        assertThat(layout.panel().z()).isCloseTo(20.5, within(EPS));
        assertThat(layout.yaw()).isCloseTo(HALF_PI, within(EPS));
        assertThat(layout.text().x()).isCloseTo(10.5 + 0.54, within(EPS));
    }

    @Test
    void aWestFrontCentresThePanelBeyondTheWestFace() {
        ScreenGeometry.Layout layout = ScreenGeometry.of(ANCHOR, Face.WEST, 1);

        assertThat(layout.panel().x()).isCloseTo(10.5 - 0.525, within(EPS));
        assertThat(layout.panel().z()).isCloseTo(20.5, within(EPS));
        assertThat(layout.yaw()).isCloseTo(-HALF_PI, within(EPS));
        assertThat(layout.text().x()).isCloseTo(10.5 - 0.54, within(EPS));
    }

    @Test
    void theBezelsBackFaceLiesAtOrInsideTheBlockFace() {
        for (Face front : new Face[] {Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST}) {
            ScreenGeometry.Layout layout = ScreenGeometry.of(ANCHOR, front, 1);
            Pos step = front.step(new Pos(0, 0, 0));
            double alongFront = (layout.bezel().x() - (ANCHOR.x() + 0.5)) * step.x()
                    + (layout.bezel().z() - (ANCHOR.z() + 0.5)) * step.z();
            double backFace = alongFront - ScreenGeometry.FRAME_THICKNESS / 2;

            assertThat(backFace).as(front.name()).isLessThanOrEqualTo(0.5 + EPS);
        }
    }

    @Test
    void aTallDeviceCentresThePanelBetweenItsTwoBlocks() {
        ScreenGeometry.Layout layout = ScreenGeometry.of(ANCHOR, Face.SOUTH, 2);

        assertThat(layout.panel().y()).isCloseTo(64 + 1.0, within(EPS));
        assertThat(layout.text().y()).isCloseTo(64 + 1.0, within(EPS));
    }

    @Test
    void thePanelsPlusZAxisPointsAlongTheFront() {
        for (Face front : new Face[] {Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST}) {
            ScreenGeometry.Layout layout = ScreenGeometry.of(ANCHOR, front, 1);
            ScreenGeometry.Vec axis = ScreenGeometry.rotateY(layout.yaw(), new ScreenGeometry.Vec(0, 0, 1));
            Pos step = front.step(new Pos(0, 0, 0));

            assertThat(axis.x()).as(front.name()).isCloseTo(step.x(), within(EPS));
            assertThat(axis.y()).as(front.name()).isCloseTo(0.0, within(EPS));
            assertThat(axis.z()).as(front.name()).isCloseTo(step.z(), within(EPS));
        }
    }

    @Test
    void theTranslationPutsThePanelsCentreOnTheEntityPoint() {
        for (Face front : new Face[] {Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST}) {
            ScreenGeometry.Layout layout = ScreenGeometry.of(ANCHOR, front, 2);
            Vector3d centre = new Vector3d(ScreenGeometry.WIDTH / 2, ScreenGeometry.HEIGHT / 2, ScreenGeometry.THICKNESS / 2);
            Vector3d turned = new Quaterniond().rotationY(layout.yaw()).transform(new Vector3d(centre));
            ScreenGeometry.Vec translation = layout.translation();

            assertThat(turned.x + translation.x()).as(front.name()).isCloseTo(0.0, within(EPS));
            assertThat(turned.y + translation.y()).as(front.name()).isCloseTo(0.0, within(EPS));
            assertThat(turned.z + translation.z()).as(front.name()).isCloseTo(0.0, within(EPS));
        }
    }

    @Test
    void aVerticalFrontIsRefused() {
        assertThatThrownBy(() -> ScreenGeometry.of(ANCHOR, Face.UP, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
