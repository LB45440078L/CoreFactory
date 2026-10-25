package io.corefactory.core.drill;

import io.corefactory.core.oil.Fluid;
import io.corefactory.core.process.ItemRef;

import java.util.Optional;

/**
 * The two kinds of drill. A petroleum drill pumps petroleum from crude deposits into the oil network, and runs on charcoal or on
 * solar power. An ore drill mines the other deposits and runs on petroleum that it draws from the oil network: a fluid, not an item.
 */
public enum DrillKind {

    /** Pumps petroleum from crude petroleum deposits into the oil network. */
    PETROLEUM("petroleum", "Petroleum drill", "charcoal", ItemRef.material("CHARCOAL"), null, 8, 0.0),

    /** Mines the solid ores and coal of the deposits. A coal deposit yields charcoal. */
    ORE("ore", "Ore drill", "petroleum", null, Fluid.PETROLEUM, 0, 0.5);

    private final String id;
    private final String label;
    private final String fuelName;
    private final ItemRef fuelItem;
    private final Fluid fuelFluid;
    private final int burnEvery;
    private final double litresPerCycle;

    DrillKind(String id, String label, String fuelName, ItemRef fuelItem, Fluid fuelFluid, int burnEvery, double litresPerCycle) {
        this.id = id;
        this.label = label;
        this.fuelName = fuelName;
        this.fuelItem = fuelItem;
        this.fuelFluid = fuelFluid;
        this.burnEvery = burnEvery;
        this.litresPerCycle = litresPerCycle;
    }

    /** The word processes.yml uses for this kind, such as {@code petroleum}. */
    public String id() {
        return id;
    }

    /** The name shown in menus and holograms. */
    public String label() {
        return label;
    }

    /** The name of the fuel, as shown to players. */
    public String fuelName() {
        return fuelName;
    }

    /** The one item the fuel slot takes, or empty for a kind whose fuel is a fluid. */
    public Optional<ItemRef> fuel() {
        return Optional.ofNullable(fuelItem);
    }

    /** The fluid the kind draws from the oil network, or empty for a kind that draws none. */
    public Optional<Fluid> fluid() {
        return Optional.ofNullable(fuelFluid);
    }

    /** How many completed cycles one charcoal item lasts. Zero for a kind with no item fuel. */
    public int burnEvery() {
        return burnEvery;
    }

    /** The litres of fluid a drill draws over one full cycle at full power. Zero for a kind that draws none. */
    public double litresPerCycle() {
        return litresPerCycle;
    }

    /**
     * The litres of fluid the drill draws in one tick at full power: its litres per cycle over the cycle's ticks.
     *
     * @param cycleTicks the ticks of one full cycle at the drill's level, positive
     * @return the litres per tick, 0 for a kind that draws none
     */
    public double litresPerTick(int cycleTicks) {
        if (cycleTicks < 1) {
            throw new IllegalArgumentException("a cycle is at least 1 tick, not " + cycleTicks);
        }
        return litresPerCycle / cycleTicks;
    }

    /** The kind with this processes.yml id, or empty when no kind has it. */
    public static Optional<DrillKind> parse(String id) {
        for (DrillKind kind : values()) {
            if (kind.id.equals(id)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
