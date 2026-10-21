package io.corefactory.resourcepack.art;

import java.util.List;
import java.util.Map;

/**
 * The upgraded turrets, levels 2 to 5, each with a model of its own. Level 1 is in {@link TurretArt}. Level 2 of each kind
 * is one block tall. Levels 3, 4 and 5 are two blocks tall: the lower half has the level's id, such as {@code laser_turret_3},
 * and the upper half has that id with {@code _top}. Each half fills a block, so the display stacks the upper half one block
 * above the lower.
 *
 * <p>The machine gun levels animate their lower half: the indicator and ammunition counter of level 1. The laser levels
 * animate their lens, which sits in the upper half of a tall laser. From level 5 the lasers are blue. The upgrade kits are
 * the item sprites at the end of this class.
 */
public final class TurretLevelArt {

    private static final ComponentArt.Painter NONE = (c, base, accent, seed) -> {
    };

    /**
     * The upgraded turrets. The machine gun is steel grey at levels 2 and 3, bronze at 4 and dark gunmetal at 5. The laser
     * is dark at every level, red at 2 to 4 and blue at 5.
     */
    public static final List<MachineArt.Spec> ALL = List.of(
            new MachineArt.Spec("machine_gun_turret_2", "#6E7884", "#E0A030", List.of(
                    new MachineArt.Box(1, 0, 1, 15, 2, 15),
                    new MachineArt.Box(5, 2, 5, 11, 7, 11),
                    new MachineArt.Box(4, 7, 4, 12, 8, 12),
                    new MachineArt.Box(3, 8, 5, 5, 13, 11),
                    new MachineArt.Box(5, 8, 5, 11, 13, 6),
                    new MachineArt.Box(5, 8, 10, 11, 13, 11),
                    new MachineArt.Box(7, 8, 6, 10, 12, 10),
                    new MachineArt.Box(5, 9, 7, 13, 11, 9),
                    new MachineArt.Box(13, 8, 6, 16, 12, 10),
                    new MachineArt.Box(1, 2, 9, 5, 7, 14),
                    new MachineArt.Box(3, 13, 6, 5, 14, 8)), NONE, Map.of(
                    MachineArt.Face.UP, TurretLevelArt::mgTop,
                    MachineArt.Face.WEST, TurretLevelArt::beltSide,
                    MachineArt.Face.EAST, TurretLevelArt::muzzleEast)),
            new MachineArt.Spec("machine_gun_turret_3", "#5E6774", "#55D8FF", List.of(
                    new MachineArt.Box(1, 0, 1, 15, 2, 15),
                    new MachineArt.Box(5, 2, 5, 11, 10, 11),
                    new MachineArt.Box(4, 10, 4, 12, 11, 12),
                    new MachineArt.Box(0, 2, 7, 4, 11, 15),
                    new MachineArt.Box(3, 11, 5, 5, 15, 11),
                    new MachineArt.Box(5, 11, 5, 11, 15, 6),
                    new MachineArt.Box(5, 11, 10, 11, 15, 11),
                    new MachineArt.Box(7, 11, 6, 10, 14, 10),
                    new MachineArt.Box(5, 12, 7, 13, 14, 9),
                    new MachineArt.Box(13, 11, 6, 16, 15, 10),
                    new MachineArt.Box(0, 11, 11, 2, 16, 13),
                    new MachineArt.Box(6, 14, 6, 10, 16, 10)), NONE, Map.of(
                    MachineArt.Face.WEST, TurretLevelArt::beltSide,
                    MachineArt.Face.EAST, TurretLevelArt::muzzleEast)),
            new MachineArt.Spec("machine_gun_turret_3_top", "#5E6774", "#55D8FF", List.of(
                    new MachineArt.Box(0, 0, 7, 4, 8, 15),
                    new MachineArt.Box(7, 0, 7, 9, 12, 9),
                    new MachineArt.Box(5, 12, 5, 11, 15, 11),
                    new MachineArt.Box(7.5, 15, 7.5, 8.5, 16, 8.5)), TurretLevelArt::sensorFront, Map.of(
                    MachineArt.Face.UP, TurretLevelArt::sensorTop,
                    MachineArt.Face.WEST, TurretLevelArt::beltSide)),
            new MachineArt.Spec("machine_gun_turret_4", "#8C6239", "#E8A040", List.of(
                    new MachineArt.Box(1, 0, 1, 15, 2, 15),
                    new MachineArt.Box(4, 2, 4, 12, 9, 12),
                    new MachineArt.Box(3, 9, 3, 13, 10, 13),
                    new MachineArt.Box(0, 2, 6, 4, 10, 14),
                    new MachineArt.Box(3, 10, 4, 5, 14, 12),
                    new MachineArt.Box(11, 10, 4, 13, 14, 12),
                    new MachineArt.Box(3, 14, 4, 13, 16, 12),
                    new MachineArt.Box(5, 11, 5, 14, 13, 7),
                    new MachineArt.Box(5, 11, 9, 14, 13, 11),
                    new MachineArt.Box(14, 10.5, 4.5, 16, 13.5, 7.5),
                    new MachineArt.Box(14, 10.5, 8.5, 16, 13.5, 11.5),
                    new MachineArt.Box(0, 10, 10, 2, 16, 12)), NONE, Map.of(
                    MachineArt.Face.WEST, TurretLevelArt::beltSide,
                    MachineArt.Face.EAST, TurretLevelArt::twinMuzzleEast)),
            new MachineArt.Spec("machine_gun_turret_4_top", "#8C6239", "#E8A040", List.of(
                    new MachineArt.Box(0, 0, 6, 4, 8, 14),
                    new MachineArt.Box(4, 2, 4, 12, 12, 12),
                    new MachineArt.Box(7, 12, 7, 9, 15, 9),
                    new MachineArt.Box(2, 15, 2, 14, 16, 14)), TurretLevelArt::housingFront, Map.of(
                    MachineArt.Face.UP, TurretLevelArt::roofVents,
                    MachineArt.Face.WEST, TurretLevelArt::beltSide)),
            new MachineArt.Spec("machine_gun_turret_5", "#2B3038", "#FF6A2A", List.of(
                    new MachineArt.Box(0, 0, 0, 16, 2, 16),
                    new MachineArt.Box(4, 2, 4, 12, 9, 12),
                    new MachineArt.Box(3, 9, 3, 13, 10, 13),
                    new MachineArt.Box(0, 2, 6, 4, 10, 14),
                    new MachineArt.Box(12, 2, 2, 16, 10, 14),
                    new MachineArt.Box(3, 10, 4, 5, 14, 12),
                    new MachineArt.Box(11, 10, 4, 13, 14, 12),
                    new MachineArt.Box(3, 14, 4, 13, 16, 12),
                    new MachineArt.Box(5, 11, 5, 14, 13, 7),
                    new MachineArt.Box(5, 11, 9, 14, 13, 11),
                    new MachineArt.Box(14, 10.5, 4.5, 16, 13.5, 7.5),
                    new MachineArt.Box(14, 10.5, 8.5, 16, 13.5, 11.5),
                    new MachineArt.Box(0, 10, 10, 2, 16, 12)), NONE, Map.of(
                    MachineArt.Face.WEST, TurretLevelArt::beltSide,
                    MachineArt.Face.EAST, TurretLevelArt::armourEast)),
            new MachineArt.Spec("machine_gun_turret_5_top", "#2B3038", "#FF6A2A", List.of(
                    new MachineArt.Box(0, 0, 6, 4, 8, 14),
                    new MachineArt.Box(4, 1, 2, 14, 12, 14),
                    new MachineArt.Box(2, 12, 2, 14, 14, 14),
                    new MachineArt.Box(4, 14, 4, 12, 16, 5),
                    new MachineArt.Box(4, 14, 11, 12, 16, 12),
                    new MachineArt.Box(4, 14, 5, 5, 16, 11),
                    new MachineArt.Box(11, 14, 5, 12, 16, 11)), TurretLevelArt::armourFront, Map.of(
                    MachineArt.Face.UP, TurretLevelArt::statusRingTop,
                    MachineArt.Face.WEST, TurretLevelArt::beltSide)),
            new MachineArt.Spec("laser_turret_2", "#33383F", "#FF2A2A", List.of(
                    new MachineArt.Box(2, 0, 2, 14, 2, 14),
                    new MachineArt.Box(4, 2, 4, 12, 6, 12),
                    new MachineArt.Box(2, 6, 2, 14, 7, 14),
                    new MachineArt.Box(3, 7, 3, 13, 8, 13),
                    new MachineArt.Box(4, 8, 4, 12, 9, 12),
                    new MachineArt.Box(5, 9, 5, 11, 12, 11),
                    new MachineArt.Box(4, 12, 4, 12, 13, 12),
                    new MachineArt.Box(3, 13, 3, 13, 14, 13),
                    new MachineArt.Box(5, 14, 5, 11, 15, 11),
                    new MachineArt.Box(0, 4, 6, 2, 9, 10),
                    new MachineArt.Box(14, 4, 6, 16, 9, 10)), NONE, Map.of(
                    MachineArt.Face.UP, TurretLevelArt::lensTopTwoRings,
                    MachineArt.Face.WEST, TurretLevelArt::finSide,
                    MachineArt.Face.EAST, TurretLevelArt::finSide)),
            new MachineArt.Spec("laser_turret_3", "#2E333B", "#FF2A2A", List.of(
                    new MachineArt.Box(2, 0, 2, 14, 2, 14),
                    new MachineArt.Box(3, 2, 3, 13, 5, 13),
                    new MachineArt.Box(1, 5, 1, 15, 8, 15),
                    new MachineArt.Box(6, 8, 6, 10, 16, 10),
                    new MachineArt.Box(0, 4, 6, 2, 9, 10),
                    new MachineArt.Box(14, 4, 6, 16, 9, 10)), TurretLevelArt::dishFront, Map.of(
                    MachineArt.Face.WEST, TurretLevelArt::finSide,
                    MachineArt.Face.EAST, TurretLevelArt::finSide)),
            new MachineArt.Spec("laser_turret_3_top", "#2E333B", "#FF2A2A", List.of(
                    new MachineArt.Box(5, 0, 5, 11, 12, 11),
                    new MachineArt.Box(3, 12, 3, 13, 14, 13),
                    new MachineArt.Box(4, 14, 4, 12, 16, 12)), NONE, Map.of(
                    MachineArt.Face.UP, TurretLevelArt::lensTop)),
            new MachineArt.Spec("laser_turret_4", "#30353D", "#FF2A2A", List.of(
                    new MachineArt.Box(2, 0, 2, 14, 2, 14),
                    new MachineArt.Box(3, 2, 3, 13, 5, 13),
                    new MachineArt.Box(0, 5, 0, 16, 8, 16),
                    new MachineArt.Box(0, 8, 4, 3, 13, 12),
                    new MachineArt.Box(13, 8, 4, 16, 13, 12),
                    new MachineArt.Box(6, 8, 6, 10, 16, 10)), TurretLevelArt::arrayFront, Map.of(
                    MachineArt.Face.WEST, TurretLevelArt::arraySide,
                    MachineArt.Face.EAST, TurretLevelArt::arraySide)),
            new MachineArt.Spec("laser_turret_4_top", "#30353D", "#FF2A2A", List.of(
                    new MachineArt.Box(5, 0, 5, 11, 10, 11),
                    new MachineArt.Box(3, 10, 3, 13, 13, 13),
                    new MachineArt.Box(4, 13, 4, 12, 16, 12)), NONE, Map.of(
                    MachineArt.Face.UP, TurretLevelArt::lensTop)),
            new MachineArt.Spec("laser_turret_5", "#23272F", "#4FB6FF", List.of(
                    new MachineArt.Box(2, 0, 2, 14, 2, 14),
                    new MachineArt.Box(3, 2, 3, 13, 5, 13),
                    new MachineArt.Box(0, 5, 0, 16, 8, 16),
                    new MachineArt.Box(0, 8, 4, 3, 13, 12),
                    new MachineArt.Box(13, 8, 4, 16, 13, 12),
                    new MachineArt.Box(6, 8, 6, 10, 16, 10)), TurretLevelArt::arrayFront, Map.of(
                    MachineArt.Face.WEST, TurretLevelArt::arraySide,
                    MachineArt.Face.EAST, TurretLevelArt::arraySide)),
            new MachineArt.Spec("laser_turret_5_top", "#23272F", "#4FB6FF", List.of(
                    new MachineArt.Box(5, 0, 5, 11, 10, 11),
                    new MachineArt.Box(3, 10, 3, 13, 13, 13),
                    new MachineArt.Box(4, 13, 4, 12, 16, 12)), NONE, Map.of(
                    MachineArt.Face.UP, TurretLevelArt::lensTop)));

