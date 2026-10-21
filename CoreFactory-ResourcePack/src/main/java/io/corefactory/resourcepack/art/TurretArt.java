package io.corefactory.resourcepack.art;

import java.util.List;
import java.util.Map;

/**
 * The two turrets, each one block tall: the machine gun turret, which fires at hostile mobs, and the laser turret. Each has a
 * 3D model in {@link MachineArt}, and the front of each is an animated strip: the machine gun's amber indicator and the
 * laser's red lens. The laser turret has an inventory sprite here, and so do the laser emitter and the optical lens, which
 * have no 3D model. The machine gun turret's inventory sprite is listed in {@link ComponentArt} with the other turret parts.
 */
public final class TurretArt {

    private static final ComponentArt.Painter NONE = (c, base, accent, seed) -> {
    };

    /**
     * The two turrets. The machine gun turret has an ammunition box on its west side, and the laser turret has two cooling
     * fins. Their animated fronts are painted by {@link MachineAnimation}, so the front painter here is empty.
     */
    public static final List<MachineArt.Spec> ALL = List.of(
            new MachineArt.Spec("machine_gun_turret", "#4B5A44", "#E0A030", List.of(
                    new MachineArt.Box(1, 0, 1, 15, 2, 15),
                    new MachineArt.Box(5, 2, 5, 11, 7, 11),
                    new MachineArt.Box(4, 7, 4, 12, 8, 12),
                    new MachineArt.Box(3, 8, 5, 5, 13, 11),
                    new MachineArt.Box(5, 8, 5, 11, 13, 6),
                    new MachineArt.Box(5, 8, 10, 11, 13, 11),
                    new MachineArt.Box(7, 8, 6, 12, 12, 10),
                    new MachineArt.Box(5, 9, 7, 15, 11, 9),
                    new MachineArt.Box(14, 8, 6, 16, 12, 10),
                    new MachineArt.Box(1, 2, 9, 5, 7, 14),
                    new MachineArt.Box(3, 13, 6, 5, 14, 8)), NONE, Map.of(
                    MachineArt.Face.UP, TurretArt::machineGunTop,
                    MachineArt.Face.WEST, TurretArt::ammoBoxSide)),
            new MachineArt.Spec("laser_turret", "#2A2E36", "#FF2A2A", List.of(
                    new MachineArt.Box(2, 0, 2, 14, 2, 14),
                    new MachineArt.Box(4, 2, 4, 12, 6, 12),
                    new MachineArt.Box(2, 6, 2, 14, 7, 14),
                    new MachineArt.Box(3, 7, 3, 13, 8, 13),
                    new MachineArt.Box(4, 8, 4, 12, 9, 12),
                    new MachineArt.Box(5, 9, 5, 11, 13, 11),
                    new MachineArt.Box(4, 13, 4, 12, 15, 12),
                    new MachineArt.Box(0, 4, 6, 2, 9, 10),
                    new MachineArt.Box(14, 4, 6, 16, 9, 10)), NONE, Map.of(
                    MachineArt.Face.UP, TurretArt::lensTop,
                    MachineArt.Face.WEST, TurretArt::finSide,
                    MachineArt.Face.EAST, TurretArt::finSide)));

    /** The animated fronts: the machine gun's indicator, which pulses every 3 ticks, and the laser's lens, every 2. */
    public static final List<MachineAnimation.Clip> CLIPS = List.of(
            new MachineAnimation.Clip("machine_gun_turret", MachineArt.Face.NORTH, 3),
            new MachineAnimation.Clip("laser_turret", MachineArt.Face.NORTH, 2));

    /**
     * The inventory sprites of the laser turret, its emitter and the optical lens. The machine gun turret's sprite is listed
     * in {@link ComponentArt} with the other turret parts.
     */
    public static final List<ComponentArt.Spec> SPRITES = List.of(
            new ComponentArt.Spec("laser_turret", "#3A3F48", "#FF3030", TurretArt::laserTurret),
            new ComponentArt.Spec("laser_emitter", "#C9A24A", "#FF3A2A", TurretArt::laserEmitter),
            new ComponentArt.Spec("optical_lens", "#CFEFFF", "#7FD0FF", TurretArt::opticalLens));

