package io.corefactory.core.deposit;

import java.util.Comparator;
import java.util.List;

/**
 * The rules of a scan of the deposits around a point: which deposits count, how they are ordered, how far a deposit is from the
 * point, and whether the point is inside one. Pure, so the scanner, the map and the waypoint share one rule.
 */
public final class DepositScan {

    private DepositScan() {
    }

    /**
     * The deposits that count for a scan: those whose circle reaches within the radius of the point and that pass the filter, the
     * nearest centre first (ties by id), at most {@code limit} of them.
     *
     * @param found  the deposits the store returned for the point, with a reach of at least the largest radius; may hold more
     * @param x      the point's x
     * @param z      the point's z
     * @param radius the scan radius in blocks
     * @param filter {@link DepositFilter#ALL} or an ore id
     * @param limit  the most deposits to return, never negative
     * @return the deposits, nearest first
     */
    public static List<Deposit> nearest(List<Deposit> found, int x, int z, int radius, String filter, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative, not " + limit);
        }
        Comparator<Deposit> nearestFirst = Comparator
                .comparingLong((Deposit deposit) -> squared(deposit.centreX() - x, deposit.centreZ() - z))
                .thenComparingLong(Deposit::id);
        return found.stream()
                .filter(deposit -> DepositStore.reaches(deposit, x, z, radius))
                .filter(deposit -> DepositFilter.matches(filter, deposit.ore()))
                .sorted(nearestFirst)
                .limit(limit)
                .toList();
    }

    /** The distance in blocks from the point to the deposit's centre, rounded to the nearest block. */
    public static long blocksTo(Deposit deposit, int x, int z) {
        double dx = (double) deposit.centreX() - x;
        double dz = (double) deposit.centreZ() - z;
        return Math.round(Math.hypot(dx, dz));
    }

    /** True when the block column (x, z) lies inside the deposit's circle, which is the point a waypoint is reached at. */
    public static boolean arrived(Deposit deposit, int x, int z) {
        return deposit.contains(x, z);
    }

    private static long squared(long dx, long dz) {
        return dx * dx + dz * dz;
    }
}
