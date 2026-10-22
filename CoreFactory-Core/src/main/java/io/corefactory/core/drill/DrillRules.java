package io.corefactory.core.drill;

import io.corefactory.core.process.ItemRef;

import java.util.Collection;
import java.util.Set;

/**
 * The drill rules that need no server: the power of a drill, its status, the item a mined ore gives, and where a drill
 * may stand. Pure, so each rule is tested with plain numbers.
 */
public final class DrillRules {

    /** The strongest redstone or daylight signal, which gives full efficiency. A redstone block emits it. */
    public static final int FULL_SIGNAL = 15;

    /** The efficiency of a petroleum drill that runs on charcoal. */
    public static final double CHARCOAL_EFFICIENCY = 0.35;

    /** The ore id of a crude petroleum deposit in ores.yml. */
    public static final String DEPOSIT = "crude_petroleum_deposit";

    /** The ore id of a coal ore in ores.yml. A drill that mines it yields charcoal, one item per unit. */
    public static final String COAL = "coal_ore";

    /** The product of a mined coal ore: vanilla charcoal. */
    public static final ItemRef CHARCOAL = ItemRef.material("CHARCOAL");

    private static final Set<String> HARD_BASE = Set.of(
            "STONE", "GRANITE", "DIORITE", "ANDESITE", "TUFF", "DEEPSLATE");

    private DrillRules() {
    }

    /**
     * The efficiency a redstone or daylight signal of this power gives: its power over {@link #FULL_SIGNAL}, clamped to 0
     * to 1. A redstone block gives 15, so it runs a drill at full efficiency.
     */
    public static double signalEfficiency(int power) {
        return Math.min(FULL_SIGNAL, Math.max(0, power)) / (double) FULL_SIGNAL;
    }

    /**
     * The power of a petroleum drill: the best of its three sources, and the source that gives it. The sources are each solar
     * panel beside the drill, the redstone around it, and charcoal in the fuel slot. A source that gives nothing never wins,
     * so with every source at zero the drill has no power. A daylight detector also sends its signal to the block beside it,
     * so a panel's power is part of the redstone reading too; a tie therefore names the panel.
     *
     * @param redstone the redstone power the drill block receives from its neighbours, 0 to 15
     * @param panels   the power of each daylight detector beside the drill, 0 to 15 each; empty for none
     * @param charcoal true when the fuel slot holds charcoal
     */
    public static DrillPower petroleumPower(int redstone, Collection<Integer> panels, boolean charcoal) {
        DrillPower best = DrillPower.NONE;
        best = stronger(best, new DrillPower(PowerSource.SOLAR, signalEfficiency(strongest(panels))));
        best = stronger(best, new DrillPower(PowerSource.REDSTONE, signalEfficiency(redstone)));
        best = stronger(best, new DrillPower(PowerSource.CHARCOAL, charcoal ? CHARCOAL_EFFICIENCY : 0.0));
        return best;
    }

    /**
     * The power of a drill with the energy network added: the stronger of its local power and the network's satisfaction.
     * The local source stays when it is at least as strong, so a network never takes a drill off its own power.
     *
     * @param local        the power of the drill's own sources
     * @param satisfaction the network's satisfaction of the drill, 0 to 1
     */
    public static DrillPower withGrid(DrillPower local, double satisfaction) {
        double grid = Math.min(1.0, Math.max(0.0, satisfaction));
        return grid > local.efficiency() ? new DrillPower(PowerSource.GRID, grid) : local;
    }

    /** The power of an ore drill: full power from the petroleum in its fuel slot, none without it. */
    public static DrillPower orePower(boolean petroleum) {
        return petroleum ? new DrillPower(PowerSource.PETROLEUM, 1.0) : DrillPower.NONE;
    }

    /** True when a petroleum drill burns charcoal in its fuel cycle: only when charcoal is the source that powers it. */
    public static boolean burnsCharcoal(DrillPower power) {
        return power.source() == PowerSource.CHARCOAL;
    }

    /**
     * The status of a drill, checked in this order: no power, then no room, then no deposit in its square, then an empty
     * deposit. A drill is running only when all four are in order.
     *
     * @param powered      true when its efficiency is positive
     * @param roomLeft     true when its output slots can take another item
     * @param targetInRange true when a deposit its kind and selection allow touches its square
     * @param working      true when the drill has a deposit with something left to work on
     */
    public static DrillStatus status(boolean powered, boolean roomLeft, boolean targetInRange, boolean working) {
        if (!powered) {
            return DrillStatus.NO_POWER;
        }
        if (!roomLeft) {
            return DrillStatus.FULL;
        }
        if (!targetInRange) {
            return DrillStatus.NO_TARGET;
        }
        return working ? DrillStatus.RUNNING : DrillStatus.EXHAUSTED;
    }

    /**
     * The item a drill yields from one mined ore unit: vanilla charcoal for coal, otherwise the ore's drop component. Coal
     * follows the same power and target rules as every other ore of an ore drill, at any level.
     *
     * @param oreId the ore's id in ores.yml
     * @param drop  the ore's drop component id
     */
    public static ItemRef mined(String oreId, String drop) {
        return COAL.equals(oreId) ? CHARCOAL : ItemRef.item(drop);
    }

    /**
     * True when a block of this material is hard base stone, so a drill may stand on it: stone, granite, diorite,
     * andesite, tuff or deepslate, the same blocks as the overworld base stone tag.
     */
    public static boolean hardBase(String material) {
        return material != null && HARD_BASE.contains(material);
    }

    private static DrillPower stronger(DrillPower current, DrillPower candidate) {
        return candidate.efficiency() > current.efficiency() ? candidate : current;
    }

    private static int strongest(Collection<Integer> powers) {
        int best = 0;
        for (int power : powers) {
            best = Math.max(best, power);
        }
        return best;
    }
}
