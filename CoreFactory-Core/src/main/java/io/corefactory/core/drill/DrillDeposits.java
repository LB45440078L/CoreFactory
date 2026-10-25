package io.corefactory.core.drill;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositSettings;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Which deposits a drill works on. A drill's square is {@link DrillBox}'s x and z span; a deposit is in range when its circle
 * touches that square. A petroleum drill takes only crude petroleum deposits, and an ore drill takes every other ore. The
 * drill's selection then removes the ores the player ignored. Pure, so the choice is tested with plain deposits.
 */
public final class DrillDeposits {

    private DrillDeposits() {
    }

    /**
     * How far from a drill's centre the store is searched for deposits. A deposit touches the square when its centre lies within
     * its radius of the square, and the square lies within {@code area} blocks of its centre, so the largest radius a deposit may
     * have plus the area reaches every deposit that touches the square. The largest radius is the constant, not the setting, so
     * a lowered setting never shortens the search past a deposit planned with a larger radius.
     *
     * @param spec the drill's level, whose area is the side of its square
     */
    public static int reach(DrillSpec spec) {
        return DepositSettings.MAX_RADIUS + spec.area();
    }

    /**
     * The deposit a drill works on after a search that picked another one. The drill moves to the chosen deposit only once the batch
     * of its old deposit is all in the outputs: while items wait for a take, it keeps the old deposit, so they are taken from it and
     * never dropped.
     *
     * @param chosen  the deposit the search picked, if any
     * @param current the deposit the drill works on after its take, or null when that deposit is gone
     * @param pending the items of the batch not yet taken from the deposit
     */
    public static Optional<Deposit> afterBatch(Optional<Deposit> chosen, Deposit current, long pending) {
        if (current != null && pending > 0) {
            return Optional.of(current);
        }
        return chosen;
    }

    /** True when the deposit's circle touches the drill's square: its nearest block column to the deposit's centre is within its radius. */
    public static boolean touches(Deposit deposit, DrillBox box) {
        long nearestX = Math.max(box.minX(), Math.min(deposit.centreX(), box.maxX()));
        long nearestZ = Math.max(box.minZ(), Math.min(deposit.centreZ(), box.maxZ()));
        long dx = deposit.centreX() - nearestX;
        long dz = deposit.centreZ() - nearestZ;
        return dx * dx + dz * dz <= (long) deposit.radius() * deposit.radius();
    }

    /** True when a drill of this kind may mine this ore, before any selection: crude petroleum for a petroleum drill, any other ore for an ore drill. */
    public static boolean kindTakes(DrillKind kind, String ore) {
        boolean crude = DrillRules.DEPOSIT.equals(ore);
        return kind == DrillKind.PETROLEUM ? crude : !crude;
    }

    /**
     * The deposits of a search that touch the square and that the kind may mine, in the order given. An ore the caller does not
     * know is left out, so a drill never works a deposit that yields nothing.
     *
     * @param found    the deposits the store returned around the drill
     * @param box      the drill's square
     * @param kind     the drill's kind
     * @param knownOre true for an ore id that has a definition
     */
    public static List<Deposit> inSquare(Collection<Deposit> found, DrillBox box, DrillKind kind, Predicate<String> knownOre) {
        return found.stream()
                .filter(deposit -> kindTakes(kind, deposit.ore()) && knownOre.test(deposit.ore()) && touches(deposit, box))
                .toList();
    }

    /** The deposits whose ore the selection mines. */
    public static List<Deposit> mineable(Collection<Deposit> inSquare, DrillSelection selection) {
        return inSquare.stream().filter(deposit -> selection.isMining(deposit.ore())).toList();
    }

    /**
     * The deposit a drill works on now: the one with the largest remaining fraction, which is what it has left over what it started
     * with. Every deposit of the square is then mined to the same fraction, so each ore of the area is produced over the run. A tie
     * goes to the lower id, and a deposit with nothing left is never chosen, so with every deposit empty the result is empty.
     *
     * @param mineable the deposits in range that the drill's selection mines, with their current remaining amounts
     */
    public static Optional<Deposit> choose(Collection<Deposit> mineable) {
        return mineable.stream()
                .filter(deposit -> deposit.remaining() > 0)
                .max((a, b) -> {
                    int share = compareShare(a, b);
                    return share != 0 ? share : Long.compare(b.id(), a.id());
                });
    }

    /**
     * Compares the remaining fractions of two deposits exactly, by cross multiplication, so no rounding can make two different
     * fractions equal. Both deposits must have something left, which makes their initial amounts positive.
     */
    private static int compareShare(Deposit a, Deposit b) {
        BigInteger left = BigInteger.valueOf(a.remaining()).multiply(BigInteger.valueOf(b.initialAmount()));
        BigInteger right = BigInteger.valueOf(b.remaining()).multiply(BigInteger.valueOf(a.initialAmount()));
        return left.compareTo(right);
    }
}
