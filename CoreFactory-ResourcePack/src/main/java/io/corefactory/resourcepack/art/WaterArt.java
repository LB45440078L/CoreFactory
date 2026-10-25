package io.corefactory.resourcepack.art;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The water system: the pump in its five levels, and the pipes and splitters that carry its water. Every pipe is one block.
 * A pipe is a cylinder approximated by two crossed boxes of cross-section (6 units across, 4 units across the other way), with
 * a flange at each open end, so it reads as a pipe along its axis. The open ends carry a dark bore.
 *
 * <p>The pipe axis is X, centred at y 8 and z 8. The open ends sit at the pipe's centre on the block's faces: the bend has its
 * ends on the -X face and the +Z face, the T has its input on the -X face and its outputs on the -Z and +Z faces, and the
 * cross has all four horizontal faces open. The splitters' hubs are short boxes in the middle of the block. The bend's
 * centreline is two straight 2-unit stubs, one under each flange, joined by a tangent quarter arc of radius 6 that bulges toward the block centre.
 *
 * <p>Levels I to III are one block. Levels IV and V are two blocks tall: the lower block has the id, and the upper block has
 * that id with {@code _top}. The pump's front is an impeller strip of eight frames, two ticks each, on both blocks of a tall
 * pump, so the impeller turns across the seam. Level V glows. The kits are the item sprites at the end of this class, laid
 * out as the turret kits are.
 */
public final class WaterArt {

    private static final String PUMP = "water_pump";
    private static final String TOP = "_top";
    private static final int BEND_SEGMENTS = 32;
    /** The bend's arc: radius 6, centred at (2, 14). Its ends are straight stubs of 2 units at each face, the flange length. */
    private static final double BEND_RADIUS = 6;
    private static final double BEND_CENTRE_X = 2;
    private static final double BEND_CENTRE_Z = 14;
    private static final String PIPE = "#3C7FA8";
    private static final String PIPE_EDGE = "#8FD0F0";
    private static final ComponentArt.Painter NONE = (c, base, accent, seed) -> {
    };

    /**
     * The colours of a pipe model: the casing, the accent that its rims, collars and sprite stripe take, the light band across the
     * top of its body, and the bore of each open end. The geometry and the painters are the same for every palette, so a palette
     * changes only these colours. The oil tubes are the second palette.
     */
    record Palette(String base, String accent, String light, String bore) {
    }

    /** The water pipe: the steel blue casing, the pale edge as its accent and light band, and a dark bore. */
    static final Palette WATER = new Palette(PIPE, PIPE_EDGE, PIPE_EDGE, "#0E1A22");

    /**
     * The canonical pumps, levels I to V: steel, bronze, silver, dark gunmetal with a cyan accent, and deep blue with a blue glow.
     * Each level has a larger impeller housing and a bigger intake than the one before. Levels IV and V are two blocks, so each
     * has an upper block as well. These are not models of their own: {@link WaterVariants} bakes each one in every orientation.
     */
    private static final List<MachineArt.Spec> PUMPS = List.of(
            pump(1, "#6E7884", "#3AA8FF"),
            pump(2, "#9A6A34", "#E8A040"),
            pump(3, "#A9B0BA", "#DDE6F0"),
            pump(4, "#2B3038", "#3FE0FF"),
            pumpTop(4, "#2B3038", "#3FE0FF"),
            pump(5, "#1B2B4A", "#5AA8FF"),
            pumpTop(5, "#1B2B4A", "#5AA8FF"));

    /** Every canonical model in a palette: the pumps, then the pipes and the junction. {@link WaterVariants} bakes each one. */
    static List<MachineArt.Spec> canonical(Palette palette) {
        return Stream.of(PUMPS, pipes(palette), junctions(palette)).flatMap(List::stream).toList();
    }

