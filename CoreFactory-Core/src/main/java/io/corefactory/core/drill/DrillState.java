package io.corefactory.core.drill;

/**
 * The running state of one drill: how far its cycle has run. The drill's barrel holds its items, and the items it mines are counted by
 * its deposit; this holds only the cycle clock, which is what gets saved.
 *
 * <p>A cycle advances by the drill's efficiency each tick the drill runs, and a full cycle is finished at once.
 */
public final class DrillState {

    private final DrillDefinition definition;
    private double progress;

    /** @param definition the drill this state belongs to */
    public DrillState(DrillDefinition definition) {
        this.definition = definition;
    }

    public DrillDefinition definition() {
        return definition;
    }

    /** The ticks of the current cycle that have run, at most the cycle length. */
    public double progress() {
        return progress;
    }

    /** Advances the cycle by the efficiency, up to a full cycle. Nothing happens for a zero efficiency or a full cycle. */
    public void advance(double efficiency) {
        if (efficiency <= 0 || ready()) {
            return;
        }
        progress = Math.min(definition.spec().cycleTicks(), progress + efficiency);
    }

    /** True when the cycle is full and waits to be finished. */
    public boolean ready() {
        return progress >= definition.spec().cycleTicks();
    }

    /** Finishes the cycle: the progress goes back to the start. */
    public void finishCycle() {
        progress = 0;
    }

    /** Restores a saved cycle. A value outside the valid range is clamped. */
    public void restore(double savedProgress) {
        progress = Math.min(definition.spec().cycleTicks(), Math.max(0, savedProgress));
    }
}
