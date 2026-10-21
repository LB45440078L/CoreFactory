package io.corefactory.core.drill;

/** Why a drill is or is not extracting right now. The text is plain, without colour codes. */
public enum DrillStatus {

    /** The drill has power, room in its outputs and a target in its box. */
    RUNNING,

    /** The drill has no power: no redstone, no panel and no charcoal, or for an ore drill no petroleum. */
    NO_POWER,

    /** The output slots have no room for another item. */
    FULL,

    /** The drill has power and room, but no deposit or special ore lies in its box. */
    NO_TARGET,

    /** The drill has power and room, and had targets, but none it may mine is left in its box. It stays until moved. */
    DONE_IN_RANGE;

    /**
     * The status as players read it.
     *
     * @param kind the drill's kind, which names its target
     * @param spec the drill's level, whose depth the target text gives
     */
    public String text(DrillKind kind, DrillSpec spec) {
        return switch (this) {
            case RUNNING -> "Running";
            case NO_POWER -> kind == DrillKind.ORE ? "No petroleum" : "No power";
            case FULL -> "Output full";
            case NO_TARGET -> kind == DrillKind.ORE ? "No special ore in range"
                    : "No deposit in range (depth " + spec.depth() + ")";
            case DONE_IN_RANGE -> "Done in range: move the drill";
        };
    }
}
