package io.corefactory.paper.chunks;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The plugin chunk tickets of a plugin, in one place. Paper keeps one plugin ticket per plugin and chunk, and the one call that removes it
 * removes it for every holder, so no holder places or removes a ticket on its own. Each holder is an owner, named by an id: an owner holds
 * the chunks it needs, and a chunk gets its ticket when the first owner needs it and loses it when the last owner lets go. The device chunk
 * keeper and the artillery both hold their chunks here, through the book of the plugin, {@link #of(Plugin)}.
 *
 * <p>{@link #hold} adds chunks to an owner, {@link #release} drops every chunk of an owner, {@link #setOwned} makes an owner hold exactly
 * the chunks given, and {@link #releaseAll} drops every owner. A chunk whose world is not loaded cannot be ticketed: the book still records
 * it for its owners and reports it as unticketed, and places its ticket on a later call that names it, or on {@link #ensure} and
 * {@link #ensureAll}.
 *
 * <p>Tickets are not saved. A restart drops them, and the owners hold their chunks again.
 */
public final class ChunkTickets {

    /** The book of each plugin, made by the first call for it. */
    private static final Map<Plugin, ChunkTickets> BOOKS = new HashMap<>();

    /** Places and removes the plugin's tickets on the chunks. */
    public interface Port {

        /**
         * Adds the plugin's ticket to a chunk, when its world is loaded. A chunk that already has the ticket keeps it.
         *
         * @return true when the chunk has the plugin's ticket now, false when its world is not loaded
         */
        boolean place(ChunkKey key);

        /** Removes the plugin's ticket from a chunk. Does nothing when its world is not loaded, since the ticket went with the world. */
        void remove(ChunkKey key);
    }

    /** A chunk that is held: the owners that hold it, and whether its ticket is on the chunk. */
    private static final class Entry {
        private final Set<String> owners = new HashSet<>();
        private boolean ticketed;
    }

    private final Port port;
    private final Map<ChunkKey, Entry> chunks = new HashMap<>();
    private final Map<String, Set<ChunkKey>> owned = new HashMap<>();

    /**
     * A book over a port. The server's book is the one {@link #of(Plugin)} makes; a port of the caller makes a book for a test.
     *
     * @param port places and removes the tickets
     */
    public ChunkTickets(Port port) {
        this.port = port;
    }

    /**
     * The book of a plugin, with the server's tickets. Every holder of the plugin's tickets asks for the same book, so a chunk held by two
     * holders has one ticket.
     *
     * @param plugin the plugin that owns the tickets
     * @return the book of the plugin
     */
    public static ChunkTickets of(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return BOOKS.computeIfAbsent(plugin, ChunkTickets::forServer);
    }

    private static ChunkTickets forServer(Plugin plugin) {
        return new ChunkTickets(new Port() {
            @Override
            public boolean place(ChunkKey key) {
                World world = Bukkit.getWorld(key.world());
                if (world == null) {
                    return false;
                }
                if (!world.getPluginChunkTickets(key.x(), key.z()).contains(plugin)) {
                    world.addPluginChunkTicket(key.x(), key.z(), plugin);
                }
                return true;
            }

            @Override
            public void remove(ChunkKey key) {
                World world = Bukkit.getWorld(key.world());
                if (world != null) {
                    world.removePluginChunkTicket(key.x(), key.z(), plugin);
                }
            }
        });
    }

    /**
     * Holds chunks under an owner. A chunk that no owner held gets its ticket, and a chunk the owner already holds without a ticket is placed
     * again. A chunk another owner holds keeps its ticket and is not placed a second time.
     *
     * @param owner  the owner, such as the device keeper or a shot; a name that no other owner has at the same time
     * @param wanted the chunks to hold
     * @return the chunks the owner holds that have no ticket, for instance because their world is not loaded
     */
    public Set<ChunkKey> hold(String owner, Collection<ChunkKey> wanted) {
        Objects.requireNonNull(owner, "owner");
        Set<ChunkKey> own = owned.computeIfAbsent(owner, id -> new HashSet<>());
        for (ChunkKey key : wanted) {
            own.add(key);
            Entry entry = chunks.computeIfAbsent(key, k -> new Entry());
            entry.owners.add(owner);
            if (!entry.ticketed) {
                entry.ticketed = port.place(key);
            }
        }
        return unticketed(own);
    }

    /**
     * Makes an owner hold exactly the chunks given: the chunks it holds that are not given are dropped, and the given ones are held.
     *
     * @param owner  the owner
     * @param wanted the chunks the owner holds from now on
     * @return the chunks the owner holds that have no ticket, as {@link #hold} reports them
     */
    public Set<ChunkKey> setOwned(String owner, Set<ChunkKey> wanted) {
        Objects.requireNonNull(owner, "owner");
        Set<ChunkKey> own = owned.get(owner);
        if (own != null) {
            for (ChunkKey key : List.copyOf(own)) {
                if (!wanted.contains(key)) {
                    own.remove(key);
                    unhold(owner, key);
                }
            }
            if (own.isEmpty()) {
                owned.remove(owner);
            }
        }
        return hold(owner, wanted);
    }

    /**
     * Drops every chunk an owner holds. A chunk that no other owner holds loses its ticket. Releasing an owner that holds nothing does nothing,
     * so a second release is harmless.
     *
     * @param owner the owner to release; null does nothing
     */
    public void release(String owner) {
        if (owner == null) {
            return;
        }
        Set<ChunkKey> own = owned.remove(owner);
        if (own == null) {
            return;
        }
        for (ChunkKey key : own) {
            unhold(owner, key);
        }
    }

    /** Drops every owner, and removes every ticket the plugin holds. Called when the plugin stops. */
    public void releaseAll() {
        for (String owner : List.copyOf(owned.keySet())) {
            release(owner);
        }
    }

    /**
     * Places again the tickets of one owner's chunks that are missing, for example after a world that was unloaded is loaded again.
     *
     * @param owner the owner; an owner that holds nothing does nothing
     */
    public void ensure(String owner) {
        Set<ChunkKey> own = owned.get(owner);
        if (own == null) {
            return;
        }
        for (ChunkKey key : own) {
            place(key, chunks.get(key));
        }
    }

    /** Places again the tickets of every held chunk that are missing. */
    public void ensureAll() {
        chunks.forEach(this::place);
    }

    /** The chunks an owner holds, with or without a ticket. */
    public Set<ChunkKey> chunksOf(String owner) {
        Set<ChunkKey> own = owned.get(owner);
        return own == null ? Set.of() : Set.copyOf(own);
    }

    /** The number of distinct chunks held by any owner. */
    public int heldChunks() {
        return chunks.size();
    }

    /**
     * True when some owner holds the chunk, with or without its ticket on it. A held chunk loads again by itself, once its world is loaded and
     * its ticket is placed.
     */
    public boolean isHeld(ChunkKey key) {
        return chunks.containsKey(key);
    }

    /** Drops one owner's hold on a chunk, and removes the ticket when no owner holds the chunk any more. */
    private void unhold(String owner, ChunkKey key) {
        Entry entry = chunks.get(key);
        if (entry == null) {
            return;
        }
        entry.owners.remove(owner);
        if (entry.owners.isEmpty()) {
            chunks.remove(key);
            if (entry.ticketed) {
                port.remove(key);
            }
        }
    }

    private void place(ChunkKey key, Entry entry) {
        entry.ticketed = port.place(key);
    }

    private Set<ChunkKey> unticketed(Set<ChunkKey> own) {
        Set<ChunkKey> missing = new HashSet<>();
        for (ChunkKey key : own) {
            Entry entry = chunks.get(key);
            if (entry != null && !entry.ticketed) {
                missing.add(key);
            }
        }
        return Set.copyOf(missing);
    }
}
