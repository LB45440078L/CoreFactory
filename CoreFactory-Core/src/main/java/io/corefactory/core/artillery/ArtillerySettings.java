package io.corefactory.core.artillery;

import io.corefactory.core.config.ConfigValues;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The typed form of the {@code artillery} section of config.yml: every number the artillery rules use. The level, charge and
 * shell tables, the patterns of the cluster shells, the phosphorus cloud and the safety margins all come from here. Every
 * value has a default, equal to the one in config.yml, and a value that is missing, not finite, or out of range gives its
 * default. Distances are in blocks, times in ticks or seconds as the name says, power in W, yields in tons of TNT.
 *
 * @param heightBlocks          the model height of levels I to VII, in blocks
 * @param powerW                the electrical power of levels I to VII, in W
 * @param rangeLevelOneBlocks   the range of a level I piece with a tier I charge, in blocks
 * @param rangeLevelSevenBlocks the range of a level VII piece with a tier I charge, in blocks
 * @param reloadLevelOneTicks   the reload of a level I piece with a tier I charge, in ticks
 * @param reloadLevelSevenTicks the reload of a level VII piece with a tier I charge, in ticks
 * @param scatterLevelOneBlocks the standard deviation of the landing scatter of a level I piece at 1000 blocks, in blocks
 * @param scatterLevelSevenBlocks the same, for a level VII piece
 * @param dragPerSecond         the linear air drag of a shell, per second
 * @param magazineCapacity      the most shells, and the most charges, a piece's magazine takes on a load
 * @param charges               the multipliers of each charge tier
 * @param shells                the yield and burst altitude range of each shell
 * @param phosphorus            the toxic cloud of a white phosphorus shell
 * @param napalm                the burst of a napalm shell
 * @param cluster               the submunitions of a cluster shell
 * @param clusterEnriched       the bombs of a cluster enriched uranium shell
 * @param safety                the distance, blast and elevation rules and the antimatter cooldown
 * @param presentation          the sounds, particles and motion of a shot: the launch, the recoil, the flight, the landing and the reload
 */
