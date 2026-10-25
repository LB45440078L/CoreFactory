package io.corefactory.paper.chunks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Keeps the chunks that hold working devices loaded. Paper keeps a chunk loaded while a plugin ticket is on it, so the tickers of the devices
 * keep running with no player near. The tickets are held through the plugin's {@link ChunkTickets}, as the owner {@link #OWNER}: the keeper
 * never places or removes a ticket itself, so a chunk the artillery also holds keeps its ticket when the keeper lets go of it.
 *
 * <p>{@link #reconcile(Set)} plans the chunks the keeper keeps with the {@link ChunkPlanner}, from the chunks it holds, and sets its chunks to
 * them. The cap {@code maxForced} applies to the keeper's own chunks. A needed chunk whose world is not loaded is left out, and a chunk the
 * book could not ticket is placed again on the next reconcile. {@link #release()} drops the keeper's chunks.
 *
 * <p>Tickets are not saved. A server restart drops them, and the first reconcile after start-up holds the chunks again.
 */
public final class ChunkKeeper {

    /** The keeper's owner id in the plugin's tickets. */
    public static final String OWNER = "device keeper";

    private final ChunkTickets tickets;
    private final Predicate<UUID> worldLoaded;
    private final Logger log;
    private final boolean enabled;
    private final int maxForced;
    private boolean startLogged;
    private boolean capWarned;

    /**
     * @param tickets     the plugin's chunk tickets, in which the keeper holds its chunks
     * @param worldLoaded tells whether the world with an id is loaded; a chunk of a world that is not loaded is not kept
     * @param log         receives the start-up count and the cap warning
     * @param enabled     false to keep no chunk: every reconcile then releases what is held
     * @param maxForced   the most chunks the keeper holds at once; a negative value counts as 0
     */
    public ChunkKeeper(ChunkTickets tickets, Predicate<UUID> worldLoaded, Logger log, boolean enabled, int maxForced) {
        this.tickets = tickets;
        this.worldLoaded = worldLoaded;
        this.log = log;
        this.enabled = enabled;
        this.maxForced = Math.max(0, maxForced);
    }

    /**
     * Brings the keeper's chunks in line with the chunks needed. A needed chunk whose world is not loaded is skipped for now. Logs the number of
     * chunks kept at the first call, and a warning once when the cap leaves needed chunks without a ticket.
     *
     * @param needed the chunks that hold a working device
     */
    public void reconcile(Set<ChunkKey> needed) {
        Set<ChunkKey> loadable = new HashSet<>();
        if (enabled) {
            for (ChunkKey key : needed) {
                if (worldLoaded.test(key.world())) {
                    loadable.add(key);
                }
            }
        }
        Set<ChunkKey> held = tickets.chunksOf(OWNER);
        ChunkPlanner.Plan plan = ChunkPlanner.plan(held, loadable, maxForced);
        Set<ChunkKey> kept = new HashSet<>(held);
        kept.removeAll(plan.toRelease());
        kept.addAll(plan.toTicket());
        Set<ChunkKey> unticketed = tickets.setOwned(OWNER, kept);
        if (plan.skipped() > 0 && !capWarned) {
            capWarned = true;
            log.warning(plan.skipped() + " chunk(s) with working devices are not kept loaded, since chunks.max-forced is "
                    + maxForced + " and that many are already kept. Raise it in config.yml to keep them loaded.");
        }
        if (!startLogged) {
            startLogged = true;
            if (enabled) {
                log.info("Keeping " + (kept.size() - unticketed.size()) + " chunk(s) with working devices loaded (chunks.max-forced "
                        + maxForced + ").");
            } else {
                log.info("Chunks with working devices are not kept loaded (chunks.keep-loaded is false).");
            }
        }
    }

    /**
     * Drops the keeper's chunks. A chunk that the artillery or another owner still holds keeps its ticket. Called when the plugin is disabled.
     */
    public void release() {
        tickets.release(OWNER);
    }
}
