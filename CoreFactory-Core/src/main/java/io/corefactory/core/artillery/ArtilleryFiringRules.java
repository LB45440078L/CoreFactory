package io.corefactory.core.artillery;

import io.corefactory.core.artillery.ArtillerySettings.ShellSpec;
import io.corefactory.core.artillery.ArtillerySettings.Safety;

import java.util.List;
import java.util.OptionalLong;

/**
 * The safety rules a shot must pass before it is fired. Each rule is a pure check that returns a {@link FiringVerdict}, so each can be
 * tested on its own, and {@link #check} runs them all and returns the first refusal.
 *
 * <p>Distances to the target are measured on the ground for the range and the least distance, and in three dimensions for the blast,
 * since a blast reaches a player above the ground as well as beside it. The blast reach of a shell is a radius around the aim point: a
 * blast radius that grows with the cube root of the yield, the reach of a cloud, a burst or a spread (see {@link #reachBlocks}).
 */
public final class ArtilleryFiringRules {

    /**
     * How far a power satisfaction may fall short of 1.0 and still count as full. It is a tolerance for the float arithmetic of the power
     * network, not a tuning value.
     */
    static final double POWER_EPSILON = 1e-6;

    private final ArtillerySettings settings;
    private final ArtilleryTable table;

    /**
     * A shot to check: the shell, the piece and its charge, the barrel's elevation, the positions, and the state of the power and the
     * antimatter cooldown.
     *
     * @param shell                    the shell fired
     * @param level                    the artillery level of the piece
     * @param tier                     the charge tier loaded
     * @param elevationDegrees         the elevation of the barrel, in degrees
     * @param artillery                the position of the piece
     * @param aim                      the point the shot is aimed at, on the ground
     * @param firingPlayer            the position of the player who fires
     * @param players                  the positions of every player on the server, which a nuclear shot keeps clear of
     * @param powerSatisfaction        the share of the piece's power that the network gives it, from 0 to 1
     * @param lastAntimatterShotMillis the time of this player's last antimatter shot, in milliseconds, or empty when there was none
     * @param nowMillis                the current time, in milliseconds
     */
    public record Shot(ArtilleryShell shell, ArtilleryLevel level, ChargeTier tier, double elevationDegrees,
                       Position artillery, Position aim, Position firingPlayer, List<Position> players,
                       double powerSatisfaction, OptionalLong lastAntimatterShotMillis, long nowMillis) {
    }

    /**
     * @param settings the settings whose safety numbers and tables the rules use
     */
    public ArtilleryFiringRules(ArtillerySettings settings) {
        this.settings = settings;
        this.table = new ArtilleryTable(settings);
    }

    /**
     * The radius a blast of a yield reaches, in blocks: the standard scaling of a blast with the cube root of its yield, and at least
     * the floor in config.yml, so a small shell is still checked as if it were a real explosion.
     *
     * @param yieldTons the yield in tons of TNT equivalent, 0 or more
     * @return the blast radius in blocks
     */
    public double blastRadiusBlocks(double yieldTons) {
        Safety safety = settings.safety();
        return Math.max(safety.blastRadiusFloorBlocks(), safety.blastRadiusConstant() * Math.cbrt(yieldTons));
    }

    /**
     * The distance from the aim point at which the shell's effect stops, in blocks: the blast radius for a blast; the cloud's radius for
     * a phosphorus shell; half the length of the burst for a napalm shell; and for a submunition shell, the spread of its submunitions
     * plus the blast of one of them.
     *
     * @param shell the shell
     * @return the reach in blocks
     */
    public double reachBlocks(ArtilleryShell shell) {
        ShellSpec spec = settings.shell(shell);
        return switch (shell.effect()) {
            case BLAST -> blastRadiusBlocks(spec.yieldTons());
            case SMOKE -> settings.phosphorus().cloudRadiusBlocks();
            case FIRE -> settings.napalm().lengthBlocks() / 2.0;
            case SUBMUNITIONS -> shell == ArtilleryShell.CLUSTER
                    ? settings.cluster().spreadRadiusBlocks() + blastRadiusBlocks(spec.yieldTons())
                    : settings.clusterEnriched().spreadRadiusBlocks() + blastRadiusBlocks(spec.yieldTons());
        };
    }

