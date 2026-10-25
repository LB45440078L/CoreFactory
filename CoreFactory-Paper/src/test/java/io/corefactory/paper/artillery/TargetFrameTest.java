package io.corefactory.paper.artillery;

import org.bukkit.map.MapPalette;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the map's pixels: the terrain of loaded and unloaded columns, the red centre mark, and the buttons laid over the frame. */
class TargetFrameTest {

    private static final int SIZE = TargetView.PIXELS;

    /** A flat surface of one colour, with the columns of x below zero not loaded. */
    private static final TargetFrame.Surface FLAT = (x, z) -> x < 0 ? -1 : (MapPalette.GRAY_1 & 0xFF);

    @Test
    void anUnloadedColumnIsDrawnDark() {
        TargetView view = new TargetView(0, 0, 0);
        byte[] terrain = TargetFrame.terrain(FLAT, view, 1.0, MapPalette.DARK_GRAY);
        assertThat(terrain[32 * SIZE + 0]).isEqualTo(MapPalette.DARK_GRAY);
        assertThat(terrain[32 * SIZE + SIZE - 1]).isEqualTo(MapPalette.GRAY_1);
    }

    @Test
    void theCentreMarkIsRedAtTheCentreOfTheMap() {
        TargetView view = new TargetView(0, 0, 0);
        byte[] frame = TargetFrame.compose(TargetFrame.terrain(FLAT, view, 1.0, MapPalette.DARK_GRAY), view, 1.0, null, false, null);
        int centre = SIZE / 2;
        assertThat(frame[centre * SIZE + centre]).isEqualTo(MapPalette.RED);
        assertThat(frame[(centre + TargetFrame.MARK_OUTLINE) * SIZE + centre]).isEqualTo(MapPalette.WHITE);
    }

    @Test
    void theButtonsAreDrawnOnlyWhenAsked() {
        TargetView view = new TargetView(0, 0, 0);
        byte[] terrain = TargetFrame.terrain(FLAT, view, 1.0, MapPalette.DARK_GRAY);
        // A pixel in the button's corner, clear of its label.
        TargetButton zoomIn = TargetButton.ZOOM_IN;
        int index = (zoomIn.y0() + 1) * SIZE + zoomIn.x0() + 1;
        byte without = TargetFrame.compose(terrain, view, 1.0, null, false, null)[index];
        byte with = TargetFrame.compose(terrain, view, 1.0, null, true, null)[index];
        assertThat(without).isEqualTo(MapPalette.GRAY_1);
        assertThat(with).isEqualTo(MapPalette.LIGHT_GRAY);
    }

    @Test
    void aHoveredButtonIsWhite() {
        TargetView view = new TargetView(0, 0, 0);
        byte[] terrain = TargetFrame.terrain(FLAT, view, 1.0, MapPalette.DARK_GRAY);
        TargetButton cancel = TargetButton.CANCEL;
        byte[] frame = TargetFrame.compose(terrain, view, 1.0, null, true, cancel);
        assertThat(frame[(cancel.y0() + 1) * SIZE + cancel.x0() + 1]).isEqualTo(MapPalette.WHITE);
    }

    @Test
    void aButtonLabelIsWrittenInTheButton() {
        TargetView view = new TargetView(0, 0, 0);
        byte[] terrain = TargetFrame.terrain(FLAT, view, 1.0, MapPalette.DARK_GRAY);
        byte[] frame = TargetFrame.compose(terrain, view, 1.0, null, true, null);
        TargetButton coordinates = TargetButton.COORDINATES;
        boolean labelled = false;
        for (int py = coordinates.y0() + 1; py < coordinates.y1() - 1 && !labelled; py++) {
            for (int px = coordinates.x0() + 1; px < coordinates.x1() - 1 && !labelled; px++) {
                labelled = frame[py * SIZE + px] == MapPalette.DARK_GRAY;
            }
        }
        assertThat(labelled).isTrue();
    }

    @Test
    void theMapScaledUpRepeatsEachPixelInASquare() {
        byte[] map = new byte[SIZE * SIZE];
        map[0] = MapPalette.RED;
        map[SIZE - 1] = MapPalette.WHITE;
        byte[] wall = TargetFrame.upscale(map, 3);

        int wide = SIZE * 3;
        assertThat(wall).hasSize(wide * wide);
        assertThat(wall[0]).isEqualTo(MapPalette.RED);
        assertThat(wall[2 * wide + 2]).isEqualTo(MapPalette.RED);
        assertThat(wall[3]).isEqualTo(map[1]);
        assertThat(wall[wide - 1]).isEqualTo(MapPalette.WHITE);
        assertThat(wall[2 * wide + wide - 1]).isEqualTo(MapPalette.WHITE);
        assertThat(wall[3 * wide]).isEqualTo(map[SIZE]);
    }

    @Test
    void composingDoesNotChangeTheTerrain() {
        TargetView view = new TargetView(0, 0, 0);
        byte[] terrain = TargetFrame.terrain(FLAT, view, 1.0, MapPalette.DARK_GRAY);
        byte[] copy = terrain.clone();
        TargetFrame.compose(terrain, view, 1.0, null, true, TargetButton.NORTH);
        assertThat(terrain).isEqualTo(copy);
    }
}
