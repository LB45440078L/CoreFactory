package io.corefactory.resourcepack.art;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The water system: the pump in its five levels, and the pipes and splitters that carry its water. Every pipe is one block.
 * A pipe is a cylinder approximated by two crossed boxes of cross-section (6 units across, 4 units across the other way), with
 * a flange at each open end, so it reads as a pipe along its axis. The open ends carry a dark bore.
 *
 * <p>The pipe axis is X, centred at y 8 and z 8. The open ends sit at the pipe's centre on the block's faces: the bend has its
 * ends on the -X face and the +Z face, the T has its input on the -X face and its outputs on the -Z and +Z faces, and the
 * cross has all four horizontal faces open. The splitters' hubs are short boxes in the middle of the block.
 *
 * <p>The pump's front is an impeller strip of eight frames, two ticks each. Level V glows. The kits are the item sprites at the
 * end of this class, laid out as the turret kits are.
 */
public final class WaterArt {

    private static final String PUMP = "water_pump";
    private static final String PIPE = "#3C7FA8";
    private static final String PIPE_EDGE = "#8FD0F0";
    private static final int BORE = Color.hex("#0E1A22");
    private static final ComponentArt.Painter NONE = (c, base, accent, seed) -> {
    };

    /**
     * The pumps, levels I to V: steel, bronze, silver, dark gunmetal with a cyan accent, and deep blue with a blue glow. Each
     * level has a larger impeller housing and a bigger intake than the one before.
     */
    public static final List<MachineArt.Spec> ALL = List.of(
            pump(1, "#6E7884", "#3AA8FF"),
            pump(2, "#9A6A34", "#E8A040"),
            pump(3, "#A9B0BA", "#DDE6F0"),
            pump(4, "#2B3038", "#3FE0FF"),
            pump(5, "#1B2B4A", "#5AA8FF"),
            new MachineArt.Spec("water_pipe", PIPE, PIPE_EDGE, pipeBoxes(), WaterArt::pipeBands, Map.of(
                    MachineArt.Face.SOUTH, WaterArt::pipeBands,
                    MachineArt.Face.UP, WaterArt::pipeBands,
                    MachineArt.Face.DOWN, WaterArt::pipeBands,
                    MachineArt.Face.EAST, WaterArt::flangeEnd,
                    MachineArt.Face.WEST, WaterArt::flangeEnd)),
            new MachineArt.Spec("water_pipe_bend", PIPE, PIPE_EDGE, bendBoxes(), WaterArt::pipeBands, Map.of(
                    MachineArt.Face.SOUTH, WaterArt::flangeEnd,
                    MachineArt.Face.WEST, WaterArt::flangeEnd,
                    MachineArt.Face.EAST, WaterArt::pipeBands,
                    MachineArt.Face.UP, WaterArt::pipeBands,
                    MachineArt.Face.DOWN, WaterArt::pipeBands)),
            new MachineArt.Spec("water_splitter_t", PIPE, PIPE_EDGE, tBoxes(), WaterArt::flangeEnd, Map.of(
                    MachineArt.Face.SOUTH, WaterArt::flangeEnd,
                    MachineArt.Face.WEST, WaterArt::flangeEnd,
                    MachineArt.Face.EAST, WaterArt::collar,
                    MachineArt.Face.UP, WaterArt::pipeBands,
                    MachineArt.Face.DOWN, WaterArt::pipeBands)),
            new MachineArt.Spec("water_splitter_x", PIPE, PIPE_EDGE, crossBoxes(), WaterArt::flangeEnd, Map.of(
                    MachineArt.Face.SOUTH, WaterArt::flangeEnd,
                    MachineArt.Face.WEST, WaterArt::flangeEnd,
                    MachineArt.Face.EAST, WaterArt::flangeEnd,
                    MachineArt.Face.UP, WaterArt::pipeBands,
                    MachineArt.Face.DOWN, WaterArt::pipeBands)));

