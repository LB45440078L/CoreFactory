package io.corefactory.spigot.machine;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Decides which players see a machine's host block concealed. A player is in range when they stand in the machine's world
 * and no farther than the range from the centre of the host block. The decision takes each player's world and squared
 * distance as plain values, so it runs without a server.
 */
public final class ConcealmentRules {

    /**
     * A player as the decision sees them.
     *
     * @param id              the player's unique id
     * @param world           the unique id of the world the player stands in
     * @param distanceSquared the squared distance from the player to the host block's centre, or infinity in another world
     */
    public record Viewer(UUID id, UUID world, double distanceSquared) {
    }

    /**
     * What to change for one machine.
     *
     * @param conceal the players in range who do not see the block hidden yet: send air to them
     * @param refresh the players in range who already see it hidden: send air again
     * @param restore the players who saw it hidden and are no longer in range, or are not online: send the real block
     */
    public record Decision(Set<UUID> conceal, Set<UUID> refresh, Set<UUID> restore) {
    }

    private ConcealmentRules() {
    }

    /**
     * Decides the change for one machine.
     *
     * @param machineWorld the unique id of the world the host block stands in
     * @param range        the range in blocks from the host block's centre; a negative range puts no one in range
     * @param concealed    the players who see the host block hidden now
     * @param online       every player online
     * @return the players to conceal, to refresh and to restore
     */
    public static Decision decide(UUID machineWorld, double range, Set<UUID> concealed, Collection<Viewer> online) {
        Set<UUID> conceal = new HashSet<>();
        Set<UUID> refresh = new HashSet<>();
        Set<UUID> inRange = new HashSet<>();
        for (Viewer viewer : online) {
            if (!inRange(machineWorld, range, viewer)) {
                continue;
            }
            inRange.add(viewer.id());
            if (concealed.contains(viewer.id())) {
                refresh.add(viewer.id());
            } else {
                conceal.add(viewer.id());
            }
        }
        Set<UUID> restore = new HashSet<>();
        for (UUID id : concealed) {
            if (!inRange.contains(id)) {
                restore.add(id);
            }
        }
        return new Decision(Set.copyOf(conceal), Set.copyOf(refresh), Set.copyOf(restore));
    }

    private static boolean inRange(UUID machineWorld, double range, Viewer viewer) {
        return range >= 0 && machineWorld.equals(viewer.world()) && viewer.distanceSquared() <= range * range;
    }
}
