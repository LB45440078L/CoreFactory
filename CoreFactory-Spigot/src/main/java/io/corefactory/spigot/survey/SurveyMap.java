package io.corefactory.spigot.survey;

import io.corefactory.core.survey.ChunkSurvey;

/**
 * Lays a chunk survey out on a map. The 16 x 16 columns of the chunk become 8 x 8 pixel cells of the 128 x 128 map.
 * Pure logic, so the pixel mapping and the shading are tested without a server.
 */
public final class SurveyMap {

    /** The side of a map in pixels. */
    public static final int SIZE = 128;

    /** The side of one cell in pixels. */
    public static final int CELL = SIZE / ChunkSurvey.SIZE;

    /** The colour of a column with no planned block. */
    public static final int EMPTY_RGB = 0x303030;

    /** The colour of the one-pixel line drawn where the dominant ore changes between two cells. */
    public static final int BORDER_RGB = 0x0A0A0A;

    /** The block count at which a cell reaches the full colour of its ore. */
    static final int FULL_COUNT = 64;

    /** The brightness of a cell with no blocks, in 64ths of its ore's colour. */
    private static final int DARKEST_LEVEL = 26;

    /**
     * The pixel square of one cell.
     *
     * @param x    left pixel
     * @param y    top pixel
     * @param size side in pixels
     */
    public record Rect(int x, int y, int size) {
    }

    private SurveyMap() {
    }

    /** The pixels of the cell at a column and row, each in 0..15. */
    public static Rect cellRect(int column, int row) {
        return new Rect(column * CELL, row * CELL, CELL);
    }

    /**
     * The colour of a cell: its ore's colour, darker for fewer blocks. A count of {@link #FULL_COUNT} or more gives the
     * full colour, and each channel scales linearly from {@code DARKEST_LEVEL / 64} of it at zero blocks.
     */
    public static int shade(int rgb, int count) {
        int blocks = Math.clamp(count, 0, FULL_COUNT);
        int level = DARKEST_LEVEL + ((FULL_COUNT - DARKEST_LEVEL) * blocks + FULL_COUNT / 2) / FULL_COUNT;
        int r = ((rgb >> 16) & 0xFF) * level / FULL_COUNT;
        int g = ((rgb >> 8) & 0xFF) * level / FULL_COUNT;
        int b = (rgb & 0xFF) * level / FULL_COUNT;
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Paints the survey: each cell is filled with its ore's shade, or {@link #EMPTY_RGB}; the left column and top row of a
     * cell are drawn {@link #BORDER_RGB} where the cell to the left or above has a different dominant ore.
     *
     * @param survey the survey of the chunk
     * @param oreRgb the base colour of each ore, indexed by ore index
     * @return the pixels, row by row, {@link #SIZE} x {@link #SIZE}, as RGB
     */
    public static int[] paint(ChunkSurvey.Survey survey, int[] oreRgb) {
        int[] pixels = new int[SIZE * SIZE];
        for (int row = 0; row < ChunkSurvey.SIZE; row++) {
            for (int column = 0; column < ChunkSurvey.SIZE; column++) {
                ChunkSurvey.Cell cell = survey.cell(column, row);
                int rgb = cell.ore() == ChunkSurvey.NONE ? EMPTY_RGB : shade(oreRgb[cell.ore()], cell.count());
                Rect rect = cellRect(column, row);
                for (int dy = 0; dy < rect.size(); dy++) {
                    for (int dx = 0; dx < rect.size(); dx++) {
                        pixels[(rect.y() + dy) * SIZE + rect.x() + dx] = rgb;
                    }
                }
                if (column > 0 && differs(survey, column - 1, row, column, row)) {
                    for (int dy = 0; dy < rect.size(); dy++) {
                        pixels[(rect.y() + dy) * SIZE + rect.x()] = BORDER_RGB;
                    }
                }
                if (row > 0 && differs(survey, column, row - 1, column, row)) {
                    for (int dx = 0; dx < rect.size(); dx++) {
                        pixels[rect.y() * SIZE + rect.x() + dx] = BORDER_RGB;
                    }
                }
            }
        }
        return pixels;
    }

    /** The map centre coordinate for a chunk coordinate: the middle block of the chunk. */
    public static int centre(int chunkCoordinate) {
        return (chunkCoordinate << 4) + ChunkSurvey.SIZE / 2;
    }

    private static boolean differs(ChunkSurvey.Survey survey, int ax, int az, int bx, int bz) {
        return survey.cell(ax, az).ore() != survey.cell(bx, bz).ore();
    }
}
