package io.corefactory.resourcepack.art;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The belt duplex, levels I to III: a logistics block two blocks wide and one block tall, lying flat across the belt. An
 * incoming belt feeds the back of the left cell (the anchor, {@code belt_duplex_N}), and the items leave both cells to the
 * north, one out of each. The right cell is {@code belt_duplex_N_right}. Each cell is one half of one machine, and the two
 * halves join along their seam: the anchor's east face and the right cell's west face.
 *
 * <p>A half is a belt slab with a splitter body on it, a rail along its outer side, and a hub on top. The top face is the belt
 * surface: it is an animated strip of chevrons that climb toward the north edge over the eight frames, so the strip loops, and
 * the frame time sets the pace (level I changes frame every two ticks, levels II and III every tick). The top also carries the
 * outer rail, the seam trim on the seam edge, the lane and the hub. The north face is the static front of the machine, with the
 * same rail, trim and lane, and a hub emblem, which is lit cyan on level III.
 *
 * <p>Textures are seen from outside, so the north face's left is the east and its right is the west, and the top face's left
 * is the west and its top is the north. The rail and the seam are placed with that in mind.
 */
public final class DuplexArt {

    /** The id prefix of every duplex model, followed by the level, such as {@code belt_duplex_2}. */
    private static final String PREFIX = "belt_duplex_";

    /** The suffix of the right cell's id. */
    private static final String RIGHT = "_right";

    /** The distance between two chevrons along the lane, in canvas pixels. */
    private static final int PERIOD = 16;

    /** How far the chevrons climb each frame: one period over the eight frames. */
    private static final int CLIMB = PERIOD / MachineAnimation.FRAMES;

    /** The width of the outer rail, in canvas pixels: two model units. */
    private static final int RAIL_WIDTH = 4;

    /** The left and right columns of the lane that the chevrons run in, in canvas pixels. */
    private static final int LANE_LEFT = 9;
    private static final int LANE_RIGHT = 23;

    private static final int RAIL_COLOUR = Color.hex("#6E7884");
    private static final int CYAN = Color.hex("#3FE8FF");
    private static final ComponentArt.Painter NONE = (c, base, accent, seed) -> {
    };

    /**
     * One level's look.
     *
     * @param number    the level, 1 to 3
     * @param base      the casing colour, {@code #RRGGBB}
     * @param accent    the chevron colour, {@code #RRGGBB}
     * @param trim      the seam trim colour, {@code #RRGGBB}
     * @param trimWidth the width of the seam trim, in canvas pixels
     * @param lamp      whether the hub carries a cyan lamp
     */
    private record Level(int number, String base, String accent, String trim, int trimWidth, boolean lamp) {
    }

    /**
     * Steel with white chevrons for level I, copper with orange chevrons and a fine gold trim for level II, and gold with yellow
     * chevrons and a cyan lamp on each half for level III. The colours are those of the belt and the vertical belt.
     */
    private static final List<Level> LEVELS = List.of(
            new Level(1, "#5A6470", "#E8ECF2", "#C8D0DA", 2, false),
            new Level(2, "#B87333", "#FFB070", "#F2C230", 1, false),
            new Level(3, "#C9A03A", "#FFE070", "#FFF0B0", 2, true));

    /** Every half of every level: the anchor first, then the right cell. */
    public static final List<MachineArt.Spec> ALL = LEVELS.stream()
            .flatMap(level -> Stream.of(half(level, false), half(level, true)))
            .toList();

    /**
     * The animated top face of each half: the chevron strip of the belt surface. The frame time is 2 for level I and 1 for
     * levels II and III.
     */
    public static final List<MachineAnimation.Clip> CLIPS = List.of(
            new MachineAnimation.Clip("belt_duplex_1", MachineArt.Face.UP, 2),
            new MachineAnimation.Clip("belt_duplex_1_right", MachineArt.Face.UP, 2),
            new MachineAnimation.Clip("belt_duplex_2", MachineArt.Face.UP, 1),
            new MachineAnimation.Clip("belt_duplex_2_right", MachineArt.Face.UP, 1),
            new MachineAnimation.Clip("belt_duplex_3", MachineArt.Face.UP, 1),
            new MachineAnimation.Clip("belt_duplex_3_right", MachineArt.Face.UP, 1));

