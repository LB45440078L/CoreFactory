package io.corefactory.core.turret;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * The coordinated choice of targets for the turrets of one world. Each turret offers the targets it can fire at now, nearest first, and the
 * targets its cannons hold; the rule answers with the targets each turret's cannons take, so that the turrets spread over the mobs before
 * any two of them share one. Pure: the Spigot side reads the entities into {@link TurretSlot}s and gives each turret its answer.
 *
 * <p>The rule works over the turrets in turret id order, so the same input always gives the same answer, and a tie between turrets goes to
 * the lower id. It runs in three passes:
 * <ol>
 * <li>Sticky claims. A cannon's held target is kept while it is still among the turret's candidates and no earlier turret kept it, so the
 * turrets do not flip between targets. A held target that is no longer a candidate is released, and so is one an earlier turret kept.</li>
 * <li>Spread. Round by round, every turret that still lacks a target takes the nearest candidate that no turret has claimed in this pass.
 * The first cannons of all turrets are served before any second cannon. A second cannon first prefers an unclaimed candidate within the
 * spacing of its turret's first target, nearest to that target, and takes the nearest unclaimed candidate only when none lies within it.
 * The preference applies only to the round that fills the second cannon, so it never takes a mob a first cannon of another turret could
 * still take.</li>
 * <li>Share. Only when every candidate is already claimed does a cannon that is still empty share one. It takes the candidate that the fewest
 * turrets hold, nearest first, that is not already one of its own targets. A turret with no candidate gets nothing.</li>
 * </ol>
 */
public final class TargetAssignment {

    private TargetAssignment() {
    }

    /**
     * What one turret offers to the rule: its cannons, the targets it holds, and the targets it can fire at this tick.
     *
     * @param turret     the turret's id; the answer is keyed by it, and the turrets are taken in its order
     * @param cannons    the cannons that fire, from none up; a turret with none takes no target
     * @param claimed    the targets its cannons hold now, in cannon order; a null entry is an empty cannon and is left out
     * @param candidates the targets it can fire at this tick, nearest first, already filtered by the verdict and the clear shot
     */
    public record TurretSlot(String turret, int cannons, List<UUID> claimed, List<UUID> candidates) {

        public TurretSlot {
            cannons = Math.max(0, cannons);
            claimed = claimed.stream().filter(Objects::nonNull).toList();
            candidates = List.copyOf(candidates);
        }
    }

    /** The working state of one turret while the rule runs. */
    private static final class Plan {

        private final TurretSlot slot;
        private final Set<UUID> reachable;
        private final List<UUID> held = new ArrayList<>();

        private Plan(TurretSlot slot) {
            this.slot = slot;
            this.reachable = new HashSet<>(slot.candidates());
        }

        private boolean full() {
            return held.size() >= slot.cannons();
        }
    }

    /**
     * The targets each turret's cannons take, with no spacing preference: the same answer as {@link #assign(List, double, Function)} with no
     * candidate places.
     *
     * @param turrets the turrets of one world, taken together; the ids are unique
     * @return the targets of each turret, by its id, unmodifiable
     */
    public static Map<String, List<UUID>> assign(List<TurretSlot> turrets) {
        return assign(turrets, 0.0, id -> null);
    }

    /**
     * The targets each turret's cannons take, with the spacing preference of the second cannon. A turret's list holds one target per cannon
     * that is filled, in cannon order, and it is empty for a turret with no candidate. The map has one entry per turret, in turret id order.
     *
     * @param turrets   the turrets of one world, taken together; the ids are unique
     * @param spacing   the most a second target may lie from the turret's first target, in blocks, inclusive; a negative value counts as zero
     * @param positions the place of each candidate, such as the centre of its box; a null answer means no place is known, and that candidate
     *                  is never chosen by the preference (it can still be taken by the plain nearest rule)
     * @return the targets of each turret, by its id, unmodifiable
     */
    public static Map<String, List<UUID>> assign(List<TurretSlot> turrets, double spacing, Function<UUID, TargetRules.Point> positions) {
        return assign(turrets, Set.of(), spacing, positions);
    }

