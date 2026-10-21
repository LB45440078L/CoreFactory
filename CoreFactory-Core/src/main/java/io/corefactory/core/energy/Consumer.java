package io.corefactory.core.energy;

import java.util.Objects;

/**
 * A device that takes power: a drill, a lab tool, a picker, a splitter or a belt. It absorbs from every grid whose pylon
 * range covers its block.
 *
 * @param id      the consumer's id; consumers are processed in id order
 * @param pos     the consumer's block
 * @param demandW the power it asks for, in W, not negative
 */
public record Consumer(String id, Pos pos, double demandW) {

    public Consumer {
        Checks.id(id, "consumer id");
        Objects.requireNonNull(pos, "consumer pos");
        Checks.finiteNonNegative(demandW, "consumer demand");
    }
}
