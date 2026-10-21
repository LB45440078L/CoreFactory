package io.corefactory.core.turret;

import java.util.UUID;

/**
 * The rule that decides whether a turret may fire at one entity. Pure: the Spigot side reads the entity into a {@link Candidate}, and
 * this class answers. The checks run in a fixed order, and the first one that settles the candidate gives the verdict.
 *
 * <p>For a player the order is:
 * <ol>
 * <li>the owner of the turret is never a target ({@link Verdict#OWNER});</li>
 * <li>a player the policy spares is never a target ({@link Verdict#SPARED}), whatever else holds;</li>
 * <li>a player the policy marks as a target skips the team and friend rules below, and goes on to the claim check. It is targetable
 * even while friendly fire is off;</li>
 * <li>the team rule: a player on the owner's scoreboard team is spared ({@link Verdict#TEAM}) under
 * {@link TargetPolicy.TeamMode#SPARE}; under {@link TargetPolicy.TeamMode#TARGET} such a player counts as a stranger; under
 * {@link TargetPolicy.TeamMode#IGNORE} the team does not count;</li>
 * <li>the friends rule: a friend of the owner, or a player trusted in the claim, is spared ({@link Verdict#FRIEND});</li>
 * <li>the friendly-fire rule: any other player is a target only while the policy's friendly-fire switch is on, and is refused
 * ({@link Verdict#PLAYER}) while it is off.</li>
 * </ol>
 * For a mob the order is: a villager, a golem and an animal are never targets; a tamed or named mob is spared unless the settings
 * allow it; a mob of no hostile kind is refused, and a mob whose kind or entity type the policy and the settings do not target is refused.
 *
 * <p>Both players and mobs then face the claim protection ({@link Verdict#PROTECTED}), the range ({@link Verdict#OUT_OF_RANGE}), the arc
 * ({@link Verdict#OUT_OF_ARC}) and the line of sight ({@link Verdict#BLOCKED}). The range is either the candidate's own distance, or a sphere
 * around the turret's centre measured to the nearest point of the target's bounding box, as
 * {@link #verdict(Candidate, TurretSettings, TargetPolicy, Point, Point)} does.
 */
public final class TargetRules {

    /** The outcome of the rule. Only {@link #VALID} lets a turret fire. */
    public enum Verdict {
        VALID,
        PLAYER,
        OWNER,
        FRIEND,
        /** A player the policy spares by name. */
        SPARED,
        /** A player on the owner's scoreboard team, spared by the team rule. */
        TEAM,
        VILLAGER,
        GOLEM,
        ANIMAL,
        PROTECTED,
        TAMED,
        NAMED,
        NOT_A_TARGET_KIND,
        NOT_HOSTILE,
        OUT_OF_RANGE,
        OUT_OF_ARC,
        /** A solid block stands on the line from the turret's head to the target, so the shot would strike the block. */
        BLOCKED
    }

    /** How a player stands to the turret: its owner, a friend (the owner's friend list, or trusted in the turret's claim), or anyone else. */
    public enum Relation {
        /** Not a player, so no relation applies. */
        NONE,
        /** The player who placed the turret. */
        OWNER,
        /** A friend of the owner, or a player trusted in the claim where the turret stands. */
        FRIEND,
        /** A player who is neither the owner nor a friend. */
        OTHER
    }

    /**
     * A place in the world, in blocks. Used for the turret's centre and for the points of a target's bounding box.
     *
     * @param x the east-west coordinate
     * @param y the height
     * @param z the north-south coordinate
     */
    public record Point(double x, double y, double z) {

