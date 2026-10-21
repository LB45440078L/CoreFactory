package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

import java.util.Objects;

/**
 * One belt unit: a block with a flow direction. Its output cell is {@code pos + flow}. Its shape and its input side are
 * never stored; {@link LogisticsNetwork} derives them from the neighbouring units and endpoints.
 *
 * @param pos   the unit's block
 * @param flow  the direction its items move in
 * @param level the unit's level, which sets its speed and power
 */
public record BeltUnit(Pos pos, Dir flow, LogisticsLevel level) {

    public BeltUnit {
        Objects.requireNonNull(pos, "belt pos");
        Objects.requireNonNull(flow, "belt flow");
        Objects.requireNonNull(level, "belt level");
    }
}
