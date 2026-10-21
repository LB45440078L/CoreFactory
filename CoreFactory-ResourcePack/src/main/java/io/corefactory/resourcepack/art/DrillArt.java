package io.corefactory.resourcepack.art;

import java.util.ArrayList;
import java.util.List;

/**
 * The drills of levels 4 and 5, each two blocks tall: a lower block with the bit and the housing, and an upper block,
 * whose id is that id with {@code _top}, with the tower and the level cap. The lower block carries the level indicator on
 * its front, one lit slot per level. The upper block's front is a drive shaft whose helical stripes scroll down, so the
 * bit reads as spinning. A petroleum drill has a pump head on the east side of its tower.
 */
public final class DrillArt {

    private static final String STEEL = "#6E7884";
    private static final String BLUE = "#3AA8FF";
    private static final String IRON = "#2B2B2F";
    private static final String RUST = "#B5482A";
    private static final int GOLD = Color.hex("#FFD040");
    private static final int SHAFT = Color.hex("#1A1E24");
    private static final int TIP = Color.hex("#D8DEE6");

    /** The two lower and upper pairs of each drill tier, in the order of the models list. */
    public static final List<MachineArt.Spec> ALL = List.of(
            lower("ore_drill_4", STEEL, BLUE, 4),
            upper("ore_drill_4_top", STEEL, BLUE, 4, false),
            lower("ore_drill_5", STEEL, BLUE, 5),
            upper("ore_drill_5_top", STEEL, BLUE, 5, false),
            lower("petroleum_drill_4", IRON, RUST, 4),
            upper("petroleum_drill_4_top", IRON, RUST, 4, true),
            lower("petroleum_drill_5", IRON, RUST, 5),
            upper("petroleum_drill_5_top", IRON, RUST, 5, true));

    /** The drive shaft of each upper drill: the animated front. */
    public static final List<MachineAnimation.Clip> CLIPS = List.of(
            new MachineAnimation.Clip("ore_drill_4_top", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("ore_drill_5_top", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("petroleum_drill_4_top", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("petroleum_drill_5_top", MachineArt.Face.NORTH, 2));

    private DrillArt() {
    }

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /** Paints the drive shaft for one frame of an upper drill. */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        if (!owns(spec.id())) {
            throw new IllegalArgumentException("No animation for " + spec.id());
        }
        drive(c, Color.hex(spec.base()), Color.hex(spec.accent()), frame);
    }

    /** The bit's half width: the lower block's bit reaches a little wider at each tier. */
    private static double bitHalf(int level) {
        return level == 4 ? 1.5 : 2;
    }

    /** The housing's half width: the lower block is wider than the tiers below. */
    private static double housingHalf(int level) {
        return level == 4 ? 3.5 : 4.5;
    }

    /** The tower's half width: the upper block's tower, taller than the tiers below. */
    private static double towerHalf(int level) {
        return level == 4 ? 3 : 3.5;
    }

    /**
     * The lower block: a bit in the bottom four units, and the housing above it up to the top of the block. Its front is
     * the level indicator.
     */
    private static MachineArt.Spec lower(String id, String base, String accent, int level) {
        double bit = bitHalf(level);
        double housing = housingHalf(level);
        return new MachineArt.Spec(id, base, accent, List.of(
                new MachineArt.Box(8 - bit, 0, 8 - bit, 8 + bit, 4, 8 + bit),
                new MachineArt.Box(8 - housing, 4, 8 - housing, 8 + housing, 16, 8 + housing)),
                (c, b, a, seed) -> indicator(c, b, a, level));
    }

    /**
     * The upper block: the tower from the bottom of the block to its cap, and the level cap on top. A petroleum drill has
     * a pump head on the east side of its tower.
     */
    private static MachineArt.Spec upper(String id, String base, String accent, int level, boolean pump) {
        double tower = towerHalf(level);
        double cap = tower + 0.5;
        List<MachineArt.Box> boxes = new ArrayList<>();
        boxes.add(new MachineArt.Box(8 - tower, 0, 8 - tower, 8 + tower, 13, 8 + tower));
        boxes.add(new MachineArt.Box(8 - cap, 13, 8 - cap, 8 + cap, 16, 8 + cap));
        if (pump) {
            boxes.add(new MachineArt.Box(8 + tower - 0.5, 4, 6, 15, 8, 10));
        }
        return new MachineArt.Spec(id, base, accent, boxes, (c, b, a, seed) -> {
        });
    }

    /**
     * The front of a lower block: a band at each end, and five slots down the housing, the first {@code level} of them
     * lit. A level 4 drill has four lit slots and a level 5 drill has all five.
     */
    private static void indicator(Canvas c, int base, int accent, int level) {
        c.paint(Area.rect(6, 3, 26, 5), accent);
        c.paint(Area.rect(6, 27, 26, 29), accent);
        for (int k = 0; k < 5; k++) {
            double y = 23 - 4 * k;
            c.paint(Area.rect(8, y, 24, y + 3), k < level ? GOLD : Color.scale(base, 0.45));
        }
    }

    /**
     * The drive shaft of an upper drill: a dark shaft with slanted stripes that step down one pixel a frame, eight
     * stripes of a period of 8 pixels, so the eight frames loop. A point finishes the shaft.
     */
    private static void drive(Canvas c, int base, int accent, int frame) {
        Area shaft = Area.rect(10, 0, 22, 24);
        c.paint(shaft, SHAFT);
        for (int i = -1; i <= 4; i++) {
            double y = 8.0 * i + frame;
            c.paint(Area.line(9, y + 6, 23, y, 2).and(shaft), accent);
        }
        c.paint(Area.polygon(10, 24, 22, 24, 16, 31), TIP);
        c.paint(Area.rect(0, 0, Canvas.SIZE, 1), Color.scale(base, 0.5));
    }
}
