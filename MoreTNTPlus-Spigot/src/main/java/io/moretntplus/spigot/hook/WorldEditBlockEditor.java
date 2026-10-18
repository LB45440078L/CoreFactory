package io.moretntplus.spigot.hook;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockState;
import io.moretntplus.api.BlockChangeSet;
import io.moretntplus.api.BlockEditor;
import org.bukkit.World;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes block batches through WorldEdit or FastAsyncWorldEdit. Both implement the same
 * {@code com.sk89q.worldedit} API, so one implementation serves either plugin.
 *
 * <p>Each call opens one edit session, writes the slice in fast mode, and flushes once. Fast mode skips
 * per-block bookkeeping that a plugin does not need, and a single flush at the end batches the chunk updates.
 */
public final class WorldEditBlockEditor implements BlockEditor {

    private final Logger log;
    private boolean reportedFailure;

    public WorldEditBlockEditor(Logger log) {
        this.log = log;
    }

    @Override
    @SuppressWarnings("deprecation") // setFastMode: WorldEdit has no builder replacement yet; it is still the fast path.
    public int apply(World world, BlockChangeSet changes, int from, int to) {
        if (to <= from) {
            return 0;
        }
        int written = 0;
        try {
            com.sk89q.worldedit.world.World target = BukkitAdapter.adapt(world);
            // Closing the session flushes every queued change to the world.
            try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(target)
                    .maxBlocks(-1)
                    .build()) {
                session.setFastMode(true);
                for (int i = from; i < to; i++) {
                    BlockState state = BukkitAdapter.adapt(changes.data(i));
                    session.setBlock(BlockVector3.at(changes.x(i), changes.y(i), changes.z(i)), state);
                    written++;
                }
            }
        } catch (WorldEditException e) {
            if (!reportedFailure) {
                reportedFailure = true;
                log.log(Level.WARNING, "WorldEdit refused a block batch; the rest of this blast was skipped.", e);
            }
        }
        return written;
    }
}
