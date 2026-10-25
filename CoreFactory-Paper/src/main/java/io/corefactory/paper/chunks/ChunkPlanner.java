package io.corefactory.paper.chunks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Decides, with no server types, which chunks get a plugin ticket and which lose theirs. The chunks the plugin keeps are chosen
 * in two steps:
 *
 * <ol>
 *   <li>The held chunks that are still needed come first, so a ticket that is already placed is not moved for nothing.</li>
 *   <li>The other needed chunks follow, in the order of {@link #ORDER}, until the cap is full.</li>
 * </ol>
 *
 * <p>The order of {@link #ORDER} is the world id, then the chunk x, then the chunk z, all in ascending order. It does not depend on
 * the players, so the chunks kept do not change while nothing is placed or removed. A value of the cap below 0 counts as 0.
 */
public final class ChunkPlanner {

    /** The order the chunks are taken in: the world id, then the chunk x, then the chunk z, all ascending. */
    public static final Comparator<ChunkKey> ORDER = Comparator.comparing(ChunkKey::world)
            .thenComparingInt(ChunkKey::x)
            .thenComparingInt(ChunkKey::z);

    /**
     * What to do with the tickets.
     *
     * @param toTicket  the chunks to add a ticket to, in {@link #ORDER}
     * @param toRelease the held chunks to remove the ticket of, in {@link #ORDER}
     * @param skipped   the needed chunks that get no ticket because the cap is full
     */
    public record Plan(List<ChunkKey> toTicket, List<ChunkKey> toRelease, int skipped) {
    }

    private ChunkPlanner() {
    }

    /**
     * Plans the tickets from the chunks held now and the chunks needed. A chunk that is needed and already held keeps its ticket, up to
     * the cap. Nothing is released that is still needed while the cap has room for it.
     *
     * @param held      the chunks that have a ticket now
     * @param needed    the chunks that hold a working device
     * @param maxForced the most chunks that may be held at once
     * @return the chunks to ticket, the chunks to release, and how many needed chunks the cap leaves out
     */
    public static Plan plan(Set<ChunkKey> held, Set<ChunkKey> needed, int maxForced) {
        Objects.requireNonNull(held, "held");
        Objects.requireNonNull(needed, "needed");
        int cap = Math.max(0, maxForced);

        List<ChunkKey> keep = new ArrayList<>();
        for (ChunkKey key : sorted(needed, held::contains)) {
            if (keep.size() < cap) {
                keep.add(key);
            }
        }
        for (ChunkKey key : sorted(needed, key -> !held.contains(key))) {
            if (keep.size() < cap) {
                keep.add(key);
            }
        }
        Set<ChunkKey> kept = new HashSet<>(keep);

        List<ChunkKey> toTicket = sorted(kept, key -> !held.contains(key));
        List<ChunkKey> toRelease = sorted(held, key -> !kept.contains(key));
        int skipped = 0;
        for (ChunkKey key : needed) {
            if (!kept.contains(key)) {
                skipped++;
            }
        }
        return new Plan(toTicket, toRelease, skipped);
    }

    /** The chunks of a collection that match a test, in {@link #ORDER}. */
    private static List<ChunkKey> sorted(Collection<ChunkKey> from, Predicate<ChunkKey> wanted) {
        List<ChunkKey> chunks = new ArrayList<>();
        for (ChunkKey key : from) {
            if (wanted.test(key)) {
                chunks.add(key);
            }
        }
        chunks.sort(ORDER);
        return List.copyOf(chunks);
    }
}
