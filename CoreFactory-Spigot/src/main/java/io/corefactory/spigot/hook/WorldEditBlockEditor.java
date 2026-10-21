package io.corefactory.spigot.hook;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.BlockEditor;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes block batches through WorldEdit or FastAsyncWorldEdit. Both implement the same
 * {@code com.sk89q.worldedit} API, so one implementation serves either plugin.
 *
 * <p>Each call opens one edit session, writes the slice in fast mode, and flushes once.
 *
 * <p>If the editing backend cannot start or fails, for example a FastAsyncWorldEdit build that does not match the
 * server version, the editor logs one clear message and switches to plain Bukkit writes for the rest of the session.
 * Those writes are slower, so they are sent in small batches per tick. Blasts keep working instead of throwing from
 * inside the explosion event.
 */
public final class WorldEditBlockEditor implements BlockEditor {

    /** Writes per tick while the Bukkit fallback is active. Each write relights and updates a chunk. */
    private static final int FALLBACK_BATCH = 1500;

    private final Logger log;
    private volatile boolean fallback;
    private boolean reported;

    public WorldEditBlockEditor(Logger log) {
        this.log = log;
    }

    /**
     * Opens and closes an empty edit session on {@code world}, to find out at enable time whether the editing backend
     * works. A broken backend is reported now, rather than on the first explosion.
     *
     * @return {@code true} when WorldEdit or FAWE can edit blocks
     */
    public boolean verify(World world) {
        try {
            session(world).close();
            log.info("Block editing: WorldEdit / FastAsyncWorldEdit is working.");
            return true;
        } catch (LinkageError | RuntimeException e) {
            fail(e);
            return false;
        }
    }

    @Override
    public int preferredBatch() {
        return fallback ? FALLBACK_BATCH : Integer.MAX_VALUE;
    }

    @Override
    @SuppressWarnings("deprecation") // setFastMode: WorldEdit has no builder replacement yet; it is still the fast path.
    public int apply(World world, BlockChangeSet changes, int from, int to) {
        if (to <= from) {
            return 0;
        }
        if (fallback) {
            return bukkit(world, changes, from, to);
        }
        try {
            int written = 0;
            // Closing the session flushes every queued change to the world.
            try (EditSession session = session(world)) {
                session.setFastMode(true);
                for (int i = from; i < to; i++) {
                    BlockState state = BukkitAdapter.adapt(changes.data(i));
                    session.setBlock(BlockVector3.at(changes.x(i), changes.y(i), changes.z(i)), state);
                    written++;
                }
            }
            return written;
        } catch (WorldEditException | LinkageError | RuntimeException e) {
            fail(e);
            return bukkit(world, changes, from, to);
        }
    }

    private static EditSession session(World world) {
        return WorldEdit.getInstance().newEditSessionBuilder()
                .world(BukkitAdapter.adapt(world))
                .maxBlocks(-1)
                .build();
    }

    /** Plain Bukkit writes, without physics, the way the fallback has always behaved. */
    private static int bukkit(World world, BlockChangeSet changes, int from, int to) {
        for (int i = from; i < to; i++) {
            BlockData data = changes.data(i);
            if (data != null) {
                world.getBlockAt(changes.x(i), changes.y(i), changes.z(i)).setBlockData(data, false);
            }
        }
        return to - from;
    }

    private void fail(Throwable cause) {
        fallback = true;
        if (!reported) {
            reported = true;
            log.log(Level.SEVERE, "WorldEdit / FastAsyncWorldEdit could not edit blocks on this server. CoreFactory is "
                    + "switching to slower Bukkit block writes. Install a WorldEdit or FAWE build made for this exact "
                    + "server version, then restart.", cause);
        }
    }
}
