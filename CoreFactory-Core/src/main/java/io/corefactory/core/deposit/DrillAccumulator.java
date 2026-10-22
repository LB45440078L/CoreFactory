package io.corefactory.core.deposit;

/**
 * The output of one drill, taken from its deposit a step at a time. The drill's rate is fractional, so the accumulator
 * keeps the part of an item that a step did not finish, and adds it to the next step. Whole items are taken from the
 * deposit and never more than it holds.
 *
 * <p>The credit is kept while the deposit has items. When a step exhausts the deposit, the credit resets to 0, so a drill
 * never keeps a fraction of an item for a deposit that has nothing left. Each drill owns one accumulator; it is not
 * shared between drills and is not thread-safe.
 */
public final class DrillAccumulator {

    /** The length of one server tick, in seconds: the step length the drill uses. */
    public static final double SECONDS_PER_TICK = 0.05;

    private final double secondsPerStep;
    private double credit;

    /**
     * @param secondsPerStep the length of each step, in seconds; positive and finite
     * @throws IllegalArgumentException when the length is not positive and finite
     */
    public DrillAccumulator(double secondsPerStep) {
        if (!(secondsPerStep > 0) || !Double.isFinite(secondsPerStep)) {
            throw new IllegalArgumentException("A step must be a positive, finite number of seconds, not " + secondsPerStep);
        }
        this.secondsPerStep = secondsPerStep;
    }

    /**
     * Adds one step of output and takes its whole items from the deposit.
     *
     * @param itemsPerSecondTimesSatisfaction the drill's rate after its satisfaction is applied, from 0 when unpowered
     * @param remainingInDeposit              what the deposit still holds, at least 0
     * @return the whole items to take from the deposit this step, from 0 to {@code remainingInDeposit}
     * @throws IllegalArgumentException when the rate is negative or not finite, or the remaining amount is negative
     */
    public long step(double itemsPerSecondTimesSatisfaction, long remainingInDeposit) {
        if (!Double.isFinite(itemsPerSecondTimesSatisfaction) || itemsPerSecondTimesSatisfaction < 0) {
            throw new IllegalArgumentException("A rate must be finite and 0 or more, not " + itemsPerSecondTimesSatisfaction);
        }
        if (remainingInDeposit < 0) {
            throw new IllegalArgumentException("A deposit cannot hold a negative amount: " + remainingInDeposit);
        }
        if (remainingInDeposit == 0) {
            credit = 0;
            return 0;
        }
        credit += itemsPerSecondTimesSatisfaction * secondsPerStep;
        if (credit >= remainingInDeposit) {
            credit = 0;
            return remainingInDeposit;
        }
        long whole = (long) Math.floor(credit);
        credit -= whole;
        return whole;
    }

    /** The fraction of an item carried into the next step, from 0 inclusive to 1 exclusive while the deposit has items. */
    public double credit() {
        return credit;
    }
}
