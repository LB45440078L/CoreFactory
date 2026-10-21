package io.corefactory.core.energy;

/**
 * The result of one generator tick.
 *
 * @param outputW the power the generator gives this tick, in W; 0 when it does not produce
 * @param state   the generator's fuel state after the tick
 */
public record GeneratorStep(double outputW, GeneratorState state) {
}
