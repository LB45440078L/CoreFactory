package io.corefactory.core.turret;

import java.util.Optional;

/**
 * The rounds a turret holds: one round type at a time, a count, and a capacity. A turret with no rounds holds no type, so any type may
 * be loaded next. A turret that holds rounds keeps their type until it has none left. Immutable: each change gives a new magazine.
 *
 * @param type     the round type held, or null when the magazine is empty
 * @param count    the rounds held, from 0 to the capacity
 * @param capacity the most rounds the magazine holds
 */
public record Magazine(RoundType type, int count, int capacity) {

    public Magazine {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1: " + capacity);
        }
        if (count < 0 || count > capacity) {
            throw new IllegalArgumentException("count must be between 0 and the capacity: " + count);
        }
        if (count == 0) {
            type = null;
        } else if (type == null) {
            throw new IllegalArgumentException("a magazine with rounds needs a round type");
        }
    }

    /** An empty magazine of a capacity. */
    public static Magazine empty(int capacity) {
        return new Magazine(null, 0, capacity);
    }

    /** True when no round is held. */
    public boolean isEmpty() {
        return count == 0;
    }

    /** How many rounds of this type fit now: the free space when the magazine is empty or holds that type, otherwise none. */
    public int roomFor(RoundType incoming) {
        if (incoming == null || (count > 0 && type != incoming)) {
            return 0;
        }
        return capacity - count;
    }

    /**
     * The magazine with some rounds of one type added.
     *
     * @param incoming the round type; the magazine must have room for it
     * @param amount   the rounds to add, from 1 to {@link #roomFor(RoundType)}
     * @throws IllegalArgumentException when the type or the amount does not fit
     */
    public Magazine add(RoundType incoming, int amount) {
        if (amount < 1 || amount > roomFor(incoming)) {
            throw new IllegalArgumentException("the magazine has no room for " + amount + " of " + incoming);
        }
        return new Magazine(incoming, count + amount, capacity);
    }

    /** True when a shot that uses this many rounds can be fired. */
    public boolean canFire(int perShot) {
        return count >= perShot && perShot > 0;
    }

    /** The magazine after one shot that uses this many rounds. Empty when it cannot be fired; use {@link #canFire(int)} first. */
    public Optional<Magazine> fire(int perShot) {
        if (!canFire(perShot)) {
            return Optional.empty();
        }
        return Optional.of(new Magazine(type, count - perShot, capacity));
    }

    /** The magazine with every round taken out. */
    public Magazine ejectAll() {
        return empty(capacity);
    }
}