    /**
     * The straight pipe, the bend, the T and the cross, in a palette. Each open end takes the palette's bore, and the bands and the
     * casing take its light band and accent.
     */
    static List<MachineArt.Spec> pipes(Palette palette) {
        String base = palette.base();
        String accent = palette.accent();
        ComponentArt.Painter bands = pipeBands(palette.light());
        ComponentArt.Painter end = flangeEnd(palette.bore());
        return List.of(
                new MachineArt.Spec("water_pipe", base, accent, pipeBoxes(), bands, Map.of(
                        MachineArt.Face.SOUTH, bands,
                        MachineArt.Face.UP, bands,
                        MachineArt.Face.DOWN, bands,
                        MachineArt.Face.EAST, end,
                        MachineArt.Face.WEST, end)),
                new MachineArt.Spec("water_pipe_bend", base, accent, bendBoxes(), bands, Map.of(
                        MachineArt.Face.SOUTH, end,
                        MachineArt.Face.WEST, end,
                        MachineArt.Face.EAST, bands,
                        MachineArt.Face.UP, bands,
                        MachineArt.Face.DOWN, bands)),
                new MachineArt.Spec("water_splitter_t", base, accent, tBoxes(), end, Map.of(
                        MachineArt.Face.SOUTH, end,
                        MachineArt.Face.WEST, end,
                        MachineArt.Face.EAST, WaterArt::collar,
                        MachineArt.Face.UP, bands,
                        MachineArt.Face.DOWN, bands)),
                new MachineArt.Spec("water_splitter_x", base, accent, crossBoxes(), end, Map.of(
                        MachineArt.Face.SOUTH, end,
                        MachineArt.Face.WEST, end,
                        MachineArt.Face.EAST, end,
                        MachineArt.Face.UP, bands,
                        MachineArt.Face.DOWN, bands)));
    }

    /**
     * The junction's hub: a short central section that every arm of a junction meets. Each arm is a separate model, {@code water_pipe_arm},
     * drawn from the hub out to one face, so a junction shows every connection it has.
     */
    static List<MachineArt.Spec> junctions(Palette palette) {
        ComponentArt.Painter bands = pipeBands(palette.light());
        return List.of(
                new MachineArt.Spec("water_pipe_hub", palette.base(), palette.accent(), hub(), bands, Map.of()),
                new MachineArt.Spec("water_pipe_arm", palette.base(), palette.accent(), armBoxes(), bands, Map.of(
                        MachineArt.Face.EAST, flangeEnd(palette.bore()))));
    }

    /**
     * The inventory sprites: the pump, the pipe and the two splitters, then the upgrade kits for levels 2 to 5. Each kit's
     * lit pips take the colour of the pump level it makes.
     */
    public static final List<ComponentArt.Spec> SPRITES = List.of(
            new ComponentArt.Spec("water_pump", "#6E7884", "#3AA8FF", WaterArt::pumpSprite),
            new ComponentArt.Spec("water_pipe", PIPE, PIPE_EDGE, WaterArt::pipeSprite),
            new ComponentArt.Spec("water_splitter_t", PIPE, PIPE_EDGE, WaterArt::tSprite),
            new ComponentArt.Spec("water_splitter_x", PIPE, PIPE_EDGE, WaterArt::crossSprite),
            new ComponentArt.Spec("water_pump_upgrade_2", "#B8914A", "#E8A040", kit(1)),
            new ComponentArt.Spec("water_pump_upgrade_3", "#B8914A", "#DDE6F0", kit(2)),
            new ComponentArt.Spec("water_pump_upgrade_4", "#B8914A", "#3FE0FF", kit(3)),
            new ComponentArt.Spec("water_pump_upgrade_5", "#B8914A", "#5AA8FF", kit(4)));

    private WaterArt() {
    }

    /**
     * One pump level, as its lower block. Its plinth stands on the block floor, the intake sits on the plinth and grows with
     * the level, and the impeller housing rises from the intake and grows too. Levels I to III fit in one block, with a cap
     * on top of the housing. Levels IV and V fill the block with the housing, and the rest of it is the upper block.
     */
    private static MachineArt.Spec pump(int level, String base, String accent) {
        String id = level == 1 ? PUMP : PUMP + "_" + level;
        double half = 2.5 + 0.5 * level;     // the impeller housing's half width, 3 at level I and 5 at level V
        double intake = 1.5 + 0.5 * level;   // the intake's half width, 2 at level I and 4 at level V
        boolean tall = level >= 4;
        double top = tall ? 16 : 4 + 2 * half;   // the housing's top, 10 at level I and 14 at level V, 16 in the lower block
        List<MachineArt.Box> boxes = new ArrayList<>(List.of(
                new MachineArt.Box(3, 0, 3, 13, 2, 13),
                new MachineArt.Box(8 - intake, 2, 8 - intake, 8 + intake, 4, 8 + intake),
                new MachineArt.Box(8 - half, 4, 8 - half, 8 + half, top, 8 + half)));
        if (!tall) {
            boxes.add(new MachineArt.Box(7, top, 7, 9, top + 1, 9));
        }
        return new MachineArt.Spec(id, base, accent, boxes, NONE);
    }

