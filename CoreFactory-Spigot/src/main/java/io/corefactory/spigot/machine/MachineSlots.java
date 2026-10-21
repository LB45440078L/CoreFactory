package io.corefactory.spigot.machine;

import io.corefactory.core.process.Machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The slots of a machine's barrel and the order the layout's slots map onto them. A lab tool lays its barrel out as inputs,
 * then outputs, then fuel; a drill lays it out as fuel, then outputs. Each role is counted from zero within itself, so input 0
 * is the first barrel slot of its role. The rest of the barrel is unused. Pure, so the mapping is tested without a server.
 */
public final class MachineSlots {

    /** The size of a barrel, the block every lab tool and drill stands on. */
    public static final int BARREL_SIZE = 27;

    /** The material every lab tool and drill stands on. Its barrel holds the machine's items, so it has {@link #BARREL_SIZE}. */
    public static final String HOST = "BARREL";

    /** What a barrel slot is for. */
    public enum Role {
        INPUT, OUTPUT, FUEL
    }

    private final int inputs;
    private final int outputs;
    private final int fuel;
    private final boolean fuelFirst;

    /**
     * @param inputs  how many input slots
     * @param outputs how many output slots
     * @param fuel    how many fuel slots
     * @throws IllegalArgumentException when a count is negative or the three roles do not fit in a barrel
     */
    public MachineSlots(int inputs, int outputs, int fuel) {
        this(inputs, outputs, fuel, false);
    }

    /**
     * @param inputs    how many input slots
     * @param outputs   how many output slots
     * @param fuel      how many fuel slots
     * @param fuelFirst true for a drill's order, fuel then outputs; false for a lab tool's, inputs then outputs then fuel
     * @throws IllegalArgumentException when a count is negative or the three roles do not fit in a barrel
     */
    public MachineSlots(int inputs, int outputs, int fuel, boolean fuelFirst) {
        if (inputs < 0 || outputs < 0 || fuel < 0) {
            throw new IllegalArgumentException("A machine cannot have a negative number of slots.");
        }
        if (inputs + outputs + fuel > BARREL_SIZE) {
            throw new IllegalArgumentException("A machine needs " + (inputs + outputs + fuel)
                    + " barrel slots and a barrel has " + BARREL_SIZE + ".");
        }
        this.inputs = inputs;
        this.outputs = outputs;
        this.fuel = fuel;
        this.fuelFirst = fuelFirst;
    }

    /** The slots of a machine kind, as processes.yml gives them. */
    public static MachineSlots of(Machine machine) {
        return new MachineSlots(machine.inputSlots(), machine.outputSlots(), machine.fuelSlots());
    }

    /** The slots of a drill: one fuel slot first, then the given number of outputs. */
    public static MachineSlots drill(int outputs) {
        return new MachineSlots(0, outputs, 1, true);
    }

    /** How many slots the role has. */
    public int count(Role role) {
        return switch (role) {
            case INPUT -> inputs;
            case OUTPUT -> outputs;
            case FUEL -> fuel;
        };
    }

    /** The barrel slot of the given numbered slot of a role. */
    public int barrelIndex(Role role, int index) {
        if (index < 0 || index >= count(role)) {
            throw new IndexOutOfBoundsException(role + " slot " + index + " of " + count(role));
        }
        return start(role) + index;
    }

    /** The barrel slots of a role, in the order of their numbers. */
    public List<Integer> indexes(Role role) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < count(role); i++) {
            indexes.add(barrelIndex(role, i));
        }
        return List.copyOf(indexes);
    }

    /** The role of a barrel slot, or empty for a slot the machine does not use. */
    public Optional<Role> roleAt(int barrelIndex) {
        if (barrelIndex < 0 || barrelIndex >= used()) {
            return Optional.empty();
        }
        for (Role role : Role.values()) {
            if (barrelIndex >= start(role) && barrelIndex < start(role) + count(role)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }

    /** The number of a barrel slot within its role, or -1 for a slot the machine does not use. */
    public int indexAt(int barrelIndex) {
        Optional<Role> role = roleAt(barrelIndex);
        if (role.isEmpty()) {
            return -1;
        }
        return barrelIndex - start(role.get());
    }

    /** How many barrel slots the machine uses. The rest of the barrel is unused. */
    public int used() {
        return inputs + outputs + fuel;
    }

    /** True when a container with this many slots holds every slot the machine uses. */
    public boolean fits(int inventorySize) {
        return used() <= inventorySize;
    }

    /** The first barrel slot of a role. */
    private int start(Role role) {
        if (fuelFirst) {
            return switch (role) {
                case FUEL -> 0;
                case OUTPUT -> fuel;
                case INPUT -> fuel + outputs;
            };
        }
        return switch (role) {
            case INPUT -> 0;
            case OUTPUT -> inputs;
            case FUEL -> inputs + outputs;
        };
    }
}
