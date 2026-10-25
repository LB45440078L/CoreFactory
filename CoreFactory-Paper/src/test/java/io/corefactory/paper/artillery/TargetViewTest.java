package io.corefactory.paper.artillery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Checks the map's view: the pixel and ground mapping, the zoom steps, and the centre's clamp to the world border. */
class TargetViewTest {

    @Test
    void theDefaultRadiusGivesOneBlockPerPixelAtTheClosestZoom() {
        assertThat(TargetView.blocksPerPixel(1.0, 64.0)).isCloseTo(1.0, within(1e-9));
        assertThat(TargetView.blocksPerPixel(4.0, 64.0)).isCloseTo(4.0, within(1e-9));
        assertThat(TargetView.spanBlocks(2.0)).isCloseTo(256.0, within(1e-9));
    }

    @Test
    void aPixelOnTheGroundAndBackIsTheSamePixel() {
        TargetView view = new TargetView(100.5, -37.25, 0);
        for (int px = 0; px < TargetView.PIXELS; px += 7) {
            for (int py = 0; py < TargetView.PIXELS; py += 5) {
                double bpp = 2.0;
                assertThat(view.pixelX(view.worldX(px, bpp), bpp)).isEqualTo(px);
                assertThat(view.pixelZ(view.worldZ(py, bpp), bpp)).isEqualTo(py);
            }
        }
    }

    @Test
    void theCentreOfTheViewFallsOnThePixelAtTheMiddle() {
        TargetView view = new TargetView(10.0, 20.0, 0);
        assertThat(view.pixelX(10.0, 1.0)).isEqualTo(TargetView.PIXELS / 2);
        assertThat(view.pixelZ(20.0, 1.0)).isEqualTo(TargetView.PIXELS / 2);
    }

    @Test
    void aPointOutsideTheMapGivesAPixelOutsideIt() {
        TargetView view = new TargetView(0, 0, 0);
        assertThat(view.pixelX(-65.0, 1.0)).isLessThan(0);
        assertThat(view.pixelX(64.0, 1.0)).isGreaterThanOrEqualTo(TargetView.PIXELS);
    }

    @Test
    void zoomStepsStayWithinTheConfiguredLevels() {
        TargetView view = new TargetView(0, 0, 0);
        assertThat(view.zoomedIn(3).zoomIndex()).isEqualTo(0);
        assertThat(view.zoomedOut(3).zoomIndex()).isEqualTo(1);
        assertThat(view.zoomedOut(3).zoomedOut(3).zoomedOut(3).zoomIndex()).isEqualTo(2);
        assertThat(new TargetView(0, 0, 2).zoomedOut(3).zoomIndex()).isEqualTo(2);
        assertThat(new TargetView(0, 0, 1).zoomedIn(3).zoomIndex()).isEqualTo(0);
    }

    @Test
    void zoomKeepsTheCentre() {
        TargetView view = new TargetView(12.0, -8.0, 0).zoomedOut(3);
        assertThat(view.centreX()).isEqualTo(12.0);
        assertThat(view.centreZ()).isEqualTo(-8.0);
    }

    @Test
    void theCentreIsHeldInsideTheWorldBorder() {
        TargetView view = new TargetView(5_000.0, -5_000.0, 0).clampedTo(-100.0, 100.0, -200.0, 200.0);
        assertThat(view.centreX()).isEqualTo(100.0);
        assertThat(view.centreZ()).isEqualTo(-200.0);
        TargetView inside = new TargetView(3.0, 4.0, 1).clampedTo(-100.0, 100.0, -200.0, 200.0);
        assertThat(inside.centreX()).isEqualTo(3.0);
        assertThat(inside.zoomIndex()).isEqualTo(1);
    }

    @Test
    void movingTheCentreMovesItOnBothAxes() {
        TargetView view = new TargetView(0, 0, 0).moved(16.0, -4.0);
        assertThat(view.centreX()).isEqualTo(16.0);
        assertThat(view.centreZ()).isEqualTo(-4.0);
    }
}