    /** The inventory sprite of each level: two housings side by side, each with an up arrow, on a belt rail. */
    public static final List<ComponentArt.Spec> SPRITES = LEVELS.stream()
            .map(level -> new ComponentArt.Spec(PREFIX + level.number(), level.base(), level.accent(), sprite(level)))
            .toList();

    private DuplexArt() {
    }

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /**
     * One half of a duplex. The anchor's seam is its east face and its outer rail is on its west; the right cell is the mirror image.
     * The top face is animated and painted by {@link #paintFrame}.
     */
    private static MachineArt.Spec half(Level level, boolean right) {
        String id = PREFIX + level.number() + (right ? RIGHT : "");
        ComponentArt.Painter outer = (c, base, accent, seed) -> sideRail(c, base);
        ComponentArt.Painter seam = (c, base, accent, seed) -> seamOutline(c, level);
        Map<MachineArt.Face, ComponentArt.Painter> faces = new EnumMap<>(MachineArt.Face.class);
        faces.put(MachineArt.Face.EAST, right ? outer : seam);
        faces.put(MachineArt.Face.WEST, right ? seam : outer);
        faces.put(MachineArt.Face.NORTH, (c, base, accent, seed) -> front(c, level, right, base, accent));
        faces.put(MachineArt.Face.SOUTH, (c, base, accent, seed) -> {
            rail(c, railOnRight(right, false));
            lane(c, base);
        });
        return new MachineArt.Spec(id, level.base(), level.accent(), boxes(right), NONE, faces);
    }

    /**
     * The cuboids of one half: the belt slab that runs the full footprint, the splitter body on it that reaches the seam, the
     * outer rail along the outside, and the hub on top of the body.
     */
    private static List<MachineArt.Box> boxes(boolean right) {
        double bodyX0 = right ? 0 : 2;
        double bodyX1 = right ? 14 : 16;
        double railX0 = right ? 14 : 0;
        double railX1 = right ? 16 : 2;
        return List.of(
                new MachineArt.Box(0, 0, 0, 16, 2, 16),
                new MachineArt.Box(bodyX0, 2, 0, bodyX1, 8, 16),
                new MachineArt.Box(railX0, 2, 0, railX1, 4, 16),
                new MachineArt.Box(5, 8, 4, 11, 10, 12));
    }

