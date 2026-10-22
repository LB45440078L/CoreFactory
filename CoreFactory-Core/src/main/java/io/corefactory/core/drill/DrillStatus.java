package io.corefactory.core.drill;

/** Why a drill is or is not extracting right now. The text is plain, without colour codes. */
public enum DrillStatus {

    /** The drill has power, room in its outputs and a deposit with something left in its square. */
    RUNNING,

    /** The drill has no power: no redstone, no panel and no charcoal, or for an ore drill no petroleum. */
    NO_POWER,

    /** The output slots have no room for another item. */
    FULL,

    /** The drill has power and room, but no deposit it may mine touches its square. */
    NO_TARGET,

    /** The drill has power and room, and deposits it may mine touch its square, but every one of them is empty. It stays until moved. */
    EXHAUSTED;

    /**
     * The status as players read it.
     *
     * @param kind the drill's kind, which names its power source
     */
    public String text(DrillKind kind) {
        return switch (this) {
            case RUNNING -> "Running";
            case NO_POWER -> kind == DrillKind.ORE ? "No petroleum" : "No power";
            case FULL -> "Output full";
            case NO_TARGET -> "No deposit in range";
            case EXHAUSTED -> "Deposit exhausted: no items left in range";
        };
    }
}
