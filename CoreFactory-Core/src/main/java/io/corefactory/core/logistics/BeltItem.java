package io.corefactory.core.logistics;

/**
 * An item on a belt as a view: where it is along its unit. Positions run from 0 at the entry to 1 at the exit.
 *
 * @param item     the item
 * @param position its place along the unit, from 0 up to and including 1
 */
public record BeltItem(Item item, double position) {
}
