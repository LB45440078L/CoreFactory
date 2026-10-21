package io.corefactory.core.drill;

/** What drives a drill in one cycle. The name is the one players read in the hologram and the info item. */
public enum PowerSource {

    /** No power: the drill cannot advance its cycle. */
    NONE("none"),

    /** A redstone block, or redstone wire or a lever's signal, beside the drill. */
    REDSTONE("redstone"),

    /** A solar panel (a daylight detector) beside the drill. */
    SOLAR("solar"),

    /** Charcoal in the fuel slot of a petroleum drill. */
    CHARCOAL("charcoal"),

    /** Petroleum in the fuel slot of an ore drill. */
    PETROLEUM("petroleum"),

    /** The energy network: the satisfaction a pylon's line gives the drill. */
    GRID("grid");

    private final String label;

    PowerSource(String label) {
        this.label = label;
    }

    /** The name shown to players, such as {@code redstone}. */
    public String label() {
        return label;
    }
}
