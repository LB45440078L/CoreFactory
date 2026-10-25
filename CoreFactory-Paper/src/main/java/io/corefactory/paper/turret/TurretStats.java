package io.corefactory.paper.turret;

import io.corefactory.core.railgun.RailgunSettings;
import io.corefactory.core.turret.DualTargeting;
import io.corefactory.core.turret.LaserSettings;
import io.corefactory.core.turret.TurretLevel;
import io.corefactory.core.turret.TurretLevels;
import io.corefactory.core.turret.TurretSettings;

import java.util.Map;

/**
 * The stats the turrets run with, read once from the configuration: the machine gun's general settings (targeting, turning, search,
 * rounds), the laser's armour bypass and beam, the rail gun's own section, the dual spacing, and the level table of each kind. A turret's range,
 * fire interval, ammunition, power, cannons and damage come from its own level, so {@link #view} gives the settings of one level. Pure once
 * built.
 */
public final class TurretStats {

    private final TurretSettings settings;
    private final LaserSettings laser;
    private final RailgunSettings railgun;
    private final double spacing;
    private final TurretLevels machineGun;
    private final TurretLevels lasers;
    private final TurretLevels railguns;
    private final TurretSettings[][] views;

    private TurretStats(TurretSettings settings, LaserSettings laser, RailgunSettings railgun, double spacing, TurretLevels machineGun,
                        TurretLevels lasers) {
        this.settings = settings;
        this.laser = laser;
        this.railgun = railgun;
        this.spacing = spacing;
        this.machineGun = machineGun;
        this.lasers = lasers;
        this.railguns = railgun.turretLevels();
        this.views = new TurretSettings[TurretKind.values().length][];
        for (TurretKind kind : TurretKind.values()) {
            TurretLevels levels = levels(kind);
            TurretSettings[] byLevel = new TurretSettings[TurretLevels.MAX_LEVEL + 1];
            for (int level = 1; level <= kind.type().maxLevel(); level++) {
                byLevel[level] = viewOf(kind, levels.level(level));
            }
            views[kind.ordinal()] = byLevel;
        }
    }

    /**
     * Reads the stats from a configuration.
     *
     * @param config the root configuration, which holds the {@code turrets} and {@code laser-turret} sections
     */
    public static TurretStats from(Map<String, Object> config) {
        return new TurretStats(TurretSettings.from(config), LaserSettings.from(config), RailgunSettings.from(config),
                DualTargeting.spacingOf(config), TurretLevels.from(TurretKind.MACHINE_GUN.type(), config),
                TurretLevels.from(TurretKind.LASER.type(), config));
    }

    /** The machine gun's general settings: targeting, turning, arc, search, idle sweep, rounds and ammunition per shot. */
    public TurretSettings settings() {
        return settings;
    }

    /**
     * The laser's own settings: the armour it ignores, its particles and its scan interval. Its range, damage, interval, power and rotation are
     * per level.
     */
    public LaserSettings laser() {
        return laser;
    }

    /**
     * The rail gun's own settings: whether rail guns run, its shot, its screen's light and its stats per level. Its range, damage, power and
     * charge and reload are per level, in {@link #levels}.
     */
    public RailgunSettings railgun() {
        return railgun;
    }

    /** The spacing between the two targets of a dual shot, in blocks. */
    public double spacing() {
        return spacing;
    }

    /** The level table of a kind. A rail gun's table is read from its own section. */
    public TurretLevels levels(TurretKind kind) {
        return switch (kind) {
            case MACHINE_GUN -> machineGun;
            case LASER -> lasers;
            case RAILGUN -> railguns;
        };
    }

    /**
     * The stats of one level of a kind.
     *
     * @param level the level, from 1 to {@link TurretLevels#MAX_LEVEL}; a value out of range is clamped
     */
    public TurretLevel level(TurretKind kind, int level) {
        return levels(kind).level(clamp(kind, level));
    }

    /**
     * The settings a turret of a kind and level runs with: the general settings, with this level's range, fire interval, ammunition and
     * power. An electric turret, a laser or a rail gun, has no spread and no magazine of its own, so its view holds one round at most.
     *
     * @param level the level, from 1 to the top of the kind; a value out of range is clamped
     */
    public TurretSettings view(TurretKind kind, int level) {
        return views[kind.ordinal()][clamp(kind, level)];
    }

    /** The rounds a turret of a kind and level holds: its belt's capacity for a machine gun, and one for a laser, which holds none. */
    public int magazineCapacity(TurretKind kind, int level) {
        return view(kind, level).ammoCapacity();
    }

    /** The damage per hit of a kind and level, with the level's boost. */
    public double damage(TurretKind kind, int level) {
        return level(kind, level).effectiveDamage();
    }

    /** The cannons a kind and level fires with. */
    public int cannons(TurretKind kind, int level) {
        return level(kind, level).cannons();
    }

    private TurretSettings viewOf(TurretKind kind, TurretLevel level) {
        boolean lasing = kind.isElectric();
        return new TurretSettings(
                settings.enabled(),
                level.range(),
                lasing ? laser.searchIntervalTicks() : settings.searchIntervalTicks(),
                level.fireIntervalTicks(),
                level.rotationDegreesOr(settings.rotationSpeedDegrees()),
                settings.arcDegrees(),
                settings.pitchLimitDegrees(),
                settings.idleSweep(),
                lasing ? 0.0 : settings.accuracySpread(),
                Math.max(1, level.ammoCapacity()),
                settings.ammoPerShot(),
                level.powerDemandW(),
                settings.targets(),
                settings.targetNamed(),
                settings.targetTamed(),
                settings.rounds());
    }

    /** The level clamped to the top of its kind: 1 to 5 for a machine gun or a laser, 1 to 3 for a rail gun. */
    private static int clamp(TurretKind kind, int level) {
        return Math.max(1, Math.min(level, kind.type().maxLevel()));
    }
}
