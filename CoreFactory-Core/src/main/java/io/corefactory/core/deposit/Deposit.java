package io.corefactory.core.deposit;

import java.util.Objects;

/**
 * One deposit: a circular patch of one ore on the surface plane of a world, with the amount left in it. A drill takes its
 * yield from the deposit it covers, so the remaining amount is the only part that changes once the planner has made it.
 *
 * @param id            a stable hash of the world, the cell and the seed, the same on every planning
 * @param world         the name of the world
 * @param centreX       the x of the centre block
 * @param centreZ       the z of the centre block
 * @param radius        the radius in blocks, at least 1
 * @param ore           the ore id, such as {@code galena_ore} or {@code crude_petroleum_deposit}
 * @param initialAmount the amount the deposit was planned with, at least 0
 * @param remaining     the amount left, from 0 to {@code initialAmount}
 */
public record Deposit(long id, String world, int centreX, int centreZ, int radius, String ore,
                      long initialAmount, long remaining) {

    public Deposit {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(ore, "ore");
        if (radius < 1) {
            throw new IllegalArgumentException("A deposit's radius is at least 1, not " + radius);
        }
        if (initialAmount < 0 || remaining < 0) {
            throw new IllegalArgumentException("A deposit's amounts cannot be negative: " + initialAmount + ", " + remaining);
        }
        if (remaining > initialAmount) {
            throw new IllegalArgumentException("A deposit cannot hold more than it started with: " + remaining + " of " + initialAmount);
        }
    }

    /** True when the block column (x, z) lies inside the deposit or on its edge. */
    public boolean contains(int x, int z) {
        long dx = (long) x - centreX;
        long dz = (long) z - centreZ;
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    /** This deposit with a different amount left in it. */
    public Deposit withRemaining(long remaining) {
        return new Deposit(id, world, centreX, centreZ, radius, ore, initialAmount, remaining);
    }
}
