package io.corefactory.core.energy;

import java.util.Objects;

/**
 * A generator device: an id, a block, a kind and its fuel state. Its output is pure: {@link #tick(GeneratorEnvironment, double)}
 * returns the power for the tick and the next fuel state, and the caller stores that state. A petroleum or diesel generator burns
 * the litres it draws from the oil network instead, so its output comes from {@link #tickFluid}.
 *
 * @param id    the stable id of the generator
 * @param pos   the generator's block
 * @param kind  the generator's kind
 * @param state the fuel state before the tick
 */
public record Generator(String id, Pos pos, GeneratorKind kind, GeneratorState state) {

    private static final double EPS = 1e-9;

    public Generator {
        Checks.id(id, "generator id");
        Objects.requireNonNull(pos, "generator pos");
        Objects.requireNonNull(kind, "generator kind");
        Objects.requireNonNull(state, "generator state");
    }

    /**
     * A nuclear generator works only underwater: its own block is water and no air or other non-water block lies above or
     * beside it.
     *
     * @param ownBlockWater true when the generator's own block is water
     * @param airCount      the blocks above or beside the generator that are not water
     */
    public static boolean isUnderwater(boolean ownBlockWater, int airCount) {
        return ownBlockWater && Checks.nonNegative(airCount, "air count") == 0;
    }

    /**
     * One tick of a generator that burns no fluid, lasting {@code dtSeconds}.
     *
     * <ul>
     *   <li>Wind gives its output at the wind speed of the world tick, with no fuel; 0 at a closed site or outside the
     *       cut-in and cut-out speeds.</li>
     *   <li>Solar gives its output at the irradiance of the environment, with no fuel.</li>
     *   <li>Charcoal and nuclear burn an item: when no item is burning and one is in the input, it starts one. While an item
     *       burns the generator gives its rated output and the item's remaining seconds run down. With no fuel it gives 0.</li>
     *   <li>Nuclear only burns underwater; out of water it gives 0 and its fuel stays as it is.</li>
     * </ul>
     *
     * @param env        the environment of the tick
     * @param dtSeconds  the length of the tick in seconds, positive
     * @return the output in W and the next state
     * @throws IllegalArgumentException when the length is not positive and finite
     * @throws IllegalStateException    for a petroleum or diesel generator, which burns a fluid: use {@link #tickFluid}
     */
    public GeneratorStep tick(GeneratorEnvironment env, double dtSeconds) {
        requireTick(dtSeconds);
        return switch (kind) {
            case WIND -> new GeneratorStep(Sky.windOutputW(env.openAir(), env.worldTick(), env.seed()), state);
            case SOLAR -> new GeneratorStep(Sky.solarOutputW(env.irradianceWPerM2()), state);
            case NUCLEAR -> env.underwater() ? burn(dtSeconds) : new GeneratorStep(0.0, state);
            case CHARCOAL -> burn(dtSeconds);
            case PETROLEUM, DIESEL -> throw new IllegalStateException(kind + " burns a fluid: its output comes from tickFluid");
        };
    }

    /**
     * One tick of the generator with the litres it drew from the oil network. A petroleum or diesel generator gives its rated output
     * for the share of the litres it wants that it drew, so a full draw gives the rated output and half a draw gives half. It keeps no
     * fuel state, since the fluid is not held in the generator. Any other kind gives its output from {@link #tick}, and the litres are not
     * used.
     *
     * @param env          the environment of the tick
     * @param dtSeconds    the length of the tick in seconds, positive
     * @param litresDrawn  the litres the generator drew this tick, finite and 0 or more
     * @return the output in W and the state, which stays empty for a fluid kind
     * @throws IllegalArgumentException when the length is not positive and finite, or the litres are negative or not finite
     */
    public GeneratorStep tickFluid(GeneratorEnvironment env, double dtSeconds, double litresDrawn) {
        requireTick(dtSeconds);
        if (!Double.isFinite(litresDrawn) || litresDrawn < 0) {
            throw new IllegalArgumentException("litres drawn must be finite and 0 or more: " + litresDrawn);
        }
        if (kind.fluid().isEmpty()) {
            return tick(env, dtSeconds);
        }
        double share = Math.min(1.0, litresDrawn / litresWanted(dtSeconds));
        return new GeneratorStep(kind.ratedW() * share, GeneratorState.EMPTY);
    }

    /**
     * The litres of fluid the generator wants in a tick at rated output: its litres per second over the tick. A kind that burns no
     * fluid wants none.
     *
     * @param dtSeconds the length of the tick in seconds, positive
     * @return the litres wanted, 0 for a kind that burns no fluid
     * @throws IllegalArgumentException when the length is not positive and finite
     */
    public double litresWanted(double dtSeconds) {
        requireTick(dtSeconds);
        return kind.litresPerSecond() * dtSeconds;
    }

    private static void requireTick(double dtSeconds) {
        if (!(dtSeconds > 0) || !Double.isFinite(dtSeconds)) {
            throw new IllegalArgumentException("dt must be positive and finite: " + dtSeconds);
        }
    }

    private GeneratorStep burn(double dtSeconds) {
        GeneratorState current = state;
        if (current.remainingSeconds() <= EPS && current.fuelItems() > 0) {
            current = new GeneratorState(kind.burnSeconds(), current.fuelItems() - 1);
        }
        if (current.remainingSeconds() > EPS) {
            double left = Math.max(0.0, current.remainingSeconds() - dtSeconds);
            return new GeneratorStep(kind.ratedW(), new GeneratorState(left, current.fuelItems()));
        }
        return new GeneratorStep(0.0, current);
    }
}
