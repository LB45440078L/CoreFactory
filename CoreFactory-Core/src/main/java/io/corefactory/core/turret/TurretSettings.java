package io.corefactory.core.turret;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The typed form of the {@code turrets} section of config.yml. Every value has a default, and every value is clamped to a sane range,
 * so a missing or odd key never breaks the turret. Angles are in degrees, distances in blocks, times in ticks, power in W.
 *
 * @param enabled           whether turrets run at all
 * @param range             the reach of a turret, in blocks
 * @param searchIntervalTicks ticks between two searches for a target while a turret has none
 * @param firingIntervalTicks ticks between two shots at full power
 * @param rotationSpeedDegrees the most a turret turns in one tick
 * @param arcDegrees        the total sweep of a turret, centred on the way it faces when placed
 * @param pitchLimitDegrees the most a turret tilts up or down
 * @param idleSweep         whether a turret with no target sweeps its arc
 * @param accuracySpread    the spread of a regular round, as the half angle in degrees
 * @param ammoCapacity      the rounds a turret holds
 * @param ammoPerShot       the rounds one shot uses
 * @param powerDemandW      the power a loaded turret asks for, in W
 * @param targets           the kinds of mob it fires at
 * @param targetNamed       whether a named mob may be a target
 * @param targetTamed       whether a tamed mob may be a target
 * @param rounds            the values of each round type
 */
