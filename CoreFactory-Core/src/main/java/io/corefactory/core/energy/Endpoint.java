package io.corefactory.core.energy;

import java.util.Objects;

/**
 * One end of a wire: the device's id, its kind and its block.
 *
 * @param id   the device's id
 * @param kind the device's kind
 * @param pos  the device's block
 */
public record Endpoint(String id, DeviceKind kind, Pos pos) {

    public Endpoint {
        Checks.id(id, "endpoint id");
        Objects.requireNonNull(kind, "endpoint kind");
        Objects.requireNonNull(pos, "endpoint pos");
    }
}
