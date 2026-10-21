package io.corefactory.spigot.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.bukkit.Material;

import java.util.List;
import java.util.Set;

/**
 * The firebox indicator of a roasting kiln. It is lit while the kiln burns fuel: it advanced a roast this tick and its fuel
 * slots hold fuel. Pure over the engine's step and the slots, so the rule is tested without a server.
 */
public final class Firebox {

    private Firebox() {
    }

    /**
     * True when the kiln burns fuel.
     *
     * @param roastAdvanced true when the engine advanced a roast this tick
     * @param fuelItems     how many fuel items the fuel slots hold
     */
    public static boolean lit(boolean roastAdvanced, int fuelItems) {
        return roastAdvanced && fuelItems > 0;
    }

    /** How many fuel items the fuel slots hold, counting only the items the machine's fuel accepts. */
    public static int fuelItems(List<Stack> fuel, Set<ItemRef> fuels) {
        int total = 0;
        for (Stack slot : fuel) {
            if (slot.ref() != null && slot.count() > 0 && fuels.contains(slot.ref())) {
                total += slot.count();
            }
        }
        return total;
    }

    /** The indicator's material: blaze powder while lit, gunpowder while unlit. */
    public static Material material(boolean lit) {
        return lit ? Material.BLAZE_POWDER : Material.GUNPOWDER;
    }
}
