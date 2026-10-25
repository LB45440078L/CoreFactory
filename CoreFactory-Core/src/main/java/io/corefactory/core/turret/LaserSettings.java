package io.corefactory.core.turret;

import io.corefactory.core.config.ConfigValues;

import java.util.Map;

/**
 * The typed form of the {@code laser-turret} section of config.yml. The laser turret is electric and fires an instant beam, so it holds no
 * rounds: its stats are these. Its targeting, arc, tilt and search come from the machine gun turret's settings, so both turrets pick the same
 * targets and turn within the same arc. Its rotation is per level, and its scan interval is its own, so a laser finds a new target sooner
 * than a machine gun does. Every value has a default, and every value is clamped to a sane range. Distances are in blocks, times in ticks,
 * power in W.
 *
 * @param range               the reach of the beam, in blocks
 * @param damage              the damage of one shot to each hit entity
 * @param armourBypass        the share of the target's armour the beam ignores, from 0 to 1; 1 ignores all of it
 * @param firingIntervalTicks ticks between two shots at full power
 * @param powerDemandW        the power a laser turret asks for, in W
 * @param beamParticles       the most dust particles one beam draws
 * @param searchIntervalTicks ticks between two passes that give the laser turrets of a world their targets
 */
public record LaserSettings(
        double range,
        double damage,
        double armourBypass,
        int firingIntervalTicks,
        int powerDemandW,
        int beamParticles,
        int searchIntervalTicks) {

    /** The reach of a level 1 laser, from its level table. Range, damage, interval and power are per level, not read from the section. */
    public static final double DEFAULT_RANGE = TurretLevels.LASER.level(1).range();
    public static final double DEFAULT_DAMAGE = TurretLevels.LASER.level(1).damage();
    public static final double DEFAULT_ARMOUR_BYPASS = 1.0;
    public static final int DEFAULT_FIRING_INTERVAL_TICKS = TurretLevels.LASER.level(1).fireIntervalTicks();
    public static final int DEFAULT_POWER_DEMAND_W = TurretLevels.LASER.level(1).powerDemandW();
    public static final int DEFAULT_BEAM_PARTICLES = 24;

    /** The default scan interval of a laser: a quarter of the machine gun's second, so a laser finds a new target within 5 ticks. */
    public static final int DEFAULT_SEARCH_INTERVAL_TICKS = 5;

    private static final String SECTION = "laser-turret";

    public LaserSettings {
        range = clamp(range, 1.0, 64.0);
        damage = clamp(damage, 0.0, 1000.0);
        armourBypass = clamp(armourBypass, 0.0, 1.0);
        firingIntervalTicks = Math.max(1, Math.min(firingIntervalTicks, 1200));
        powerDemandW = Math.max(0, powerDemandW);
        beamParticles = Math.max(0, Math.min(beamParticles, BeamPlan.MAX_BEAM_PARTICLES));
        searchIntervalTicks = Math.max(1, Math.min(searchIntervalTicks, 1200));
    }

    /** The settings with every default. */
    public static LaserSettings defaults() {
        return from(Map.of());
    }

    /**
     * Reads the {@code laser-turret} section of a configuration. A missing section gives the defaults.
     *
     * @param config the root configuration as plain data, or any section that holds a {@code laser-turret} section
     */
    public static LaserSettings from(Map<String, Object> config) {
        Map<String, Object> laser = ConfigValues.section(config, SECTION);
        return new LaserSettings(
                DEFAULT_RANGE,
                DEFAULT_DAMAGE,
                ConfigValues.number(laser, "armour-bypass", DEFAULT_ARMOUR_BYPASS),
                DEFAULT_FIRING_INTERVAL_TICKS,
                DEFAULT_POWER_DEMAND_W,
                ConfigValues.integer(laser, "beam-particles", DEFAULT_BEAM_PARTICLES),
                ConfigValues.integer(laser, "search-interval-ticks", DEFAULT_SEARCH_INTERVAL_TICKS));
    }

    /**
     * The turret settings a laser turret runs with: the machine gun's targeting, arc, tilt and idle sweep, with this laser's range, scan
     * interval, interval and power. A laser has no spread, so its spread is zero. Its rotation is the machine gun's shared speed here; the
     * level's own rotation, which a laser turret runs with, is applied from its {@link TurretLevel}.
     *
     * @param machineGun the settings of the machine gun turrets
     */
    public TurretSettings asTurret(TurretSettings machineGun) {
        return new TurretSettings(
                machineGun.enabled(),
                range,
                searchIntervalTicks,
                firingIntervalTicks,
                machineGun.rotationSpeedDegrees(),
                machineGun.arcDegrees(),
                machineGun.pitchLimitDegrees(),
                machineGun.idleSweep(),
                0.0,
                machineGun.ammoCapacity(),
                machineGun.ammoPerShot(),
                powerDemandW,
                machineGun.targets(),
                machineGun.targetNamed(),
                machineGun.targetTamed(),
                machineGun.rounds());
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
