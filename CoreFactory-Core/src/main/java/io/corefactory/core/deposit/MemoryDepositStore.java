package io.corefactory.core.deposit;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deposits held in memory, with the same semantics as the database store. The amounts are lost when the process stops.
 */
public final class MemoryDepositStore implements DepositStore {

    private static final Comparator<Deposit> BY_ID = Comparator.comparingLong(Deposit::id);

    private final ConcurrentHashMap<Long, Deposit> deposits = new ConcurrentHashMap<>();

    @Override
    public Optional<Deposit> find(long id) {
        return Optional.ofNullable(deposits.get(id));
    }

    @Override
    public List<Deposit> overlapping(String world, int x, int z, int reach) {
        Objects.requireNonNull(world, "world");
        if (reach < 0) {
            throw new IllegalArgumentException("reach must not be negative, not " + reach);
        }
        return deposits.values().stream()
                .filter(deposit -> deposit.world().equals(world) && DepositStore.reaches(deposit, x, z, reach))
                .sorted(BY_ID)
                .toList();
    }

    @Override
    public boolean insertIfAbsent(Deposit deposit) {
        Objects.requireNonNull(deposit, "deposit");
        return deposits.putIfAbsent(deposit.id(), deposit) == null;
    }

    /**
     * The check and the subtraction happen inside one {@code computeIfPresent}, so concurrent takes never overlap.
     */
    @Override
    public long take(long id, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative, not " + amount);
        }
        long[] taken = {0};
        deposits.computeIfPresent(id, (key, deposit) -> {
            long removed = Math.min(amount, deposit.remaining());
            if (removed <= 0) {
                return deposit;
            }
            taken[0] = removed;
            return deposit.withRemaining(deposit.remaining() - removed);
        });
        return taken[0];
    }

    @Override
    public List<Deposit> all(String world) {
        Objects.requireNonNull(world, "world");
        return deposits.values().stream()
                .filter(deposit -> deposit.world().equals(world))
                .sorted(BY_ID)
                .toList();
    }

    @Override
    public int size() {
        return deposits.size();
    }
}
