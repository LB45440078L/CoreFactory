package io.corefactory.resourcepack.art;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The artillery pieces, levels I to VII, and the Artillery Command Center Unit.
 *
 * <p>A piece is made of parts, each a model of its own, and each model is as tall as its level: 1.5 blocks for levels I and II, 2.5
 * for III to V, 3.0 for VI and 3.75 for VII. A vanilla model keeps its elements between -16 and 32 units on every axis, which is
 * three blocks of height, so every model is written from y -16, the floor of its block, and reaches up to its part of the piece.
 * The body, {@code artillery_N_body}, is the carriage, the turret ring and the base: everything that turns to the aim. The barrel,
 * {@code artillery_N_barrel}, is the barrel and its breech, which tilt to the elevation about a trunnion at {@link #trunnionBlocks}.
 * Level VII has a third model, {@link #LEVEL_SEVEN_TOP}, the top 0.75 block, which turns with the body and does not tilt. Level VII's
 * barrel lies in that upper part, so {@link #LEVEL_SEVEN_BARREL} is written in the upper frame. The plugin lifts each display up by
 * {@link #displayLift} blocks, so the floor of the model sits on its block, and it places a model {@link #displayOffset} blocks above
 * the block of the base.
 *
 * <p>The north face is the front of a piece. It carries the muzzle, two warning lights that pulse, and from level III a glow
 * around the bore that grows with the level. From level VI the top has smoke vents, with a red glow in them. Each level has
 * a casing colour of its own, more armour bands than the one below, and from level III warning stripes along its foot.
 *
 * <p>The command center is a table: a top slab on a front pedestal. Its screen is a flat area on the pedestal's north face,
 * which the plugin fills with a hologram at {@link #SCREEN}. The screen's scan lines scroll over eight frames.
 */
public final class ArtilleryArt {

    /** The item sprite and the model of the command center. */
    public static final String COMMAND_CENTER = "command_center";

    /**
     * The upper model of level VII: the top 0.75 block of the piece, without its barrel. It turns with the body and does not tilt. It has
     * no item sprite; the plugin places its display {@link #displayOffset} blocks above the display of {@code artillery_7_body}.
     */
    public static final String LEVEL_SEVEN_TOP = "artillery_7_top";

    /**
     * The barrel of level VII. The barrel lies in the upper 0.75 block, so it is written in the upper frame, like
     * {@link #LEVEL_SEVEN_TOP}, and the plugin places its display {@link #displayOffset} blocks above the body.
     */
    public static final String LEVEL_SEVEN_BARREL = "artillery_7_barrel";

    /**
     * The flat screen of the command center, in model units: x0, y0, x1, y1 and the plane z of the pedestal's north face,
     * which the screen lies on. The plugin places the hologram in front of this rectangle. The screen is 7 by 5.25 units.
     */
    public static final double[] SCREEN = {4.5, 4.5, 11.5, 9.75, 2};

    /** Paints nothing: the front of an animated face is painted by {@link #paintFrame}, so no front painter is needed. */
    private static final ComponentArt.Painter NONE = (c, base, accent, seed) -> {
    };

    /** The model units of one block. */
    private static final double BLOCK = 16;

    /** The tallest span of one model, in model units: from y -16 to y 32, three blocks. */
    private static final double MODEL_SPAN = 48;

    /** The suffixes that name the models of a piece: its body, its barrel and its upper model. */
    private static final String BODY = "_body";
    private static final String BARREL = "_barrel";
    private static final String TOP = "_top";

    /** The frames of the command center's screen. */
    private static final int COMMAND_FRAMES = 8;

    /** The period of the scan lines, in pixels. It divides the screen so that the strip loops after {@link #COMMAND_FRAMES}. */
    private static final int SCAN_PERIOD = 8;

    /**
     * The phase of the warning lights' pulse, in radians: 15 degrees. A cosine is the same on both sides of its peak, so with no
     * phase an odd frame count repeats a frame. The phase keeps every frame apart from its neighbours for 4 to 8 frames.
     */
    private static final double PHASE = Math.PI / 12;

    private static final int RED = Color.hex("#FF2A2A");
    private static final int GLASS = Color.hex("#061418");
    private static final int CYAN = Color.hex("#55FFFF");
    private static final int FLAME_CORE = Color.hex("#FFE070");

    /**
     * One level of an artillery piece.
     *
     * @param level   the level, from 1 to 7
     * @param base    the casing colour, {@code #RRGGBB}
     * @param accent  the warning colour, {@code #RRGGBB}
     * @param blocks  the height of the level in blocks
     * @param barrel  the width of the barrel, in model units
     * @param plates  the armour bands across the front, from 1 to 3
     * @param stripes true from level III: warning stripes along the foot of the front
     * @param glow    the strength of the glow around the bore, from 0 for none to 1 for the brightest
     * @param vents   true from level VI: smoke vents on the top, and a red glow in them
     * @param frames  the frames of the animated front, from 4 to 8
     * @param frametime the ticks each frame is shown, from 2 to 4
     */
    private record Piece(int level, String base, String accent, double blocks, int barrel, int plates, boolean stripes,
                         double glow, boolean vents, int frames, int frametime) {

        String id() {
            return "artillery_" + level;
        }

        /** The id of the body model: the carriage, the turret ring and the base. */
        String bodyId() {
            return id() + BODY;
        }

        /** The id of the barrel model: the barrel and its breech. */
        String barrelId() {
            return id() + BARREL;
        }
    }

    /**
     * The seven levels. Each one is taller and has a wider barrel than the one below, more armour, and a stronger warning.
     * Levels I and II have no glow. The glow grows from level III, and the animation runs for more frames from level IV.
     */
    private static final List<Piece> PIECES = List.of(
            new Piece(1, "#4A5058", "#E0A030", 1.5, 4, 1, false, 0, false, 4, 4),
            new Piece(2, "#5A5E4A", "#E8B040", 1.5, 5, 2, false, 0, false, 4, 4),
            new Piece(3, "#5E6470", "#FF8A20", 2.5, 5, 2, true, 0.15, false, 4, 3),
            new Piece(4, "#55595F", "#FF6A20", 2.5, 6, 3, true, 0.3, false, 5, 3),
            new Piece(5, "#4A4846", "#FF4A1A", 2.5, 6, 3, true, 0.45, false, 6, 3),
            new Piece(6, "#3A3436", "#FF2A2A", 3.0, 7, 3, true, 0.6, true, 7, 2),
            new Piece(7, "#2A2A30", "#FF1E1E", 3.75, 8, 3, true, 0.8, true, 8, 2));

    /**
     * Every machine of this class: the body and the barrel of each of the seven pieces, with the upper model of level VII after its
     * barrel, then the command center.
     */
    public static final List<MachineArt.Spec> ALL = Stream.concat(
                    PIECES.stream().flatMap(p -> models(p).stream()),
                    Stream.of(new MachineArt.Spec(COMMAND_CENTER, "#3A4658", "#55FFFF", commandBoxes(), NONE)))
            .toList();

    /** The animated north faces: the warning lights of each piece's models, and the screen of the command center. */
    public static final List<MachineAnimation.Clip> CLIPS = Stream.concat(
                    PIECES.stream().flatMap(p -> models(p).stream()
                            .map(m -> new MachineAnimation.Clip(m.id(), MachineArt.Face.NORTH, p.frametime(), p.frames()))),
                    Stream.of(new MachineAnimation.Clip(COMMAND_CENTER, MachineArt.Face.NORTH, 2, COMMAND_FRAMES)))
            .toList();

    /** The inventory sprites: a field piece for each level, with its barrel's width, and the table seen from above. */
    public static final List<ComponentArt.Spec> SPRITES = Stream.concat(
                    PIECES.stream().map(p -> new ComponentArt.Spec(p.id(), p.base(), p.accent(), sprite(p.barrel()))),
                    Stream.of(new ComponentArt.Spec(COMMAND_CENTER, "#3A4658", "#55FFFF", ArtilleryArt::commandTop)))
            .toList();

    private ArtilleryArt() {
    }

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /**
     * The height of a machine's model, in model units: the height of its level for an artillery model, whichever of its parts it is, and
     * one block for every other machine, the command center included.
     */
    public static double modelHeight(String id) {
        return isPieceModel(id) ? pieceOf(id).blocks() * BLOCK : BLOCK;
    }

    /**
     * How far the plugin translates a model's display up, in blocks, so that the model's frame starts on its block. Every artillery model
     * is written from y -16 of its frame, the barrels and the upper model included, so this is one block for each of them. The command
     * center is written from y 0 and is not lifted.
     *
     * @throws IllegalArgumentException if the id is not an artillery model
     */
    public static double displayLift(String id) {
        if (!isPieceModel(id)) {
            throw new IllegalArgumentException("No artillery model " + id);
        }
        return 1.0;
    }

    /**
     * How far above the block of the base the plugin places a model's display, in blocks. A model in the upper frame stands three blocks
     * up: {@link #LEVEL_SEVEN_TOP} and {@link #LEVEL_SEVEN_BARREL}, since the base covers the bottom three blocks of level VII. Every other
     * model stands on its own block, so its offset is 0.
     */
    public static double displayOffset(String id) {
        return frameOf(id) / BLOCK;
    }

    /**
     * The height of a barrel model's trunnion above the floor of its block, in blocks: the middle of the barrel's cross-section, at the
     * middle of the block on the horizontal axes. The plugin turns the barrel about this point, which is where the barrel's model is
     * written around.
     *
     * @throws IllegalArgumentException if the id is not a barrel model
     */
    public static double trunnionBlocks(String id) {
        if (!isPieceModel(id) || !id.endsWith(BARREL)) {
            throw new IllegalArgumentException("No artillery barrel " + id);
        }
        Piece piece = pieceOf(id);
        return (barrelTop(piece) - piece.barrel() / 2.0) / BLOCK;
    }

    /** Whether an id names a model of an artillery piece: a body, a barrel or the upper model of level VII. */
    private static boolean isPieceModel(String id) {
        return PIECES.stream().anyMatch(p -> id.equals(p.id()) || id.startsWith(p.id() + "_"));
    }

    /** The piece that a model of an artillery piece belongs to. */
    private static Piece pieceOf(String id) {
        return PIECES.stream().filter(p -> id.equals(p.id()) || id.startsWith(p.id() + "_")).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No artillery piece " + id));
    }

    /**
     * The frame a model is written in, in model units from the floor of its level: 0 for the lower part, and {@link #MODEL_SPAN} for the
     * upper part of level VII, which holds its upper model and its barrel. Every other id, the command center included, is in the lower
     * frame.
     */
    private static double frameOf(String id) {
        if (!isPieceModel(id)) {
            return 0;
        }
        if (id.endsWith(TOP)) {
            return MODEL_SPAN;
        }
        if (id.endsWith(BARREL)) {
            return barrelFrame(pieceOf(id));
        }
        return 0;
    }

    /** The frame of a piece's barrel: the upper frame when the whole barrel lies above {@link #MODEL_SPAN}, which only level VII's does. */
    private static double barrelFrame(Piece piece) {
        return barrelTop(piece) - piece.barrel() >= MODEL_SPAN ? MODEL_SPAN : 0;
    }

    /** The height of a piece's barrel at its top, in model units of the level: 4 units under the top of the level. */
    private static double barrelTop(Piece piece) {
        return piece.blocks() * BLOCK - 4;
    }

    /**
     * Paints one frame of an animated face over the casing: the front of a piece with its lights, glow and vents, or the screen of
     * the command center with its scan lines.
     */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        if (spec.id().equals(COMMAND_CENTER)) {
            commandFront(c, frame);
            return;
        }
        Piece piece = pieceOf(spec.id());
        int base = Color.hex(spec.base());
        int accent = Color.hex(spec.accent());
        double p = pulse(frame, piece.frames());

        pieceFront(c, piece, base, accent);
        int lamp = Color.mix(Color.scale(accent, 0.5), Color.mix(accent, Color.WHITE, 0.5), p);
        c.paint(Area.disc(7, 8, 1.3), lamp);
        c.paint(Area.disc(25, 8, 1.3), lamp);
        if (piece.glow() > 0) {
            c.paint(Area.ring(16, 10, 6.5, 2.2), Color.withAlpha(accent, piece.glow() * (0.35 + 0.65 * p)));
        }
        if (piece.vents()) {
            int red = Color.withAlpha(RED, 0.3 + 0.6 * p);
            c.paint(Area.rect(6, 1, 12, 3), red);
            c.paint(Area.rect(20, 1, 26, 3), red);
        }
    }

    /**
     * The pulse of a cycle of {@code frames} frames: a cosine shifted by {@link #PHASE}, so the lights are nearly dark at frame 0
     * and brightest a little past the middle of the cycle. The strip loops, and each frame differs from the next.
     */
    private static double pulse(int frame, int frames) {
        return 0.5 - 0.5 * Math.cos(2 * Math.PI * frame / frames - PHASE);
    }

    /**
     * The static front of a piece: the bore inside a muzzle ring, the armour bands with their bolts, the warning stripes and the
     * vent slits. The lights, glow and red glow are painted over it per frame.
     */
    private static void pieceFront(Canvas c, Piece piece, int base, int accent) {
        c.paint(Area.disc(16, 10, 4.2), Color.hex("#0E1012"));
        c.paint(Area.ring(16, 10, 4.2, 1.2), accent);
        int plate = Color.mix(base, Color.WHITE, 0.2);
        for (int k = 0; k < piece.plates(); k++) {
            double y = 16 + 4 * k;
            c.paint(Area.rect(4, y, 28, y + 1.5), plate);
            c.paint(Area.disc(6, y + 0.75, 0.9), Color.scale(base, 0.5));
            c.paint(Area.disc(26, y + 0.75, 0.9), Color.scale(base, 0.5));
        }
        if (piece.stripes()) {
            c.shade(Area.rect(0, 27, Canvas.SIZE, Canvas.SIZE), (x, y, current) ->
                    ((x + y) / 4) % 2 == 0 ? accent : Color.scale(base, 0.35));
        }
        if (piece.vents()) {
            c.paint(Area.rect(6, 1, 12, 3), Color.scale(base, 0.4));
            c.paint(Area.rect(20, 1, 26, 3), Color.scale(base, 0.4));
        }
    }

    /**
     * The models of a piece, in model units: the body, the barrel and, for level VII, the upper model. The body is the level from its floor
     * to {@link #MODEL_SPAN} units, and level VII's upper model takes the rest, so the plugin places it three blocks above the base. The
     * barrel is written in the frame of {@link #frameOf}, so a barrel of level VII is written from the upper frame.
     */
    private static List<MachineArt.Spec> models(Piece p) {
        double height = p.blocks() * BLOCK;
        List<MachineArt.Box> body = bodyBoxes(p);
        List<MachineArt.Spec> result = new ArrayList<>();
        result.add(new MachineArt.Spec(p.bodyId(), p.base(), p.accent(), part(body, 0, Math.min(height, MODEL_SPAN)), NONE));
        result.add(new MachineArt.Spec(p.barrelId(), p.base(), p.accent(), part(barrelBoxes(p), frameOf(p.barrelId()), height),
                NONE));
        if (height > MODEL_SPAN) {
            result.add(new MachineArt.Spec(p.id() + TOP, p.base(), p.accent(), part(body, MODEL_SPAN, height), NONE));
        }
        return result;
    }

    /**
     * The cuboids of a piece between the heights {@code from} and {@code to} of the level, in model units. Each cuboid is cut to
     * that band and written from y -16 of the band's bottom, so the band's bottom sits on the model's frame, and a cuboid that lies
     * outside the band is left out.
     */
    private static List<MachineArt.Box> part(List<MachineArt.Box> geometry, double from, double to) {
        List<MachineArt.Box> cut = new ArrayList<>();
        for (MachineArt.Box box : geometry) {
            double y0 = Math.max(box.y0(), from);
            double y1 = Math.min(box.y1(), to);
            if (y1 > y0) {
                cut.add(new MachineArt.Box(box.x0(), y0 - from - BLOCK, box.z0(), box.x1(), y1 - from - BLOCK, box.z1()));
            }
        }
        return cut;
    }

    /**
     * The cuboids of a piece's body, in the level's own height: from 0 at the ground up to the height of the level. A plinth sits on the
     * ground, and a column rises from the ground to the top of the level. From level I a shield stands in front of the column, with side
     * wings and a lower band as the levels rise. From level VI the column stops two units short of the top, and two smoke vents stand on
     * it up to the full height. The barrel is not part of the body: it tilts, so it has a model of its own, see {@link #barrelBoxes}.
     */
    private static List<MachineArt.Box> bodyBoxes(Piece p) {
        double h = p.blocks() * BLOCK;
        double top = p.vents() ? h - 2 : h;
        List<MachineArt.Box> boxes = new ArrayList<>();
        boxes.add(new MachineArt.Box(1, 0, 1, 15, 2, 15));
        boxes.add(new MachineArt.Box(4, 0, 4, 12, top, 12));
        boxes.add(new MachineArt.Box(4, 2, 2, 12, top - 8, 4));
        if (p.plates() >= 2) {
            boxes.add(new MachineArt.Box(1, 4, 5, 4, top - 10, 11));
            boxes.add(new MachineArt.Box(12, 4, 5, 15, top - 10, 11));
        }
        if (p.plates() >= 3) {
            boxes.add(new MachineArt.Box(3, 2, 1, 13, 6, 2));
        }
        if (p.vents()) {
            boxes.add(new MachineArt.Box(5, top, 5, 7, h, 7));
            boxes.add(new MachineArt.Box(9, top, 9, 11, h, 11));
        }
        return boxes;
    }

    /**
     * The cuboids of a piece's barrel, in the level's own height: a tube of the level's barrel width from the muzzle at z = 0 back to the
     * column at z = 4, with its top 4 units under the top of the level. Its breech is the tube's back end, which sits in the column. The
     * trunnion, at the middle of the block, is where the barrel tilts about, so the tube is written in front of it.
     */
    private static List<MachineArt.Box> barrelBoxes(Piece p) {
        double w = p.barrel();
        double top = barrelTop(p);
        return List.of(new MachineArt.Box(8 - w / 2, top - w, 0, 8 + w / 2, top, 4));
    }

    /**
     * The command center's cuboids: a top slab, and a front pedestal whose north face carries the screen at {@link #SCREEN}. The
     * pedestal reaches the ground, and the table is 14 units, under one block.
     */
    private static List<MachineArt.Box> commandBoxes() {
        return List.of(
                new MachineArt.Box(0, 12, 0, 16, 14, 16),
                new MachineArt.Box(1, 0, 2, 15, 12, 14));
    }

    /**
     * The front of the command center for one frame: a thin bright frame around a dark glass screen, scan lines that move down
     * one pixel a frame, a readout bar, and a keypad strip under the screen.
     */
    private static void commandFront(Canvas c, int frame) {
        c.paint(Area.rect(7, 5, 25, 21), CYAN);
        c.paint(Area.rect(8, 6, 24, 20), GLASS);
        for (int y = 6; y < 20; y++) {
            if (Math.floorMod(y - frame, SCAN_PERIOD) < 2) {
                c.paint(Area.rect(8, y, 24, y + 1), Color.withAlpha(CYAN, 0.35));
            }
        }
        c.paint(Area.rect(10, 16, 22, 17), Color.withAlpha(CYAN, 0.6));
        c.paint(Area.rect(9, 22, 23, 24), Color.hex("#2A3440"));
        for (double x = 10; x < 23; x += 3) {
            c.paint(Area.disc(x + 0.5, 23, 0.6), CYAN);
        }
    }

    /** The inventory sprite of a piece: a carriage with a warning band, two wheels, and a barrel of the level's width. */
    private static ComponentArt.Painter sprite(int barrel) {
        return (c, base, accent, seed) -> {
            double w = 1.6 + 0.4 * barrel;
            Sprite.body(c, Area.roundRect(7, 18, 25, 26, 2), base, seed);
            c.paint(Area.rect(7, 18, 25, 20), accent);
            c.paint(Area.disc(10, 27, 2.6), Color.hex("#1A1A1E"));
            c.paint(Area.disc(22, 27, 2.6), Color.hex("#1A1A1E"));
            c.paint(Area.line(12, 19, 23, 5, w), Color.scale(base, 0.85));
            c.paint(Area.disc(23.5, 4.5, w / 2 + 0.6), accent);
        };
    }

    /** The command center seen from above: the table top, its screen standing at the back edge, and its front edge. */
    private static void commandTop(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(4, 5, 28, 27, 3), base, seed);
        c.paint(Area.rect(4, 23, 28, 27), Color.scale(base, 0.5));
        c.paint(Area.rect(6, 6, 26, 12), accent);
        c.paint(Area.rect(7, 7, 25, 11), GLASS);
        c.paint(Area.rect(9, 14, 23, 15), accent);
        c.paint(Area.rect(9, 17, 17, 18), Color.withAlpha(accent, 0.6));
    }
}
