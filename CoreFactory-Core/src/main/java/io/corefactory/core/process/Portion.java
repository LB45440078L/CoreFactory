package io.corefactory.core.process;

/**
 * An amount of one item reference: an input of a process, or the items rolled from an output.
 *
 * @param ref    the item
 * @param amount how many, at least one
 */
public record Portion(ItemRef ref, int amount) {

    public Portion {
        if (amount < 1) {
            throw new IllegalArgumentException("A portion needs at least one item: " + ref);
        }
    }
}
