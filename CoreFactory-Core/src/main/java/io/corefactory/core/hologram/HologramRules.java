package io.corefactory.core.hologram;

import io.corefactory.core.process.MachineState;
import io.corefactory.core.process.ProcessEngine;

/** Decides which machines show a hologram. Pure, so it is tested with plain state. */
public final class HologramRules {

    private HologramRules() {
    }

    /**
     * True when a machine should show a hologram: a process is running in it, or some process of it can start now on its
     * inputs and fuel. False when every process has finished and the machine is idle.
     */
    public static boolean shouldShow(ProcessEngine engine, MachineState state) {
        return state.processId() != null || engine.select(state).isPresent();
    }
}
