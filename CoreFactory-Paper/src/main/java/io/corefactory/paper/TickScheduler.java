package io.corefactory.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The plugin's one repeating task. Each server tick it runs the {@link TickPlan}, so the periodic work of every feature is an
 * action registered here instead of a task of its own. Actions are registered and removed on the main thread.
 *
 * <p>An action that throws is logged, at most once a minute for each action, and the other actions still run.
 */
public final class TickScheduler {

    /** The least time, in milliseconds, between two log entries for the same action. */
    static final long LOG_INTERVAL_MILLIS = 60_000L;

    private final TickPlan plan;
    private final Logger log;
    private final Map<String, Long> lastLogged = new HashMap<>();
    private BukkitTask task;

    /**
     * @param log receives the errors of the actions
     */
    public TickScheduler(Logger log) {
        this.log = log;
        this.plan = new TickPlan(this::failed);
    }

    /**
     * Registers an action that first runs after {@code periodTicks} ticks, and then every {@code periodTicks} ticks.
     *
     * @param name        the name of the action, unique among the actions not yet removed
     * @param periodTicks the ticks between two runs, at least 1
     * @param action      the work to run
     */
    public void register(String name, int periodTicks, Runnable action) {
        plan.add(name, periodTicks, periodTicks, action);
    }

    /**
     * Registers an action that first runs after {@code delayTicks} ticks, and then every {@code periodTicks} ticks. A delay of 0 runs
     * it on the next tick.
     *
     * @param name        the name of the action, unique among the actions not yet removed
     * @param delayTicks  the ticks before the first run, at least 0
     * @param periodTicks the ticks between two runs, at least 1
     * @param action      the work to run
     */
    public void register(String name, int delayTicks, int periodTicks, Runnable action) {
        plan.add(name, delayTicks, periodTicks, action);
    }

    /** Removes an action, so it does not run again. Does nothing when no action has that name. */
    public void remove(String name) {
        plan.remove(name);
        lastLogged.remove(name);
    }

    /** Starts the repeating task that runs the actions each server tick. Does nothing when it is already running. */
    public void start(Plugin plugin) {
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, plan::tick, 1L, 1L);
        }
    }

    /** Cancels the repeating task. The actions stay registered. Called when the plugin is disabled. */
    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void failed(String name, Throwable error) {
        long now = System.currentTimeMillis();
        Long last = lastLogged.get(name);
        if (last != null && now - last < LOG_INTERVAL_MILLIS) {
            return;
        }
        lastLogged.put(name, now);
        log.log(Level.SEVERE, "The tick action '" + name + "' failed. The other actions still run.", error);
    }
}
