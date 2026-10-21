package io.corefactory.resourcepack.art;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * The tops of the transport belts, one set per level. A top is a 16 x 16 texture: a dark steel base with a rail along
 * each side and a band of arrows in the level's colour. The straight top's arrows point toward the bottom edge of the
 * texture, and the corner top's arrows point along its arc toward the bottom edge as well.
 *
 * <p>Both tops are animated. Each is a vertical strip of {@link #FRAMES} square frames, and frame {@code k} shows the
 * arrows moved {@code k} pixels toward the bottom edge, which is the direction their tips point. The strip repeats after
 * {@link #FRAMES} frames, because the arrow pattern has a period of {@link #PERIOD} pixels.
 */
public final class BeltArt {

    /** The width and height of one frame, in pixels. */
    public static final int TILE = 16;

    /** The number of frames in an animated strip. */
    public static final int FRAMES = 8;

    /** Ticks each frame is shown, written to the strip's .mcmeta. */
    public static final int FRAME_TIME = 2;

    /** The height of the belt slab, in model units. */
    public static final int SLAB_HEIGHT = 2;

    /** The distance along the belt between two arrows, in pixels. */
    private static final double PERIOD = 8;

    /** The distance from an arrow's tip to the ends of its arms, along the belt. */
    private static final double ARM = 4.5;

    /** Where the first arrow's tip sits in frame 0. */
    private static final double PHASE = 4.5;

    private static final int RAIL = Color.hex("#6E7884");

    /**
     * One belt level's look.
     *
     * @param id     the component id, such as {@code belt_1}
     * @param base   the steel colour, {@code #RRGGBB}
     * @param accent the arrow colour of this level, {@code #RRGGBB}
     */
    public record Spec(String id, String base, String accent) {
    }

    /** Every belt level: grey-white arrows, then yellow, then red-orange. */
    public static final List<Spec> ALL = List.of(
            new Spec("belt_1", "#2A2E36", "#E8ECF2"),
            new Spec("belt_2", "#2A2E36", "#FFD040"),
            new Spec("belt_3", "#2A2E36", "#FF5A20"));

    private BeltArt() {
    }

    /**
     * An animated top: {@link #FRAMES} frames of {@link #TILE} pixels, stacked top to bottom, so the image is
     * {@code TILE} wide and {@code TILE * FRAMES} tall.
     *
     * @param corner true for the corner top, with the curved arrows; false for the straight top
     */
    public static BufferedImage strip(Spec spec, boolean corner) {
        BufferedImage strip = new BufferedImage(TILE, TILE * FRAMES, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < FRAMES; frame++) {
            int[] pixels = corner ? cornerFrame(spec, frame) : straightFrame(spec, frame);
            strip.setRGB(0, frame * TILE, TILE, TILE, pixels, 0, TILE);
        }
        return strip;
    }

    /** The side of a belt: steel with a light edge along its top row. The slab shows only the bottom two rows. */
    public static BufferedImage side(Spec spec) {
        int steel = Color.hex(spec.base());
        int[] pixels = new int[TILE * TILE];
        long seed = spec.id().hashCode() + 3L;
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                int colour = Color.scale(steel, 1 + (Noise.hash(x, y, seed) - 0.5) * 0.12);
                if (y == TILE - 2) {
                    colour = Color.mix(colour, Color.WHITE, 0.25);
                }
                pixels[y * TILE + x] = colour;
            }
        }
        return image(pixels);
    }

    private static int[] straightFrame(Spec spec, int frame) {
        int[] pixels = ground(spec, false);
        int arrow = Color.hex(spec.accent());
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                if (chevron(y + 0.5, x + 0.5 - TILE / 2.0, PHASE + frame)) {
                    pixels[y * TILE + x] = arrow;
                }
            }
        }
        return pixels;
    }

    /**
     * The corner top. The belt is the quarter disc of radius {@link #TILE} centred on the back-right corner of the
     * texture, so its arrows lie on the arc of radius {@code TILE / 2}. Their position along the arc is the arc length,
     * measured from the right edge toward the bottom edge, so that the tips point toward the bottom edge.
     */
    private static int[] cornerFrame(Spec spec, int frame) {
        int[] pixels = ground(spec, true);
        int arrow = Color.hex(spec.accent());
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                double dx = x + 0.5 - TILE;
                double dy = y + 0.5 - TILE;
                if (dx >= 0 || dy >= 0 || Math.hypot(dx, dy) > TILE) {
                    continue;
                }
                double along = -TILE / 2.0 * Math.atan2(-dy, -dx);
                if (chevron(along, Math.hypot(dx, dy) - TILE / 2.0, PHASE + frame)) {
                    pixels[y * TILE + x] = arrow;
                }
            }
        }
        return pixels;
    }

    /**
     * Whether a point lies on an arrow. The arrows repeat every {@link #PERIOD} along the belt. Each is a chevron whose
     * tip is at {@code phase}, with its arms running back along the belt to {@link #ARM} on either side.
     *
     * @param along  the position along the arrows, increasing in the direction their tips point
     * @param across the distance from the belt's centre line
     * @param phase  the position of an arrow's tip
     */
    private static boolean chevron(double along, double across, double phase) {
        double q = along - phase;
        q -= PERIOD * Math.floor(q / PERIOD);
        double behind = q - PERIOD;
        return behind >= -ARM && Math.abs(Math.abs(across) + behind) <= 0.9;
    }

    /** The dark steel of a top, with rails along its sides (straight) or its outer arc (corner), and the frame outside. */
    private static int[] ground(Spec spec, boolean corner) {
        int steel = Color.hex(spec.base());
        int frame = Color.scale(steel, 0.6);
        long seed = spec.id().hashCode();
        int[] pixels = new int[TILE * TILE];
        for (int y = 0; y < TILE; y++) {
            for (int x = 0; x < TILE; x++) {
                int colour = Color.scale(steel, 1 + (Noise.hash(x, y, seed) - 0.5) * 0.12);
                if (corner) {
                    double dx = x + 0.5 - TILE;
                    double dy = y + 0.5 - TILE;
                    if (dx >= 0 || dy >= 0 || Math.hypot(dx, dy) > TILE) {
                        colour = frame;
                    } else if (Math.hypot(dx, dy) >= TILE - 1) {
                        colour = RAIL;
                    }
                } else if (x == 0 || x == TILE - 1) {
                    colour = RAIL;
                }
                pixels[y * TILE + x] = colour;
            }
        }
        return pixels;
    }

    private static BufferedImage image(int[] pixels) {
        BufferedImage image = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, TILE, TILE, pixels, 0, TILE);
        return image;
    }
}
