package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryEffects.Column;
import io.corefactory.core.artillery.GroundPoint;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MinecraftFont;

import java.util.HashSet;
import java.util.Set;

/**
 * The pixels of a Command Center's map: the terrain around the view's centre, the strike area of the shell, the red centre mark and the
 * buttons with their labels. The map is {@link TargetView#PIXELS} by {@link TargetView#PIXELS} pixels; the wall of item frames shows it
 * scaled up by {@link #upscale}. The terrain is drawn once and kept, and the rest is laid over a copy of it at each redraw. Colours are map
 * palette bytes, and the surface is read through {@link Surface}, so the drawing is tested without a server.
 */
public final class TargetFrame {

    /** The red centre mark's half width in pixels: the mark fills 7 by 7 pixels around the centre. */
    static final int MARK_HALF = 3;

    /** The white outline of the centre mark, one pixel beyond the red. */
    static final int MARK_OUTLINE = MARK_HALF + 1;

    /** The font of the button labels: the map's own font. */
    private static final MapFont FONT = MinecraftFont.Font;

    /** The colour of the button labels. */
    private static final byte LABEL = MapPalette.DARK_GRAY;

    /** The pixel row of a label's top, below the top of its button. */
    private static final int LABEL_TOP = 3;

    /** The colour of the terrain under a point: the loaded world, or the terrain kept for a chunk that is not loaded. */
    public interface Surface {

        /**
         * The map colour of the top of a column.
         *
         * @param x the block x of the column
         * @param z the block z of the column
         * @return the palette byte as 0 to 255, or -1 when the column's chunk is not loaded
         */
        int colourAt(int x, int z);
    }

    private TargetFrame() {
    }

    /**
     * The terrain of the map: the surface colour of each pixel's column. A column the surface gives no colour for, -1, is drawn in the given colour.
     *
     * @param surface    the surface the colours are read from
     * @param view       the view, for the centre
     * @param bpp        the blocks per pixel
     * @param unloaded   the colour of a column that is not loaded
     * @return the pixels, row by row, as {@link TargetView#PIXELS} by {@link TargetView#PIXELS} bytes
     */
    public static byte[] terrain(Surface surface, TargetView view, double bpp, byte unloaded) {
        int size = TargetView.PIXELS;
        byte[] pixels = new byte[size * size];
        for (int py = 0; py < size; py++) {
            int z = (int) Math.floor(view.worldZ(py, bpp));
            for (int px = 0; px < size; px++) {
                int x = (int) Math.floor(view.worldX(px, bpp));
                int colour = surface.colourAt(x, z);
                pixels[py * size + px] = colour < 0 ? unloaded : (byte) colour;
            }
        }
        return pixels;
    }

    /**
     * The map's pixels for one redraw: a copy of the terrain with the strike area, the centre mark and, when asked, the buttons laid over it.
     *
     * @param terrain  the terrain, from {@link #terrain}
     * @param view     the view, for the centre and the pixels' ground
     * @param bpp      the blocks per pixel
     * @param overlay  the strike area, or null when no shell is chosen
     * @param buttons  true to draw the buttons, for the player's own map
     * @param hover    the button under the player's view, highlighted, or null
     * @return the pixels, a new array
     */
    public static byte[] compose(byte[] terrain, TargetView view, double bpp, TargetOverlay overlay, boolean buttons,
                                 TargetButton hover) {
        byte[] out = terrain.clone();
        if (overlay != null) {
            drawOverlay(out, view, bpp, overlay);
        }
        drawCentreMark(out);
        if (buttons) {
            drawButtons(out, hover);
        }
        return out;
    }

    private static void drawOverlay(byte[] out, TargetView view, double bpp, TargetOverlay overlay) {
        int size = TargetView.PIXELS;
        Set<Column> napalm = new HashSet<>(overlay.napalm());
        Set<Column> phosphorus = new HashSet<>(overlay.phosphorus());
        double half = bpp / 2;
        for (int py = 0; py < size; py++) {
            double z = view.worldZ(py, bpp);
            int columnZ = (int) Math.floor(z);
            for (int px = 0; px < size; px++) {
                double x = view.worldX(px, bpp);
                Column column = new Column((int) Math.floor(x), columnZ);
                int index = py * size + px;
                if (phosphorus.contains(column)) {
                    out[index] = MapPalette.LIGHT_GRAY;
                }
                if (napalm.contains(column)) {
                    out[index] = MapPalette.BROWN;
                }
                double distance = Math.hypot(x - overlay.aimX(), z - overlay.aimZ());
                if (Math.abs(distance - overlay.reachBlocks()) <= half) {
                    out[index] = MapPalette.RED;
                }
            }
        }
        for (GroundPoint point : overlay.submunitions()) {
            int px = view.pixelX(point.x(), bpp);
            int py = view.pixelZ(point.z(), bpp);
            for (int dy = 0; dy <= 1; dy++) {
                for (int dx = 0; dx <= 1; dx++) {
                    setIfInside(out, px + dx, py + dy, MapPalette.WHITE);
                }
            }
        }
    }

