package io.corefactory.paper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * The timing of the plugin's periodic work, with no Bukkit types, so that it can be tested without a server. Each action has a
 * period in ticks and a first run. An action runs on each tick its period comes round, and the actions due on one tick run in
 * the order they were added. {@link TickScheduler} calls {@link #tick()} once per server tick.
 *
 * <p>An action that throws is reported to the failure handler, and the actions after it still run on that tick. An action added
 * during a tick first runs on a later tick, and an action removed during a tick does not run again.
 */
public final class TickPlan {

    private final BiConsumer<String, Throwable> failures;
    private final List<Action> actions = new ArrayList<>();
    private long tick;
    private boolean pruneNeeded;

    /**
     * @param failures receives the name of an action and the error it threw, on the tick the error happened
     */
    public TickPlan(BiConsumer<String, Throwable> failures) {
        this.failures = Objects.requireNonNull(failures, "failures");
    }

    /**
     * Adds an action. Its first run is {@code delayTicks} ticks after the current tick, and never on the current tick, so a delay of
     * 0 means the next tick. After that it runs every {@code periodTicks} ticks.
     *
     * @param name        the name of the action, unique among the actions not yet removed
     * @param delayTicks  the ticks before the first run, at least 0
     * @param periodTicks the ticks between two runs, at least 1
     * @param action      the work to run
     * @throws IllegalArgumentException when the period is below 1, the delay is negative, or the name is already taken
     */
    public void add(String name, int delayTicks, int periodTicks, Runnable action) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
        if (periodTicks < 1) {
            throw new IllegalArgumentException("The period of '" + name + "' must be at least 1 tick, not " + periodTicks);
        }
        if (delayTicks < 0) {
            throw new IllegalArgumentException("The delay of '" + name + "' cannot be negative: " + delayTicks);
        }
        if (find(name) != null) {
            throw new IllegalArgumentException("An action named '" + name + "' is already registered");
        }
        actions.add(new Action(name, tick + Math.max(1, delayTicks), periodTicks, action));
    }

    /**
     * Removes an action, so it does not run again.
     *
     * @return false when no action of that name is registered
     */
    public boolean remove(String name) {
        Action action = find(name);
        if (action == null) {
            return false;
        }
        action.removed = true;
        pruneNeeded = true;
        return true;
    }

    /** Whether an action of that name is registered. */
    public boolean has(String name) {
        return find(name) != null;
    }

    /** The tick last run, counted from 0 when the plan was made. */
    public long currentTick() {
        return tick;
    }

    /** Advances one tick and runs every action due on it, in the order they were added. */
    public void tick() {
        tick++;
        int count = actions.size();
        for (int i = 0; i < count; i++) {
            Action action = actions.get(i);
            if (action.removed || !action.isDueOn(tick)) {
                continue;
            }
            try {
                action.work.run();
            } catch (Throwable error) {
                // Caught as the Bukkit scheduler catches a task's error, so one action cannot stop the others.
                failures.accept(action.name, error);
            }
        }
        if (pruneNeeded) {
            actions.removeIf(action -> action.removed);
            pruneNeeded = false;
        }
    }

    private Action find(String name) {
        for (Action action : actions) {
            if (!action.removed && action.name.equals(name)) {
                return action;
            }
        }
        return null;
    }

    private static final class Action {
        private final String name;
        private final long firstTick;
        private final int period;
        private final Runnable work;
        private boolean removed;

        Action(String name, long firstTick, int period, Runnable work) {
            this.name = name;
            this.firstTick = firstTick;
            this.period = period;
            this.work = work;
        }

        boolean isDueOn(long tick) {
            return tick >= firstTick && (tick - firstTick) % period == 0;
        }
    }
}
