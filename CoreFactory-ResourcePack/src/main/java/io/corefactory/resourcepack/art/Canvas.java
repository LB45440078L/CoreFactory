package io.corefactory.resourcepack.art;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/** A 32 x 32 ARGB painting surface. Drawing outside the canvas is ignored. */
public final class Canvas {

    public static final int SIZE = 32;

    private final int[] pixels = new int[SIZE * SIZE];

    /** Chooses a pixel's new colour from its position and the colour already there. */
    @FunctionalInterface
    public interface Shader {
        int shade(int x, int y, int current);
    }

    public static boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE;
    }

    public int get(int x, int y) {
        return contains(x, y) ? pixels[y * SIZE + x] : 0;
    }

    public void set(int x, int y, int argb) {
        if (contains(x, y)) {
            pixels[y * SIZE + x] = argb;
        }
    }

    public void clear(int argb) {
        Arrays.fill(pixels, argb);
    }

    /** Composites {@code argb} over every pixel the area covers. */
    public void paint(Area area, int argb) {
        shade(area, (x, y, current) -> Color.over(current, argb));
    }

    /** Replaces every pixel the area covers with the shader's result. */
    public void shade(Area area, Shader shader) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (area.covers(x + 0.5, y + 0.5)) {
                    set(x, y, shader.shade(x, y, get(x, y)));
                }
            }
        }
    }

    public BufferedImage toImage() {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, SIZE, SIZE, pixels, 0, SIZE);
        return image;
    }

    /** A 64-bit fingerprint of every pixel, used to check that no two textures are the same. */
    public long digest() {
        long h = 1125899906842597L;
        for (int p : pixels) {
            h = 31 * h + p;
        }
        return h;
    }
}
