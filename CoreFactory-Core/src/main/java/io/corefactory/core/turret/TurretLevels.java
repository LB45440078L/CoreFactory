package io.corefactory.core.turret;

import io.corefactory.core.config.ConfigValues;
import io.corefactory.core.railgun.RailgunSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The upgrade levels of one turret kind: {@link TurretType#maxLevel()} {@link TurretLevel}s, level 1 first. The machine gun and the laser
 * have five levels each, and the railgun three. The tables in code are the defaults; each level's entry under the {@code levels} section of
 * the kind's config section overrides them, key by key. The tall flag is not configurable: it follows the upgrade rule
 * ({@link TurretUpgrade#TALL_FROM}), so a turret is two blocks high from level 3 on.
 *
 * <p>A laser has no ammunition belt, so its levels have no ammo capacity, and an {@code ammo-capacity} key under the laser's levels is
 * ignored. A machine gun's capacity is at least one round. A laser's levels also read {@code rotation-degrees-per-tick}, the most the
 * laser turns in one tick; a machine gun's levels keep the turrets' shared rotation speed. A railgun's levels are read by
 * {@link RailgunSettings}, which holds its damage, range, power and its charge and reload times; its levels here are derived from those,
 * and they keep the shared rotation speed.
 *
 * @param type   the kind the levels belong to
 * @param levels the levels, level 1 first, one for each level of the kind
 */
public record TurretLevels(TurretType type, List<TurretLevel> levels) {

    /** The highest level of any turret kind: the machine gun's and the laser's five. A kind's own top is {@link TurretType#maxLevel()}. */
    public static final int MAX_LEVEL = 5;

    /** The machine gun's levels, with the defaults in code. */
    public static final TurretLevels MACHINE_GUN = defaults(TurretType.MACHINE_GUN);

    /** The laser turret's levels, with the defaults in code. */
    public static final TurretLevels LASER = defaults(TurretType.LASER);

    /** The railgun's levels, with the defaults in code. */
    public static final TurretLevels RAILGUN = defaults(TurretType.RAILGUN);

    /** The reach ceiling of a machine gun's and a laser's level, in blocks. A config value above it is clamped to it. */
    private static final double OLD_MAX_RANGE_BLOCKS = 64.0;

    /** The damage ceiling of a machine gun's and a laser's level, in HP. A config value above it is clamped to it. */
    private static final double OLD_MAX_DAMAGE = 1000.0;

    /** The power ceiling of a machine gun's and a laser's level, in W. A config value above it is clamped to it. */
    private static final int OLD_MAX_POWER_W = 1_000_000;

    public TurretLevels {
        Objects.requireNonNull(type, "type");
        levels = List.copyOf(levels);
        if (levels.size() != type.maxLevel()) {
            throw new IllegalArgumentException(name(type) + " has " + type.maxLevel() + " levels, not " + levels.size());
        }
        for (int i = 0; i < levels.size(); i++) {
            if (levels.get(i).level() != i + 1) {
                throw new IllegalArgumentException("level " + (i + 1) + " is listed as level " + levels.get(i).level());
            }
        }
    }

    /**
     * The defaults of a kind, as written in code.
     *
     * @param type the kind
     */
    public static TurretLevels defaults(TurretType type) {
        return switch (type) {
            case MACHINE_GUN -> new TurretLevels(type, machineGunDefaults());
            case LASER -> new TurretLevels(type, laserDefaults());
            case RAILGUN -> RailgunSettings.defaults().turretLevels();
        };
    }

    /**
     * The levels of a kind as the configuration gives them. A missing section, or a missing level or key, keeps the default in code.
     *
     * @param type   the kind
     * @param config the root configuration as plain data, or any section that holds the kind's section
     */
    public static TurretLevels from(TurretType type, Map<String, Object> config) {
        if (type == TurretType.RAILGUN) {
            return RailgunSettings.from(config).turretLevels();
        }
        Map<String, Object> kind = ConfigValues.optionalSection(config, type.section());
        Map<String, Object> table = kind == null ? null : ConfigValues.optionalSection(kind, "levels");
        List<TurretLevel> read = new ArrayList<>();
        for (TurretLevel fallback : defaults(type).levels()) {
            Map<String, Object> section = table == null ? null
                    : ConfigValues.optionalSection(table, String.valueOf(fallback.level()));
            read.add(section == null ? fallback : readLevel(type, fallback, section));
        }
        return new TurretLevels(type, read);
    }

    /**
     * The stats of one level.
     *
     * @param level the level, from 1 to {@link TurretType#maxLevel()} of the kind
     * @throws IllegalArgumentException when the level is outside 1 to the kind's top level, such as a railgun's level 4
     */
    public TurretLevel level(int level) {
        if (level < 1 || level > type.maxLevel()) {
            throw new IllegalArgumentException("no " + name(type) + " level " + level + "; its levels are 1 to " + type.maxLevel());
        }
        return levels.get(level - 1);
    }

    /** The highest level of this kind: {@link TurretType#maxLevel()}. */
    public int maxLevel() {
        return type.maxLevel();
    }

    private static TurretLevel readLevel(TurretType type, TurretLevel fallback, Map<String, Object> section) {
        int ammo = type.hasMagazine() ? Math.max(1, ConfigValues.integer(section, "ammo-capacity", fallback.ammoCapacity())) : 0;
        double rotation = type.hasMagazine() ? fallback.rotationDegreesPerTick()
                : ConfigValues.number(section, "rotation-degrees-per-tick", fallback.rotationDegreesPerTick());
        return new TurretLevel(
                fallback.level(),
                Math.min(OLD_MAX_DAMAGE, ConfigValues.number(section, "damage", fallback.damage())),
                ConfigValues.integer(section, "damage-boost-percent", fallback.damageBoostPercent()),
                ConfigValues.integer(section, "fire-interval-ticks", fallback.fireIntervalTicks()),
                Math.min(OLD_MAX_RANGE_BLOCKS, ConfigValues.number(section, "range", fallback.range())),
                ammo,
                ConfigValues.integer(section, "cannons", fallback.cannons()),
                Math.min(OLD_MAX_POWER_W, ConfigValues.integer(section, "power-demand-w", fallback.powerDemandW())),
                fallback.tall(),
                rotation);
    }

    private static String name(TurretType type) {
        return type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static List<TurretLevel> machineGunDefaults() {
        return List.of(
                new TurretLevel(1, 6.0, 0, 10, 24.0, 128, 1, 24_000, TurretUpgrade.TALL_FROM <= 1),
                new TurretLevel(2, 6.0, 10, 8, 26.0, 256, 1, 33_000, TurretUpgrade.TALL_FROM <= 2),
                new TurretLevel(3, 6.0, 20, 7, 28.0, 384, 1, 42_000, TurretUpgrade.TALL_FROM <= 3),
                new TurretLevel(4, 6.0, 35, 5, 30.0, 512, 2, 51_000, TurretUpgrade.TALL_FROM <= 4),
                new TurretLevel(5, 6.0, 50, 4, 32.0, 768, 2, 60_000, TurretUpgrade.TALL_FROM <= 5));
    }

    /** The laser's levels. Levels 3 to 5 turn faster than the machine gun's shared speed, so a laser turns onto its next target sooner. */
    private static List<TurretLevel> laserDefaults() {
        return List.of(
                new TurretLevel(1, 12.0, 0, 10, 40.0, 0, 1, 60_000, TurretUpgrade.TALL_FROM <= 1, 6.0),
                new TurretLevel(2, 12.0, 10, 8, 44.0, 0, 1, 75_000, TurretUpgrade.TALL_FROM <= 2, 6.0),
                new TurretLevel(3, 12.0, 20, 6, 48.0, 0, 1, 90_000, TurretUpgrade.TALL_FROM <= 3, 8.0),
                new TurretLevel(4, 12.0, 35, 5, 54.0, 0, 1, 105_000, TurretUpgrade.TALL_FROM <= 4, 10.0),
                new TurretLevel(5, 12.0, 50, 4, 60.0, 0, 1, 120_000, TurretUpgrade.TALL_FROM <= 5, 12.0));
    }
}
