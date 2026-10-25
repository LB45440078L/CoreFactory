package io.corefactory.paper.artillery;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.function.Supplier;

/**
 * Draws one tile of a Command Center's map wall on the map of one item frame. The canvas of the wall is shared by all its tiles, and this
 * renderer only copies its own tile from it when the server asks the map to render, so a redraw of the wall costs a copy of the pixels. A
 * wall with no canvas yet leaves its map blank.
 */
final class WallTileRenderer extends MapRenderer {

    private final int tiles;
    private final int column;
    private final int row;
    private final Supplier<byte[]> canvas;

    /**
     * @param tiles  the side of the wall, in tiles
     * @param column the tile's column in the wall, from the left
     * @param row    the tile's row in the wall, from the top
     * @param canvas the wall's canvas: its pixels, row by row, {@code tiles} x 128 pixels a side, or null when there is none yet
     */
    WallTileRenderer(int tiles, int column, int row, Supplier<byte[]> canvas) {
        this.tiles = tiles;
        this.column = column;
        this.row = row;
        this.canvas = canvas;
    }

    @Override
    public void render(MapView map, MapCanvas target, Player player) {
        byte[] pixels = canvas.get();
        if (pixels == null) {
            return;
        }
        int size = tiles * TargetView.PIXELS;
        int originX = column * TargetView.PIXELS;
        int originY = row * TargetView.PIXELS;
        for (int py = 0; py < TargetView.PIXELS; py++) {
            for (int px = 0; px < TargetView.PIXELS; px++) {
                target.setPixel(px, py, pixels[(originY + py) * size + originX + px]);
            }
        }
    }
}