    /**
     * The least distance from the piece to the target for a shell, in blocks: a conventional shell, a nuclear one, or the antimatter
     * shell, which may be fired only from far away.
     *
     * @param shell the shell
     * @return the least distance in blocks
     */
    public double minimumDistanceBlocks(ArtilleryShell shell) {
        Safety safety = settings.safety();
        if (shell == ArtilleryShell.CONDENSED_ANTIMATTER) {
            return safety.minDistanceAntimatterBlocks();
        }
        return shell.nuclear() ? safety.minDistanceNuclearBlocks() : safety.minDistanceConventionalBlocks();
    }

    /**
     * Whether the charge is at least the tier the shell needs.
     *
     * @param shell the shell
     * @param tier  the charge tier loaded
     * @return OK, or CHARGE_TOO_LOW
     */
    public FiringVerdict charge(ArtilleryShell shell, ChargeTier tier) {
        return tier.ordinal() >= shell.requiredTier().ordinal() ? FiringVerdict.OK : FiringVerdict.CHARGE_TOO_LOW;
    }

    /**
     * Whether the piece's level is at least the level the shell needs.
     *
     * @param shell the shell
     * @param level the artillery level
     * @return OK, or LEVEL_TOO_LOW
     */
    public FiringVerdict level(ArtilleryShell shell, ArtilleryLevel level) {
        return level.number() >= shell.minimumLevel().number() ? FiringVerdict.OK : FiringVerdict.LEVEL_TOO_LOW;
    }

    /**
     * Whether the barrel's elevation is within the limits: from the least to the greatest elevation of any shot, and, for a cluster
     * enriched uranium shot, at least its steeper minimum.
     *
     * @param shell            the shell
     * @param elevationDegrees the elevation, in degrees
     * @return OK, ELEVATION_TOO_LOW, ELEVATION_TOO_HIGH or CLUSTER_ELEVATION_TOO_LOW
     */
    public FiringVerdict elevation(ArtilleryShell shell, double elevationDegrees) {
        Safety safety = settings.safety();
        if (!(elevationDegrees >= safety.minElevationDegrees())) {
            return FiringVerdict.ELEVATION_TOO_LOW;
        }
        if (!(elevationDegrees <= safety.maxElevationDegrees())) {
            return FiringVerdict.ELEVATION_TOO_HIGH;
        }
        if (shell == ArtilleryShell.CLUSTER_ENRICHED_URANIUM
                && !(elevationDegrees >= settings.clusterEnriched().minElevationDegrees())) {
            return FiringVerdict.CLUSTER_ELEVATION_TOO_LOW;
        }
        return FiringVerdict.OK;
    }

    /**
     * Whether the target is within the range of the piece at its level and its charge.
     *
     * @param level          the artillery level
     * @param tier           the charge tier
     * @param distanceBlocks the horizontal distance to the target, in blocks
     * @return OK, or OUT_OF_RANGE
     */
    public FiringVerdict range(ArtilleryLevel level, ChargeTier tier, double distanceBlocks) {
        return distanceBlocks <= table.rangeBlocks(level, tier) ? FiringVerdict.OK : FiringVerdict.OUT_OF_RANGE;
    }

    /**
     * Whether the target is far enough from the piece for the shell.
     *
     * @param shell          the shell
     * @param distanceBlocks the horizontal distance to the target, in blocks
     * @return OK, or TOO_CLOSE
     */
    public FiringVerdict minimumDistance(ArtilleryShell shell, double distanceBlocks) {
        return distanceBlocks >= minimumDistanceBlocks(shell) ? FiringVerdict.OK : FiringVerdict.TOO_CLOSE;
    }

    /**
     * Whether the blast stays clear of the piece and of the player who fires: the aim point must be further from each of them than the
     * shell's reach, plus the margin in config.yml, plus the scatter the shot may land with.
     *
     * @param shell         the shell
     * @param aim           the aim point
     * @param artillery     the piece's position
     * @param firingPlayer  the firing player's position
     * @param scatterBlocks the scatter the shot allows for, in blocks, which is the scatter sigma times the configured number of sigmas
     * @return OK, BLAST_REACHES_ARTILLERY or BLAST_REACHES_PLAYER
     */
    public FiringVerdict blast(ArtilleryShell shell, Position aim, Position artillery, Position firingPlayer,
                               double scatterBlocks) {
        double clearance = reachBlocks(shell) + settings.safety().blastMarginBlocks() + scatterBlocks;
        if (aim.distance(artillery) <= clearance) {
            return FiringVerdict.BLAST_REACHES_ARTILLERY;
        }
        if (aim.distance(firingPlayer) <= clearance) {
            return FiringVerdict.BLAST_REACHES_PLAYER;
        }
        return FiringVerdict.OK;
    }