    /**
     * Paints one frame of the top face over the casing: the outer rail, the seam trim on the seam edge, the lane, the chevrons,
     * which climb toward the north edge {@link #CLIMB} pixels a frame, and the hub on top of them.
     */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        Level level = levelOf(spec.id());
        boolean right = spec.id().endsWith(RIGHT);
        boolean railRight = railOnRight(right, false);
        rail(c, railRight);
        trim(c, level, railRight);
        lane(c, Color.hex(spec.base()));
        chevrons(c, Color.hex(spec.accent()), frame);
        hub(c, level, Color.hex(spec.accent()));
    }

    /**
     * The north face, the machine's static front: the outer rail, the seam trim, the lane and the hub emblem, lit on level III.
     */
    private static void front(Canvas c, Level level, boolean right, int base, int accent) {
        boolean railRight = railOnRight(right, true);
        rail(c, railRight);
        trim(c, level, railRight);
        lane(c, base);
        hub(c, level, accent);
    }

    /**
     * Whether the outer rail is on the right of a face, seen from outside. The north face is seen from the north, where the right
     * of the texture is the west, so the anchor's rail (on its west) is on the right of its north face and on the left of its top
     * face. The right cell is the mirror image.
     *
     * @param right     whether this is the right cell
     * @param northFace whether the face is the north face
     */
    private static boolean railOnRight(boolean right, boolean northFace) {
        return right != northFace;
    }

    /** The outer rail: a column along one edge of a face, at the side the rail is on. */
    private static void rail(Canvas c, boolean onRight) {
        int x0 = onRight ? Canvas.SIZE - RAIL_WIDTH : 0;
        c.paint(Area.rect(x0, 0, x0 + RAIL_WIDTH, Canvas.SIZE), RAIL_COLOUR);
    }

    /** The seam trim: a column along the edge opposite the rail, where the two halves join. */
    private static void trim(Canvas c, Level level, boolean railOnRight) {
        int w = level.trimWidth();
        int x0 = railOnRight ? 0 : Canvas.SIZE - w;
        c.paint(Area.rect(x0, 0, x0 + w, Canvas.SIZE), Color.hex(level.trim()));
    }

    /** The lane of the belt across a face: a recess between the two rails. */
    private static void lane(Canvas c, int base) {
        c.paint(Area.rect(LANE_LEFT, 0, LANE_RIGHT, Canvas.SIZE), Color.scale(base, 0.45));
    }

    /**
     * The chevrons of the belt surface: each is a V whose tip is on the north side, so it points toward the top edge of the
     * texture, which is the north. Each frame moves them {@link #CLIMB} pixels toward that edge.
     */
    private static void chevrons(Canvas c, int accent, int frame) {
        int climb = CLIMB * frame;
        for (int period = -1; period <= 1; period++) {
            double tip = 24 + PERIOD * period - climb;
            c.paint(Area.line(16, tip, LANE_LEFT, tip + 7, 2), accent);
            c.paint(Area.line(16, tip, LANE_RIGHT, tip + 7, 2), accent);
        }
    }

    /** The hub: a ring with a disc in its middle, and a cyan lamp on level III. */
    private static void hub(Canvas c, Level level, int accent) {
        c.paint(Area.ring(16, 16, 4.5, 1.2), accent);
        c.paint(Area.disc(16, 16, 2), accent);
        if (level.lamp()) {
            c.paint(Area.disc(16, 16, 1.4), CYAN);
        }
    }

    /** The outer side: a rail band with rivets, over the casing. */
    private static void sideRail(Canvas c, int base) {
        c.paint(Area.rect(0, 6, Canvas.SIZE, 10), RAIL_COLOUR);
        c.paint(Area.rect(0, 10, Canvas.SIZE, 11), Color.scale(base, 0.5));
        for (int x = 3; x < Canvas.SIZE; x += 8) {
            c.paint(Area.disc(x, 8, 0.8), Color.scale(RAIL_COLOUR, 0.6));
        }
    }

    /** The seam face: a light trim along all four edges, as wide as the level's seam trim. */
    private static void seamOutline(Canvas c, Level level) {
        int w = level.trimWidth();
        Area outline = Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE)
                .minus(Area.rect(w, w, Canvas.SIZE - w, Canvas.SIZE - w));
        c.paint(outline, Color.hex(level.trim()));
    }

    /** The inventory sprite: two housings side by side with a link between them, an up arrow on each, and a belt rail below. */
    private static ComponentArt.Painter sprite(Level level) {
        return (c, base, accent, seed) -> {
            Area housings = Area.roundRect(3, 9, 15, 24, 3).or(Area.roundRect(17, 9, 29, 24, 3));
            Sprite.body(c, housings, base, seed);
            c.paint(Area.rect(14, 14, 18, 19), Color.scale(base, 0.8));
            c.paint(Area.rect(2, 25, 30, 27), RAIL_COLOUR);
            for (double cx : new double[]{9, 23}) {
                c.paint(Area.line(cx, 12, cx - 4, 16, 1.8), accent);
                c.paint(Area.line(cx, 12, cx + 4, 16, 1.8), accent);
                c.paint(Area.line(cx, 12, cx, 20, 1.8), accent);
                if (level.lamp()) {
                    c.paint(Area.disc(cx, 22, 1.2), CYAN);
                }
            }
        };
    }

    /** The level of a duplex model id, from its digit: {@code belt_duplex_2_right} is level II. */
    private static Level levelOf(String id) {
        return LEVELS.get(id.charAt(PREFIX.length()) - '1');
    }
}
