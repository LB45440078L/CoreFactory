package io.corefactory.paper.artillery;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Checks the map wall's frames, the tile each pixel belongs to, and the hit of a view on the wall and the button under it. */
class WallLayoutTest {

    /** The wall of three frames for a center at block (10, 64, 20): frames at x 9 to 11, y 65 to 67, z 18; barriers at z 19. */
    private static final WallLayout THREE = WallLayout.of(CommandCenterGeometry.screen(), 10, 64, 20, 3);

    /** The wall of five frames for the same center: frames at x 8 to 12, y 65 to 69, z 18. */
    private static final WallLayout FIVE = WallLayout.of(CommandCenterGeometry.screen(), 10, 64, 20, 5);

    @Test
    void theWallIsCentredAcrossTheBlockAndOneBlockFromTheFace() {
        assertThat(THREE.x0()).isEqualTo(9);
        assertThat(THREE.y0()).isEqualTo(65);
        assertThat(THREE.z0()).isEqualTo(18);
        assertThat(THREE.backingZ()).isEqualTo(19);
        assertThat(THREE.planeZ()).isCloseTo(19 - 1.0 / 16, within(1e-9));
        assertThat(20 - THREE.planeZ()).isBetween(1.0, 1.1);
        assertThat(FIVE.x0()).isEqualTo(8);
        assertThat(FIVE.y0()).isEqualTo(65);
    }

    @Test
    void theLowestRowIsAboveTheCentreBlockSoTheWallNeverReachesTheGround() {
        for (WallLayout layout : List.of(THREE, FIVE)) {
            for (int i = 0; i < layout.frames(); i++) {
                assertThat(layout.frameY(i)).isGreaterThan(64);
            }
        }
    }

    @Test
    void theFramesAreNByNOneBlockApartAndOnThePlane() {
        for (WallLayout layout : List.of(THREE, FIVE)) {
            int tiles = layout.tiles();
            Set<String> cells = new HashSet<>();
            for (int i = 0; i < layout.frames(); i++) {
                cells.add(layout.frameX(i) + ":" + layout.frameY(i));
                assertThat(layout.covers(layout.frameX(i), layout.frameY(i), layout.z0())).isTrue();
                assertThat(layout.covers(layout.frameX(i), layout.frameY(i), layout.backingZ())).isTrue();
                assertThat(layout.planeZ()).isGreaterThan(layout.z0()).isLessThan(layout.backingZ());
                if (layout.column(i) > 0) {
                    assertThat(layout.frameX(i - 1) - layout.frameX(i)).isEqualTo(1);
                }
                if (layout.row(i) > 0) {
                    assertThat(layout.frameY(i - tiles) - layout.frameY(i)).isEqualTo(1);
                }
            }
            assertThat(cells).hasSize(tiles * tiles);
            assertThat(layout.frames()).isEqualTo(tiles * tiles);
            assertThat(layout.pixels()).isEqualTo(tiles * TargetView.PIXELS);
        }
    }

    @Test
    void theViewersLeftIsTheEastAndTheTopRowIsFirst() {
        assertThat(THREE.frameX(0)).isEqualTo(11);
        assertThat(THREE.frameY(0)).isEqualTo(67);
        assertThat(THREE.frameX(2)).isEqualTo(9);
        assertThat(THREE.frameY(8)).isEqualTo(65);
    }

    @Test
    void aPixelBelongsToExactlyOneTileAndTheTileIsRight() {
        int side = THREE.pixels();
        int[] counts = new int[THREE.frames()];
        int misplaced = 0;
        for (int py = 0; py < side; py++) {
            for (int px = 0; px < side; px++) {
                int tile = THREE.tileOf(px, py);
                counts[tile]++;
                if (THREE.column(tile) != px / TargetView.PIXELS || THREE.row(tile) != py / TargetView.PIXELS) {
                    misplaced++;
                }
            }
        }
        assertThat(misplaced).isZero();
        for (int count : counts) {
            assertThat(count).isEqualTo(TargetView.PIXELS * TargetView.PIXELS);
        }
        assertThat(THREE.tileOf(0, 0)).isEqualTo(0);
        assertThat(THREE.tileOf(127, 127)).isEqualTo(0);
        assertThat(THREE.tileOf(128, 0)).isEqualTo(1);
        assertThat(THREE.tileOf(0, 128)).isEqualTo(3);
        assertThat(THREE.tileOf(383, 383)).isEqualTo(8);
    }

