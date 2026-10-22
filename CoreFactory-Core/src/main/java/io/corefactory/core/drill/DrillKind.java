package io.corefactory.core.drill;

import io.corefactory.core.process.ItemRef;

import java.util.Optional;

/**
 * The two kinds of drill. A petroleum drill pumps petroleum from crude deposits and runs on charcoal or on solar power.
 * An ore drill mines the other deposits and runs on petroleum only.
 */
public enum DrillKind {

    /** Pumps petroleum from crude petroleum deposits. */
    PETROLEUM("petroleum", "Petroleum drill", "charcoal", ItemRef.material("CHARCOAL"), 8),

    /** Mines the solid ores and coal of the deposits. A coal deposit yields charcoal. */
    ORE("ore", "Ore drill", "petroleum", ItemRef.item("petroleum"), 2);

    private final String id;
    private final String label;
    private final String fuelName;
    private final ItemRef fuel;
    private final int burnEvery;

    DrillKind(String id, String label, String fuelName, ItemRef fuel, int burnEvery) {
        this.id = id;
        this.label = label;
        this.fuelName = fuelName;
        this.fuel = fuel;
        this.burnEvery = burnEvery;
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

    /** How many completed fuel cycles one fuel item lasts. */
    public int burnEvery() {
        return burnEvery;
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
