package io.corefactory.resourcepack.art;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The rail gun turret, levels I to III. Each level is a body, which turns to the aim, and a barrel, which tilts about its trunnion.
 * Level III also has a top assembly on its body, which turns with the body and does not tilt.
 *
 * <p>The models share the artillery's frame (see {@link ArtilleryArt}). Level units run from 0 at the floor of the turret's block, 16
 * to a block, and each model is written from y -16, so a model's y is the level unit minus 16. The plugin lifts each display one block,
 * and it places the top of level III {@link #displayOffset} blocks above the body, which is two blocks: the body covers the bottom two.
 * Level I is one block tall, level II one and a half, and level III two and a half, with its top.
 *
 * <p>The body has a flat screen on its front, at {@link #SCREEN}, and a rail-coil set on each side: a red helix on the west side and a
 * blue spiral on the east side. The charge is shown by the animated front of each model: the body's charge rails and the muzzle's coil
 * glow. A charge builds over {@link #FRAMES} frames, the last frame is full charge and the brightest, and the strip loops.
 */
public final class RailgunArt {

    /** The turret's item. Its inventory sprite is the level I turret. Levels II and III are reached by the turret upgrade kits. */
    public static final String TURRET = "railgun_turret";

    /** The rail coil, a crafted part of the turret, and its inventory sprite. */
    public static final String COIL = "rail_coil";

    /** The rail barrel, a crafted part of the turret, and its inventory sprite. */
    public static final String RAIL = "rail_barrel";

    /** The upper assembly of level III, which sits two blocks above the floor. It has no inventory sprite of its own. */
    public static final String LEVEL_THREE_TOP = "railgun_3_top";

    /** The frames of the charge animation. Frame 0 is empty and the last frame is full charge. */
    public static final int FRAMES = 8;

    /** The ticks each frame is shown: the slowest the pack allows, so a full charge takes 28 ticks to build. */
    public static final int FRAMETIME = 4;

    /**
     * The flat screen of the turret, in the model units of its body: x0, y0, x1, y1 and the plane z of the body's front. The rectangle
     * is in the body's own frame, so y is the level unit minus 16 (for example -13 is 3 units above the floor of the block). The same
     * rectangle lies on the front of every level's body. It is 7 units wide and 6 tall.
     */
    public static final double[] SCREEN = {4.5, -13, 11.5, -7, 2};

    /** The model units of one block. */
    private static final double BLOCK = 16;

    /** The level unit at which the top of level III starts: the top of its body, two blocks up. */
    private static final double TOP_FROM = 32;

    /** The suffixes that name the models of a level: its body, its barrel and, for level III, its top. */
    private static final String BODY = "_body";
    private static final String BARREL = "_barrel";
    private static final String TOP = "_top";

    private static final int RED = Color.hex("#FF2A2A");
    private static final int BLUE = Color.hex("#4FB6FF");
    private static final int DARK = Color.hex("#0E1012");
    private static final int PANEL = Color.hex("#16191E");
    private static final int GLASS = Color.hex("#061418");
    private static final int COPPER = Color.hex("#B87333");

    private static final ComponentArt.Painter NONE = (c, base, accent, seed) -> {
    };

    /**
     * One level's geometry, in level units. The barrel is written around its trunnion, the height of the middle of its cross-section.
     *
     * @param number  the level, from 1 to 3
     * @param base    the casing colour, {@code #RRGGBB}
     * @param accent  the charge colour, {@code #RRGGBB}
     * @param height  the height of the level, in level units, its top included
     * @param body    the cuboids of the body
     * @param barrel  the cuboids of the barrel
     * @param trunnion the height of the barrel's trunnion, in level units
     * @param top     the cuboids of the top assembly, or none
     */
    private record Level(int number, String base, String accent, double height, List<MachineArt.Box> body,
                         List<MachineArt.Box> barrel, double trunnion, List<MachineArt.Box> top) {

        String id() {
            return "railgun_" + number;
        }
    }

    /** The three levels, each taller and wider than the one below. */
    private static final List<Level> LEVELS = List.of(
            new Level(1, "#3A3F48", "#FF2A2A", 16, levelOneBody(), levelOneBarrel(), 12, List.of()),
            new Level(2, "#353B45", "#FF2A2A", 24, levelTwoBody(), levelTwoBarrel(), 20, List.of()),
            new Level(3, "#2A2E36", "#FF2A2A", 40, levelThreeBody(), levelThreeBarrel(), 26, levelThreeTop()));

    /** Every railgun model: the body and the barrel of each level, and the top of level III after its barrel. */
    public static final List<MachineArt.Spec> ALL = LEVELS.stream().flatMap(level -> models(level).stream()).toList();

    /** The animated north faces: the front of every model, which shows the charge. */
    public static final List<MachineAnimation.Clip> CLIPS = ALL.stream()
            .map(spec -> new MachineAnimation.Clip(spec.id(), MachineArt.Face.NORTH, FRAMETIME, FRAMES))
            .toList();

    /** The inventory sprites of the turret, the rail coil and the rail barrel. */
    public static final List<ComponentArt.Spec> SPRITES = List.of(
            new ComponentArt.Spec(TURRET, "#3A3F48", "#FF2A2A", RailgunArt::turretSprite),
            new ComponentArt.Spec(COIL, "#B87333", "#FF2A2A", RailgunArt::coilSprite),
            new ComponentArt.Spec(RAIL, "#5E6774", "#4FB6FF", RailgunArt::railSprite));

    private RailgunArt() {
    }

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /**
     * The height of a railgun model's level, in model units: 16, 24 or 40, for every model of the level.
     *
     * @throws IllegalArgumentException if the id is not a railgun model
     */
    public static double modelHeight(String id) {
        return levelOf(id).height();
    }

    /**
     * How far the plugin lifts a model's display up, in blocks. Every railgun model is written from y -16 of its frame, so this is one
     * block for each of them.
     *
     * @throws IllegalArgumentException if the id is not a railgun model
     */
    public static double displayLift(String id) {
        levelOf(id);
        return 1.0;
    }

    /**
     * How far above the block of the base the plugin places a model's display, in blocks: two for the top of level III, which sits on the
     * body, and zero for every other model.
     *
     * @throws IllegalArgumentException if the id is not a railgun model
     */
    public static double displayOffset(String id) {
        levelOf(id);
        return id.endsWith(TOP) ? TOP_FROM / BLOCK : 0;
    }

    /**
     * The height of a barrel's trunnion above the floor of its block, in blocks: 0.75, 1.25 and 1.625 for levels I to III. The barrel
     * tilts about this point, at the middle of the block on the horizontal axes.
     *
     * @throws IllegalArgumentException if the id is not a barrel model
     */
    public static double trunnionBlocks(String id) {
        Level level = levelOf(id);
        if (!id.endsWith(BARREL)) {
            throw new IllegalArgumentException("No railgun barrel " + id);
        }
        return level.trunnion() / BLOCK;
    }

    /**
     * Paints one frame of an animated face over the casing: the body's front with its charge rails, or the muzzle's coil glow, or the
     * top's charge band. The charge rises with the frame, so the last frame is full charge.
     */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        int base = Color.hex(spec.base());
        int accent = Color.hex(spec.accent());
        double charge = charge(frame);
        if (spec.id().endsWith(BARREL)) {
            muzzleFrame(c, base, accent, charge);
        } else if (spec.id().endsWith(TOP)) {
            topFrame(c, base, accent, charge);
        } else {
            bodyFrame(c, base, accent, charge);
        }
    }

    /** The charge at a frame: 0 at the first frame, rising to 1 at the last, which is full charge. */
    static double charge(int frame) {
        return frame / (double) (FRAMES - 1);
    }

    /** The models of one level: the body, the barrel and, for level III, the top. */
    private static List<MachineArt.Spec> models(Level level) {
        List<MachineArt.Spec> result = new ArrayList<>();
        result.add(new MachineArt.Spec(level.id() + BODY, level.base(), level.accent(), written(level.body(), 0), NONE, Map.of(
                MachineArt.Face.WEST, RailgunArt::westCoil,
                MachineArt.Face.EAST, RailgunArt::eastCoil)));
        result.add(new MachineArt.Spec(level.id() + BARREL, level.base(), level.accent(), written(level.barrel(), 0), NONE));
        if (!level.top().isEmpty()) {
            result.add(new MachineArt.Spec(level.id() + TOP, level.base(), level.accent(), written(level.top(), TOP_FROM), NONE,
                    Map.of(MachineArt.Face.UP, RailgunArt::topUp)));
        }
        return result;
    }

    /** The cuboids of a model, in model units: each level unit minus {@code from} and minus one block. */
    private static List<MachineArt.Box> written(List<MachineArt.Box> boxes, double from) {
        return boxes.stream()
                .map(b -> new MachineArt.Box(b.x0(), b.y0() - from - BLOCK, b.z0(), b.x1(), b.y1() - from - BLOCK, b.z1()))
                .toList();
    }

    /** The level that a model id belongs to. */
    private static Level levelOf(String id) {
        return LEVELS.stream().filter(level -> id.startsWith(level.id() + "_")).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No railgun model " + id));
    }

    /** Level I: a one-block turret, its plinth the size of the laser turret's base, a short column and a front housing. */
    private static List<MachineArt.Box> levelOneBody() {
        return List.of(
                new MachineArt.Box(1, 0, 1, 15, 2, 15),
                new MachineArt.Box(4, 2, 4, 12, 9, 12),
                new MachineArt.Box(3, 2, 2, 13, 10, 4),
                new MachineArt.Box(1, 3, 5, 4, 9, 11),
                new MachineArt.Box(12, 3, 5, 15, 9, 11),
                new MachineArt.Box(3, 9, 3, 13, 10, 13));
    }

    /** Level I's barrel: one rail tube with a muzzle ring at its front and a breech at the trunnion. */
    private static List<MachineArt.Box> levelOneBarrel() {
        return List.of(
                new MachineArt.Box(6, 10, 0, 10, 14, 8),
                new MachineArt.Box(5, 10, 0, 11, 14, 1),
                new MachineArt.Box(5, 9, 6, 11, 15, 9));
    }

    /** Level II: a wider plinth, a taller column, and a coil on each side in two bands. */
    private static List<MachineArt.Box> levelTwoBody() {
        return List.of(
                new MachineArt.Box(0, 0, 0, 16, 2, 16),
                new MachineArt.Box(2, 2, 2, 14, 4, 14),
                new MachineArt.Box(4, 4, 4, 12, 16, 12),
                new MachineArt.Box(3, 2, 2, 13, 16, 4),
                new MachineArt.Box(0, 5, 4, 4, 9, 12),
                new MachineArt.Box(0, 11, 4, 4, 15, 12),
                new MachineArt.Box(12, 5, 4, 16, 9, 12),
                new MachineArt.Box(12, 11, 4, 16, 15, 12));
    }

    /** Level II's barrel: a rail tube with a second rail section stacked on it, so the trunnion is at their join. */
    private static List<MachineArt.Box> levelTwoBarrel() {
        return List.of(
                new MachineArt.Box(5, 16, 0, 11, 20, 8),
                new MachineArt.Box(6, 20, 0, 10, 24, 8),
                new MachineArt.Box(4, 16, 6, 12, 24, 9),
                new MachineArt.Box(4, 16, 0, 12, 24, 1));
    }

    /** Level III: a two-block tower on a wide plinth, with a coil set of four bands on each side, red to the west and blue to the east. */
    private static List<MachineArt.Box> levelThreeBody() {
        return List.of(
                new MachineArt.Box(0, 0, 0, 16, 2, 16),
                new MachineArt.Box(2, 2, 2, 14, 4, 14),
                new MachineArt.Box(4, 4, 4, 12, 28, 12),
                new MachineArt.Box(3, 2, 2, 13, 22, 4),
                new MachineArt.Box(2, 28, 2, 14, 32, 14),
                new MachineArt.Box(0, 5, 4, 4, 8, 12),
                new MachineArt.Box(0, 10, 4, 4, 13, 12),
                new MachineArt.Box(0, 15, 4, 4, 18, 12),
                new MachineArt.Box(0, 20, 4, 4, 23, 12),
                new MachineArt.Box(12, 5, 4, 16, 8, 12),
                new MachineArt.Box(12, 10, 4, 16, 13, 12),
                new MachineArt.Box(12, 15, 4, 16, 18, 12),
                new MachineArt.Box(12, 20, 4, 16, 23, 12));
    }

    /** Level III's barrel: a heavy rail tube with a second section stacked on it, a breech and a muzzle ring, two blocks up. */
    private static List<MachineArt.Box> levelThreeBarrel() {
        return List.of(
                new MachineArt.Box(5, 22, 0, 11, 26, 8),
                new MachineArt.Box(6, 26, 0, 10, 30, 8),
                new MachineArt.Box(4, 22, 6, 12, 30, 9),
                new MachineArt.Box(4, 22, 0, 12, 30, 1));
    }

    /** Level III's top assembly: a capacitor crown on a flange, with a dome at the very top, two blocks and a half up. */
    private static List<MachineArt.Box> levelThreeTop() {
        return List.of(
                new MachineArt.Box(4, 32, 4, 12, 35, 12),
                new MachineArt.Box(2, 35, 2, 14, 36, 14),
                new MachineArt.Box(6, 36, 6, 10, 40, 10));
    }

    /**
     * The front of a body for one frame: a plate with a dark face, a screen recess at {@link #SCREEN}, a charge rail on each side that fills
     * from the foot up to the charge, and a band along the head of the face, which glows with the charge.
     */
    private static void bodyFrame(Canvas c, int base, int accent, double charge) {
        c.paint(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), PANEL);
        c.paint(Area.rect(1, 1, 31, 31), Color.mix(base, Color.WHITE, 0.15));
        c.paint(Area.rect(3, 3, 29, 29), DARK);
        int fill = Color.mix(Color.scale(accent, 0.5), Color.mix(accent, Color.WHITE, 0.5), charge);
        for (double x : new double[]{5, 24}) {
            c.paint(Area.rect(x, 5, x + 3, 27), Color.hex("#1A1E24"));
            c.paint(Area.rect(x, 27 - 22 * charge, x + 3, 27), fill);
        }
        c.paint(Area.rect(9, 6, 23, 19), GLASS);
        c.paint(Area.rect(9, 17, 23, 19), Color.withAlpha(accent, 0.2 + 0.8 * charge));
        c.paint(Area.rect(9, 22, 23, 24), Color.withAlpha(accent, 0.15 + 0.85 * charge));
    }

    /**
     * The muzzle of a barrel for one frame: a collar around a dark bore, two coil rings around the bore that glow with the charge, a
     * charge core that swells, and a white-hot centre that shows only near full charge.
     */
    private static void muzzleFrame(Canvas c, int base, int accent, double charge) {
        c.paint(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), PANEL);
        c.paint(Area.disc(16, 16, 14), Color.mix(base, Color.WHITE, 0.1));
        c.paint(Area.disc(16, 16, 12), Color.scale(base, 0.5));
        c.paint(Area.ring(16, 16, 12, 1), Color.withAlpha(accent, 0.25 + 0.75 * charge));
        c.paint(Area.ring(16, 16, 9.5, 1.2), Color.withAlpha(accent, 0.25 + 0.75 * charge));
        c.paint(Area.disc(16, 16, 8), DARK);
        c.paint(Area.disc(16, 16, 2 + 5 * charge), Color.mix(accent, Color.WHITE, 0.2 + 0.7 * charge));
        c.paint(Area.disc(16, 16, 1 + 2 * charge), Color.withAlpha(Color.WHITE, charge));
    }

    /** The top of level III for one frame: a dark housing with louvres, and a band of charge across its middle. */
    private static void topFrame(Canvas c, int base, int accent, double charge) {
        c.paint(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), PANEL);
        c.paint(Area.rect(2, 2, 30, 30), Color.mix(base, Color.WHITE, 0.12));
        for (double y = 6; y <= 24; y += 4) {
            c.paint(Area.rect(6, y, 26, y + 1.5), Color.scale(base, 0.45));
        }
        c.paint(Area.rect(6, 14, 26, 18), Color.withAlpha(accent, 0.2 + 0.8 * charge));
    }

    /** The west side of a body: a red helix, diagonal windings on dark metal. */
    private static void westCoil(Canvas c, int base, int accent, long seed) {
        helix(c, base, RED, 1);
    }

    /** The east side of a body: a blue spiral, the windings running the other way. */
    private static void eastCoil(Canvas c, int base, int accent, long seed) {
        helix(c, base, BLUE, -1);
    }

    /** Diagonal windings of one colour on dark metal. The direction is +1 for one slope and -1 for the other. */
    private static void helix(Canvas c, int base, int winding, int direction) {
        int metal = Color.scale(base, 0.55);
        c.shade(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), (x, y, current) ->
                Math.floorMod(x + direction * 2 * y, 8) < 2 ? winding : metal);
    }

    /** The top of level III: a charge ring around a lamp. */
    private static void topUp(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ring(16, 16, 9, 1.5), accent);
        c.paint(Area.disc(16, 16, 4), Color.mix(accent, Color.WHITE, 0.3));
    }

    /** The turret's inventory sprite: a carriage with a red coil on the west and a blue coil on the east, and a barrel with its muzzle. */
    private static void turretSprite(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(5, 20, 27, 27, 2), base, seed);
        c.paint(Area.roundRect(4, 13, 9, 25, 1.5), Color.hex("#2A2E36"));
        c.paint(Area.rect(4, 15, 9, 17), RED);
        c.paint(Area.rect(4, 21, 9, 23), RED);
        c.paint(Area.roundRect(23, 13, 28, 25, 1.5), Color.hex("#2A2E36"));
        c.paint(Area.rect(23, 15, 28, 17), BLUE);
        c.paint(Area.rect(23, 21, 28, 23), BLUE);
        c.paint(Area.roundRect(9, 13, 22, 18, 1), Color.mix(base, Color.WHITE, 0.2));
        c.paint(Area.rect(20, 11, 23, 20), accent);
        c.paint(Area.disc(10, 27, 2.4), Color.hex("#1A1A1E"));
        c.paint(Area.disc(22, 27, 2.4), Color.hex("#1A1A1E"));
    }

    /** The rail coil's inventory sprite: four copper windings on a red lead. */
    private static void coilSprite(Canvas c, int base, int accent, long seed) {
        for (int k = 0; k < 4; k++) {
            double y = 6 + 6 * k;
            c.paint(Area.roundRect(6, y, 26, y + 4, 2), Color.mix(base, Color.WHITE, 0.08 * k));
        }
        c.paint(Area.rect(15, 2, 17, 30), accent);
    }

    /** The rail barrel's inventory sprite: a diagonal tube with a rail along it and a bright muzzle. */
    private static void railSprite(Canvas c, int base, int accent, long seed) {
        c.paint(Area.line(5, 27, 26, 7, 6), Color.scale(base, 0.85));
        c.paint(Area.line(5, 27, 26, 7, 1.5), accent);
        c.paint(Area.disc(26, 7, 3.5), Color.scale(base, 0.5));
        c.paint(Area.disc(26, 7, 1.5), accent);
    }
}
