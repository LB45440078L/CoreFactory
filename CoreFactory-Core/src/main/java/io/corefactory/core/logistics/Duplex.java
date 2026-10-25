package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

import java.util.List;
import java.util.Objects;

/**
 * A belt duplex: a splitter two cells wide. It occupies two cells side by side, perpendicular to its flow: the anchor, which is the
 * cell the player placed it on, and the second cell, {@code anchor + right(flow)}. Its items enter from one of the two back cells,
 * {@code cell - flow}, which must be a belt flowing in {@code flow}. Its outputs are the two front cells, {@code cell + flow}, and each
 * item goes to them in turn; an output that refuses an item gives it to the other.
 *
 * @param id     the duplex's id, unique among duplexes
 * @param anchor the cell the duplex was placed on
 * @param flow   the direction the items travel through the duplex
 * @param level  the duplex's level, which sets its throughput and power
 */
public record Duplex(String id, Pos anchor, Dir flow, LogisticsLevel level) {

    public Duplex {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("duplex id must not be blank");
        }
        Objects.requireNonNull(anchor, "duplex anchor");
        Objects.requireNonNull(flow, "duplex flow");
        Objects.requireNonNull(level, "duplex level");
    }

    /** The cell beside the anchor on the right of the flow: {@code anchor + right(flow)}. */
    public static Pos secondCell(Pos anchor, Dir flow) {
        return flow.turnRight().step(anchor);
    }

    /** The second cell, {@code anchor + right(flow)}. */
    public Pos second() {
        return secondCell(anchor, flow);
    }

    /** Both cells the duplex occupies: the anchor, then the second cell. */
    public List<Pos> cells() {
        return List.of(anchor, second());
    }

    /** The two back cells, {@code cell - flow}, where an input belt must stand, in the order of {@link #cells()}. */
    public List<Pos> backCells() {
        return List.of(flow.opposite().step(anchor), flow.opposite().step(second()));
    }

    /** The two front cells, {@code cell + flow}, which the duplex gives to, in the order of {@link #cells()}. */
    public List<Pos> outputCells() {
        return List.of(flow.step(anchor), flow.step(second()));
    }
}
