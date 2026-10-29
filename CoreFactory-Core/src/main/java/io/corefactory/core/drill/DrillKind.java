package io.corefactory.core.drill;

import io.corefactory.core.oil.Fluid;

import java.util.Optional;

/**
 * The two kinds of drill, and both are powered by the energy network. A petroleum drill pumps petroleum from crude deposits into the
 * oil network, which is its output. An ore drill mines the other deposits and takes nothing from the oil network.
 */
public enum DrillKind {

    /** Pumps petroleum from crude petroleum deposits into the oil network. */
    PETROLEUM("petroleum", "Petroleum drill", Fluid.PETROLEUM),

    /** Mines the solid ores and coal of the deposits. A coal deposit yields charcoal. */
    ORE("ore", "Ore drill", null);

    private final String id;
    private final String label;
    private final Fluid pumped;

    DrillKind(String id, String label, Fluid pumped) {
        this.id = id;
        this.label = label;
        this.pumped = pumped;
    }

    /** The word processes.yml uses for this kind, such as {@code petroleum}. */
    public String id() {
        return id;
    }

    /** The name shown in menus and holograms. */
    public String label() {
        return label;
    }

    /**
     * The fluid this kind pumps into the oil network, or empty for a kind that joins no network. Only a petroleum drill joins one, as a
     * source; an ore drill never does.
     */
    public Optional<Fluid> pumps() {
        return Optional.ofNullable(pumped);
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