public record ArtillerySettings(
        List<Double> heightBlocks,
        List<Double> powerW,
        double rangeLevelOneBlocks,
        double rangeLevelSevenBlocks,
        int reloadLevelOneTicks,
        int reloadLevelSevenTicks,
        double scatterLevelOneBlocks,
        double scatterLevelSevenBlocks,
        double dragPerSecond,
        int magazineCapacity,
        Map<ChargeTier, ChargeMultipliers> charges,
        Map<ArtilleryShell, ShellSpec> shells,
        Phosphorus phosphorus,
        Napalm napalm,
        Cluster cluster,
        ClusterEnriched clusterEnriched,
        Safety safety,
        Presentation presentation) {

    /** The section's name in config.yml. */
    public static final String SECTION = "artillery";

    /** The model heights of levels I to VII in config.yml, in blocks. */
    public static final List<Double> DEFAULT_HEIGHT_BLOCKS = List.of(1.5, 1.5, 2.5, 2.5, 2.5, 3.0, 3.75);

    /** The electrical powers of levels I to VII in config.yml, in W: a rounded geometric rise of about 1.5 times a level. */
    public static final List<Double> DEFAULT_POWER_W =
            List.of(100_000.0, 150_000.0, 230_000.0, 350_000.0, 540_000.0, 830_000.0, 1_270_000.0);

    /** The range of a level I piece in config.yml, in blocks. */
    public static final double DEFAULT_RANGE_LEVEL_ONE_BLOCKS = 1500.0;

    /** The range of a level VII piece in config.yml, in blocks. */
    public static final double DEFAULT_RANGE_LEVEL_SEVEN_BLOCKS = 9000.0;

    /** The reload of a level I piece in config.yml, in ticks: 20 seconds. */
    public static final int DEFAULT_RELOAD_LEVEL_ONE_TICKS = 400;

    /** The reload of a level VII piece in config.yml, in ticks: 60 seconds. */
    public static final int DEFAULT_RELOAD_LEVEL_SEVEN_TICKS = 1200;

    /** The landing scatter of a level I piece in config.yml, in blocks at 1000 blocks of range. */
    public static final double DEFAULT_SCATTER_LEVEL_ONE_BLOCKS = 60.0;

    /** The landing scatter of a level VII piece in config.yml, in blocks at 1000 blocks of range. */
    public static final double DEFAULT_SCATTER_LEVEL_SEVEN_BLOCKS = 8.0;

    /** The launch sound's volume in config.yml. A volume of 4 is heard about 64 blocks away. */
    public static final double DEFAULT_LAUNCH_VOLUME = 4.0;

    /** The launch sound's pitch in config.yml: low, so the report sounds heavy. */
    public static final double DEFAULT_LAUNCH_PITCH = 0.5;

    /** The barrel's recoil in config.yml, in blocks: how far the barrel kicks back at a shot. */
    public static final double DEFAULT_RECOIL_BLOCKS = 0.5;

    /** The flight whistle's volume in config.yml. A volume of 0.5 is heard about 8 blocks away, so the whistle is near the shell only. */
    public static final double DEFAULT_WHISTLE_VOLUME = 0.5;

    /** The ticks between two whistles of one shell in config.yml: 40 ticks is two seconds. */
    public static final int DEFAULT_WHISTLE_INTERVAL_TICKS = 40;

    /** The trail puffs one shell leaves each tick in config.yml, at most. */
    public static final int DEFAULT_TRAIL_PUFFS_PER_TICK = 3;

    /** The volume of the thud of a shell that lands without a blast, in config.yml. */
    public static final double DEFAULT_IMPACT_VOLUME = 3.0;

    /** The debris particles of a blast on the ground, in config.yml. */
    public static final int DEFAULT_DEBRIS_PARTICLES = 60;

    /** The volume of the hiss of a phosphorus cloud and the crackle of a napalm fire, in config.yml. */
    public static final double DEFAULT_HAZARD_VOLUME = 0.6;

    /** The ticks between two hisses or crackles of one cloud or fire in config.yml: 80 ticks is four seconds. */
    public static final int DEFAULT_HAZARD_INTERVAL_TICKS = 80;

    /** The volume of the reload sound, when a piece is ready to fire again, in config.yml. */
    public static final double DEFAULT_RELOAD_VOLUME = 0.7;

    /** The linear air drag in config.yml, per second: a shell loses about 5 per cent of its range at 9000 blocks. */
    public static final double DEFAULT_DRAG_PER_SECOND = 0.002;

    /** The most shells, and the most charges, a piece's magazine takes on a load, in config.yml. */
    public static final int DEFAULT_MAGAZINE_CAPACITY = 16;

    /** The number of artillery levels. */
    static final int LEVEL_COUNT = ArtilleryLevel.values().length;

    public ArtillerySettings {
        heightBlocks = List.copyOf(heightBlocks);
        powerW = List.copyOf(powerW);
        if (heightBlocks.size() != LEVEL_COUNT || powerW.size() != LEVEL_COUNT) {
            throw new IllegalArgumentException("heights and powers need one value per artillery level, " + LEVEL_COUNT);
        }
        charges = Collections.unmodifiableMap(new EnumMap<>(charges));
        shells = Collections.unmodifiableMap(new EnumMap<>(shells));
        if (charges.size() != ChargeTier.values().length || shells.size() != ArtilleryShell.values().length) {
            throw new IllegalArgumentException("every charge tier and every shell needs its settings");
        }
    }

    /** The settings the code runs with when config.yml gives none: every default of this class. */
    public static ArtillerySettings defaults() {
        return from(null);
    }

    /**
     * Reads the {@code artillery} section, with the yields of the nuclear warheads from tnts.yml. A missing section or key takes its
     * default, and so does a value that is not finite or is out of range.
     *
     * @param config the root of config.yml as plain data, or null for the defaults
     * @param tnts   the root of tnts.yml as plain data, or null for the values shipped in it
     * @return the settings
     */
    public static ArtillerySettings from(Map<String, Object> config, Map<String, Object> tnts) {
        Map<String, Object> art = ConfigValues.section(config, SECTION);
        NuclearYields nuclear = NuclearYields.from(tnts);

        Map<ChargeTier, ChargeMultipliers> charges = new EnumMap<>(ChargeTier.class);
        for (ChargeTier tier : ChargeTier.values()) {
            ChargeMultipliers def = defaultCharge(tier);
            String prefix = "charges." + tier.key() + ".";
            charges.put(tier, new ChargeMultipliers(
                    positive(art, prefix + "range", def.range()),
                    positive(art, prefix + "reload", def.reload()),
                    positive(art, prefix + "scatter", def.scatter())));
        }

        double tntBlockTons = positive(art, "shells.tnt-block-tons", 1.0);
        int tntBlocks = positiveInt(art, "shells.high-explosive.tnt-blocks", 10);
        double bombShare = share(art, "shells.cluster-enriched-uranium.bomb-share-of-davy-crockett", 0.75);
        Map<ArtilleryShell, ShellSpec> shells = new EnumMap<>(ArtilleryShell.class);
        for (ArtilleryShell shell : ArtilleryShell.values()) {
            ShellSpec def = defaultShell(shell);
            String prefix = "shells." + shell.key() + ".";
            double yield = switch (shell) {
                case HIGH_EXPLOSIVE -> tntBlocks * tntBlockTons;
                // The Little Boy and the Davy Crockett are read from tnts.yml, so their numbers live there only.
                case ENRICHED_URANIUM -> nuclear.littleBoyTons();
                case CLUSTER_ENRICHED_URANIUM -> nuclear.davyCrockettTons() * bombShare;
                // A smoke shell and a fire shell make no blast, so they have no yield to read.
                case WHITE_PHOSPHORUS, NAPALM -> 0.0;
                default -> nonNegative(art, prefix + "yield-tons", def.yieldTons());
            };
            double burstMin = nonNegative(art, prefix + "burst-min-blocks", def.burstMinBlocks());
            double burstMax = Math.max(burstMin, nonNegative(art, prefix + "burst-max-blocks", def.burstMaxBlocks()));
            shells.put(shell, new ShellSpec(yield, burstMin, burstMax));
        }

        return new ArtillerySettings(
                positiveList(ConfigValues.numberList(art, "levels.height-blocks", DEFAULT_HEIGHT_BLOCKS),
                        DEFAULT_HEIGHT_BLOCKS),
                positiveList(ConfigValues.numberList(art, "levels.power-w", DEFAULT_POWER_W), DEFAULT_POWER_W),
                positive(art, "levels.range-level-1-blocks", DEFAULT_RANGE_LEVEL_ONE_BLOCKS),
                positive(art, "levels.range-level-7-blocks", DEFAULT_RANGE_LEVEL_SEVEN_BLOCKS),
                positiveInt(art, "levels.reload-level-1-ticks", DEFAULT_RELOAD_LEVEL_ONE_TICKS),
                positiveInt(art, "levels.reload-level-7-ticks", DEFAULT_RELOAD_LEVEL_SEVEN_TICKS),
                positive(art, "levels.scatter-level-1-blocks", DEFAULT_SCATTER_LEVEL_ONE_BLOCKS),
                positive(art, "levels.scatter-level-7-blocks", DEFAULT_SCATTER_LEVEL_SEVEN_BLOCKS),
                nonNegative(art, "ballistics.drag-per-second", DEFAULT_DRAG_PER_SECOND),
                positiveInt(art, "magazine-capacity", DEFAULT_MAGAZINE_CAPACITY),
                charges,
                shells,
                new Phosphorus(
                        positive(art, "shells.white-phosphorus.cloud-radius-blocks", 8.0),
                        nonNegative(art, "shells.white-phosphorus.damage-per-tick", 0.05),
                        positive(art, "shells.white-phosphorus.duration-seconds", 60.0),
                        positive(art, "shells.white-phosphorus.half-life-seconds", 20.0),
                        positiveInt(art, "shells.white-phosphorus.light-particles", 600)),
                new Napalm(
                        positive(art, "shells.napalm.length-blocks", 30.0),
                        positive(art, "shells.napalm.width-blocks", 12.0)),
                new Cluster(
                        positiveInt(art, "shells.cluster.submunitions", 40),
                        positive(art, "shells.cluster.spread-radius-blocks", 30.0),
                        nonNegative(art, "shells.cluster.min-spacing-blocks", 4.0)),
                new ClusterEnriched(
                        positiveInt(art, "shells.cluster-enriched-uranium.bombs", 5),
                        positive(art, "shells.cluster-enriched-uranium.spread-radius-blocks", 50.0),
                        nonNegative(art, "shells.cluster-enriched-uranium.min-spacing-blocks", 40.0),
                        nonNegative(art, "shells.cluster-enriched-uranium.min-elevation-degrees", 25.0)),
                new Safety(
                        nonNegative(art, "safety.minimum-distance-conventional-blocks", 50.0),
                        nonNegative(art, "safety.minimum-distance-nuclear-blocks", 200.0),
                        nonNegative(art, "safety.minimum-distance-antimatter-blocks", 2000.0),
                        nonNegative(art, "safety.blast-margin-blocks", 20.0),
                        nonNegative(art, "safety.scatter-sigmas", 3.0),
                        positive(art, "safety.blast-radius-constant", 1.15),
                        nonNegative(art, "safety.blast-radius-floor-blocks", 15.0),
                        positive(art, "safety.fallout-multiple", 3.0),
                        nonNegative(art, "safety.min-elevation-degrees", 5.0),
                        nonNegative(art, "safety.max-elevation-degrees", 85.0),
                        nonNegative(art, "safety.antimatter-cooldown-hours", 12.0)),
                new Presentation(
                        positive(art, "presentation.launch-volume", DEFAULT_LAUNCH_VOLUME),
                        positive(art, "presentation.launch-pitch", DEFAULT_LAUNCH_PITCH),
                        nonNegative(art, "presentation.recoil-blocks", DEFAULT_RECOIL_BLOCKS),
                        positive(art, "presentation.whistle-volume", DEFAULT_WHISTLE_VOLUME),
                        positiveInt(art, "presentation.whistle-interval-ticks", DEFAULT_WHISTLE_INTERVAL_TICKS),
                        nonNegativeInt(art, "presentation.trail-puffs-per-tick", DEFAULT_TRAIL_PUFFS_PER_TICK),
                        positive(art, "presentation.impact-volume", DEFAULT_IMPACT_VOLUME),
                        nonNegativeInt(art, "presentation.debris-particles", DEFAULT_DEBRIS_PARTICLES),
                        positive(art, "presentation.hazard-volume", DEFAULT_HAZARD_VOLUME),
                        positiveInt(art, "presentation.hazard-interval-ticks", DEFAULT_HAZARD_INTERVAL_TICKS),
                        positive(art, "presentation.reload-volume", DEFAULT_RELOAD_VOLUME)));
    }

    /**
     * Reads the {@code artillery} section with the nuclear yields of the shipped tnts.yml.
     *
     * @param config the root of config.yml as plain data, or null for the defaults
     * @return the settings
     */
    public static ArtillerySettings from(Map<String, Object> config) {
        return from(config, null);
    }

    /** The model height of a level, in blocks. */
    public double heightOf(ArtilleryLevel level) {
        return heightBlocks.get(level.ordinal());
    }

    /** The electrical power of a level, in W. */
    public double powerOf(ArtilleryLevel level) {
        return powerW.get(level.ordinal());
    }

    /** The multipliers of a charge tier. */
    public ChargeMultipliers charge(ChargeTier tier) {
        return charges.get(tier);
    }

    /** The yield and burst altitudes of a shell. */
    public ShellSpec shell(ArtilleryShell shell) {
        return shells.get(shell);
    }

    /** The sounds, particles and motion of a shot: the launch, the recoil, the flight, the landing and the reload. */
    public record Presentation(double launchVolume, double launchPitch, double recoilBlocks, double whistleVolume,
                               int whistleIntervalTicks, int trailPuffsPerTick, double impactVolume, int debrisParticles,
                               double hazardVolume, int hazardIntervalTicks, double reloadVolume) {
    }

    /** The antimatter cooldown in milliseconds, from the hours in config.yml. */
    public long antimatterCooldownMillis() {
        return Math.round(safety.antimatterCooldownHours() * 3_600_000.0);
    }

    /**
     * The multipliers of one charge tier. Each one scales the value of the level it applies to: a tier II charge with a range
     * multiplier of 1.25 reaches a quarter further than a tier I charge.
     *
     * @param range   the multiplier of the range
     * @param reload  the multiplier of the reload time, so a higher value reloads more slowly
     * @param scatter the multiplier of the landing scatter's standard deviation, so a higher value spreads the shells more
     */
    public record ChargeMultipliers(double range, double reload, double scatter) {
    }

    /**
     * The yield and the burst altitude range of a shell. A shell that detonates on contact has no burst range, so both
     * altitudes are 0; a shell with a fixed burst height has the same minimum and maximum.
     *
     * @param yieldTons      the yield in tons of TNT equivalent: for a cluster shell, the submunition's, for a cluster enriched uranium
     *                       shell, the bomb's
     * @param burstMinBlocks the lowest altitude above the ground the shell bursts at, in blocks
     * @param burstMaxBlocks the highest altitude above the ground the shell bursts at, in blocks
     */
    public record ShellSpec(double yieldTons, double burstMinBlocks, double burstMaxBlocks) {
    }

    /**
     * The toxic cloud of a white phosphorus shell. The cloud is a disc on the ground; a living creature in it takes damage every
     * tick, and the damage halves every half-life, so a cloud that has hung for a while hurts less.
     *
     * @param cloudRadiusBlocks the radius of the cloud on the ground, in blocks
     * @param damagePerTick     the damage a creature takes each tick at the start of the cloud, in HP
     * @param durationSeconds   how long the cloud lasts, in seconds
     * @param halfLifeSeconds   the time the damage takes to halve, in seconds
     * @param lightParticles    the light particles one burst spawns, for the visual
     */
    public record Phosphorus(double cloudRadiusBlocks, double damagePerTick, double durationSeconds, double halfLifeSeconds,
                             int lightParticles) {
    }

    /**
     * The burst of a napalm shell: an ellipse on the ground, elongated along the direction of the shot.
     *
     * @param lengthBlocks the length of the burst along the shot, in blocks
     * @param widthBlocks  the width of the burst across the shot, in blocks
     */
    public record Napalm(double lengthBlocks, double widthBlocks) {
    }

    /**
     * The submunitions of a cluster shell.
     *
     * @param submunitions     how many submunitions one shell releases
     * @param spreadRadiusBlocks the radius of the disc the submunitions land in, around the burst point, in blocks
     * @param minSpacingBlocks the least distance between two submunitions, in blocks
     */
    public record Cluster(int submunitions, double spreadRadiusBlocks, double minSpacingBlocks) {
    }

    /**
     * The bombs of a cluster enriched uranium shell. Each bomb is a little weaker than a Davy Crockett, and the bombs are placed
     * apart so that one does not set off the next.
     *
     * @param bombs              how many bombs one shell releases
     * @param spreadRadiusBlocks the radius of the disc the bombs land in, around the burst point, in blocks
     * @param minSpacingBlocks   the least distance between two bombs, in blocks
     * @param minElevationDegrees the steepest the shot may be allowed to be low: the shot needs at least this elevation, so the
     *                           bombs fall from above
     */
    public record ClusterEnriched(int bombs, double spreadRadiusBlocks, double minSpacingBlocks, double minElevationDegrees) {
    }

    /**
     * The safety rules: the least distance from the artillery to the target by kind of shell, the margin a blast keeps from the
     * artillery and the player, the scale of the blast radius, the elevation limits and the antimatter cooldown.
     *
     * @param minDistanceConventionalBlocks the least distance to a target for a conventional shell, in blocks
     * @param minDistanceNuclearBlocks      the least distance to a target for a nuclear shell, in blocks
     * @param minDistanceAntimatterBlocks   the least distance to a target for the antimatter shell, in blocks
     * @param blastMarginBlocks             the gap kept between the reach of a blast and the artillery or the player, in blocks
     * @param scatterSigmas                 how many standard deviations of scatter the blast checks allow for
     * @param blastRadiusConstant           the blast radius per cube root of the yield in tons, in blocks: radius = constant x cbrt(tons)
     * @param blastRadiusFloorBlocks        the least blast radius, in blocks, so a small shell is not checked as if it were tiny
     * @param falloutMultiple               the fallout radius as a multiple of the blast radius; a nuclear shell may not be fired at
     *                                      a target within this radius of a player
     * @param minElevationDegrees           the lowest elevation of any shot, in degrees
     * @param maxElevationDegrees           the highest elevation of any shot, in degrees
     * @param antimatterCooldownHours       the time a player must wait between two antimatter shots, in hours
     */
    public record Safety(double minDistanceConventionalBlocks, double minDistanceNuclearBlocks,
                         double minDistanceAntimatterBlocks, double blastMarginBlocks, double scatterSigmas,
                         double blastRadiusConstant, double blastRadiusFloorBlocks, double falloutMultiple,
                         double minElevationDegrees, double maxElevationDegrees, double antimatterCooldownHours) {
    }

    private static ChargeMultipliers defaultCharge(ChargeTier tier) {
        return switch (tier) {
            case TIER_I -> new ChargeMultipliers(1.0, 1.0, 1.0);
            case TIER_II -> new ChargeMultipliers(1.25, 1.25, 1.2);
            case TIER_III -> new ChargeMultipliers(1.6, 1.5, 1.5);
        };
    }

    private static ShellSpec defaultShell(ArtilleryShell shell) {
        return switch (shell) {
            case EXPLOSIVE -> new ShellSpec(1.0, 0.0, 0.0);
            case HIGH_EXPLOSIVE -> new ShellSpec(10.0, 0.0, 0.0);
            case WHITE_PHOSPHORUS -> new ShellSpec(0.0, 25.0, 35.0);
            case NAPALM -> new ShellSpec(0.0, 0.0, 0.0);
            case CLUSTER -> new ShellSpec(3.0, 30.0, 50.0);
            case ENRICHED_URANIUM -> new ShellSpec(15_000.0, 8.0, 8.0);
            case HYDROGEN_THERMONUCLEAR -> new ShellSpec(1_000_000.0, 20.0, 30.0);
            case CLUSTER_ENRICHED_URANIUM -> new ShellSpec(15.0, 8.0, 8.0);
            case CONDENSED_ANTIMATTER -> new ShellSpec(500_000_000.0, 50.0, 50.0);
        };
    }

    private static List<Double> positiveList(List<Double> values, List<Double> fallback) {
        boolean ok = values.size() == LEVEL_COUNT && values.stream().allMatch(v -> Double.isFinite(v) && v > 0);
        return ok ? values : fallback;
    }

    private static double positive(Map<String, Object> section, String path, double def) {
        double value = ConfigValues.number(section, path, def);
        return Double.isFinite(value) && value > 0 ? value : def;
    }

    private static double nonNegative(Map<String, Object> section, String path, double def) {
        double value = ConfigValues.number(section, path, def);
        return Double.isFinite(value) && value >= 0 ? value : def;
    }

    /** A share of a whole, above 0 and at most 1. Anything else gives the default. */
    private static double share(Map<String, Object> section, String path, double def) {
        double value = ConfigValues.number(section, path, def);
        return Double.isFinite(value) && value > 0 && value <= 1 ? value : def;
    }

    private static int positiveInt(Map<String, Object> section, String path, int def) {
        int value = ConfigValues.integer(section, path, def);
        return value > 0 ? value : def;
    }

    private static int nonNegativeInt(Map<String, Object> section, String path, int def) {
        int value = ConfigValues.integer(section, path, def);
        return value >= 0 ? value : def;
    }
}
