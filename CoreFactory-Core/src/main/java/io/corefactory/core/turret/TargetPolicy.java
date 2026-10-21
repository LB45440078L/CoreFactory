package io.corefactory.core.turret;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The targeting choices of one turret, on top of the settings of config.yml: the friendly-fire switch, per-turret overrides of the
 * target kinds and of single entity types, a stance for single players, and how the owner's scoreboard team is treated. An override is
 * keyed by an entity type's name, such as {@code ZOMBIE}, or by a kind's config name, such as {@code monsters}. An entity override wins
 * over its kind's override, and a kind override wins over the config. A turret with no overrides follows the config, so it targets what
 * the config says.
 *
 * <p>A player stance is {@link PlayerStance#TARGET} or {@link PlayerStance#SPARE}; a player with no stance follows the friend rules. The
 * team mode is {@link TeamMode#SPARE} by default, which spares the owner's scoreboard team as friends. The verdict order that reads these
 * is in {@link TargetRules}.
 *
 * @param friendlyFire whether the turret may fire at players that are not its owner's friends
 * @param overrides    the per-turret overrides of entity types and kinds, true to target, false to spare
 * @param players      the stance of each player that has one, by unique id
 * @param teamMode     how the owner's scoreboard team is treated
 */
public record TargetPolicy(boolean friendlyFire, Map<String, Boolean> overrides, Map<UUID, TargetPolicy.PlayerStance> players,
                           TargetPolicy.TeamMode teamMode) {

    /** The stance of one player: a target even while friendly fire is off, or never a target. */
    public enum PlayerStance {
        /** A hostile player: targetable even when friendly fire is off, and whatever its friend or team standing. */
        TARGET,
        /** Never a target, whatever the other settings say. */
        SPARE
    }

    /**
     * How the owner's scoreboard team is treated.
     */
    public enum TeamMode {
        /** Team members are spared, as friends are. */
        SPARE,
        /** Team members are targetable like strangers, subject to friendly fire. */
        TARGET,
        /** The team does not count: only the friend rules apply. */
        IGNORE
    }

    /** The policy of a new turret: no friendly fire, no overrides, no player stances, and the team spared. */
    public static final TargetPolicy DEFAULT = new TargetPolicy(false, Map.of());

    /** A policy with the given friendly fire and entity overrides, no player stances, and the team spared. */
    public TargetPolicy(boolean friendlyFire, Map<String, Boolean> overrides) {
        this(friendlyFire, overrides, Map.of(), TeamMode.SPARE);
    }

    public TargetPolicy {
        overrides = Map.copyOf(overrides);
        players = Map.copyOf(players);
        teamMode = teamMode == null ? TeamMode.SPARE : teamMode;
    }

    /**
     * Whether a mob may be a target under this policy.
     *
     * @param entity   the entity type's name, such as {@code ZOMBIE}; null when unknown
     * @param kind     the kind the entity counts as
     * @param settings the settings of the config
     */
    public boolean allows(String entity, TargetKind kind, TurretSettings settings) {
        if (entity != null) {
            Boolean own = overrides.get(entity);
            if (own != null) {
                return own;
            }
        }
        Boolean byKind = overrides.get(kind.configName());
        if (byKind != null && kind != TargetKind.OTHER) {
            return byKind;
        }
        return settings.targets(kind);
    }

    /** The override of a key, an entity type's name or a kind's config name, or empty when the key follows the config. */
    public Optional<Boolean> overrideOf(String key) {
        return Optional.ofNullable(overrides.get(key));
    }

    /** The stance of a player, by unique id, or empty when the player has none and follows the friend rules. */
    public Optional<PlayerStance> stanceOf(UUID player) {
        if (player == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(players.get(player));
    }

    /** The same policy with the friendly-fire switch set. */
    public TargetPolicy withFriendlyFire(boolean on) {
        return new TargetPolicy(on, overrides, players, teamMode);
    }

    /** The same policy with one override set, replacing any earlier override of that key. */
    public TargetPolicy withOverride(String key, boolean value) {
        Map<String, Boolean> next = new TreeMap<>(overrides);
        next.put(key, value);
        return new TargetPolicy(friendlyFire, next, players, teamMode);
    }

    /** The same policy with one player's stance set, replacing any earlier stance. A null stance clears it, so the player follows the friend rules. */
    public TargetPolicy withPlayer(UUID player, PlayerStance stance) {
        Objects.requireNonNull(player, "player");
        Map<UUID, PlayerStance> next = new HashMap<>(players);
        if (stance == null) {
            next.remove(player);
        } else {
            next.put(player, stance);
        }
        return new TargetPolicy(friendlyFire, overrides, next, teamMode);
    }

    /** The same policy with the team mode set. */
    public TargetPolicy withTeamMode(TeamMode mode) {
        return new TargetPolicy(friendlyFire, overrides, players, mode);
    }
}
