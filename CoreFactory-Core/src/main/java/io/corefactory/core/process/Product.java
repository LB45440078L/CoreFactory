package io.corefactory.core.process;

/**
 * One output of a process. It is made when the pass ends, with the given chance.
 *
 * @param ref    the item made
 * @param amount how many are made when it is made, at least one
 * @param chance the probability from 0 to 1 that it is made at the end of a pass
 */
public record Product(ItemRef ref, int amount, double chance) {

    public Product {
        if (amount < 1) {
            throw new IllegalArgumentException("A product needs at least one item: " + ref);
        }
        if (!(chance >= 0 && chance <= 1)) {
            throw new IllegalArgumentException("A product chance must be from 0 to 1: " + ref);
        }
    }
}
