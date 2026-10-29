package io.corefactory.core.drill;

/** Why a drill is or is not extracting right now. The text is plain, without colour codes. */
public enum DrillStatus {

    /** The drill has power, room in its outputs and a deposit with something left in its square. */
    RUNNING,

    /** The energy network gives the drill no power, so it does not advance. */
    NO_POWER,

    /** The output slots have no room for another item, or for a petroleum drill the oil network takes no more petroleum. */
    FULL,

    /** The drill has power and room, but no deposit it may mine touches its square. */
    NO_TARGET,

    /** The drill has power and room, and deposits it may mine touch its square, but every one of them is empty. It stays until moved. */
    EXHAUSTED;

    /**
     * The status as players read it.
     *
     * @param kind the drill's kind
     */
    public String text(DrillKind kind) {
        return switch (this) {
            case RUNNING -> "Running";
            case NO_POWER -> "No power";
            case FULL -> kind == DrillKind.PETROLEUM ? "Network full" : "Output full";
            case NO_TARGET -> "No deposit in range";
            case EXHAUSTED -> "Deposit exhausted";
        };
    }
}
