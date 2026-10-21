package io.corefactory.core.turret;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The upgrade levels of one turret kind: five {@link TurretLevel}s, level 1 first. The tables in code are the defaults; each level's
 * entry under the {@code levels} section of the kind's config section overrides them, key by key. The tall flag is not configurable: it
 * follows the upgrade rule ({@link TurretUpgrade#TALL_FROM}), so a turret is two blocks high from level 3 on.
 *
 * <p>A laser has no ammunition belt, so its levels have no ammo capacity, and an {@code ammo-capacity} key under the laser's levels is
 * ignored. A machine gun's capacity is at least one round.
 *
 * @param type   the kind the levels belong to
 * @param levels the five levels, level 1 first
 */
public record TurretLevels(TurretType type, List<TurretLevel> levels) {

    /** The highest level a turret can be upgraded to. */
    public static final int MAX_LEVEL = 5;

    /** The machine gun's levels, with the defaults in code. */
    public static final TurretLevels MACHINE_GUN = defaults(TurretType.MACHINE_GUN);

    /** The laser turret's levels, with the defaults in code. */
    public static final TurretLevels LASER = defaults(TurretType.LASER);

    public TurretLevels {
        Objects.requireNonNull(type, "type");
        levels = List.copyOf(levels);
        if (levels.size() != MAX_LEVEL) {
            throw new IllegalArgumentException("a turret has " + MAX_LEVEL + " levels, not " + levels.size());
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
        return new TurretLevels(type, type == TurretType.MACHINE_GUN ? machineGunDefaults() : laserDefaults());
    }

    /**
     * The levels of a kind as the configuration gives them. A missing section, or a missing level or key, keeps the default in code.
     *
     * @param type   the kind
     * @param config the root configuration, or any section that holds the kind's section
     */
    public static TurretLevels from(TurretType type, ConfigurationSection config) {
        ConfigurationSection kind = config == null ? null : config.getConfigurationSection(type.section());
        ConfigurationSection table = kind == null ? null : kind.getConfigurationSection("levels");
        List<TurretLevel> read = new ArrayList<>();
        for (TurretLevel fallback : defaults(type).levels()) {
            ConfigurationSection section = table == null ? null : table.getConfigurationSection(String.valueOf(fallback.level()));
            read.add(section == null ? fallback : readLevel(type, fallback, section));
        }
        return new TurretLevels(type, read);
    }

    /**
     * The stats of one level.
     *
     * @param level the level, from 1 to {@link #MAX_LEVEL}
     * @throws IllegalArgumentException when the level is outside 1 to {@link #MAX_LEVEL}
     */
    public TurretLevel level(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("no turret level " + level);
        }
        return levels.get(level - 1);
    }

    private static TurretLevel readLevel(TurretType type, TurretLevel fallback, ConfigurationSection section) {
        int ammo = type == TurretType.LASER ? 0 : Math.max(1, section.getInt("ammo-capacity", fallback.ammoCapacity()));
        return new TurretLevel(
                fallback.level(),
                section.getDouble("damage", fallback.damage()),
                section.getInt("damage-boost-percent", fallback.damageBoostPercent()),
                section.getInt("fire-interval-ticks", fallback.fireIntervalTicks()),
                section.getDouble("range", fallback.range()),
                ammo,
                section.getInt("cannons", fallback.cannons()),
                section.getInt("power-demand-w", fallback.powerDemandW()),
                fallback.tall());
    }

    private static List<TurretLevel> machineGunDefaults() {
        return List.of(
                new TurretLevel(1, 6.0, 0, 10, 24.0, 128, 1, 24_000, TurretUpgrade.TALL_FROM <= 1),
                new TurretLevel(2, 6.0, 10, 8, 26.0, 256, 1, 33_000, TurretUpgrade.TALL_FROM <= 2),
                new TurretLevel(3, 6.0, 20, 7, 28.0, 384, 1, 42_000, TurretUpgrade.TALL_FROM <= 3),
                new TurretLevel(4, 6.0, 35, 5, 30.0, 512, 2, 51_000, TurretUpgrade.TALL_FROM <= 4),
                new TurretLevel(5, 6.0, 50, 4, 32.0, 768, 2, 60_000, TurretUpgrade.TALL_FROM <= 5));
    }

    private static List<TurretLevel> laserDefaults() {
        return List.of(
                new TurretLevel(1, 12.0, 0, 10, 40.0, 0, 1, 60_000, TurretUpgrade.TALL_FROM <= 1),
                new TurretLevel(2, 12.0, 10, 8, 44.0, 0, 1, 75_000, TurretUpgrade.TALL_FROM <= 2),
                new TurretLevel(3, 12.0, 20, 6, 48.0, 0, 1, 90_000, TurretUpgrade.TALL_FROM <= 3),
                new TurretLevel(4, 12.0, 35, 5, 54.0, 0, 1, 105_000, TurretUpgrade.TALL_FROM <= 4),
                new TurretLevel(5, 12.0, 50, 4, 60.0, 0, 1, 120_000, TurretUpgrade.TALL_FROM <= 5));
    }
}
