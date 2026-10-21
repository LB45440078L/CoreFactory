package io.corefactory.api;

/**
 * Produces block writes lazily, a slice at a time, so that a very large blast (a crater several million
 * blocks deep) never has to be held in memory at once. The core calls {@link #next} once per tick.
 */
@FunctionalInterface
public interface BlockGenerator {

    /**
     * Appends at most {@code limit} writes to {@code out}.
     *
     * @return {@code true} while more writes remain, {@code false} once the generator is finished
     */
    boolean next(BlockChangeSet out, int limit);
}
