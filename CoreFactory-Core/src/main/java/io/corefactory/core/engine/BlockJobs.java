package io.corefactory.core.engine;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.BlockEditor;
import io.corefactory.api.BlockGenerator;
import io.corefactory.core.config.CoreSettings;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;

/**
 * The staged block pipeline. Small change sets are written at once. Larger ones are written in slices of
 * {@code blocks-per-tick} across as many ticks as they need, so one big blast never stalls the server.
 */
public final class BlockJobs {

    /** Called before each write, so the core can unregister placed TNT that is about to be overwritten. */
    @FunctionalInterface
    public interface OverwriteHook {
        void beforeWrite(World world, int x, int y, int z);
    }

    private interface Job {
        /** Performs at most {@code budget} writes and returns how many it did. */
        int step(int budget);

        boolean finished();

        void complete();
    }

    private final Plugin plugin;
    private final BlockEditor editor;
    private CoreSettings settings;
    private final OverwriteHook hook;
    private final Deque<Job> jobs = new ArrayDeque<>();
    private BukkitTask ticker;

    public BlockJobs(Plugin plugin, BlockEditor editor, CoreSettings settings, OverwriteHook hook) {
        this.plugin = plugin;
        this.editor = editor;
        this.settings = settings;
        this.hook = hook;
    }

    /** Applies new budgets. Unfinished jobs carry on with the new numbers. */
    public void reconfigure(CoreSettings settings) {
        this.settings = settings;
    }

    public void submit(World world, BlockChangeSet changes, Runnable onComplete) {
        if (changes.size() <= Math.min(settings.instantThreshold(), editor.preferredBatch())) {
            write(world, changes, 0, changes.size());
            onComplete.run();
            return;
        }
        jobs.add(new SetJob(world, changes, onComplete));
        ensureTicking();
    }

    public void submit(World world, BlockGenerator generator, Runnable onComplete) {
        jobs.add(new GeneratorJob(world, generator, onComplete));
        ensureTicking();
    }

    /** Number of jobs still running. */
    public int pending() {
        return jobs.size();
    }

    /** Stops the ticker and drops unfinished work. Used on disable. */
    public void shutdown() {
        jobs.clear();
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    private void ensureTicking() {
        if (ticker == null) {
            ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    private void tick() {
        int budget = Math.min(settings.blocksPerTick(), editor.preferredBatch());
        while (budget > 0 && !jobs.isEmpty()) {
            Job job = jobs.peek();
            try {
                budget -= job.step(budget);
            } catch (RuntimeException e) {
                // A failing job is dropped so it cannot block every job queued after it.
                jobs.poll();
                plugin.getLogger().log(Level.SEVERE, "A block job failed and was abandoned.", e);
                continue;
            }
            if (!job.finished()) {
                break;
            }
            jobs.poll();
            job.complete();
        }
        if (jobs.isEmpty() && ticker != null) {
            ticker.cancel();
            ticker = null;
        }
    }

    private void write(World world, BlockChangeSet changes, int from, int to) {
        for (int i = from; i < to; i++) {
            hook.beforeWrite(world, changes.x(i), changes.y(i), changes.z(i));
        }
        editor.apply(world, changes, from, to);
    }

    private final class SetJob implements Job {
        private final World world;
        private final BlockChangeSet changes;
        private final Runnable onComplete;
        private int offset;

        SetJob(World world, BlockChangeSet changes, Runnable onComplete) {
            this.world = world;
            this.changes = changes;
            this.onComplete = onComplete;
        }

        @Override
        public int step(int budget) {
            int count = Math.min(budget, changes.size() - offset);
            if (count <= 0 || world == null) {
                offset = changes.size();
                return 0;
            }
            write(world, changes, offset, offset + count);
            offset += count;
            return count;
        }

        @Override
        public boolean finished() {
            return offset >= changes.size() || world == null;
        }

        @Override
        public void complete() {
            onComplete.run();
        }
    }

    private final class GeneratorJob implements Job {
        private final World world;
        private final BlockGenerator generator;
        private final Runnable onComplete;
        private final BlockChangeSet buffer = new BlockChangeSet(1024);
        private boolean more = true;

        GeneratorJob(World world, BlockGenerator generator, Runnable onComplete) {
            this.world = world;
            this.generator = generator;
            this.onComplete = onComplete;
        }

        @Override
        public int step(int budget) {
            if (!more || world == null) {
                more = false;
                return 0;
            }
            buffer.clear();
            more = generator.next(buffer, budget);
            write(world, buffer, 0, buffer.size());
            return buffer.size();
        }

        @Override
        public boolean finished() {
            return !more || world == null;
        }

        @Override
        public void complete() {
            onComplete.run();
        }
    }
}
