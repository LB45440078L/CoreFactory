package io.corefactory.core.energy;

/**
 * The fuel state of a generator. Wind and solar keep the empty state.
 *
 * @param remainingSeconds the seconds left on the fuel item now burning; 0 when none is burning
 * @param fuelItems        the fuel items in the generator's input, waiting to burn
 */
public record GeneratorState(double remainingSeconds, int fuelItems) {

    /** A generator with no fuel burning and none in its input. */
    public static final GeneratorState EMPTY = new GeneratorState(0.0, 0);

    public GeneratorState {
        Checks.finiteNonNegative(remainingSeconds, "remaining seconds");
        Checks.nonNegative(fuelItems, "fuel items");
    }

    /**
     * The state after fuel is put into the input.
     *
     * @param items the items added, not negative
     */
    public GeneratorState withFuel(int items) {
        return new GeneratorState(remainingSeconds, Math.addExact(fuelItems, Checks.nonNegative(items, "added fuel")));
    }
}
