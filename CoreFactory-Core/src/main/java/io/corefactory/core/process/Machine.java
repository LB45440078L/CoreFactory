package io.corefactory.core.process;

/**
 * A machine kind from processes.yml.
 *
 * @param id          the machine id, the same as its component id
 * @param name        the display name, with colour codes
 * @param host        the vanilla material the machine occupies in the world
 * @param inputSlots  how many input slots it has
 * @param outputSlots how many output slots it has
 * @param fuelSlots   how many fuel slots it has
 * @param powerW     the electric demand while it works, in W; zero means the lab tool default
 */
public record Machine(String id, String name, String host, int inputSlots, int outputSlots, int fuelSlots,
                      int powerW) {

    public Machine {
        if (inputSlots < 0 || outputSlots < 0 || fuelSlots < 0) {
            throw new IllegalArgumentException("Machine " + id + " has a negative slot count.");
        }
        if (powerW < 0) {
            throw new IllegalArgumentException("Machine " + id + " has a negative power demand.");
        }
    }

    /** A machine with no power of its own, so it uses the lab tool default demand. */
    public Machine(String id, String name, String host, int inputSlots, int outputSlots, int fuelSlots) {
        this(id, name, host, inputSlots, outputSlots, fuelSlots, 0);
    }
}