    /**
     * The animated fronts: the indicator of each machine gun level's lower half, which pulses every 3 ticks as level 1's
     * does, and the lens of each laser level, every 2 ticks.
     */
    public static final List<MachineAnimation.Clip> CLIPS = List.of(
            new MachineAnimation.Clip("machine_gun_turret_2", MachineArt.Face.NORTH, 3),
            new MachineAnimation.Clip("machine_gun_turret_3", MachineArt.Face.NORTH, 3),
            new MachineAnimation.Clip("machine_gun_turret_4", MachineArt.Face.NORTH, 3),
            new MachineAnimation.Clip("machine_gun_turret_5", MachineArt.Face.NORTH, 3),
            new MachineAnimation.Clip("laser_turret_2", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("laser_turret_3_top", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("laser_turret_4_top", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("laser_turret_5_top", MachineArt.Face.NORTH, 2));

    /**
     * The upgrade kits, one per level from 2 to 5. Each is a sealed brass cartridge with a window of pips, one pip lit for
     * level 2 up to four for level 5. The pip colours follow the kits' names in materials.yml: silver, green, blue and gold.
     */
    public static final List<ComponentArt.Spec> SPRITES = List.of(
            new ComponentArt.Spec("turret_upgrade_2", "#B8914A", "#C8CED6", kit(1)),
            new ComponentArt.Spec("turret_upgrade_3", "#B8914A", "#55E07A", kit(2)),
            new ComponentArt.Spec("turret_upgrade_4", "#B8914A", "#4A9BFF", kit(3)),
            new ComponentArt.Spec("turret_upgrade_5", "#B8914A", "#FFC040", kit(4)));

    private TurretLevelArt() {
    }

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /** Paints the animated front of one frame of an upgraded turret. */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        int accent = Color.hex(spec.accent());
        switch (spec.id()) {
            case "machine_gun_turret_2", "machine_gun_turret_3", "machine_gun_turret_4", "machine_gun_turret_5" ->
                    machineGunFrame(c, accent, frame);
            case "laser_turret_2", "laser_turret_3_top", "laser_turret_4_top", "laser_turret_5_top" ->
                    laserFrame(c, accent, frame);
            default -> throw new IllegalArgumentException("No animation for " + spec.id());
        }
    }

    /**
     * The front of a machine gun level for one frame: the level 1 pattern, a dark housing, an indicator whose halo and lamp
     * pulse over the cycle, and an ammunition counter bar of four cells, three of them lit.
     */
    private static void machineGunFrame(Canvas c, int accent, int frame) {
        double p = MachineAnimation.pulse(frame);
        c.paint(Area.roundRect(6, 6, 26, 26, 3), Color.hex("#2A2D2A"));
        c.paint(Area.roundRect(8, 8, 24, 24, 2), Color.hex("#101210"));
        c.paint(Area.disc(16, 13, 5 + 1.5 * p), Color.withAlpha(accent, 0.15 + 0.35 * p));
        c.paint(Area.ring(16, 13, 4.5, 1.6), Color.mix(Color.scale(accent, 0.5), accent, p));
        c.paint(Area.disc(16, 13, 2.5), Color.mix(accent, Color.WHITE, 0.2 + 0.5 * p));
        for (int k = 0; k < 4; k++) {
            double x = 9 + 3.5 * k;
            c.paint(Area.rect(x, 20, x + 3, 23), k < 3 ? accent : Color.scale(accent, 0.25));
        }
    }

    /**
     * The lens of a laser level for one frame: a dark housing, a halo that swells and a ring that pulses, a second, thinner
     * ring outside it that dims as the first brightens, and a white-hot core. The lens takes the level's colour.
     */
    private static void laserFrame(Canvas c, int accent, int frame) {
        double p = MachineAnimation.pulse(frame);
        c.paint(Area.roundRect(5, 5, 27, 27, 4), Color.hex("#3A3F48"));
        c.paint(Area.roundRect(7, 7, 25, 25, 3), Color.hex("#0A0C10"));
        c.paint(Area.disc(16, 16, 7 + 1.5 * p), Color.withAlpha(accent, 0.15 + 0.35 * p));
        c.paint(Area.ring(16, 16, 9, 0.9), Color.withAlpha(accent, 0.45 + 0.35 * (1 - p)));
        c.paint(Area.ring(16, 16, 6, 2), Color.mix(Color.scale(accent, 0.5), accent, p));
        c.paint(Area.disc(16, 16, 3.5), Color.mix(accent, Color.WHITE, 0.15 + 0.5 * p));
        c.paint(Area.disc(16, 16, 1.4), Color.withAlpha(Color.WHITE, 0.4 + 0.5 * p));
    }

    /** The top of a machine gun's upper mount: a steel plate with a ring of rivets, and a lamp in the middle. */
    private static void mgTop(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ring(16, 16, 5, 1.2), Color.scale(base, 0.4));
        c.paint(Area.disc(16, 16, 2.6), accent);
        c.paint(Area.disc(16, 16, 1), Color.WHITE);
    }

    /** The top of the sensor mast: a dark dish with an accent ring and a cyan lens. */
    private static void sensorTop(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 16, 7), Color.scale(base, 0.5));
        c.paint(Area.ring(16, 16, 6, 1), accent);
        c.paint(Area.disc(16, 16, 2.4), Color.withAlpha(accent, 0.8));
        c.paint(Area.disc(15, 15, 0.8), Color.WHITE);
    }

    /** The roof of the level 4 upper half: four louvred slots across the plate and a bronze boss in the middle. */
    private static void roofVents(Canvas c, int base, int accent, long seed) {
        for (int k = 0; k < 4; k++) {
            c.paint(Area.rect(3, 4 + 3 * k, 29, 5.5 + 3 * k), Color.scale(base, 0.45));
        }
        c.paint(Area.disc(16, 16, 2), accent);
    }

    /** The roof of the level 5 upper half: a status ring in the accent colour around a lamp. */
    private static void statusRingTop(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ring(16, 16, 9, 1.6), accent);
        c.paint(Area.ring(16, 16, 6, 1), Color.scale(base, 0.5));
        c.paint(Area.disc(16, 16, 3), Color.mix(accent, Color.WHITE, 0.3));
    }

    /** The side of a belt drum: two accent bands, and a row of brass cartridges between them. */
    private static void beltSide(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 5, Canvas.SIZE, 8), accent);
        c.paint(Area.rect(0, 24, Canvas.SIZE, 27), accent);
        for (double x = 4; x < Canvas.SIZE; x += 5) {
            c.paint(Area.rect(x, 12, x + 2, 20), Color.hex("#C8A24A"));
            c.paint(Area.disc(x + 1, 12, 1.2), Color.hex("#9AA3B0"));
        }
    }

    /** The end of a single barrel: a dark bore inside a ring of flash-hider slots. */
    private static void muzzleEast(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 16, 10), Color.scale(base, 0.5));
        c.paint(Area.disc(16, 16, 7), Color.hex("#0E1012"));
        c.paint(Area.ring(16, 16, 7, 1.2), accent);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + Math.PI / 4;
            c.paint(Area.disc(16 + Math.cos(a) * 4, 16 + Math.sin(a) * 4, 1), accent);
        }
    }

    /** The end of a twin barrel: two dark bores side by side, each with an accent ring. */
    private static void twinMuzzleEast(Canvas c, int base, int accent, long seed) {
        for (double x : new double[]{10, 22}) {
            c.paint(Area.disc(x, 16, 5.5), Color.scale(base, 0.5));
            c.paint(Area.disc(x, 16, 3.5), Color.hex("#0E1012"));
            c.paint(Area.ring(x, 16, 3.5, 0.9), accent);
        }
    }

    /** The east side of a heavy armour plate: stacked plates with a lighter edge, and bolts at the corners. */
    private static void armourEast(Canvas c, int base, int accent, long seed) {
        for (double y = 3; y < Canvas.SIZE; y += 8) {
            c.paint(Area.rect(0, y, Canvas.SIZE, y + 2), Color.mix(base, Color.WHITE, 0.15));
        }
        double[][] bolts = {{5, 5}, {27, 5}, {5, 21}, {27, 21}};
        for (double[] p : bolts) {
            c.paint(Area.disc(p[0], p[1], 1.1), Color.scale(base, 0.5));
        }
        c.paint(Area.rect(0, 28, Canvas.SIZE, 30), accent);
    }

    /** The front of the level 3 sensor mast: a dark panel with a cyan lens and a bar of readouts. */
    private static void sensorFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(6, 4, 26, 28), Color.hex("#0E1218"));
        c.paint(Area.disc(16, 12, 4), Color.withAlpha(accent, 0.7));
        c.paint(Area.ring(16, 12, 4, 1), accent);
        c.paint(Area.rect(9, 20, 23, 22), Color.scale(base, 0.5));
        c.paint(Area.rect(9, 25, 19, 27), accent);
    }

    /** The front of the level 4 upper half: louvres down the housing, with an accent band at each end. */
    private static void housingFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 2, Canvas.SIZE, 4), accent);
        for (double x = 6; x <= 26; x += 4) {
            c.paint(Area.rect(x, 5, x + 1.5, 27), Color.scale(base, 0.5));
        }
        c.paint(Area.rect(0, 28, Canvas.SIZE, 30), accent);
    }

    /** The front of the level 5 upper half: armour plates with bolts, and a dark slit with an accent lamp. */
    private static void armourFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(4, 8, 28, 24), Color.mix(base, Color.WHITE, 0.1));
        double[][] bolts = {{7, 11}, {25, 11}, {7, 21}, {25, 21}};
        for (double[] p : bolts) {
            c.paint(Area.disc(p[0], p[1], 1.1), Color.scale(base, 0.45));
        }
        c.paint(Area.rect(12, 14, 20, 18), Color.hex("#14161A"));
        c.paint(Area.disc(16, 16, 1.5), accent);
        c.paint(Area.rect(0, 2, Canvas.SIZE, 4), accent);
        c.paint(Area.rect(0, 28, Canvas.SIZE, 30), accent);
    }

    /** The front of the level 3 laser's dish: a ribbed dish with two accent rings and a dark centre. */
    private static void dishFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 16, 14), Color.scale(base, 0.6));
        c.paint(Area.ring(16, 16, 11, 1), Color.withAlpha(accent, 0.6));
        c.paint(Area.ring(16, 16, 7, 1.2), accent);
        c.paint(Area.disc(16, 16, 3), Color.hex("#14080A"));
    }

    /** The front of the level 4 and 5 laser's lower half: two emitter arrays, each a column of accent slits. */
    private static void arrayFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(3, 4, 13, 26), Color.hex("#0E1012"));
        c.paint(Area.rect(19, 4, 29, 26), Color.hex("#0E1012"));
        for (double y = 7; y <= 22; y += 4) {
            c.paint(Area.rect(5, y, 11, y + 1.5), accent);
            c.paint(Area.rect(21, y, 27, y + 1.5), accent);
        }
        c.paint(Area.rect(0, 28, Canvas.SIZE, 30), Color.scale(base, 0.6));
    }

    /** The side of an emitter array: a column of accent slits. */
    private static void arraySide(Canvas c, int base, int accent, long seed) {
        for (double y = 4; y <= 26; y += 6) {
            c.paint(Area.rect(4, y, 28, y + 2), accent);
        }
    }

    /** The top of a laser's lens housing: a dark cap with a bright ring around the lens. */
    private static void lensTop(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 16, 9), Color.scale(base, 0.6));
        c.paint(Area.ring(16, 16, 6, 2), accent);
        c.paint(Area.disc(16, 16, 3), Color.mix(accent, Color.WHITE, 0.3));
    }

    /** The top of the level 2 laser's lens housing: a dark cap with two bright rings around the lens. */
    private static void lensTopTwoRings(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 16, 10), Color.scale(base, 0.6));
        c.paint(Area.ring(16, 16, 8, 1), accent);
        c.paint(Area.ring(16, 16, 5.5, 2), accent);
        c.paint(Area.disc(16, 16, 2.5), Color.mix(accent, Color.WHITE, 0.3));
    }

    /** The outer side of a cooling fin: vertical ribs in a dark shade of the casing, and a red edge at the foot. */
    private static void finSide(Canvas c, int base, int accent, long seed) {
        for (double x = 3; x < Canvas.SIZE; x += 5) {
            c.paint(Area.rect(x, 3, x + 1.5, 29), Color.scale(base, 0.45));
        }
        c.paint(Area.rect(0, 28, Canvas.SIZE, 30), Color.scale(accent, 0.7));
    }

    /** The kit sprite of one level: a sealed brass case with a crimped mouth, a window, and {@code pips} lit pips of four. */
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
