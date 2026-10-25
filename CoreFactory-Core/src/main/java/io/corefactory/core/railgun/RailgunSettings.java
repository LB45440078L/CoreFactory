package io.corefactory.core.railgun;

import io.corefactory.core.config.ConfigValues;
import io.corefactory.core.turret.TurretLevel;
import io.corefactory.core.turret.TurretLevels;
import io.corefactory.core.turret.TurretSettings;
import io.corefactory.core.turret.TurretType;
import io.corefactory.core.turret.TurretUpgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The typed form of the {@code railgun} section of config.yml: the rail gun's levels, and the look and the flight of its shot. Every value
 * has a default, and every value is clamped to a sane range; a value that is not a finite number gives its default. Distances are in blocks,
 * times in seconds unless a name says ticks, power in W, damage in HP (health points: 2 HP is one heart, as for the laser).
 *
 * <p>A rail gun shares the machine gun's targeting, arc, tilt and idle sweep, as the laser does. It holds no rounds. Its charge and reload
 * are per level: a shot is ready {@link LevelStats#chargeSeconds()} after a target is acquired, and the next charge starts
 * {@link LevelStats#reloadSeconds()} after a shot. The shot delay of the turret, {@link LevelStats#fireIntervalTicks()}, is the two added
 * together, so the turret's shot-to-shot time is the charge and the reload.
 *
 * <p>The helix speed, {@link #helixSpeed(RailgunLevel)}, is the speed of the flame frontier in blocks per second. It is set at level I and at
 * level III, and level II is halfway between them, so a level II frontier moves at the mean of the two. The frontier moves this way for the
 * whole flight; a target that outruns it is never reached, and the caller must give up on it.
 *
 * @param enabled                  whether rail guns run at all, on top of the turrets' own switch
 * @param hitDistanceBlocks        the distance from the frontier to the target at which the shot has arrived
 * @param armourBypass             the share of the target's armour the shot ignores, from 0 to 1; 1 ignores all of it
 * @param helixSpeedLevelOne       the frontier's speed at level I, in blocks per second
 * @param helixSpeedLevelThree     the frontier's speed at level III, in blocks per second
 * @param spiralDelaySeconds       the seconds after the frontier leaves the barrel before the blue spiral begins
 * @param strandRadiusBlocks       the radius of the four red strands around the path, in blocks
 * @param strands                  the number of red strands, which are spaced evenly around the path
 * @param strandTwistTurnsPerBlock the turns the red strands make around the path for each block it travels
 * @param spiralRadiusBlocks       the radius of the blue spiral around the path, in blocks; larger than the strands' radius
 * @param spiralTurnsPerBlock      the turns the blue spiral makes around the path for each block it travels
 * @param sampleSpacingBlocks      the distance along the path between two drawn points of a strand or the spiral, in blocks, when the
 *                                 particle budget allows it; a longer path is drawn with wider spacing rather than cut short
 * @param spinTurnsPerSecond       the turns the strands and the spiral turn about the path each second, toward the target
 * @param particleBudget           the most points one shot draws in one tick, its frontier, strands and spiral together
 * @param maxParticlesPerTick      the most points all the shots in flight draw in one tick together; each shot gets its share of it
 * @param maxLight                 the light level of a charged rail gun, from 0 to 15, reached when it is ready to fire
 * @param levels                   the stats of levels I to III, level I first
 */
public record RailgunSettings(
        boolean enabled,
        double hitDistanceBlocks,
        double armourBypass,
        double helixSpeedLevelOne,
        double helixSpeedLevelThree,
        double spiralDelaySeconds,
        double strandRadiusBlocks,
        int strands,
        double strandTwistTurnsPerBlock,
        double spiralRadiusBlocks,
        double spiralTurnsPerBlock,
        double sampleSpacingBlocks,
        double spinTurnsPerSecond,
        int particleBudget,
        int maxParticlesPerTick,
        int maxLight,
        List<LevelStats> levels) {

    /** The ticks in one second of the server clock. */
    public static final int TICKS_PER_SECOND = 20;

    /** The section's name in config.yml. */
    public static final String SECTION = "railgun";

    /** The distance to the target at which a shot arrives, in blocks: half a block, the reach of a hit on a mob's centre. */
    public static final double DEFAULT_HIT_DISTANCE_BLOCKS = 0.5;

    /** The share of the armour a shot ignores in config.yml: three quarters, so armour still counts for a quarter of it. */
    public static final double DEFAULT_ARMOUR_BYPASS = 0.75;

    /** The frontier's speed at level I in config.yml, in blocks per second. */
    public static final double DEFAULT_HELIX_SPEED_LEVEL_ONE = 2.0;

    /** The frontier's speed at level III in config.yml, in blocks per second. */
    public static final double DEFAULT_HELIX_SPEED_LEVEL_THREE = 9.0;

    /** The seconds before the blue spiral begins, in config.yml. */
    public static final double DEFAULT_SPIRAL_DELAY_SECONDS = 0.5;

    /** The radius of the red strands in config.yml, in blocks. */
    public static final double DEFAULT_STRAND_RADIUS_BLOCKS = 0.3;

    /** The number of red strands in config.yml. */
    public static final int DEFAULT_STRANDS = 4;

    /** The strands' twist in config.yml: one turn every two blocks. */
    public static final double DEFAULT_STRAND_TWIST_TURNS_PER_BLOCK = 0.5;

    /** The radius of the blue spiral in config.yml, in blocks: 2.5 times the strands' radius. */
    public static final double DEFAULT_SPIRAL_RADIUS_BLOCKS = 0.75;

    /** The blue spiral's turns in config.yml: one turn for each block, so it wraps the strands quickly. */
    public static final double DEFAULT_SPIRAL_TURNS_PER_BLOCK = 1.0;

    /** The most points one shot draws in one tick, in config.yml. */
    public static final int DEFAULT_PARTICLE_BUDGET = 96;

    /** The distance along the path between two drawn points of a strand, in blocks: four points for each turn of the default helix. */
    public static final double DEFAULT_SAMPLE_SPACING_BLOCKS = 0.25;

    /** The turns the helix turns about the path each second, which makes it look as if it flows toward the target. */
    public static final double DEFAULT_SPIN_TURNS_PER_SECOND = 1.0;

    /** The most points all the shots in flight draw in one tick: the laser's cap of 160, so a battery of rail guns costs what a battery of lasers does. */
    public static final int DEFAULT_MAX_PARTICLES_PER_TICK = 192;

    /** The light level of a charged rail gun in config.yml: the most vanilla gives. */
    public static final int DEFAULT_MAX_LIGHT = 15;

    /** The most base damage one shot may deal: the widest a turret level may have. */
    private static final double MAX_DAMAGE = 10_000.0;

    /** The most power one level may ask for, in W: the widest a turret level may have. */
    private static final double MAX_POWER_W = 10_000_000.0;

    /** The most reach one level may have, in blocks: the widest a turret level may have. */
    private static final double MAX_RANGE_BLOCKS = 128.0;

    /** The most seconds one charge or one reload may take. Two of them together stay within the 60 seconds a turret level can hold. */
    private static final double MAX_PHASE_SECONDS = 30.0;

    public RailgunSettings {
        Objects.requireNonNull(levels, "levels");
        levels = List.copyOf(levels);
        if (levels.size() != RailgunLevel.values().length) {
            throw new IllegalArgumentException("a rail gun has " + RailgunLevel.values().length + " levels, not " + levels.size());
        }
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).level() != RailgunLevel.ofNumber(i + 1)) {
                throw new IllegalArgumentException("level " + (i + 1) + " is listed as level " + levels.get(i).level().number());
            }
        }
        hitDistanceBlocks = clamp(hitDistanceBlocks, 0.05, 4.0);
        armourBypass = clamp(armourBypass, 0.0, 1.0);
        helixSpeedLevelOne = clamp(helixSpeedLevelOne, 0.1, 200.0);
        helixSpeedLevelThree = clamp(helixSpeedLevelThree, 0.1, 200.0);
        spiralDelaySeconds = clamp(spiralDelaySeconds, 0.0, 60.0);
        strandRadiusBlocks = clamp(strandRadiusBlocks, 0.01, 2.0);
        strands = Math.max(1, Math.min(strands, 8));
        strandTwistTurnsPerBlock = clamp(strandTwistTurnsPerBlock, 0.0, 4.0);
        spiralRadiusBlocks = clamp(spiralRadiusBlocks, 0.01, 3.0);
        spiralTurnsPerBlock = clamp(spiralTurnsPerBlock, 0.0, 8.0);
        sampleSpacingBlocks = clamp(sampleSpacingBlocks, 0.05, 2.0);
        spinTurnsPerSecond = clamp(spinTurnsPerSecond, 0.0, 10.0);
        particleBudget = Math.max(0, Math.min(particleBudget, 256));
        maxParticlesPerTick = Math.max(0, Math.min(maxParticlesPerTick, 2000));
        maxLight = Math.max(0, Math.min(maxLight, 15));
    }

    /**
     * The stats of one level of the rail gun.
     *
     * @param level         the level, I to III
     * @param damage        the damage of one shot to the entity it hits, in HP, before the armour the shot does not ignore
     * @param powerDemandW  the power the level draws while it charges and while it fires, in W; it draws none while it reloads
     * @param range         the reach in blocks: the sight of the turret, and how far the shot travels
     * @param chargeSeconds the wind-up from a target acquired to a shot ready, in seconds
     * @param reloadSeconds the delay after a shot before the next charge starts, in seconds
     */
    public record LevelStats(RailgunLevel level, double damage, int powerDemandW, double range, double chargeSeconds,
                             double reloadSeconds) {

        public LevelStats {
            Objects.requireNonNull(level, "level");
            damage = clamp(damage, 0.0, MAX_DAMAGE);
            powerDemandW = (int) clamp(powerDemandW, 0.0, MAX_POWER_W);
            range = clamp(range, 1.0, MAX_RANGE_BLOCKS);
            chargeSeconds = clamp(chargeSeconds, 0.0, MAX_PHASE_SECONDS);
            reloadSeconds = clamp(reloadSeconds, 0.0, MAX_PHASE_SECONDS);
        }

        /** The ticks from one shot to the next: the charge and the reload, at least one tick. */
        public int fireIntervalTicks() {
            return Math.max(1, (int) Math.round((chargeSeconds + reloadSeconds) * TICKS_PER_SECOND));
        }
    }

    /** The settings with every default. */
    public static RailgunSettings defaults() {
        return from(Map.of());
    }

    /**
     * Reads the {@code railgun} section of a configuration. A missing section, or a missing key, gives its default. A level's stats are read
     * from the {@code levels} block of the section, with a block for each level from {@code 1} to {@code 3}.
     *
     * @param config the root configuration as plain data, or any section that holds a {@code railgun} section
     */
    public static RailgunSettings from(Map<String, Object> config) {
        Map<String, Object> section = ConfigValues.section(config, SECTION);
        Map<String, Object> table = ConfigValues.optionalSection(section, "levels");
        List<LevelStats> levels = new ArrayList<>();
        for (RailgunLevel level : RailgunLevel.values()) {
            levels.add(levelOf(level, table));
        }
        return new RailgunSettings(
                ConfigValues.bool(section, "enabled", true),
                finite(section, "hit-distance-blocks", DEFAULT_HIT_DISTANCE_BLOCKS, 0.05, 4.0),
                finite(section, "armour-bypass", DEFAULT_ARMOUR_BYPASS, 0.0, 1.0),
                finite(section, "helix-speed-level-1", DEFAULT_HELIX_SPEED_LEVEL_ONE, 0.1, 200.0),
                finite(section, "helix-speed-level-3", DEFAULT_HELIX_SPEED_LEVEL_THREE, 0.1, 200.0),
                finite(section, "spiral-delay-seconds", DEFAULT_SPIRAL_DELAY_SECONDS, 0.0, 60.0),
                finite(section, "strand-radius-blocks", DEFAULT_STRAND_RADIUS_BLOCKS, 0.01, 2.0),
                (int) finite(section, "strands", DEFAULT_STRANDS, 1, 8),
                finite(section, "strand-twist-turns-per-block", DEFAULT_STRAND_TWIST_TURNS_PER_BLOCK, 0.0, 4.0),
                finite(section, "spiral-radius-blocks", DEFAULT_SPIRAL_RADIUS_BLOCKS, 0.01, 3.0),
                finite(section, "spiral-turns-per-block", DEFAULT_SPIRAL_TURNS_PER_BLOCK, 0.0, 8.0),
                finite(section, "sample-spacing-blocks", DEFAULT_SAMPLE_SPACING_BLOCKS, 0.05, 2.0),
                finite(section, "spin-turns-per-second", DEFAULT_SPIN_TURNS_PER_SECOND, 0.0, 10.0),
                (int) finite(section, "particle-budget", DEFAULT_PARTICLE_BUDGET, 0, 256),
                (int) finite(section, "max-particles-per-tick", DEFAULT_MAX_PARTICLES_PER_TICK, 0, 2000),
                (int) finite(section, "max-light", DEFAULT_MAX_LIGHT, 0, 15),
                levels);
    }

    /** The stats of one level. */
    public LevelStats level(RailgunLevel level) {
        return levels.get(level.ordinal());
    }

    /**
     * The frontier's speed at a level, in blocks per second. Level I and level III are the two configured speeds; level II is halfway
     * between them.
     *
     * @param level the level
     */
    public double helixSpeed(RailgunLevel level) {
        double fraction = (level.number() - 1) / 2.0;
        return helixSpeedLevelOne + (helixSpeedLevelThree - helixSpeedLevelOne) * fraction;
    }

    /** The ticks between the start of the flight and the start of the blue spiral: the spiral delay, rounded to whole ticks. */
    public int spiralDelayTicks() {
        return (int) Math.round(spiralDelaySeconds * TICKS_PER_SECOND);
    }

    /**
     * The levels of the rail gun as the turret's own table: the damage, the range, the power and the shot-to-shot time of each level. The
     * levels keep the turrets' shared rotation speed, have one cannon and no magazine, and are two blocks high from level 3, as the upgrade
     * rule sets.
     */
    public TurretLevels turretLevels() {
        List<TurretLevel> out = new ArrayList<>();
        for (LevelStats stats : levels) {
            int number = stats.level().number();
            out.add(new TurretLevel(number, stats.damage(), 0, stats.fireIntervalTicks(), stats.range(), 0, 1,
                    stats.powerDemandW(), number >= TurretUpgrade.TALL_FROM, 0.0));
        }
        return new TurretLevels(TurretType.RAILGUN, out);
    }

    /**
     * The turret settings a rail gun runs with at a level: the machine gun's targeting, arc, tilt, idle sweep and scan interval, with this
     * level's range and shot-to-shot time, and this section's power. A rail gun has no spread. It runs only while both this section and the
     * turrets section are enabled.
     *
     * @param machineGun the settings of the machine gun turrets
     * @param level      the level of the rail gun
     */
    public TurretSettings asTurret(TurretSettings machineGun, RailgunLevel level) {
        LevelStats stats = level(level);
        return new TurretSettings(
                enabled && machineGun.enabled(),
                stats.range(),
                machineGun.searchIntervalTicks(),
                stats.fireIntervalTicks(),
                machineGun.rotationSpeedDegrees(),
                machineGun.arcDegrees(),
                machineGun.pitchLimitDegrees(),
                machineGun.idleSweep(),
                0.0,
                machineGun.ammoCapacity(),
                machineGun.ammoPerShot(),
                stats.powerDemandW(),
                machineGun.targets(),
                machineGun.targetNamed(),
                machineGun.targetTamed(),
                machineGun.rounds());
    }

    private static LevelStats levelOf(RailgunLevel level, Map<String, Object> table) {
        LevelStats fallback = defaultLevel(level);
        Map<String, Object> section = table == null ? null : ConfigValues.optionalSection(table, String.valueOf(level.number()));
        if (section == null) {
            return fallback;
        }
        return new LevelStats(level,
                finite(section, "damage", fallback.damage(), 0.0, MAX_DAMAGE),
                (int) finite(section, "power-demand-w", fallback.powerDemandW(), 0.0, MAX_POWER_W),
                finite(section, "range", fallback.range(), 1.0, MAX_RANGE_BLOCKS),
                finite(section, "charge-seconds", fallback.chargeSeconds(), 0.0, MAX_PHASE_SECONDS),
                finite(section, "reload-seconds", fallback.reloadSeconds(), 0.0, MAX_PHASE_SECONDS));
    }

    /** The built-in stats of a level: the damage in HP, the power in W, the range in blocks, the charge and the reload in seconds. */
    private static LevelStats defaultLevel(RailgunLevel level) {
        return switch (level) {
            case I -> new LevelStats(RailgunLevel.I, 400.0, 2_000_000, 60.0, 6.0, 10.0);
            case II -> new LevelStats(RailgunLevel.II, 1200.0, 4_000_000, 80.0, 5.0, 9.0);
            case III -> new LevelStats(RailgunLevel.III, 3500.0, 8_000_000, 110.0, 4.0, 8.0);
        };
    }

    /** The number at a key when it is finite, clamped to the range; the default when it is missing, not a number or not finite. */
    private static double finite(Map<String, Object> section, String key, double def, double min, double max) {
        double value = ConfigValues.number(section, key, def);
        return Double.isFinite(value) ? clamp(value, min, max) : def;
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
