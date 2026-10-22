package io.corefactory.core.drill;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositSettings;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
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
     * The deposit with the most remaining amount, with a tie going to the lower id. A deposit with nothing left is never picked.
     *
     * @param deposits the candidates, in any order
     */
    public static Optional<Deposit> pick(Collection<Deposit> deposits) {
        return deposits.stream()
                .filter(deposit -> deposit.remaining() > 0)
                .max(Comparator.comparingLong(Deposit::remaining).thenComparing(Comparator.comparingLong(Deposit::id).reversed()));
    }

    /**
     * The deposit a drill works on now. A drill keeps working its current deposit while that deposit is still mineable and has
     * something left, so it works one deposit at a time; otherwise it takes {@link #pick}.
     *
     * @param mineable the deposits in range that the drill's selection mines, with their current remaining amounts
     * @param current  the id of the deposit the drill works on, if any
     */
    public static Optional<Deposit> choose(Collection<Deposit> mineable, OptionalLong current) {
        if (current.isPresent()) {
            Optional<Deposit> kept = mineable.stream()
                    .filter(deposit -> deposit.id() == current.getAsLong() && deposit.remaining() > 0)
                    .findFirst();
            if (kept.isPresent()) {
                return kept;
            }
        }
        return pick(mineable);
    }
}
