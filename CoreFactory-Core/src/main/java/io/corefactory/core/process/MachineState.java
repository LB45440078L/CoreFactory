package io.corefactory.core.process;

import java.util.List;

/**
 * The running state of one machine: the slots it reads and writes, the process it is in, how many ticks that process has
 * run, and the products rolled for a pass that is waiting for room in its output slots. Progress and the process id are
 * what gets saved; the rolled products are not, so a pass that is waiting after a restart rolls again.
 */
public final class MachineState {

    private final String machineId;
    private final List<Stack> inputs;
    private final List<Stack> outputs;
    private final List<Stack> fuel;
    private String processId;
    private int progress;
    private List<Portion> pending;

    /**
     * @param machineId the machine this state belongs to
     * @param inputs    the input slots
     * @param outputs   the output slots
     * @param fuel      the fuel slots, empty for a machine that burns none
     */
    public MachineState(String machineId, List<Stack> inputs, List<Stack> outputs, List<Stack> fuel) {
        this.machineId = machineId;
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        this.fuel = List.copyOf(fuel);
    }

    public String machineId() {
        return machineId;
    }

    public List<Stack> inputs() {
        return inputs;
    }

    public List<Stack> outputs() {
        return outputs;
    }

    public List<Stack> fuel() {
        return fuel;
    }

    /** The process being run, or null when the machine is idle. */
    public String processId() {
        return processId;
    }

    /** The ticks the current pass has run so far. */
    public int progress() {
        return progress;
    }

    /** The products rolled for the current pass, or null while none has been rolled. */
    public List<Portion> pending() {
        return pending;
    }

    /** Puts the machine into a process, from the start of a pass. */
    public void start(String process) {
        this.processId = process;
        this.progress = 0;
        this.pending = null;
    }

    /** Restores a saved process and progress. The rolled products are not restored. */
    public void restore(String process, int ticksRun) {
        this.processId = process;
        this.progress = Math.max(0, ticksRun);
        this.pending = null;
    }

    /** Leaves the current process. The machine is idle until a process can run again. */
    public void stop() {
        this.processId = null;
        this.progress = 0;
        this.pending = null;
    }

    void advance() {
        progress++;
    }

    void setPending(List<Portion> products) {
        this.pending = List.copyOf(products);
    }
}