    /**
     * Whether a nuclear shell's aim point is clear of every player. The safety radius is the fallout radius: the reach times the
     * fallout multiple in config.yml, plus the scatter the shot allows for. A non-nuclear shell has no such limit.
     *
     * @param shell         the shell
     * @param aim           the aim point
     * @param players       the positions of every player
     * @param scatterBlocks the scatter the shot allows for, in blocks
     * @return OK, or NUCLEAR_NEAR_PLAYER
     */
    public FiringVerdict nuclearNearPlayers(ArtilleryShell shell, Position aim, List<Position> players, double scatterBlocks) {
        if (!shell.nuclear()) {
            return FiringVerdict.OK;
        }
        double safetyRadius = settings.safety().falloutMultiple() * reachBlocks(shell) + scatterBlocks;
        for (Position player : players) {
            if (aim.distance(player) <= safetyRadius) {
                return FiringVerdict.NUCLEAR_NEAR_PLAYER;
            }
        }
        return FiringVerdict.OK;
    }

    /**
     * Whether the antimatter cooldown has passed for this player. Only the antimatter shell has one; the cooldown is the configured
     * number of hours, and a shot is allowed again exactly when that much time has passed.
     *
     * @param shell                    the shell
     * @param lastAntimatterShotMillis the player's last antimatter shot, in milliseconds, or empty when there was none
     * @param nowMillis                the current time, in milliseconds
     * @return OK, or COOLDOWN_ACTIVE
     */
    public FiringVerdict cooldown(ArtilleryShell shell, OptionalLong lastAntimatterShotMillis, long nowMillis) {
        if (shell != ArtilleryShell.CONDENSED_ANTIMATTER || lastAntimatterShotMillis.isEmpty()) {
            return FiringVerdict.OK;
        }
        long elapsed = nowMillis - lastAntimatterShotMillis.getAsLong();
        return elapsed >= settings.antimatterCooldownMillis() ? FiringVerdict.OK : FiringVerdict.COOLDOWN_ACTIVE;
    }

    /**
     * Whether the piece has its full power: a satisfaction of 1.0, within the tolerance POWER_EPSILON.
     *
     * @param powerSatisfaction the share of the power the network gives the piece, from 0 to 1
     * @return OK, or POWER_SHORT
     */
    public FiringVerdict power(double powerSatisfaction) {
        return powerSatisfaction >= 1.0 - POWER_EPSILON ? FiringVerdict.OK : FiringVerdict.POWER_SHORT;
    }

    /**
     * Runs every rule on a shot and returns the first refusal, in the order: charge, level, elevation, range, least distance, blast,
     * nuclear distance from players, cooldown and power.
     *
     * @param shot the shot
     * @return OK when the shot may be fired, otherwise the first rule it breaks
     */
    public FiringVerdict check(Shot shot) {
        double distance = shot.artillery().horizontalDistance(shot.aim());
        double scatter = settings.safety().scatterSigmas()
                * table.scatterSigmaBlocks(shot.level(), shot.tier(), distance);
        List<FiringVerdict> verdicts = List.of(
                charge(shot.shell(), shot.tier()),
                level(shot.shell(), shot.level()),
                elevation(shot.shell(), shot.elevationDegrees()),
                range(shot.level(), shot.tier(), distance),
                minimumDistance(shot.shell(), distance),
                blast(shot.shell(), shot.aim(), shot.artillery(), shot.firingPlayer(), scatter),
                nuclearNearPlayers(shot.shell(), shot.aim(), shot.players(), scatter),
                cooldown(shot.shell(), shot.lastAntimatterShotMillis(), shot.nowMillis()),
                power(shot.powerSatisfaction()));
        for (FiringVerdict verdict : verdicts) {
            if (!verdict.ok()) {
                return verdict;
            }
        }
        return FiringVerdict.OK;
    }
}