    /**
     * The upper block of a tall pump, level IV or V: the rest of the impeller housing from the top of the lower block, which is
     * 10 units of housing, and the cap on top of it. Its id is the lower block's id with {@code _top}.
     */
    private static MachineArt.Spec pumpTop(int level, String base, String accent) {
        double half = 2.5 + 0.5 * level;
        return new MachineArt.Spec(PUMP + "_" + level + TOP, base, accent, List.of(
                new MachineArt.Box(8 - half, 0, 8 - half, 8 + half, 10, 8 + half),
                new MachineArt.Box(7, 10, 7, 9, 11, 9)), NONE);
    }

    /**
     * The pump level of a model id, which {@link WaterVariants} names water_pump_n_front, or water_pump_n_front_top for an upper
     * block: the number after water_pump_.
     */
    private static int pumpLevel(String id) {
        return Integer.parseInt(id.split("_")[2]);
    }

    /**
     * Paints the impeller of one frame of a pump, over the casing. The housing is a dark recess on the north face, with four
     * vanes that turn a quarter of a turn over the eight frames. Four vanes repeat every quarter turn, so the strip loops.
     * Level V has a blue glow around the housing that brightens and dims with the pulse.
     *
     * <p>A tall pump's impeller is centred on the seam between its two blocks. The lower block paints the half of it below the
     * seam (its top row is y 16), and the upper block paints the half above it (its bottom row is y 0). The housing recess runs
     * through both blocks, so the impeller turns as one across them.
     */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        int level = pumpLevel(spec.id());
        int accent = Color.hex(spec.accent());
        double half = 2.5 + 0.5 * level;
        double left = 16 - 2 * half;
        double right = 16 + 2 * half;
        double p = MachineAnimation.pulse(frame);
        double cx = 16;
        double radius = 2 * half - 2.5;
        double spin = Math.PI / 16 * frame;
        if (level >= 4) {
            boolean upper = spec.id().endsWith(TOP);
            double cy = upper ? 32 : 0;   // the seam: row 32 of the upper block is y 0, row 0 of the lower block is y 16
            // The recess runs from its rounded end out of the canvas at the seam, so it has no visible cap there.
            Area recess = upper ? Area.roundRect(left, 12, right, 36, 3) : Area.roundRect(left, -4, right, 24, 3);
            c.paint(recess, Color.hex("#14171B"));
            impeller(c, cx, cy, radius, accent, spin, level, p);
            return;
        }
        double top = 4 + 2 * half;
        double topY = 32 - 2 * top;          // canvas rows: model y is 16 at row 0 and 0 at row 32
        double bottom = 24;
        double cy = (topY + bottom) / 2;
        c.paint(Area.roundRect(left, topY, right, bottom, 3), Color.hex("#14171B"));
        impeller(c, cx, cy, radius, accent, spin, level, p);
    }

    /** The impeller at a centre: a glow on level V, the ring, four vanes at the spin angle, and the hub. */
    private static void impeller(Canvas c, double cx, double cy, double radius, int accent, double spin, int level, double p) {
        if (level == 5) {
            c.paint(Area.disc(cx, cy, radius + 3.5), Color.withAlpha(accent, 0.15 + 0.35 * p));
        }
        c.paint(Area.ring(cx, cy, radius, 1.2), Color.scale(accent, 0.6));
        for (int k = 0; k < 4; k++) {
            double a = spin + k * Math.PI / 2;
            c.paint(Area.line(cx, cy, cx + Math.cos(a) * (radius - 1), cy + Math.sin(a) * (radius - 1), 1.8), accent);
        }
        c.paint(Area.disc(cx, cy, 2), Color.mix(accent, Color.WHITE, 0.4));
    }

    /**
     * A pipe's body: a light band across the top of the cylinder and a shaded band below it, so the round section shows on
     * every side that carries it. The band takes the palette's light colour.
     */
    static ComponentArt.Painter pipeBands(String light) {
        int band = Color.hex(light);
        return (c, base, accent, seed) -> {
            c.paint(Area.rect(0, 7, Canvas.SIZE, 10), Color.withAlpha(band, 0.7));
            c.paint(Area.rect(0, 22, Canvas.SIZE, 25), Color.scale(base, 0.55));
        };
    }

    /**
     * An open end: the flange as a darker collar with a light rim, and the dark bore at its centre. The bore is a few pixels
     * across, so it sits on the pipe's end inside the flange. The bore takes the palette's colour.
     */
    static ComponentArt.Painter flangeEnd(String bore) {
        int hole = Color.hex(bore);
        return (c, base, accent, seed) -> {
            c.paint(Area.disc(16, 16, 8), Color.scale(base, 0.8));
            c.paint(Area.ring(16, 16, 8, 1), accent);
            c.paint(Area.ring(16, 16, 6, 0.8), Color.scale(base, 0.5));
            c.paint(Area.disc(16, 16, 3.2), hole);
        };
    }

    /**
     * A splitter's closed hub face: a dark collar with a thin accent stripe across its middle. The collar darkens the casing under
     * it rather than replacing it, so it keeps the casing's grain, and each splitter's collar is its own texture.
     */
    static void collar(Canvas c, int base, int accent, long seed) {
        c.shade(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), (x, y, current) -> Color.scale(current, 0.65));
        c.paint(Area.rect(0, 15, Canvas.SIZE, 17), accent);
    }

    /** One arm of a junction: a pipe run from the centre of the block out to its +X face, with a flange at the face. */
    private static List<MachineArt.Box> armBoxes() {
        List<MachineArt.Box> boxes = new ArrayList<>();
        pipeX(boxes, 8, 16);
        flangeX(boxes, 14, 16);
        return boxes;
    }

    /** The straight pipe: a 16 unit run along X, with a flange at each end. */
    private static List<MachineArt.Box> pipeBoxes() {
        List<MachineArt.Box> boxes = new ArrayList<>();
        pipeX(boxes, 0, 16);
        flangeX(boxes, 0, 2);
        flangeX(boxes, 14, 16);
        return boxes;
    }

    /**
     * The bend, as option A: two straight ends meet the faces square, joined by a quarter arc that is tangent to both. The -X end is a
     * stub of 2 units along X at z 8, and the +Z end a stub of 2 units along Z at x 8, each under its flange. The arc has radius 6,
     * centred at (x 2, z 14), so it starts tangent to the -X stub at (x 2, z 8) and ends tangent to the +Z stub at (x 8, z 14). It bulges
     * toward the block centre. The arc is {@link #BEND_SEGMENTS} runs of pipe, each on the axis its chord leans toward, with the section
     * of {@link #pipeX} and {@link #pipeZ}. Flanges sit on both stubs: flangeX at the -X end and flangeZ at the +Z end. Open ends: -X at
     * (y 8, z 8), and +Z at (x 8, y 8).
     */
    private static List<MachineArt.Box> bendBoxes() {
        List<MachineArt.Box> boxes = new ArrayList<>();
        double[] from = bendPoint(0);
        for (int i = 1; i <= BEND_SEGMENTS; i++) {
            double[] to = bendPoint(i);
            if (Math.abs(to[0] - from[0]) >= Math.abs(to[1] - from[1])) {
                bendRunX(boxes, Math.min(from[0], to[0]), Math.max(from[0], to[0]), (from[1] + to[1]) / 2);
            } else {
                bendRunZ(boxes, Math.min(from[1], to[1]), Math.max(from[1], to[1]), (from[0] + to[0]) / 2);
            }
            from = to;
        }
        flangeX(boxes, 0, 2);
        flangeZ(boxes, 14, 16);
        return boxes;
    }

    /**
     * Point {@code i} of the bend's arc, as {x, z}: the arc of radius {@link #BEND_RADIUS} about (x 2, z 14), from angle -90 degrees (point 0,
     * at (2, 8), where the -X stub ends) to 0 degrees (point {@link #BEND_SEGMENTS}, at (8, 14), where the +Z stub begins).
     */
    private static double[] bendPoint(int i) {
        double angle = -Math.PI / 2 + Math.PI / 2 * i / BEND_SEGMENTS;
        return new double[]{BEND_CENTRE_X + BEND_RADIUS * Math.cos(angle), BEND_CENTRE_Z + BEND_RADIUS * Math.sin(angle)};
    }

    /** A run of pipe along X from x0 to x1, centred on z = cz and y 8, with the section of {@link #pipeX}. */
    private static void bendRunX(List<MachineArt.Box> boxes, double x0, double x1, double cz) {
        boxes.add(new MachineArt.Box(x0, 5, clamp(cz - 2), x1, 11, clamp(cz + 2)));
        boxes.add(new MachineArt.Box(x0, 6, clamp(cz - 3), x1, 10, clamp(cz + 3)));
    }

    /** Keeps a model coordinate inside the block: a tube's section that reaches past a face is cut at the face. */
    private static double clamp(double v) {
        return Math.max(0, Math.min(16, v));
    }

    /** A run of pipe along Z from z0 to z1, centred on x = cx and y 8, with the section of {@link #pipeZ}. */
    private static void bendRunZ(List<MachineArt.Box> boxes, double z0, double z1, double cx) {
        boxes.add(new MachineArt.Box(clamp(cx - 2), 5, z0, clamp(cx + 2), 11, z1));
        boxes.add(new MachineArt.Box(clamp(cx - 3), 6, z0, clamp(cx + 3), 10, z1));
    }

    /**
     * The T: a hub in the middle, the input arm from the -X face (x 0) to the hub, and the outputs from the hub to the -Z face
     * (z 0) and the +Z face (z 16). Open ends: -X at (y 8, z 8), -Z at (x 8, y 8), +Z at (x 8, y 8).
     */
    private static List<MachineArt.Box> tBoxes() {
        List<MachineArt.Box> boxes = hub();
        pipeX(boxes, 0, 4);
        flangeX(boxes, 0, 2);
        pipeZ(boxes, 0, 4);
        flangeZ(boxes, 0, 2);
        pipeZ(boxes, 12, 16);
        flangeZ(boxes, 14, 16);
        return boxes;
    }

    /** The cross: the same hub, with arms and flanges on all four horizontal faces. */
    private static List<MachineArt.Box> crossBoxes() {
        List<MachineArt.Box> boxes = hub();
        pipeX(boxes, 0, 4);
        flangeX(boxes, 0, 2);
        pipeX(boxes, 12, 16);
        flangeX(boxes, 14, 16);
        pipeZ(boxes, 0, 4);
        flangeZ(boxes, 0, 2);
        pipeZ(boxes, 12, 16);
        flangeZ(boxes, 14, 16);
        return boxes;
    }

    /** The short central box of a splitter: an 8 unit cube at the block's centre. */
    private static List<MachineArt.Box> hub() {
        List<MachineArt.Box> boxes = new ArrayList<>();
        boxes.add(new MachineArt.Box(4, 4, 4, 12, 12, 12));
        return boxes;
    }

    /** A run of pipe along X, from x0 to x1: a plus of a 6 by 4 and a 4 by 6 section, centred on y 8 and z 8. */
    private static void pipeX(List<MachineArt.Box> boxes, double x0, double x1) {
        boxes.add(new MachineArt.Box(x0, 5, 6, x1, 11, 10));
        boxes.add(new MachineArt.Box(x0, 6, 5, x1, 10, 11));
    }

    /** A run of pipe along Z, from z0 to z1, centred on x 8 and y 8. */
    private static void pipeZ(List<MachineArt.Box> boxes, double z0, double z1) {
        boxes.add(new MachineArt.Box(6, 5, z0, 10, 11, z1));
        boxes.add(new MachineArt.Box(5, 6, z0, 11, 10, z1));
    }

    /** A flange on the X axis, from x0 to x1: an 8 unit disc of two boxes, centred on y 8 and z 8. */
    private static void flangeX(List<MachineArt.Box> boxes, double x0, double x1) {
        boxes.add(new MachineArt.Box(x0, 4, 5, x1, 12, 11));
        boxes.add(new MachineArt.Box(x0, 5, 4, x1, 11, 12));
    }

    /** A flange on the Z axis, from z0 to z1: an 8 unit disc of two boxes, centred on x 8 and y 8. */
    private static void flangeZ(List<MachineArt.Box> boxes, double z0, double z1) {
        boxes.add(new MachineArt.Box(4, 5, z0, 12, 11, z1));
        boxes.add(new MachineArt.Box(5, 4, z0, 11, 12, z1));
    }

    /** The inventory sprite of the pump: a volute with four vanes and an intake pipe below it. */
    private static void pumpSprite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.disc(16, 15, 9).or(Area.rect(11, 21, 21, 30)), base, seed);
        c.paint(Area.ring(16, 15, 6.5, 1), Color.scale(base, 0.6));
        c.paint(Area.rect(12, 26, 20, 30), Color.scale(base, 0.5));
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            c.paint(Area.line(16, 15, 16 + Math.cos(a) * 5, 15 + Math.sin(a) * 5, 1.8), accent);
        }
        c.paint(Area.disc(16, 15, 2), Color.mix(accent, Color.WHITE, 0.3));
    }

    /** The inventory sprite of the pipe: a short round run with a flange at each end. */
    static void pipeSprite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(4, 12, 28, 20, 3).or(Area.rect(2, 9, 6, 23)).or(Area.rect(26, 9, 30, 23)), base, seed);
        c.paint(Area.rect(2, 9, 6, 23), Color.scale(base, 0.7));
        c.paint(Area.rect(26, 9, 30, 23), Color.scale(base, 0.7));
        c.paint(Area.rect(7, 13, 25, 14.5), accent);
    }

    /** The inventory sprite of the T: a bar with an output at each end, and the input stem below the middle. */
    static void tSprite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(2, 10, 30, 17, 2.5).or(Area.rect(12, 16, 20, 30)), base, seed);
        c.paint(Area.rect(2, 9, 6, 18), Color.scale(base, 0.7));
        c.paint(Area.rect(26, 9, 30, 18), Color.scale(base, 0.7));
        c.paint(Area.rect(11, 26, 21, 30), Color.scale(base, 0.7));
        c.paint(Area.rect(4, 12, 28, 13.5), accent);
    }

    /** The inventory sprite of the cross: four arms with a collar on each end, and a ringed hub. */
    static void crossSprite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(2, 13, 30, 19, 2.5).or(Area.roundRect(13, 2, 19, 30, 2.5)), base, seed);
        c.paint(Area.rect(2, 12, 6, 20), Color.scale(base, 0.7));
        c.paint(Area.rect(26, 12, 30, 20), Color.scale(base, 0.7));
        c.paint(Area.rect(12, 2, 20, 6), Color.scale(base, 0.7));
        c.paint(Area.rect(12, 26, 20, 30), Color.scale(base, 0.7));
        c.paint(Area.disc(16, 16, 4), Color.scale(base, 0.8));
        c.paint(Area.ring(16, 16, 4, 1), accent);
    }

    /**
     * An upgrade kit: a sealed brass cartridge with a window of pips, one pip lit for level 2 up to four for level 5. The
     * layout matches the turret kits in {@link TurretLevelArt}.
     */
    private static ComponentArt.Painter kit(int pips) {
        return (c, base, accent, seed) -> {
            Sprite.body(c, Area.roundRect(7, 3, 25, 29, 4), base, seed);
            c.paint(Area.rect(7, 3, 25, 7), Color.scale(base, 0.6));
            c.paint(Area.rect(7, 25, 25, 27), Color.scale(base, 0.5));
            c.paint(Area.roundRect(9, 10, 23, 21, 1.5), Color.hex("#1A1A1E"));
            for (int k = 0; k < 4; k++) {
                double x = 11.5 + 3.5 * k;
                c.paint(Area.disc(x, 15.5, 1.3), k < pips ? accent : Color.hex("#3A3F48"));
            }
            c.paint(Area.disc(10, 6, 0.9), Color.withAlpha(Color.WHITE, 0.5));
        };
    }
}
