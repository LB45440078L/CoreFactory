package io.corefactory.core.drill;

import io.corefactory.core.geology.Placement;
import io.corefactory.core.process.ItemRef;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * The drill rules that need no server: the power of a drill, its rate, which registered block it works on, and where
 * a drill may stand. Pure, so each rule is tested with plain numbers.
 */
public final class DrillRules {

    /** The strongest redstone or daylight signal, which gives full efficiency. A redstone block emits it. */
    public static final int FULL_SIGNAL = 15;

    /** The efficiency of a petroleum drill that runs on charcoal. */
    public static final double CHARCOAL_EFFICIENCY = 0.35;

    /** Ticks in a second of the server clock. */
    public static final int TICKS_PER_SECOND = 20;

    /** Ticks in a minute of the server clock. */
    public static final int TICKS_PER_MINUTE = 60 * TICKS_PER_SECOND;

    /** The ore id of a crude petroleum deposit in ores.yml. */
    public static final String DEPOSIT = "crude_petroleum_deposit";

    /** The ore id of a coal ore in ores.yml. A drill that mines it yields charcoal, one item per block. */
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

    /** True when a petroleum drill burns charcoal this cycle: only when charcoal is the source that powers it. */
    public static boolean burnsCharcoal(DrillPower power) {
        return power.source() == PowerSource.CHARCOAL;
    }

    /**
     * The items a drill makes per second: the efficiency times the ticks in a second, over the ticks of one cycle.
     *
     * @param efficiency the drill's efficiency, 1 at full power
     * @param cycleTicks the ticks one cycle takes at full efficiency
     * @return the rate, or zero when the efficiency is not positive or the cycle is not valid
     */
    public static double itemsPerSecond(double efficiency, int cycleTicks) {
        if (efficiency <= 0 || cycleTicks <= 0) {
            return 0.0;
        }
        return efficiency * TICKS_PER_SECOND / cycleTicks;
    }

    /**
     * The items a drill makes per minute: the efficiency times the ticks in a minute, over the ticks of one cycle.
     *
     * @param efficiency the drill's efficiency, 1 at full power
     * @param cycleTicks the ticks one cycle takes at full efficiency
     * @return the rate, or zero when the efficiency is not positive or the cycle is not valid
     */
    public static double itemsPerMinute(double efficiency, int cycleTicks) {
        if (efficiency <= 0 || cycleTicks <= 0) {
            return 0.0;
        }
        return efficiency * TICKS_PER_MINUTE / cycleTicks;
    }

    /**
     * The status of a drill, checked in this order: no power, then no room, then no target in its box. A drill is running
     * only when all three are in order.
     *
     * @param powered       true when its efficiency is positive
     * @param roomLeft      true when its output slots can take another item
     * @param targetInRange true when a deposit or special ore that its kind may take lies in its box
     */
    public static DrillStatus status(boolean powered, boolean roomLeft, boolean targetInRange) {
        return status(powered, roomLeft, targetInRange, false);
    }

    /**
     * The status of a drill, as {@link #status(boolean, boolean, boolean)}, with the history that tells a drill with nothing
     * to do apart: {@link DrillStatus#DONE_IN_RANGE} once it had targets (it mined or saw some), and {@link DrillStatus#NO_TARGET}
     * when its box never held one.
     *
     * @param hasHistory true when the box held a block of the drill's kind, selected or not, or the drill has mined before
     */
    public static DrillStatus status(boolean powered, boolean roomLeft, boolean targetInRange, boolean hasHistory) {
        if (!powered) {
            return DrillStatus.NO_POWER;
        }
        if (!roomLeft) {
            return DrillStatus.FULL;
        }
        if (targetInRange) {
            return DrillStatus.RUNNING;
        }
        return hasHistory ? DrillStatus.DONE_IN_RANGE : DrillStatus.NO_TARGET;
    }

    /**
     * Which registered blocks a drill of this kind may work on. A petroleum drill takes only deposits; an ore drill takes
     * every ore except a deposit. A block with no ore index (negative) is never taken.
     *
     * @param kind         the drill's kind
     * @param depositIndex the ore index of the deposit, or -1 when there is none
     */
    public static IntPredicate targets(DrillKind kind, int depositIndex) {
        if (kind.removesTarget()) {
            return index -> index >= 0 && index != depositIndex;
        }
        return index -> index >= 0 && index == depositIndex;
    }

    /**
     * The registered block a drill works on next: the nearest one inside its region whose ore the predicate takes. The
     * distance is measured from the drill's block, in three dimensions. Ties go to the lower y, then x, then z.
     *
     * @param candidates the registered blocks to choose from, in any order
     * @param box        the drill's region; a block outside it is never chosen
     * @param x          the drill's x
     * @param y          the drill's y
     * @param z          the drill's z
     * @param targets    which ore indices the drill may take
     * @return the chosen block, or empty when none qualifies
     */
    public static Optional<Placement> nearest(Collection<Placement> candidates, DrillBox box, int x, int y, int z,
                                              IntPredicate targets) {
        Comparator<Placement> order = Comparator.<Placement>comparingLong(p -> distanceSquared(p, x, y, z))
                .thenComparingInt(Placement::y)
                .thenComparingInt(Placement::x)
                .thenComparingInt(Placement::z);
        return candidates.stream()
                .filter(box::contains)
                .filter(p -> targets.test(p.oreIndex()))
                .min(order);
    }

    /**
     * The item a drill yields from one mined ore block: vanilla charcoal for coal, otherwise the ore's drop component. Coal
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

    private static long distanceSquared(Placement p, int x, int y, int z) {
        long dx = p.x() - x;
        long dy = p.y() - y;
        long dz = p.z() - z;
        return dx * dx + dy * dy + dz * dz;
    }
}
