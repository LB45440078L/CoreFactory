package io.corefactory.spigot.energy;

import io.corefactory.core.energy.Generator;
import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.GeneratorState;
import io.corefactory.core.energy.Pos;
import io.corefactory.spigot.energy.StoredEnergy.StoredGenerator;

/**
 * The mapping from a generator's saved or live fields to the core {@link Generator} it ticks as. A kind with no fuel always starts
 * empty, whatever was saved, and a fuel count is capped by {@link GeneratorFuel#countOf(int)}. Pure, so the mapping is tested
 * without a server.
 */
public final class GeneratorSnapshots {

    private GeneratorSnapshots() {
    }

    /**
     * The core generator of a generator at a block, with its fuel state.
     *
     * @param id             the device id
     * @param pos            the generator's block
     * @param kind           the generator kind
     * @param fuel           the fuel items kept; capped, and ignored for a kind with no fuel
     * @param remainingSeconds the seconds left on the burning item; zero when none, and ignored for a kind with no fuel
     */
    public static Generator core(String id, Pos pos, GeneratorKind kind, int fuel, double remainingSeconds) {
        GeneratorState state = kind.burnsFuel()
                ? new GeneratorState(Math.max(0.0, remainingSeconds), GeneratorFuel.countOf(fuel))
                : GeneratorState.EMPTY;
        return new Generator(id, pos, kind, state);
    }

    /** The core generator of a saved generator. */
    public static Generator core(StoredGenerator stored) {
        return core(stored.id(), stored.pos(), stored.kind(), stored.fuel(), stored.remainingSeconds());
    }
}