    private TurretArt() {
    }

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /** Paints the animated front of one frame of a turret. */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        int accent = Color.hex(spec.accent());
        switch (spec.id()) {
            case "machine_gun_turret" -> machineGunFrame(c, accent, frame);
            case "laser_turret" -> laserFrame(c, accent, frame);
            default -> throw new IllegalArgumentException("No animation for " + spec.id());
        }
    }

    /**
     * The front of the machine gun turret for one frame: a dark housing, an amber indicator whose halo and lamp pulse over the
     * cycle, and an ammunition counter bar of four cells, three of them lit.
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
     * The front of the laser turret for one frame: a dark housing and a red lens that glows and pulses over the cycle, with a
     * halo that swells, a bright ring, and a white-hot core.
     */
    private static void laserFrame(Canvas c, int accent, int frame) {
        double p = MachineAnimation.pulse(frame);
        c.paint(Area.roundRect(5, 5, 27, 27, 4), Color.hex("#3A3F48"));
        c.paint(Area.roundRect(7, 7, 25, 25, 3), Color.hex("#0A0C10"));
        c.paint(Area.disc(16, 16, 7 + 1.5 * p), Color.withAlpha(accent, 0.15 + 0.35 * p));
        c.paint(Area.ring(16, 16, 6, 2), Color.mix(Color.scale(accent, 0.5), accent, p));
        c.paint(Area.disc(16, 16, 3.5), Color.mix(accent, Color.WHITE, 0.15 + 0.5 * p));
        c.paint(Area.disc(16, 16, 1.4), Color.withAlpha(Color.WHITE, 0.4 + 0.5 * p));
    }

    /** The top of the machine gun turret: a steel plate with a ring of rivets, and the status lamp in the middle. */
    private static void machineGunTop(Canvas c, int base, int accent, long seed) {
        c.paint(Area.ring(16, 16, 5, 1.2), Color.scale(base, 0.4));
        c.paint(Area.disc(16, 16, 2.6), accent);
        c.paint(Area.disc(16, 16, 1), Color.WHITE);
    }

    /** The side of the ammunition box: two bands in the accent colour, and a row of brass cartridges between them. */
    private static void ammoBoxSide(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 5, Canvas.SIZE, 8), accent);
        c.paint(Area.rect(0, 24, Canvas.SIZE, 27), accent);
        for (double x = 4; x < Canvas.SIZE; x += 5) {
            c.paint(Area.rect(x, 12, x + 2, 20), Color.hex("#C8A24A"));
            c.paint(Area.disc(x + 1, 12, 1.2), Color.hex("#9AA3B0"));
        }
    }

    /** The top of the laser turret's lens housing: a dark cap with a bright red ring around the lens. */
    private static void lensTop(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 16, 9), Color.scale(base, 0.6));
        c.paint(Area.ring(16, 16, 6, 2), accent);
        c.paint(Area.disc(16, 16, 3), Color.mix(accent, Color.WHITE, 0.3));
    }

    /** The outer side of a cooling fin: vertical ribs in a dark shade of the casing, and a red edge at the foot. */
    private static void finSide(Canvas c, int base, int accent, long seed) {
        for (double x = 3; x < Canvas.SIZE; x += 5) {
            c.paint(Area.rect(x, 3, x + 1.5, 29), Color.scale(base, 0.45));
        }
        c.paint(Area.rect(0, 28, Canvas.SIZE, 30), Color.scale(accent, 0.7));
    }

    /** The laser turret: a dark body on a base, a stepped emitter dish on top, and a red lens on the front, with fins at the sides. */
    private static void laserTurret(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.roundRect(4, 24, 28, 29, 2), Color.scale(base, 0.6), seed + 1);
        Sprite.body(c, Area.rect(11, 18, 21, 24), Color.scale(base, 0.8), seed + 2);
        Sprite.body(c, Area.rect(3, 13, 7, 21), Color.scale(base, 0.7), seed + 3);
        Sprite.body(c, Area.rect(25, 13, 29, 21), Color.scale(base, 0.7), seed + 4);
        Sprite.body(c, Area.roundRect(7, 11, 25, 22, 3), base, seed + 5);
        Sprite.body(c, Area.ellipse(16, 10, 9, 3), Color.mix(base, Color.WHITE, 0.25), seed + 6);
        c.paint(Area.disc(16, 17, 4.6), Color.hex("#2A0A0A"));
        c.paint(Area.disc(16, 17, 3.4), accent);
        c.paint(Area.disc(15, 16, 1), Color.withAlpha(Color.WHITE, 0.85));
    }

    /** A laser emitter: a brass tube with red collars, a dark end cap, and a red lens tip. */
    private static void laserEmitter(Canvas c, int base, int accent, long seed) {
        Sprite.body(c, Area.rect(2, 13, 6, 19), Color.scale(base, 0.6), seed + 1);
        Sprite.body(c, Area.roundRect(5, 12, 23, 20, 3), base, seed);
        c.paint(Area.rect(12, 12, 14, 20), accent);
        c.paint(Area.rect(18, 12, 19, 20), accent);
        Sprite.body(c, Area.disc(25, 16, 4.5), accent, seed + 2);
        c.paint(Area.disc(24, 15, 1.2), Color.withAlpha(Color.WHITE, 0.85));
    }

    /** An optical lens: a clear disc with six facet lines, a rim, and a blue-white highlight. */
    private static void opticalLens(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 16, 13), Color.withAlpha(accent, 0.35));
        c.paint(Area.disc(16, 16, 12), Color.withAlpha(base, 0.8));
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            c.paint(Area.line(16, 16, 16 + Math.cos(a) * 11, 16 + Math.sin(a) * 11, 0.8), Color.withAlpha(accent, 0.7));
        }
        c.paint(Area.ring(16, 16, 6, 0.8), Color.withAlpha(accent, 0.6));
        c.paint(Area.ring(16, 16, 12, 1.4), accent);
        c.paint(Area.ellipse(11, 11, 4.5, 2.2), Color.hex("#F4FBFF"));
        c.paint(Area.disc(21, 22, 1.6), Color.hex("#9FE0FF"));
    }
}
