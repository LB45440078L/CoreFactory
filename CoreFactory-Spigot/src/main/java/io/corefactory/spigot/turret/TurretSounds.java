package io.corefactory.spigot.turret;

import io.corefactory.spigot.audio.MachineSounds;
import org.bukkit.Location;

/**
 * The turret's sounds, played through the machine sound helper by their pack keys: {@code turret_fire}, {@code turret_reload} and
 * {@code turret_rotate} for the machine gun, and {@code laser_fire} and {@code laser_charge} for the laser turret, which also turns with
 * {@code turret_rotate}. The pack sets their volume, and the calls below play them at a reduced volume and normal pitch. The turret throttles them itself: a
 * fire sound at most every few ticks, and a rotation sound only while the turret turns toward a target it has detected: an idle sweep is silent.
 */
public final class TurretSounds {

    /** The pack key of the firing sound. */
    static final String FIRE = "turret_fire";

    /** The pack key of the reload sound. */
    static final String RELOAD = "turret_reload";

    /** The pack key of the rotation sound. */
    static final String ROTATE = "turret_rotate";

    /** The pack key of the laser's firing sound. */
    static final String LASER_FIRE = "laser_fire";

    /** The pack key of the sound a laser turret makes when it starts a shot cycle. */
    static final String LASER_CHARGE = "laser_charge";

    private TurretSounds() {
    }

    /** Plays the laser's firing sound at a point. */
    public static void laserFire(Location at) {
        MachineSounds.play(at, LASER_FIRE, 0.35f, 1.0f);
    }

    /** Plays the laser's charging sound at a point, when it starts a shot cycle. */
    public static void laserCharge(Location at) {
        MachineSounds.play(at, LASER_CHARGE, 0.35f, 1.0f);
    }

    /** Plays the firing sound at a point. */
    public static void fire(Location at) {
        MachineSounds.play(at, FIRE, 0.35f, 1.0f);
    }

    /** Plays the reload sound at a point. */
    public static void reload(Location at) {
        MachineSounds.play(at, RELOAD, 0.35f, 1.0f);
    }

    /** Plays the rotation sound at a point. The caller plays it only while the turret turns toward a detected target, see TurretAim. */
    public static void rotate(Location at) {
        MachineSounds.play(at, ROTATE, 0.35f, 1.0f);
    }
}
