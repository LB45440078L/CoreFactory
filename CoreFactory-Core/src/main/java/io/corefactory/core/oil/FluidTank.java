package io.corefactory.core.oil;

import java.util.Objects;
import java.util.Optional;

/**
 * The fluid a network holds: one kind at a time, up to a capacity in litres. A tank refuses a fluid of another kind, so two fluids never mix
 * in it, and it gives out no more than it holds. Pure.
 */
public final class FluidTank {

    /** Volumes below this many litres are rounding noise, and count as empty. */
    static final double EPSILON = 1e-9;

    private final double capacityLitres;
    private Fluid fluid;
    private double litres;

    /**
     * @param capacityLitres the most litres the tank holds, finite and above 0
     * @throws IllegalArgumentException when the capacity is not finite or not above 0
     */
    public FluidTank(double capacityLitres) {
        if (!Double.isFinite(capacityLitres) || capacityLitres <= 0) {
            throw new IllegalArgumentException("capacity must be finite and above 0, got " + capacityLitres);
        }
        this.capacityLitres = capacityLitres;
    }

    /** The fluid the tank holds, or empty when it holds none. */
    public Optional<Fluid> fluid() {
        return Optional.ofNullable(fluid);
    }

    /** The litres held. */
    public double litres() {
        return litres;
    }

    /** The most litres the tank holds. */
    public double capacity() {
        return capacityLitres;
    }

    /** The litres the tank can still take in, whatever the fluid. */
    public double space() {
        return capacityLitres - litres;
    }

    /**
     * Takes in up to {@code amount} litres of a fluid. A fluid of another kind than the one held is refused in full.
     *
     * @param incoming the fluid
     * @param amount   the litres offered, finite and 0 or more
     * @return the litres taken in
     */
    public double accept(Fluid incoming, double amount) {
        Objects.requireNonNull(incoming, "incoming");
        requireAmount(amount);
        if (fluid != null && fluid != incoming) {
            return 0.0;
        }
        double taken = Math.max(0.0, Math.min(amount, capacityLitres - litres));
        if (taken > 0) {
            fluid = incoming;
            litres += taken;
        }
        return taken;
    }

    /**
     * Gives out up to {@code amount} litres of a fluid. Nothing is given out for a fluid the tank does not hold.
     *
     * @param wanted the fluid asked for
     * @param amount the litres asked for, finite and 0 or more
     * @return the litres given out
     */
    public double draw(Fluid wanted, double amount) {
        Objects.requireNonNull(wanted, "wanted");
        requireAmount(amount);
        if (fluid != wanted) {
            return 0.0;
        }
        double given = Math.min(amount, litres);
        litres -= given;
        if (litres <= EPSILON) {
            litres = 0.0;
            fluid = null;
        }
        return given;
    }

    private static void requireAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("amount must be finite and 0 or more, got " + amount);
        }
    }
}