    /**
     * The targets each turret's cannons take, as {@link #assign(List, double, Function)}, with the targets that turrets outside the pass hold.
     * A reserved target is taken by no cannon of the pass while another candidate is free, and a cannon of the pass that holds one loses it.
     * A reserved target is taken only as the last candidate a cannon can take, so a cannon is never left empty for want of one.
     *
     * @param turrets   the turrets given targets in this pass; the ids are unique
     * @param reserved  the targets the cannons of the turrets outside the pass hold; empty when every turret of the world is in the pass
     * @param spacing   the most a second target may lie from the turret's first target, in blocks, inclusive; a negative value counts as zero
     * @param positions the place of each candidate; a null answer means no place is known
     * @return the targets of each turret, by its id, unmodifiable
     */
    public static Map<String, List<UUID>> assign(List<TurretSlot> turrets, Set<UUID> reserved, double spacing,
                                                 Function<UUID, TargetRules.Point> positions) {
        double limit = Math.max(0.0, spacing);
        List<TurretSlot> ordered = new ArrayList<>(turrets);
        ordered.sort(Comparator.comparing(TurretSlot::turret));
        List<Plan> plans = new ArrayList<>(ordered.size());
        for (TurretSlot slot : ordered) {
            plans.add(new Plan(slot));
        }
        Map<UUID, Integer> holders = new HashMap<>();

        for (Plan plan : plans) {
            for (UUID id : plan.slot.claimed()) {
                if (!plan.full() && plan.reachable.contains(id) && !holders.containsKey(id) && !reserved.contains(id)) {
                    hold(plan, id, holders);
                }
            }
        }

        int rounds = 0;
        for (Plan plan : plans) {
            rounds = Math.max(rounds, plan.slot.cannons());
        }
        for (int round = 0; round < rounds; round++) {
            for (Plan plan : plans) {
                if (!plan.full() && plan.held.size() <= round) {
                    nextUnclaimed(plan, holders, reserved, limit, positions).ifPresent(id -> hold(plan, id, holders));
                }
            }
        }

        for (Plan plan : plans) {
            while (!plan.full()) {
                Optional<UUID> shared = leastHeldShare(plan, holders, reserved);
                if (shared.isEmpty()) {
                    break;
                }
                hold(plan, shared.get(), holders);
            }
        }

        Map<String, List<UUID>> answer = new LinkedHashMap<>();
        for (Plan plan : plans) {
            answer.put(plan.slot.turret(), List.copyOf(plan.held));
        }
        return Collections.unmodifiableMap(answer);
    }

    private static void hold(Plan plan, UUID id, Map<UUID, Integer> holders) {
        plan.held.add(id);
        holders.merge(id, 1, Integer::sum);
    }

    /**
     * The target of the next cannon of a turret in the spread. A first cannon takes the nearest unclaimed candidate. A second cannon takes the
     * unclaimed candidate within the spacing of the turret's first target when there is one, and the nearest unclaimed candidate otherwise.
     */
    private static Optional<UUID> nextUnclaimed(Plan plan, Map<UUID, Integer> holders, Set<UUID> reserved, double limit,
                                                Function<UUID, TargetRules.Point> positions) {
        if (!plan.held.isEmpty()) {
            Optional<UUID> within = nearestUnclaimedWithin(plan, holders, reserved, limit, positions);
            if (within.isPresent()) {
                return within;
            }
        }
        return nearestUnclaimed(plan, holders, reserved);
    }

    /**
     * The unclaimed candidate that lies within the spacing of the turret's first target, inclusive, and lies nearest to that target. Empty when
     * none does, or when the first target has no known place.
     */
    private static Optional<UUID> nearestUnclaimedWithin(Plan plan, Map<UUID, Integer> holders, Set<UUID> reserved, double limit,
                                                         Function<UUID, TargetRules.Point> positions) {
        TargetRules.Point first = positions.apply(plan.held.get(0));
        if (first == null) {
            return Optional.empty();
        }
        double reach = limit * limit;
        UUID best = null;
        double bestSquared = Double.POSITIVE_INFINITY;
        for (UUID id : plan.slot.candidates()) {
            if (holders.containsKey(id) || reserved.contains(id)) {
                continue;
            }
            TargetRules.Point at = positions.apply(id);
            if (at == null) {
                continue;
            }
            double squared = first.distanceSquaredTo(at);
            if (squared <= reach && squared < bestSquared) {
                best = id;
                bestSquared = squared;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * The nearest candidate that no turret holds in this pass and that is not reserved, or empty when every candidate is held or reserved.
     */
    private static Optional<UUID> nearestUnclaimed(Plan plan, Map<UUID, Integer> holders, Set<UUID> reserved) {
        for (UUID id : plan.slot.candidates()) {
            if (!holders.containsKey(id) && !reserved.contains(id)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    /**
     * The candidate that the fewest turrets hold, nearest first, that the turret does not already hold. A reserved candidate comes after every
     * other one. Empty when the turret has no such candidate.
     */
    private static Optional<UUID> leastHeldShare(Plan plan, Map<UUID, Integer> holders, Set<UUID> reserved) {
        UUID best = null;
        boolean bestReserved = false;
        int bestHolders = Integer.MAX_VALUE;
        for (UUID id : plan.slot.candidates()) {
            if (plan.held.contains(id)) {
                continue;
            }
            boolean isReserved = reserved.contains(id);
            int count = holders.getOrDefault(id, 0);
            boolean better = best == null || (bestReserved != isReserved ? bestReserved : count < bestHolders);
            if (better) {
                best = id;
                bestReserved = isReserved;
                bestHolders = count;
            }
        }
        return Optional.ofNullable(best);
    }
}
