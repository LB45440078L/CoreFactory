package io.corefactory.core.research;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Keeps every player's research in memory and writes each change to the store in the background, so the main thread
 * never waits for the database. A player's research is read once when they join, and kept until they leave.
 *
 * <p>A gated id is unlocked when any node that lists it is finished. A node is finished when its requirements are, and
 * its cost has been fed in. A node with no cost finishes on its own once its requirements are.
 */
public final class ResearchService {

    private final ResearchTree tree;
    private final ResearchStore store;
    private final Executor writes;
    private final Logger log;
    private final Map<UUID, Set<String>> finished = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Map<String, Integer>>> progress = new ConcurrentHashMap<>();
    private final List<BiConsumer<UUID, String>> finishListeners = new CopyOnWriteArrayList<>();

    /**
     * @param writes runs the blocking store calls, normally a single background thread. Tests pass a direct executor.
     */
    public ResearchService(ResearchTree tree, ResearchStore store, Executor writes, Logger log) {
        this.tree = tree;
        this.store = store;
        this.writes = writes;
        this.log = log;
    }

    public ResearchTree tree() {
        return tree;
    }

    /**
     * Calls the listener on the thread that feeds a node, each time a player finishes one through {@link #feed}. Not
     * called for {@link #grant}, which is an administrator's act.
     */
    public void onFinished(BiConsumer<UUID, String> listener) {
        finishListeners.add(listener);
    }

    /** Reads a player's finished nodes and progress in the background. Safe to call again. */
    public CompletableFuture<Void> load(UUID player) {
        return CompletableFuture.runAsync(() -> {
            Set<String> done = new HashSet<>(store.unlocked(player));
            Map<String, Map<String, Integer>> partial = new HashMap<>();
            for (ResearchTree.Node node : tree.nodes()) {
                if (done.contains(node.id()) || node.free()) {
                    continue;
                }
                Map<String, Integer> fed = new HashMap<>();
                for (String pack : node.cost().keySet()) {
                    int units = store.progress(player, progressKey(node.id(), pack));
                    if (units > 0) {
                        fed.put(pack, units);
                    }
                }
                if (!fed.isEmpty()) {
                    partial.put(node.id(), fed);
                }
            }
            finished.put(player, done);
            progress.put(player, partial);
        }, writes);
    }

    /** Drops a player's research from memory once they leave. Nothing is lost: it is already stored. */
    public void forget(UUID player) {
        finished.remove(player);
        progress.remove(player);
    }

    /** True when the player has finished the node, or it is free once its requirements are finished. */
    public boolean isFinished(UUID player, String node) {
        Set<String> done = finished.getOrDefault(player, Set.of());
        if (done.contains(node)) {
            return true;
        }
        return tree.node(node)
                .filter(ResearchTree.Node::free)
                .map(n -> n.requires().stream().allMatch(required -> isFinished(player, required)))
                .orElse(false);
    }

    /** True when the id may be crafted, placed or ignited by the player. Ids no node gates are always allowed. */
    public boolean isUnlocked(UUID player, String id) {
        if (!tree.isGated(id)) {
            return true;
        }
        return tree.gatesOf(id).stream().anyMatch(node -> isFinished(player, node));
    }

    /**
     * The node a player should finish next to unlock an id: the first gating node that is not finished, or empty when
     * the id is already unlocked or free.
     */
    public Optional<ResearchTree.Node> nodeFor(UUID player, String id) {
        if (isUnlocked(player, id)) {
            return Optional.empty();
        }
        List<String> gates = tree.gatesOf(id);
        return gates.isEmpty() ? Optional.empty() : tree.node(gates.get(0));
    }

    /** The packs fed into a node so far, by pack id. */
    public Map<String, Integer> progressOf(UUID player, String node) {
        return progress.getOrDefault(player, Map.of()).getOrDefault(node, Map.of());
    }

    /**
     * Feeds packs into a node. Nothing happens unless the node's requirements are finished and it is not already done.
     *
     * @param offered the packs the player has to offer, by pack id; the packs used are reported in the result
     * @return what was taken, or an empty result when the node cannot take packs now
     */
    public Progression.Result feed(UUID player, String node, Map<String, Integer> offered) {
        ResearchTree.Node definition = tree.node(node).orElse(null);
        if (definition == null || isFinished(player, node) || definition.free()
                || !definition.requires().stream().allMatch(required -> isFinished(player, required))) {
            return new Progression.Result(Map.of(), progressOf(player, node), false);
        }
        Progression.Result result = Progression.apply(definition.cost(), progressOf(player, node), offered);
        progress.computeIfAbsent(player, key -> new ConcurrentHashMap<>()).put(node, result.done());
        if (!result.consumed().isEmpty()) {
            saveProgress(player, node, result.done());
        }
        if (result.finished()) {
            finished.computeIfAbsent(player, key -> ConcurrentHashMap.newKeySet()).add(node);
            progress.get(player).remove(node);
            saveUnlock(player, node);
            for (BiConsumer<UUID, String> listener : finishListeners) {
                listener.accept(player, node);
            }
        }
        return result;
    }

    /** Finishes a node outright, for an administrator. Does not check requirements or cost. */
    public void grant(UUID player, String node) {
        if (tree.node(node).isEmpty()) {
            return;
        }
        finished.computeIfAbsent(player, key -> ConcurrentHashMap.newKeySet()).add(node);
        saveUnlock(player, node);
    }

    /** The store key of one pack's progress in a node: the store keeps one number per key. */
    static String progressKey(String node, String pack) {
        return node + "/" + pack;
    }

    private void saveUnlock(UUID player, String node) {
        writes.execute(() -> {
            try {
                store.unlock(player, node, System.currentTimeMillis());
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "Could not save research " + node + " for " + player, e);
            }
        });
    }

    private void saveProgress(UUID player, String node, Map<String, Integer> done) {
        Map<String, Integer> copy = Map.copyOf(done);
        writes.execute(() -> {
            try {
                for (Map.Entry<String, Integer> part : copy.entrySet()) {
                    store.setProgress(player, progressKey(node, part.getKey()), part.getValue());
                }
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "Could not save research progress for " + node + " of " + player, e);
            }
        });
    }
}