    /** The animated impeller front of each pump level, eight frames, two ticks each. */
    public static final List<MachineAnimation.Clip> CLIPS = List.of(
            new MachineAnimation.Clip("water_pump", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("water_pump_2", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("water_pump_3", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("water_pump_4", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("water_pump_5", MachineArt.Face.NORTH, 2));

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

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /**
     * One pump level. Its plinth stands on the block floor, the intake sits on the plinth and grows with the level, and the
     * impeller housing rises from the intake and grows too. A cap sits on top of the housing.
     */
    private static MachineArt.Spec pump(int level, String base, String accent) {
        String id = level == 1 ? PUMP : PUMP + "_" + level;
        double half = 2.5 + 0.5 * level;     // the impeller housing's half width, 3 at level I and 5 at level V
        double intake = 1.5 + 0.5 * level;   // the intake's half width, 2 at level I and 4 at level V
        double top = 4 + 2 * half;           // the housing's top, 10 at level I and 14 at level V
        return new MachineArt.Spec(id, base, accent, List.of(
                new MachineArt.Box(3, 0, 3, 13, 2, 13),
                new MachineArt.Box(8 - intake, 2, 8 - intake, 8 + intake, 4, 8 + intake),
                new MachineArt.Box(8 - half, 4, 8 - half, 8 + half, top, 8 + half),
                new MachineArt.Box(7, top, 7, 9, top + 1, 9)), NONE);
    }

    /** The pump level of a machine id: 1 for water_pump, and n for water_pump_n. */
    private static int pumpLevel(String id) {
        return id.equals(PUMP) ? 1 : id.charAt(id.length() - 1) - '0';
    }

    /**
     * Paints the impeller of one frame of a pump, over the casing. The housing is a dark recess on the north face, with four
     * vanes that turn a quarter of a turn over the eight frames. Four vanes repeat every quarter turn, so the strip loops.
     * Level V has a blue glow around the housing that brightens and dims with the pulse.
     */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        int level = pumpLevel(spec.id());
        int accent = Color.hex(spec.accent());
        double half = 2.5 + 0.5 * level;
        double top = 4 + 2 * half;
        double left = 16 - 2 * half;
        double right = 16 + 2 * half;
        double topY = 32 - 2 * top;          // canvas rows: model y is 16 at row 0 and 0 at row 32
        double bottom = 24;
        double cx = 16;
        double cy = (topY + bottom) / 2;
        double radius = 2 * half - 2.5;
        double p = MachineAnimation.pulse(frame);
        c.paint(Area.roundRect(left, topY, right, bottom, 3), Color.hex("#14171B"));
        if (level == 5) {
            c.paint(Area.disc(cx, cy, radius + 3.5), Color.withAlpha(accent, 0.15 + 0.35 * p));
        }
        c.paint(Area.ring(cx, cy, radius, 1.2), Color.scale(accent, 0.6));
        double spin = Math.PI / 16 * frame;
        for (int k = 0; k < 4; k++) {
            double a = spin + k * Math.PI / 2;
            c.paint(Area.line(cx, cy, cx + Math.cos(a) * (radius - 1), cy + Math.sin(a) * (radius - 1), 1.8), accent);
        }
        c.paint(Area.disc(cx, cy, 2), Color.mix(accent, Color.WHITE, 0.4));
    }

    /**
     * A pipe's body: a light band across the top of the cylinder and a shaded band below it, so the round section shows on
     * every side that carries it.
     */
    static void pipeBands(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 7, Canvas.SIZE, 10), Color.withAlpha(accent, 0.7));
        c.paint(Area.rect(0, 22, Canvas.SIZE, 25), Color.scale(base, 0.55));
    }

    /**
     * An open end: the flange as a darker collar with a light rim, and the dark bore at its centre. The bore is a few pixels
     * across, so it sits on the pipe's end inside the flange.
     */
    static void flangeEnd(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 16, 8), Color.scale(base, 0.8));
        c.paint(Area.ring(16, 16, 8, 1), accent);
        c.paint(Area.ring(16, 16, 6, 0.8), Color.scale(base, 0.5));
        c.paint(Area.disc(16, 16, 3.2), BORE);
    }

    /** A splitter's closed hub face: a dark collar with a thin accent stripe across its middle. */
    static void collar(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), Color.scale(base, 0.65));
        c.paint(Area.rect(0, 15, Canvas.SIZE, 17), accent);
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
     * The bend: the X arm runs from the -X face (x 0) to x 11, and the Z arm runs from z 5 to the +Z face (z 16). The arms
     * overlap in the corner, so the inner angle is the -X, +Z corner at (x 5, z 11). Open ends: -X at (y 8, z 8), and +Z at
     * (x 8, y 8).
     */
    private static List<MachineArt.Box> bendBoxes() {
        List<MachineArt.Box> boxes = new ArrayList<>();
        pipeX(boxes, 0, 11);
        pipeZ(boxes, 5, 16);
        flangeX(boxes, 0, 2);
        flangeZ(boxes, 14, 16);
        return boxes;
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
    private static void pipeSprite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(4, 12, 28, 20, 3).or(Area.rect(2, 9, 6, 23)).or(Area.rect(26, 9, 30, 23)), base, seed);
        c.paint(Area.rect(2, 9, 6, 23), Color.scale(base, 0.7));
        c.paint(Area.rect(26, 9, 30, 23), Color.scale(base, 0.7));
        c.paint(Area.rect(7, 13, 25, 14.5), accent);
    }

    /** The inventory sprite of the T: a bar with an output at each end, and the input stem below the middle. */
    private static void tSprite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(2, 10, 30, 17, 2.5).or(Area.rect(12, 16, 20, 30)), base, seed);
        c.paint(Area.rect(2, 9, 6, 18), Color.scale(base, 0.7));
        c.paint(Area.rect(26, 9, 30, 18), Color.scale(base, 0.7));
        c.paint(Area.rect(11, 26, 21, 30), Color.scale(base, 0.7));
        c.paint(Area.rect(4, 12, 28, 13.5), accent);
    }

    /** The inventory sprite of the cross: four arms with a collar on each end, and a ringed hub. */
    private static void crossSprite(Canvas c, int base, int accent, long seed) {
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