    private static void drawCentreMark(byte[] out) {
        int centre = TargetView.PIXELS / 2;
        for (int dy = -MARK_OUTLINE; dy <= MARK_OUTLINE; dy++) {
            for (int dx = -MARK_OUTLINE; dx <= MARK_OUTLINE; dx++) {
                boolean outline = Math.max(Math.abs(dx), Math.abs(dy)) == MARK_OUTLINE;
                setIfInside(out, centre + dx, centre + dy, outline ? MapPalette.WHITE : MapPalette.RED);
            }
        }
    }

    /**
     * The map scaled up by a whole factor: each pixel of the map becomes a square of factor by factor pixels. The wall of item frames shows
     * its map this way, so the buttons and the labels keep their places on the map.
     *
     * @param map    the map's pixels, square, row by row
     * @param factor the factor, at least 1
     * @return the scaled pixels, a new array
     */
    public static byte[] upscale(byte[] map, int factor) {
        int side = (int) Math.round(Math.sqrt(map.length));
        int size = side * factor;
        byte[] out = new byte[size * size];
        for (int y = 0; y < size; y++) {
            int row = (y / factor) * side;
            for (int x = 0; x < size; x++) {
                out[y * size + x] = map[row + x / factor];
            }
        }
        return out;
    }

    private static void drawButtons(byte[] out, TargetButton hover) {
        for (TargetButton button : TargetButton.values()) {
            byte fill = fillOf(button, button == hover);
            for (int py = button.y0(); py < button.y1(); py++) {
                for (int px = button.x0(); px < button.x1(); px++) {
                    boolean border = px == button.x0() || px == button.x1() - 1 || py == button.y0() || py == button.y1() - 1;
                    setIfInside(out, px, py, border ? MapPalette.DARK_GRAY : fill);
                }
            }
        }
        drawTick(out, TargetButton.CONFIRM, MapPalette.WHITE);
        drawLabels(out);
    }

    /** The labels of the buttons that have one, centred on each button, in the map's own font. */
    private static void drawLabels(byte[] out) {
        for (TargetButton button : TargetButton.values()) {
            if (button.label().isEmpty()) {
                continue;
            }
            int width = FONT.getWidth(button.label());
            int x = button.x0() + (button.x1() - button.x0() - width) / 2;
            drawText(out, x, button.y0() + LABEL_TOP, button.label());
        }
    }

    /** A line of text in the map's font, with its top-left pixel at the given pixel. Each character is one sprite, then a pixel of space. */
    private static void drawText(byte[] out, int x, int y, String text) {
        int left = x;
        for (char c : text.toCharArray()) {
            MapFont.CharacterSprite sprite = FONT.getChar(c);
            if (sprite == null) {
                continue;
            }
            for (int row = 0; row < sprite.getHeight(); row++) {
                for (int col = 0; col < sprite.getWidth(); col++) {
                    if (sprite.get(row, col)) {
                        setIfInside(out, left + col, y + row, LABEL);
                    }
                }
            }
            left += sprite.getWidth() + 1;
        }
    }

    /** The fill colour of a button: green for confirm, grey for the others, and white under the player's view. */
    static byte fillOf(TargetButton button, boolean hovered) {
        if (hovered) {
            return MapPalette.WHITE;
        }
        return switch (button) {
            case CONFIRM -> MapPalette.DARK_GREEN;
            case CANCEL -> MapPalette.GRAY_1;
            default -> MapPalette.LIGHT_GRAY;
        };
    }

    /** The green tick of the confirm button: two strokes inside the button's rectangle. */
    private static void drawTick(byte[] out, TargetButton button, byte colour) {
        int left = button.x0();
        int top = button.y0();
        line(out, left + 5, top + 6, left + 9, top + 9, colour);
        line(out, left + 9, top + 9, left + 16, top + 3, colour);
    }

    /** A straight line of pixels between two pixels, drawn with the colour. */
    static void line(byte[] out, int x0, int y0, int x1, int y1, byte colour) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : (double) i / steps;
            setIfInside(out, (int) Math.round(x0 + (x1 - x0) * t), (int) Math.round(y0 + (y1 - y0) * t), colour);
        }
    }

    private static void setIfInside(byte[] out, int px, int py, byte colour) {
        int size = TargetView.PIXELS;
        if (px >= 0 && px < size && py >= 0 && py < size) {
            out[py * size + px] = colour;
        }
    }
}
