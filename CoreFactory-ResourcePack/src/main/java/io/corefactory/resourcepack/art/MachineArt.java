package io.corefactory.resourcepack.art;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The 3D model of every placeable machine. A machine is a few cuboids (its silhouette) and six face textures, one per
 * side of the model: the front ({@link Face#NORTH}) carries the machine's own detail, and the other faces carry shared
 * casing patterns in the machine's colours. Each face is a 32 x 32 texture painted by {@link Canvas}. The animated faces
 * of some machines are painted by {@link MachineAnimation} instead, as strips of frames.
 */
public final class MachineArt {

    /** The six sides of a model. The lower-case key is the texture variable in the model JSON. */
    public enum Face {
        NORTH, SOUTH, EAST, WEST, UP, DOWN;

        /** The texture variable and the file name for this side, such as {@code north}. */
        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * One cuboid of the model. Corners are in model units, from 0 to 16 across one block, with y pointing up.
     *
     * @param x0 minimum x
     * @param y0 minimum y
     * @param z0 minimum z
     * @param x1 maximum x
     * @param y1 maximum y
     * @param z1 maximum z
     */
    public record Box(double x0, double y0, double z0, double x1, double y1, double z1) {
    }

    /**
     * One machine's look.
     *
     * @param id     the machine id from materials.yml and processes.yml
     * @param base   the casing colour, {@code #RRGGBB}
     * @param accent the detail colour, {@code #RRGGBB}
     * @param boxes  the cuboids that make the silhouette
     * @param front  paints the front face, the machine's own detail
     * @param faces  paints the other sides that carry their own detail; a side not listed gets the shared pattern
     */
    public record Spec(String id, String base, String accent, List<Box> boxes, ComponentArt.Painter front,
                       Map<Face, ComponentArt.Painter> faces) {

        public Spec(String id, String base, String accent, List<Box> boxes, ComponentArt.Painter front) {
            this(id, base, accent, boxes, front, Map.of());
        }
    }

    private static final String IRON = "#2B2B2F";
    private static final String RUST = "#B5482A";
    private static final String STEEL = "#6E7884";
    private static final String BLUE = "#3AA8FF";
    private static final String DARK_STEEL = "#3A4048";

    /** The machines modelled here: the process chain, the drills of levels 1 to 3, and the logistics devices. */
    private static final List<Spec> BASE = List.of(
            new Spec("crusher", "#5A6470", "#FFB000", List.of(
                    new Box(1, 0, 1, 15, 9, 15),
                    new Box(4, 9, 4, 12, 15, 12),
                    new Box(0, 4, 4, 16, 8, 12)), MachineArt::crusherFront),
            new Spec("roaster", "#6A3A24", "#FF7A20", List.of(
                    new Box(1, 0, 1, 15, 6, 15),
                    new Box(5, 6, 5, 11, 16, 11),
                    new Box(11, 2, 6, 16, 4, 10)), MachineArt::roasterFront),
            new Spec("separator", "#3A5A6A", "#9FF0FF", List.of(
                    new Box(2, 0, 2, 14, 10, 14),
                    new Box(0, 10, 0, 16, 12, 16),
                    new Box(6, 12, 6, 10, 16, 10)), MachineArt::separatorFront),
            new Spec("purifier", "#2F6A5A", "#7FFFD0", List.of(
                    new Box(4, 0, 4, 12, 14, 12),
                    new Box(2, 3, 2, 14, 5, 14),
                    new Box(2, 9, 2, 14, 11, 14),
                    new Box(5, 14, 5, 11, 16, 11)), MachineArt::purifierFront),
            new Spec("converter", "#5A5A6A", "#FFD040", List.of(
                    new Box(4, 0, 4, 12, 15, 12),
                    new Box(2, 0, 2, 14, 2, 14),
                    new Box(12, 6, 6, 16, 11, 10)), MachineArt::converterFront),
            new Spec("transmuter", "#2A2038", "#B56BFF", List.of(
                    new Box(3, 0, 3, 13, 4, 13),
                    new Box(5, 4, 5, 11, 12, 11),
                    new Box(1, 6, 1, 15, 9, 3),
                    new Box(1, 6, 13, 15, 9, 15),
                    new Box(1, 6, 3, 3, 9, 13),
                    new Box(13, 6, 3, 15, 9, 13)), MachineArt::transmuterFront),
            new Spec("spectrometer", "#2A3A44", "#55FFFF", List.of(
                    new Box(0, 0, 0, 16, 2, 12),
                    new Box(2, 2, 3, 13, 8, 9),
                    new Box(13, 3, 4, 15, 7, 8),
                    new Box(4, 8, 9.5, 12, 14, 10.5)), MachineArt::spectrometerFront),
            new Spec("research_console", "#4A5058", "#66FF99", List.of(
                    new Box(0, 7, 2, 16, 8, 14),
                    new Box(0, 0, 3, 2, 7, 13),
                    new Box(14, 0, 3, 16, 7, 13),
                    new Box(3, 8, 8, 13, 15, 9),
                    new Box(3, 8, 3, 13, 9, 7)), MachineArt::consoleFront),
            new Spec("pylon_1", "#7C8590", "#C87533", pylonBoxes(1), MachineArt::pylonFront),
            new Spec("pylon_2", "#7C8590", "#E0B040", pylonBoxes(2), MachineArt::pylonFront),
            new Spec("pylon_3", "#7C8590", "#9B5CFF", pylonBoxes(3), MachineArt::pylonFront),
            new Spec("accumulator_1", "#8A929C", "#5BC8F0", accumulatorBoxes(), MachineArt::animatedFace),
            new Spec("accumulator_2", "#8A929C", "#3F7BFF", accumulatorBoxes(), MachineArt::animatedFace),
            new Spec("accumulator_3", "#8A929C", "#8A5CFF", accumulatorBoxes(), MachineArt::animatedFace),
            drill("petroleum_drill_1", IRON, RUST, 1, true),
            drill("petroleum_drill_2", IRON, RUST, 2, true),
            drill("petroleum_drill_3", IRON, RUST, 3, true),
            drill("ore_drill_1", STEEL, BLUE, 1, false),
            drill("ore_drill_2", STEEL, BLUE, 2, false),
            drill("ore_drill_3", STEEL, BLUE, 3, false),
            picker(1, "#B87333"),
            picker(2, "#E0B030"),
            picker(3, "#9B5CFF"),
            splitter(1, "#4A4F58", "#C87533"),
            splitter(2, "#5A5230", "#D8A83A"),
            splitter(3, "#5E4E2A", "#9B5CFF"));

    /**
     * Every machine: the modelled machines above, the generators and solar panel, the drills of levels 4 and 5, the
     * large lab tools and the turrets, each in its own class.
     */
    public static final List<Spec> ALL = Stream.of(BASE, GeneratorArt.ALL, DrillArt.ALL, BigLabArt.ALL, TurretArt.ALL,
                    TurretLevelArt.ALL)
            .flatMap(List::stream).toList();

    private MachineArt() {
    }

    /**
     * A picker of one level: a small cube housing. Its north face is the input, marked by an arrow pointing into the
     * block, and its south face is the output, marked by an arrow pointing out. East and west carry a connector stripe.
     */
    private static Spec picker(int level, String accent) {
        return new Spec("picker_" + level, DARK_STEEL, accent, List.of(
                new Box(3, 0, 3, 13, 11, 13),
                new Box(5, 11, 5, 11, 13, 11)), MachineArt::inputArrow, Map.of(
                Face.SOUTH, MachineArt::outputArrow,
                Face.EAST, MachineArt::connectorEast,
                Face.WEST, MachineArt::connectorWest));
    }

    /**
     * A splitter of one level: a wide housing, sixteen units across. North is the input, south the straight output, and
     * east and west the right and left outputs. Up and down carry the filter panel, whose slots differ in each.
     */
    private static Spec splitter(int level, String base, String accent) {
        return new Spec("splitter_" + level, base, accent, List.of(
                new Box(0, 0, 3, 16, 9, 13),
                new Box(5, 9, 5, 11, 11, 11)), MachineArt::inputArrow, Map.of(
                Face.SOUTH, MachineArt::outputArrow,
                Face.EAST, MachineArt::rightArrow,
                Face.WEST, MachineArt::leftArrow,
                Face.UP, MachineArt::filterPanelUp,
                Face.DOWN, MachineArt::filterPanelDown));
    }

    /** A drill of one level, 1 to 5: a bit, a housing, a tower and a level cap. */
    private static Spec drill(String id, String base, String accent, int level, boolean pump) {
        return new Spec(id, base, accent, drillBoxes(level, pump), drillFront(level));
    }

    /**
     * The cuboids of a drill. The bit points down from y 0 and is taller and wider at higher levels, the housing sits on
     * it, the tower rises from the housing and grows with the level, and the cap on top is the level indicator. A
     * petroleum drill has a pump head on the east side of the tower.
     */
    private static List<Box> drillBoxes(int level, boolean pump) {
        double bit = 1 + 0.5 * level;
        double tower = 1.5 + 0.5 * level;
        double top = bit + 6 + level;
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(8 - bit * 0.5, 0, 8 - bit * 0.5, 8 + bit * 0.5, bit * 0.5, 8 + bit * 0.5));
        boxes.add(new Box(8 - bit, bit * 0.5, 8 - bit, 8 + bit, bit, 8 + bit));
        boxes.add(new Box(2, bit, 2, 14, bit + 3, 14));
        boxes.add(new Box(8 - tower, bit + 3, 8 - tower, 8 + tower, top, 8 + tower));
        boxes.add(new Box(8 - tower - 0.5, top, 8 - tower - 0.5, 8 + tower + 0.5, top + 1.5, 8 + tower + 0.5));
        if (pump) {
            boxes.add(new Box(8 + tower - 0.5, bit + 3.5, 6, 16, bit + 5.5, 10));
        }
        return boxes;
    }

    /**
     * The north face of a drill: a band at each end, and five slots down the tower, the first {@code level} of them lit.
     * The slots are the level indicator, so a level 1 drill has one lit slot and a level 5 drill has all five.
     */
    private static ComponentArt.Painter drillFront(int level) {
        return (c, base, accent, seed) -> {
            c.paint(Area.rect(6, 3, 26, 5), accent);
            c.paint(Area.rect(6, 27, 26, 29), accent);
            for (int k = 0; k < 5; k++) {
                double y = 23 - 4 * k;
                c.paint(Area.rect(8, y, 24, y + 3), k < level ? Color.hex("#FFD040") : Color.scale(base, 0.45));
            }
        };
    }

    /**
     * Paints one face of a machine onto a fresh canvas. Every face of every machine is different. An animated face
     * ({@link MachineAnimation#animates}) shows its first frame here; the strip holds all of them.
     */
    public static void paintFace(Canvas c, Spec spec, Face face) {
        int base = Color.hex(spec.base());
        int accent = Color.hex(spec.accent());
        long seed = spec.id().hashCode() * 31L + face.ordinal();

        paintCasing(c, spec, face);
        if (MachineAnimation.animates(spec, face)) {
            MachineAnimation.paintFrame(c, spec, 0);
            return;
        }

        ComponentArt.Painter detail = spec.faces().get(face);
        if (detail != null) {
            detail.paint(c, base, accent, seed);
            return;
        }
        switch (face) {
            case NORTH -> spec.front().paint(c, base, accent, seed);
            case SOUTH -> {
                for (int y = 6; y <= 26; y += 4) {
                    c.paint(Area.rect(6, y, 26, y + 1), Color.scale(base, 0.45));
                }
                c.paint(Area.disc(24, 28, 1.2), accent);
            }
            case EAST -> {
                c.paint(Area.rect(0, 13, Canvas.SIZE, 16), accent);
                for (int x = 3; x < Canvas.SIZE; x += 6) {
                    c.paint(Area.disc(x, 10, 0.9), Color.scale(base, 0.5));
                }
            }
            case WEST -> {
                for (int x = 4; x < 28; x += 6) {
                    c.paint(Area.rect(x, 4, x + 1.5, 28), accent);
                }
            }
            case UP -> {
                c.paint(Area.ring(16, 16, 7, 1.2), accent);
                c.paint(Area.disc(16, 16, 3), Color.scale(base, 0.4));
            }
            case DOWN -> c.shade(Area.rect(0, 25, Canvas.SIZE, Canvas.SIZE), (x, y, current) ->
                    ((x + y) / 4) % 2 == 0 ? accent : Color.scale(base, 0.35));
        }
    }

    /** The casing every face starts from: the base colour with a little grain, darker toward the bottom, and a dark rim. */
    static void paintCasing(Canvas c, Spec spec, Face face) {
        int base = Color.hex(spec.base());
        long seed = spec.id().hashCode() * 31L + face.ordinal();

        c.shade(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), (x, y, current) -> {
            double falloff = 1.08 - 0.16 * y / 31.0;
            double grain = (Noise.hash(x, y, seed) - 0.5) * 0.12;
            return Color.scale(base, falloff * (1 + grain));
        });
        Area rim = Area.rect(0, 0, Canvas.SIZE, 1).or(Area.rect(0, 31, Canvas.SIZE, Canvas.SIZE))
                .or(Area.rect(0, 0, 1, Canvas.SIZE)).or(Area.rect(31, 0, Canvas.SIZE, Canvas.SIZE));
        c.paint(rim, Color.scale(base, 0.4));
    }

    private static void crusherFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(6, 3, 26, 7), Color.hex("#1A1A1A"));
        c.paint(Area.ring(16, 19, 6, 2), accent);
        c.paint(Area.disc(16, 19, 3.5), Color.scale(base, 0.4));
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            c.paint(Area.line(16, 19, 16 + Math.cos(a) * 3.5, 19 + Math.sin(a) * 3.5, 0.8), accent);
        }
    }

    private static void roasterFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.roundRect(9, 12, 23, 28, 6), Color.hex("#140804"));
        c.paint(Area.roundRect(11, 14, 21, 26, 4), Color.withAlpha(accent, 0.75));
        for (int y = 17; y <= 23; y += 3) {
            c.paint(Area.rect(11, y, 21, y + 1), Color.scale(base, 0.4));
        }
    }

    private static void separatorFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ring(16, 13, 6, 1.6), accent);
        c.paint(Area.line(16, 13, 21, 9, 1), Color.hex("#F4F5F7"));
        c.paint(Area.disc(16, 13, 1.5), Color.hex("#F4F5F7"));
        c.paint(Area.rect(7, 22, 25, 26), Color.hex("#1A2A30"));
        c.paint(Area.rect(9, 23, 23, 25), Color.withAlpha(accent, 0.5));
    }

    private static void purifierFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(13, 6, 19, 26), Color.withAlpha(Color.WHITE, 0.35));
        for (int y = 4; y <= 22; y += 6) {
            c.paint(Area.rect(3, y, 29, y + 2), accent);
        }
    }

    private static void converterFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 16, 8), Color.hex("#E8E8E8"));
        c.paint(Area.ring(16, 16, 8, 1.6), accent);
        c.paint(Area.line(16, 16, 21, 11, 1), Color.hex("#D03020"));
        c.paint(Area.disc(16, 16, 1.2), Color.hex("#202020"));
    }

    private static void transmuterFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ring(16, 16, 9, 2.5), Color.withAlpha(accent, 0.6));
        c.paint(Area.ring(16, 16, 9, 1), accent);
        c.paint(Area.disc(16, 16, 3.5), Color.withAlpha(accent, 0.8));
        c.paint(Area.disc(16, 16, 1.5), Color.withAlpha(Color.WHITE, 0.9));
    }

    private static void spectrometerFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 12, 7), Color.hex("#101820"));
        c.paint(Area.ring(16, 12, 7, 1.4), accent);
        c.paint(Area.disc(16, 12, 3.5), Color.withAlpha(accent, 0.6));
        c.paint(Area.rect(7, 21, 25, 27), Color.hex("#0E1A1E"));
        c.paint(Area.line(9, 25, 14, 22, 0.8), accent);
        c.paint(Area.line(14, 22, 18, 24, 0.8), accent);
        c.paint(Area.line(18, 24, 23, 22, 0.8), accent);
    }

    private static void consoleFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(6, 4, 26, 17), Color.hex("#0E1A14"));
        c.paint(Area.line(8, 14, 13, 9, 1), accent);
        c.paint(Area.line(13, 9, 18, 12, 1), accent);
        c.paint(Area.line(18, 12, 24, 6, 1), accent);
        c.paint(Area.rect(4, 20, 28, 22), Color.scale(base, 0.5));
    }

    /**
     * A lattice tower: a base, four posts, a cap and a band of cross-bars on each side. The first level is short, the
     * second taller, and the third tallest, with three cross-arms through the top.
     */
    private static List<Box> pylonBoxes(int level) {
        double top = level == 1 ? 11 : level == 2 ? 14 : 15;
        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(3, 0, 3, 13, 1, 13));
        boxes.add(new Box(4, top, 4, 12, top + 1, 12));
        for (double x : new double[]{3, 12}) {
            for (double z : new double[]{3, 12}) {
                boxes.add(new Box(x, 1, z, x + 1, top, z + 1));
            }
        }
        double[] bands = level == 1 ? new double[]{3, 7} : new double[]{3, 7, 11};
        for (double y : bands) {
            boxes.add(new Box(3, y, 3, 13, y + 1, 4));
            boxes.add(new Box(3, y, 12, 13, y + 1, 13));
            boxes.add(new Box(3, y, 4, 4, y + 1, 12));
            boxes.add(new Box(12, y, 4, 13, y + 1, 12));
        }
        if (level == 3) {
            for (double y : new double[]{6, 10, 14}) {
                boxes.add(new Box(0, y, 7, 16, y + 1, 9));
            }
        }
        return boxes;
    }

    /** A steel cell, rounded by three overlapping blocks, with a cap and a foot. */
    private static List<Box> accumulatorBoxes() {
        return List.of(
                new Box(3, 0, 3, 13, 1, 13),
                new Box(4, 1, 2, 12, 15, 14),
                new Box(2, 1, 4, 14, 15, 12),
                new Box(3, 1, 3, 13, 15, 13),
                new Box(4, 15, 4, 12, 16, 12));
    }

    /** The north face of a pylon: an accent lattice of diagonals, insulator discs, and a band at the foot. */
    private static void pylonFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.line(0, 0, 32, 32, 1.2), Color.scale(base, 0.6));
        c.paint(Area.line(32, 0, 0, 32, 1.2), Color.scale(base, 0.6));
        c.paint(Area.disc(5, 5, 2.2), accent);
        c.paint(Area.disc(27, 5, 2.2), accent);
        c.paint(Area.disc(16, 16, 2.2), accent);
        c.paint(Area.rect(0, 26, Canvas.SIZE, 29), accent);
    }

    /**
     * The front of a machine whose front face is animated. {@link MachineAnimation} paints that face, frame by frame, over
     * the casing, so this painter is never called for it.
     */
    private static void animatedFace(Canvas c, int base, int accent, long seed) {
    }

    /** An arrow pointing down into the block, in a recess: the input of a picker or a splitter. */
    private static void inputArrow(Canvas c, int base, int accent, long seed) {
        arrow(c, base, accent, 16, 8, 16, 23);
    }

    /** An arrow pointing up, away from the block: the straight output of a picker or a splitter. */
    private static void outputArrow(Canvas c, int base, int accent, long seed) {
        arrow(c, base, accent, 16, 24, 16, 9);
    }

    /** An arrow pointing right: the right output of a splitter. */
    private static void rightArrow(Canvas c, int base, int accent, long seed) {
        arrow(c, base, accent, 8, 16, 25, 16);
    }

    /** An arrow pointing left: the left output of a splitter. */
    private static void leftArrow(Canvas c, int base, int accent, long seed) {
        arrow(c, base, accent, 24, 16, 7, 16);
    }

    /** A bright arrow from ({@code x0}, {@code y0}) to its head at ({@code x1}, {@code y1}), in a dark recess. */
    private static void arrow(Canvas c, int base, int accent, double x0, double y0, double x1, double y1) {
        c.paint(Area.roundRect(6, 6, 26, 26, 3), Color.scale(base, 0.45));
        double length = Math.hypot(x1 - x0, y1 - y0);
        double ux = (x1 - x0) / length;
        double uy = (y1 - y0) / length;
        int bright = Color.mix(accent, Color.WHITE, 0.25);
        c.paint(Area.line(x0, y0, x1, y1, 2.4), bright);
        for (double turn : new double[]{Math.PI / 4, -Math.PI / 4}) {
            double bx = -ux * Math.cos(turn) + uy * Math.sin(turn);
            double by = -ux * Math.sin(turn) - uy * Math.cos(turn);
            c.paint(Area.line(x1, y1, x1 + bx * 5, y1 + by * 5, 2.4), bright);
        }
    }

    /** The connector stripe of a picker's east face: a band across the face, with two rivets. */
    private static void connectorEast(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 14, Canvas.SIZE, 18), accent);
        c.paint(Area.disc(5, 16, 1), Color.scale(base, 0.5));
        c.paint(Area.disc(27, 16, 1), Color.scale(base, 0.5));
    }

    /** The connector of a picker's west face: two thin bands across the face. */
    private static void connectorWest(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 9, Canvas.SIZE, 11), accent);
        c.paint(Area.rect(0, 21, Canvas.SIZE, 23), accent);
    }

    /** The top of a splitter: a 4 x 4 grid of filter slots. */
    private static void filterPanelUp(Canvas c, int base, int accent, long seed) {
        filterPanel(c, base, accent, 6, 6, 6, 4);
    }

    /** The bottom of a splitter: a 3 x 3 grid of larger filter slots, spaced wider. */
    private static void filterPanelDown(Canvas c, int base, int accent, long seed) {
        filterPanel(c, base, accent, 5, 5, 8, 3);
    }

    private static void filterPanel(Canvas c, int base, int accent, double first, double step, double size, int count) {
        c.paint(Area.rect(3, 3, 29, 29), Color.hex("#14181C"));
        for (int row = 0; row < count; row++) {
            for (int col = 0; col < count; col++) {
                double x = first + col * step;
                double y = first + row * step;
                c.paint(Area.rect(x, y, x + size - 1, y + size - 1), Color.scale(accent, 0.6));
            }
        }
    }
}
