package io.corefactory.core.energy;

import java.util.Objects;

/**
 * A device that takes power: a drill, a lab tool, a picker, a splitter, a belt, a turret or a water pump. It absorbs from every grid whose
 * pylon range covers its block.
 *
 * <p>A critical consumer is served before every other one on its grid, whatever its id: a water pump that feeds a nuclear generator is one,
 * since the generator stops when its pump stops, and a stopped generator starves the grid the pump needs. Among the rest, the order is by id.
 *
 * @param id       the consumer's id; consumers are processed in id order, the critical ones first
 * @param pos      the consumer's block
 * @param demandW  the power it asks for, in W, not negative
 * @param critical true when the consumer is served before the others
 */
public record Consumer(String id, Pos pos, double demandW, boolean critical) {

    public Consumer {
        Checks.id(id, "consumer id");
        Objects.requireNonNull(pos, "consumer pos");
        Checks.finiteNonNegative(demandW, "consumer demand");
    }

    /**
     * An ordinary consumer, served in id order.
     *
     * @param id      the consumer's id
     * @param pos     the consumer's block
     * @param demandW the power it asks for, in W
     */
    public Consumer(String id, Pos pos, double demandW) {
        this(id, pos, demandW, false);
    }
}
