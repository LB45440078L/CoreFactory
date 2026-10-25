package io.corefactory.paper.artillery;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Checks the ray's cut by the screen's plane: a known ray hits the known point, a parallel or a backward ray hits nothing. */
class ScreenRayTest {

    private static final double PLANE_Z = 10.125;

    @Test
    void aRayStraightSouthHitsThePlaneAtTheSameXAndHeight() {
        // From the north, looking south: the eye is at z 8, and the plane is at z 10.125, two and an eighth blocks ahead.
        Optional<double[]> hit = ScreenRay.hitPlane(3.5, 6.0, 8.0, 0, 0, 1, PLANE_Z);
        assertThat(hit).isPresent();
        assertThat(hit.get()[0]).isCloseTo(3.5, within(1e-9));
        assertThat(hit.get()[1]).isCloseTo(6.0, within(1e-9));
    }

    @Test
    void aSlantedRayHitsThePlaneAtTheKnownPoint() {
        // The ray runs down one block for every two it goes south, and sideways one for every two.
        Optional<double[]> hit = ScreenRay.hitPlane(0.0, 4.0, 8.0, 0.5, -0.5, 1.0, PLANE_Z);
        assertThat(hit).isPresent();
        double t = (PLANE_Z - 8.0) / 1.0;
        assertThat(hit.get()[0]).isCloseTo(0.5 * t, within(1e-9));
        assertThat(hit.get()[1]).isCloseTo(4.0 - 0.5 * t, within(1e-9));
    }

    @Test
    void aRayParallelToThePlaneHitsNothing() {
        assertThat(ScreenRay.hitPlane(0.0, 0.0, 8.0, 1, 0, 0, PLANE_Z)).isEmpty();
        assertThat(ScreenRay.hitPlane(0.0, 0.0, 8.0, 1, 2, 0, PLANE_Z)).isEmpty();
    }

    @Test
    void aRayPointingAwayFromThePlaneHitsNothing() {
        assertThat(ScreenRay.hitPlane(0.0, 0.0, 8.0, 0, 0, -1, PLANE_Z)).isEmpty();
    }

    @Test
    void aRayFromTheSouthSideHitsNothing() {
        // The screen faces north: a ray from behind it, travelling north, does not read it.
        assertThat(ScreenRay.hitPlane(0.0, 0.0, 12.0, 0, 0, -1, PLANE_Z)).isEmpty();
    }

    @Test
    void aPlaneBehindTheEyeHitsNothing() {
        assertThat(ScreenRay.hitPlane(0.0, 0.0, 12.0, 0, 0, 1, PLANE_Z)).isEmpty();
    }

    @Test
    void theScreensCentreIsTheMiddlePixel() {
        CommandCenterGeometry.Screen screen = CommandCenterGeometry.screen();
        int blockX = 20;
        int blockY = 64;
        int blockZ = 10;
        double x = blockX + screen.centreX();
        double y = blockY + screen.centreY();
        Optional<int[]> pixel = ScreenRay.pixelUnder(x, y, blockZ - 2.0, 0, 0, 1, blockX, blockY, blockZ, screen);
        assertThat(pixel).isPresent();
        assertThat(pixel.get()[0]).isCloseTo(TargetView.PIXELS / 2, within(1));
        assertThat(pixel.get()[1]).isCloseTo(TargetView.PIXELS / 2, within(1));
    }

    @Test
    void theTopLeftOfTheScreenIsPixelZeroZero() {
        CommandCenterGeometry.Screen screen = CommandCenterGeometry.screen();
        int blockX = 0;
        int blockY = 0;
        int blockZ = 0;
        double x = screen.x0() + 1e-6;
        double y = screen.y1() - 1e-6;
        Optional<int[]> pixel = ScreenRay.pixelUnder(x, y, -2.0, 0, 0, 1, blockX, blockY, blockZ, screen);
        assertThat(pixel).isPresent();
        assertThat(pixel.get()[0]).isEqualTo(0);
        assertThat(pixel.get()[1]).isEqualTo(0);
    }

    @Test
    void aRayThatMissesTheScreenRectangleHitsNoPixel() {
        CommandCenterGeometry.Screen screen = CommandCenterGeometry.screen();
        // Above the screen's top edge, and beside its left edge.
        assertThat(ScreenRay.pixelUnder(0.5, screen.y1() + 0.2, -2.0, 0, 0, 1, 0, 0, 0, screen)).isEmpty();
        assertThat(ScreenRay.pixelUnder(screen.x0() - 0.2, 0.5, -2.0, 0, 0, 1, 0, 0, 0, screen)).isEmpty();
    }

    @Test
    void theHitPixelIsInsideTheMap() {
        CommandCenterGeometry.Screen screen = CommandCenterGeometry.screen();
        Optional<int[]> pixel = ScreenRay.pixelUnder(screen.x1() - 1e-9, screen.y0() + 1e-9, -2.0, 0, 0, 1, 0, 0, 0, screen);
        assertThat(pixel).isPresent();
        assertThat(pixel.get()[0]).isBetween(0, TargetView.PIXELS - 1);
        assertThat(pixel.get()[1]).isBetween(0, TargetView.PIXELS - 1);
    }
}
