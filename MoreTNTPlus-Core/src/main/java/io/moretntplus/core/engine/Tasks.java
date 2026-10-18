package io.moretntplus.core.engine;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;

import java.util.function.IntConsumer;

/**
 * Small scheduling helpers over the Bukkit scheduler. All callbacks run on the main thread.
 */
public final class Tasks {

    private Tasks() {
    }

    /**
     * Calls {@code step} with 0, 1, ... {@code times - 1}, once every {@code period} ticks, then {@code done}.
     */
    public static BukkitTask repeat(Plugin plugin, long period, int times, IntConsumer step, Runnable done) {
        return new BukkitRunnable() {
            private int index;

            @Override
            public void run() {
                if (index >= times) {
                    cancel();
                    done.run();
                    return;
                }
                step.accept(index++);
            }
        }.runTaskTimer(plugin, 0L, Math.max(1L, period));
    }

    /** Runs {@code task} on the main thread after {@code delayTicks} ticks. */
    public static void later(Plugin plugin, long delayTicks, Runnable task) {
        if (delayTicks <= 0) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
}