    @Test
    void aRayThroughTheMiddleOfTheWallHitsTheMiddlePixel() {
        // The wall's middle is at x 10.5 and y 66.5. From the north, looking south, the ray meets the wall's plane there.
        Optional<int[]> pixel = THREE.pixelUnder(10.5, 66.5, 15.0, 0, 0, 1);
        assertThat(pixel).isPresent();
        assertThat(pixel.get()[0]).isEqualTo(192);
        assertThat(pixel.get()[1]).isEqualTo(192);
    }

    @Test
    void aRayThatMissesTheWallHitsNoPixel() {
        assertThat(THREE.pixelUnder(12.5, 66.5, 15.0, 0, 0, 1)).isEmpty();
        assertThat(THREE.pixelUnder(10.5, 68.5, 15.0, 0, 0, 1)).isEmpty();
        assertThat(THREE.pixelUnder(10.5, 64.5, 15.0, 0, 0, 1)).isEmpty();
        assertThat(THREE.pixelUnder(8.9, 66.5, 15.0, 0, 0, 1)).isEmpty();
    }

    @Test
    void aRayFromBehindParallelOrPastThePlaneHitsNothing() {
        assertThat(THREE.pixelUnder(10.5, 66.5, 25.0, 0, 0, -1)).isEmpty();
        assertThat(THREE.pixelUnder(10.5, 66.5, 15.0, 1, 0, 0)).isEmpty();
        assertThat(THREE.pixelUnder(10.5, 66.5, 25.0, 0, 0, 1)).isEmpty();
    }

    @Test
    void theCornersOfTheWallAreTheCornerPixels() {
        // The top left of the wall, as the viewer sees it, is at the east and the top.
        Optional<int[]> top = THREE.pixelUnder(12 - 1e-6, 68 - 1e-6, 15.0, 0, 0, 1);
        assertThat(top).isPresent();
        assertThat(top.get()).containsExactly(0, 0);
        Optional<int[]> bottom = THREE.pixelUnder(9 + 1e-6, 65 + 1e-6, 15.0, 0, 0, 1);
        assertThat(bottom).isPresent();
        assertThat(bottom.get()).containsExactly(383, 383);
    }

    @Test
    void aPixelOfTheWallIsTheButtonUnderItAtKnownPoints() {
        // The map's buttons are on the 128-pixel map, and the wall shows that map at three times its size.
        assertThat(THREE.buttonAt(337, 13)).contains(TargetButton.ZOOM_IN);
        assertThat(THREE.buttonAt(13, 13)).contains(TargetButton.COORDINATES);
        assertThat(THREE.buttonAt(300, 370)).contains(TargetButton.CONFIRM);
        assertThat(FIVE.buttonAt(150, 450)).contains(TargetButton.NORTH);
        assertThat(THREE.buttonAt(192, 192)).isEmpty();
        assertThat(THREE.buttonAt(372, 13)).isEmpty();
        assertThat(THREE.buttonAt(-1, 13)).isEmpty();
    }

    @Test
    void aRayOnTheZoomInButtonGivesZoomIn() {
        // The pixel 337, 13 is on the zoom in button: its ground point is found from the pixel's middle.
        double x = 12 - 337.5 * 3 / 384.0;
        double y = 68 - 13.5 * 3 / 384.0;
        Optional<TargetButton> button = THREE.pixelUnder(x, y, 15.0, 0, 0, 1).flatMap(p -> THREE.buttonAt(p[0], p[1]));
        assertThat(button).contains(TargetButton.ZOOM_IN);
    }
}