public record TurretSettings(
        boolean enabled,
        double range,
        int searchIntervalTicks,
        int firingIntervalTicks,
        double rotationSpeedDegrees,
        double arcDegrees,
        double pitchLimitDegrees,
        boolean idleSweep,
        double accuracySpread,
        int ammoCapacity,
        int ammoPerShot,
        int powerDemandW,
        Set<TargetKind> targets,
        boolean targetNamed,
        boolean targetTamed,
        Map<RoundType, Round> rounds) {

    /** The values of one round type. */
    public record Round(double damage, double armourBypass, int fireTicks, double spreadFactor) {

        public Round {
            damage = clamp(damage, 0.0, 1000.0);
            armourBypass = clamp(armourBypass, 0.0, 1.0);
            fireTicks = Math.max(0, Math.min(fireTicks, 6000));
            spreadFactor = clamp(spreadFactor, 0.0, 4.0);
        }
    }

    /** The reach of a level 1 machine gun, from its level table. Range, fire interval, ammunition and power are per level, not here. */
    public static final double DEFAULT_RANGE = TurretLevels.MACHINE_GUN.level(1).range();
    public static final int DEFAULT_SEARCH_INTERVAL_TICKS = 20;
    public static final int DEFAULT_FIRING_INTERVAL_TICKS = TurretLevels.MACHINE_GUN.level(1).fireIntervalTicks();
    public static final double DEFAULT_ROTATION_SPEED_DEGREES = 6.0;
    /** The default sweep: a full turn, so no facing refuses a target in range. */
    public static final double DEFAULT_ARC_DEGREES = 360.0;
    /** The default tilt: straight up or down, so no pitch refuses a target in range. */
    public static final double DEFAULT_PITCH_LIMIT_DEGREES = 90.0;
    public static final double DEFAULT_ACCURACY_SPREAD = 3.0;
    public static final int DEFAULT_AMMO_CAPACITY = TurretLevels.MACHINE_GUN.level(1).ammoCapacity();
    public static final int DEFAULT_AMMO_PER_SHOT = 1;
    public static final int DEFAULT_POWER_DEMAND_W = TurretLevels.MACHINE_GUN.level(1).powerDemandW();

    private static final String SECTION = "turrets";

    public TurretSettings {
        Set<TargetKind> kinds = EnumSet.noneOf(TargetKind.class);
        kinds.addAll(targets);
        kinds.remove(TargetKind.OTHER);
        targets = Set.copyOf(kinds);
        Map<RoundType, Round> copy = new EnumMap<>(RoundType.class);
        for (RoundType type : RoundType.values()) {
            Round round = rounds.get(type);
            copy.put(type, round == null ? defaultRound(type) : round);
        }
        rounds = Map.copyOf(copy);
        range = clamp(range, 1.0, 64.0);
        searchIntervalTicks = Math.max(1, Math.min(searchIntervalTicks, 1200));
        firingIntervalTicks = Math.max(1, Math.min(firingIntervalTicks, 1200));
        rotationSpeedDegrees = clamp(rotationSpeedDegrees, 0.5, 90.0);
        arcDegrees = clamp(arcDegrees, 10.0, 360.0);
        pitchLimitDegrees = clamp(pitchLimitDegrees, 0.0, 90.0);
        accuracySpread = clamp(accuracySpread, 0.0, 45.0);
        ammoCapacity = Math.max(1, Math.min(ammoCapacity, 4096));
        ammoPerShot = Math.max(1, Math.min(ammoPerShot, ammoCapacity));
        powerDemandW = Math.max(0, powerDemandW);
    }

    /** The settings with every default. */
    public static TurretSettings defaults() {
        return from(new MemoryConfiguration());
    }

    /**
     * Reads the {@code turrets} section of a configuration. A missing section gives the defaults. A target name that no kind has is
     * skipped; a round section that is missing keeps that round's defaults.
     *
     * @param config the root configuration, or any section that holds a {@code turrets} section
     */
    public static TurretSettings from(ConfigurationSection config) {
        ConfigurationSection turrets = config == null ? null : config.getConfigurationSection(SECTION);
        if (turrets == null) {
            turrets = new MemoryConfiguration();
        }
        Set<TargetKind> targets = targetsOf(turrets);
        Map<RoundType, Round> rounds = new EnumMap<>(RoundType.class);
        ConfigurationSection roundSection = turrets.getConfigurationSection("rounds");
        for (RoundType type : RoundType.values()) {
            rounds.put(type, roundOf(type, roundSection));
        }
        return new TurretSettings(
                turrets.getBoolean("enabled", true),
                DEFAULT_RANGE,
                turrets.getInt("search-interval-ticks", DEFAULT_SEARCH_INTERVAL_TICKS),
                DEFAULT_FIRING_INTERVAL_TICKS,
                turrets.getDouble("rotation-speed-degrees", DEFAULT_ROTATION_SPEED_DEGREES),
                turrets.getDouble("arc-degrees", DEFAULT_ARC_DEGREES),
                turrets.getDouble("pitch-limit-degrees", DEFAULT_PITCH_LIMIT_DEGREES),
                turrets.getBoolean("idle-sweep", true),
                turrets.getDouble("accuracy-spread", DEFAULT_ACCURACY_SPREAD),
                DEFAULT_AMMO_CAPACITY,
                turrets.getInt("ammo-per-shot", DEFAULT_AMMO_PER_SHOT),
                DEFAULT_POWER_DEMAND_W,
                targets,
                turrets.getBoolean("target-named", false),
                turrets.getBoolean("target-tamed", false),
                rounds);
    }

    /** The values of a round type. */
    public Round round(RoundType type) {
        return rounds.get(type);
    }

    /** True when a mob of this kind may be a target. */
    public boolean targets(TargetKind kind) {
        return targets.contains(kind);
    }

    private static Set<TargetKind> targetsOf(ConfigurationSection turrets) {
        if (!turrets.contains("targets")) {
            return EnumSet.of(TargetKind.MONSTER);
        }
        List<String> names = turrets.getStringList("targets");
        Set<TargetKind> targets = EnumSet.noneOf(TargetKind.class);
        for (String name : names) {
            TargetKind.ofConfigName(name).ifPresent(targets::add);
        }
        return targets;
    }

    private static Round roundOf(RoundType type, ConfigurationSection roundSection) {
        Round fallback = defaultRound(type);
        if (roundSection == null) {
            return fallback;
        }
        ConfigurationSection section = roundSection.getConfigurationSection(type.configKey());
        if (section == null) {
            return fallback;
        }
        return new Round(section.getDouble("damage", fallback.damage()),
                section.getDouble("armour-bypass", fallback.armourBypass()),
                section.getInt("fire-ticks", fallback.fireTicks()),
                section.getDouble("spread-factor", fallback.spreadFactor()));
    }

    /** The built-in values of a round type. */
    static Round defaultRound(RoundType type) {
        return switch (type) {
            case REGULAR -> new Round(6.0, 0.0, 0, 1.0);
            case ARMOR_PIERCING -> new Round(7.0, 0.5, 0, 1.0);
            case INCENDIARY -> new Round(5.0, 0.0, 80, 1.0);
            case DEPLETED_URANIUM -> new Round(9.0, 0.35, 0, 0.5);
        };
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
