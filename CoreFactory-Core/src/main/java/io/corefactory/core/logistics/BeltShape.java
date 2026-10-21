package io.corefactory.core.logistics;

/**
 * The shape of a belt unit, derived from the side its items come from. A corner turns from its input side into its flow:
 * the left and right names describe the turn as seen by an item travelling along it.
 */
public enum BeltShape {

    /** Items come from behind the unit, or nothing feeds it. */
    STRAIGHT,

    /** Items come from one side and turn left into the flow. */
    CORNER_LEFT,

    /** Items come from one side and turn right into the flow. */
    CORNER_RIGHT
}
