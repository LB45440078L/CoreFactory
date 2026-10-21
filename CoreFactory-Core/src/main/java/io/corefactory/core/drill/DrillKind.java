package io.corefactory.core.drill;

import io.corefactory.core.process.ItemRef;

import java.util.Optional;

/**
 * The two kinds of drill. A petroleum drill pumps petroleum from crude deposits and runs on charcoal or on solar power.
 * An ore drill mines the special ores and runs on petroleum only.
 */
public enum DrillKind {

    /** Pumps petroleum from deposits. A deposit is a well: it is never removed. */
    PETROLEUM("petroleum", "Petroleum drill", "charcoal", ItemRef.material("CHARCOAL"), 8, false),

    /** Mines special ores and coal. Each cycle removes one ore block from the ground; a coal block yields charcoal. */
    ORE("ore", "Ore drill", "petroleum", ItemRef.item("petroleum"), 2, true);

    private final String id;
    private final String label;
    private final String fuelName;
    private final ItemRef fuel;
    private final int burnEvery;
    private final boolean removesTarget;

    DrillKind(String id, String label, String fuelName, ItemRef fuel, int burnEvery, boolean removesTarget) {
        this.id = id;
        this.label = label;
        this.fuelName = fuelName;
        this.fuel = fuel;
        this.burnEvery = burnEvery;
        this.removesTarget = removesTarget;
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

    /** The one item the fuel slot takes. */
    public ItemRef fuel() {
        return fuel;
    }

    /** How many completed cycles one fuel item lasts. */
    public int burnEvery() {
        return burnEvery;
    }

    /** True when a cycle takes its target out of the world. Only the ore drill does. */
    public boolean removesTarget() {
        return removesTarget;
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
