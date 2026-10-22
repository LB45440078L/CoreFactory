package io.corefactory.core.deposit;

import java.util.List;
import java.util.Optional;

/**
 * The planned deposits of the ore geology, one per id. A deposit is planned once and its remaining amount only falls,
 * through {@link #take}. Implementations are safe for concurrent drills. The database store blocks on JDBC.
 */
public interface DepositStore {

    /**
     * @param id the deposit id
     * @return the deposit, or empty when the id is unknown
     */
    Optional<Deposit> find(long id);

    /**
     * The deposits of a world whose circle reaches a point: their centre is at most {@code radius + reach} blocks from
     * (x, z). The reach must be at least the largest radius stored, so every such centre lies within {@code 2 * reach}
     * of the point and the store can scan the centre box.
     *
     * @param world the world name
     * @param x     the point's x
     * @param z     the point's z
     * @param reach blocks beyond a circle's edge that still count, at least the largest radius; never negative
     * @return the deposits ordered by id
     */
    List<Deposit> overlapping(String world, int x, int z, int reach);

    /**
     * Stores the deposit unless one with the same id is already stored, so a deposit planned twice is stored once.
     *
     * @return true when the deposit was inserted
     */
    boolean insertIfAbsent(Deposit deposit);

    /**
     * Removes up to {@code amount} from the remaining amount of a deposit. The remaining amount never goes below zero.
     *
     * @param id     the deposit id
     * @param amount how much to take, never negative
     * @return the amount actually removed, 0 when the deposit is empty or unknown
     */
    long take(long id, long amount);

    /**
     * @param world the world name
     * @return every deposit of the world, ordered by id
     */
    List<Deposit> all(String world);

    /** @return the number of stored deposits */
    int size();

    /**
     * @param deposit the deposit
     * @param x       the point's x
     * @param z       the point's z
     * @param reach   blocks beyond the circle's edge that still count; never negative
     * @return true when the distance from (x, z) to the deposit's centre is at most radius + reach
     */
    static boolean reaches(Deposit deposit, int x, int z, int reach) {
        if (reach < 0) {
            throw new IllegalArgumentException("reach must not be negative, not " + reach);
        }
        long dx = (long) deposit.centreX() - x;
        long dz = (long) deposit.centreZ() - z;
        long limit = (long) deposit.radius() + reach;
        return dx * dx + dz * dz <= limit * limit;
    }
}
