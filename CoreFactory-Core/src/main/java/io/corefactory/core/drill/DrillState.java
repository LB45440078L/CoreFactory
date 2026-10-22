package io.corefactory.core.drill;

/**
 * The running state of one drill: how far its fuel cycle has run, and how many completed cycles it has burned fuel for. The
 * drill's barrel holds its items, and the items it mines are counted by its deposit; this holds only the fuel clock, which
 * is what gets saved.
 *
 * <p>A cycle advances by the drill's efficiency each tick the drill runs, and a full cycle is finished at once. Fuel is
 * counted per finished cycle that burned fuel.
 */
public final class DrillState {

    private final DrillDefinition definition;
    private double progress;
    private int fuelCycles;

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

    /** The finished cycles since the last fuel item was burned. */
    public int fuelCycles() {
        return fuelCycles;
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

    /**
     * Finishes the cycle: the progress goes back to the start, and a cycle that burned fuel is counted.
     *
     * @param burnedFuel true when this cycle is one that uses fuel
     */
    public void finishCycle(boolean burnedFuel) {
        progress = 0;
        if (burnedFuel) {
            fuelCycles++;
        }
    }

    /** True when the fuel counter has reached the kind's burn interval, so one fuel item is used now. */
    public boolean fuelDue() {
        return fuelCycles >= definition.kind().burnEvery();
    }

    /** Resets the fuel counter after one fuel item was used. */
    public void fuelPaid() {
        fuelCycles = 0;
    }

    /** Restores a saved cycle. Values outside the valid range are clamped. */
    public void restore(double savedProgress, int savedFuelCycles) {
        progress = Math.min(definition.spec().cycleTicks(), Math.max(0, savedProgress));
        fuelCycles = Math.max(0, savedFuelCycles);
    }
}
