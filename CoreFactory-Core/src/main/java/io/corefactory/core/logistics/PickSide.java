package io.corefactory.core.logistics;

import java.util.Objects;

/**
 * Which belt a picker takes items from, decided from the picker's directions, the belt at its input cell and that belt's shape.
 * A picker takes from a belt in two ways. From the front: the belt flows into the picker, so the picker takes from the belt's
 * exit, as before. From the side: a straight belt flows past the picker, so the picker takes any item on the belt, the item
 * nearest the belt's exit first. A belt that flows away from the picker gives nothing, and a corner beside the picker gives
 * nothing. Pure.
 */
public enum PickSide {

    /** The belt flows into the picker, and the picker takes from its exit. */
    FRONT,

    /** A straight belt flows past the picker's side, and the picker takes an item from anywhere on it. */
    SIDE,

    /** The picker takes nothing from the belt at its input cell, or there is no belt there. */
    NONE;

    /**
     * The way a picker takes from the belt at its input cell.
     *
     * @param picker the picker
     * @param belt   the belt at the picker's input cell, or null when there is none
     * @param shape  the shape of that belt
     * @return the side the picker takes from; NONE when there is no belt
     */
    public static PickSide of(Picker picker, BeltUnit belt, BeltShape shape) {
        Objects.requireNonNull(picker, "picker");
        if (belt == null) {
            return NONE;
        }
        Objects.requireNonNull(shape, "belt shape");
        if (belt.flow() == picker.outDir()) {
            return FRONT;
        }
        if (belt.flow() == picker.inDir()) {
            return NONE;
        }
        return shape == BeltShape.STRAIGHT ? SIDE : NONE;
    }
}
