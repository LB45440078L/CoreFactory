package io.corefactory.core.energy;

import java.util.Objects;

/**
 * A generator as the network sees it in one tick: its id, its block and the output the caller computed with
 * {@link Generator#tick(GeneratorEnvironment, double)}.
 *
 * @param id      the generator's id
 * @param pos     the generator's block, used to check its wire
 * @param outputW the output this tick, in W, not negative
 */
public record GeneratorOutput(String id, Pos pos, double outputW) {

    public GeneratorOutput {
        Checks.id(id, "generator id");
        Objects.requireNonNull(pos, "generator pos");
        Checks.finiteNonNegative(outputW, "generator output");
    }

    /** The output of a generator after its tick. */
    public static GeneratorOutput of(Generator generator, GeneratorStep step) {
        return new GeneratorOutput(generator.id(), generator.pos(), step.outputW());
    }
}