        /** The squared distance to another point, in square blocks. */
        public double distanceSquaredTo(Point other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    /**
     * What the Spigot side knows about one entity.
     *
     * @param kind          the kind the entity counts as: a monster, a phantom, a slime, a ghast or other
     * @param player        true for a player
     * @param villager      true for a villager
     * @param golem         true for an iron or snow golem, or any other golem
     * @param animal        true for an animal
     * @param tamed         true when the entity is tamed or has an owner
     * @param named         true when the entity has a custom name
     * @param protectedHere true when the entity stands in a region a claim system protects
     * @param distance      the distance from the turret's barrel to the entity, in blocks; used by the verdicts without a geometry
     * @param withinArc     true when the turret can turn to the entity within its arc and tilt limit
     * @param entity        the entity type's name, such as {@code ZOMBIE}; null when unknown
     * @param relation      for a player, how it stands to the turret; {@link Relation#NONE} otherwise
     * @param playerId      for a player, its unique id, which the per-player stances are keyed by; null otherwise
     * @param teamMate      true when the entity is on the owner's scoreboard team; the team rule reads it for players only
     * @param clearShot     true when no solid block stands on the line from the turret's head to the entity's centre
     */
    public record Candidate(
            TargetKind kind,
            boolean player,
            boolean villager,
            boolean golem,
            boolean animal,
            boolean tamed,
            boolean named,
            boolean protectedHere,
            double distance,
            boolean withinArc,
            String entity,
            Relation relation,
            UUID playerId,
            boolean teamMate,
            boolean clearShot) {

        /** A candidate with a per-player stance and team, and a clear shot. */
        public Candidate(TargetKind kind, boolean player, boolean villager, boolean golem, boolean animal, boolean tamed,
                         boolean named, boolean protectedHere, double distance, boolean withinArc, String entity, Relation relation,
                         UUID playerId, boolean teamMate) {
            this(kind, player, villager, golem, animal, tamed, named, protectedHere, distance, withinArc, entity, relation, playerId,
                    teamMate, true);
        }

        /** A candidate with an entity type and a relation, and no per-player stance or team, with a clear shot. */
        public Candidate(TargetKind kind, boolean player, boolean villager, boolean golem, boolean animal, boolean tamed,
                         boolean named, boolean protectedHere, double distance, boolean withinArc, String entity, Relation relation) {
            this(kind, player, villager, golem, animal, tamed, named, protectedHere, distance, withinArc, entity, relation, null, false,
                    true);
        }

        /** A candidate with no entity type and no relation, as the rule was first written. */
        public Candidate(TargetKind kind, boolean player, boolean villager, boolean golem, boolean animal, boolean tamed,
                         boolean named, boolean protectedHere, double distance, boolean withinArc) {
            this(kind, player, villager, golem, animal, tamed, named, protectedHere, distance, withinArc, null, Relation.NONE);
        }
    }

    private TargetRules() {
    }

    /** The verdict for one candidate under the given settings, with the turret's default policy. */
    public static Verdict verdict(Candidate candidate, TurretSettings settings) {
        return verdict(candidate, settings, TargetPolicy.DEFAULT);
    }

    /** The verdict for one candidate under the given settings and the turret's own policy. The range is the candidate's distance. */
    public static Verdict verdict(Candidate candidate, TurretSettings settings, TargetPolicy policy) {
        return decide(candidate, settings, policy, candidate.distance() <= settings.range());
    }

    /**
     * The verdict for one candidate with the range measured as a sphere: a target is in range when its nearest point of the bounding box
     * lies within the range of the turret's centre. The candidate's own distance is not used.
     *
     * @param turretCentre  the centre of the turret's head
     * @param targetNearest the nearest point of the target's bounding box to the turret's centre
     */
    public static Verdict verdict(Candidate candidate, TurretSettings settings, TargetPolicy policy, Point turretCentre,
                                  Point targetNearest) {
        return decide(candidate, settings, policy, inRange(turretCentre, targetNearest, settings.range()));
    }

    /** True when the sphere of the given range around the turret's centre holds the target's nearest point. The boundary counts as inside. */
    public static boolean inRange(Point turretCentre, Point targetNearestPoint, double range) {
        return turretCentre.distanceSquaredTo(targetNearestPoint) <= range * range;
    }

    /**
     * The point of an axis-aligned bounding box that is nearest to a point: each coordinate is clamped to the box's extent.
     *
     * @param point the point, such as the turret's centre
     * @param min   the box's least corner
     * @param max   the box's greatest corner
     */
    public static Point nearestPoint(Point point, Point min, Point max) {
        return new Point(clamp(point.x(), min.x(), max.x()), clamp(point.y(), min.y(), max.y()), clamp(point.z(), min.z(), max.z()));
    }

    private static Verdict decide(Candidate candidate, TurretSettings settings, TargetPolicy policy, boolean inRange) {
        if (candidate.player()) {
            Verdict player = playerVerdict(candidate, policy);
            if (player != null) {
                return player;
            }
        } else {
            if (candidate.villager()) {
                return Verdict.VILLAGER;
            }
            if (candidate.golem()) {
                return Verdict.GOLEM;
            }
            if (candidate.animal()) {
                return Verdict.ANIMAL;
            }
        }
        if (candidate.protectedHere()) {
            return Verdict.PROTECTED;
        }
        if (!candidate.player()) {
            if (candidate.tamed() && !settings.targetTamed()) {
                return Verdict.TAMED;
            }
            if (candidate.named() && !settings.targetNamed()) {
                return Verdict.NAMED;
            }
            if (candidate.kind() == TargetKind.OTHER) {
                return Verdict.NOT_HOSTILE;
            }
            if (!policy.allows(candidate.entity(), candidate.kind(), settings)) {
                return Verdict.NOT_A_TARGET_KIND;
            }
        }
        if (!inRange) {
            return Verdict.OUT_OF_RANGE;
        }
        if (!candidate.withinArc()) {
            return Verdict.OUT_OF_ARC;
        }
        if (!candidate.clearShot()) {
            return Verdict.BLOCKED;
        }
        return Verdict.VALID;
    }

    /**
     * The verdict that settles a player, or null when the player may go on to the protection and range checks. The owner is never a
     * target, and a player the policy spares is never one either. A player the policy marks as a target goes on at once. Otherwise the
     * team rule, the friends rule and the friendly-fire switch decide, in that order.
     */
    private static Verdict playerVerdict(Candidate candidate, TargetPolicy policy) {
        if (candidate.relation() == Relation.OWNER) {
            return Verdict.OWNER;
        }
        TargetPolicy.PlayerStance stance = candidate.playerId() == null
                ? null
                : policy.stanceOf(candidate.playerId()).orElse(null);
        if (stance == TargetPolicy.PlayerStance.SPARE) {
            return Verdict.SPARED;
        }
        if (stance == TargetPolicy.PlayerStance.TARGET) {
            return null;
        }
        if (candidate.teamMate() && policy.teamMode() == TargetPolicy.TeamMode.SPARE) {
            return Verdict.TEAM;
        }
        boolean friend = candidate.relation() == Relation.FRIEND
                && !(candidate.teamMate() && policy.teamMode() == TargetPolicy.TeamMode.TARGET);
        if (friend) {
            return Verdict.FRIEND;
        }
        if (!policy.friendlyFire()) {
            return Verdict.PLAYER;
        }
        return null;
    }

    /** True when the candidate may be fired at, with the turret's default policy. */
    public static boolean isValid(Candidate candidate, TurretSettings settings) {
        return verdict(candidate, settings) == Verdict.VALID;
    }

    /** True when the candidate may be fired at under the given policy. */
    public static boolean isValid(Candidate candidate, TurretSettings settings, TargetPolicy policy) {
        return verdict(candidate, settings, policy) == Verdict.VALID;
    }

    /** True when the candidate may be fired at under the given policy, with the range measured as a sphere. */
    public static boolean isValid(Candidate candidate, TurretSettings settings, TargetPolicy policy, Point turretCentre,
                                  Point targetNearest) {
        return verdict(candidate, settings, policy, turretCentre, targetNearest) == Verdict.VALID;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
