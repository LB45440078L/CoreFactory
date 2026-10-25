package io.corefactory.core.oil;

import io.corefactory.core.water.WaterKind;

import java.util.Optional;

/**
 * The placed oil tubes: the straight pipe (with its bends and junctions), the T splitter and the cross splitter. Each names its component
 * id, which is also its item, and the kind it has in a network. The kinds are the water network's, since an oil tube joins the way a water
 * pipe does.
 */
public enum OilPart {
    /** A pipe: joins on all six faces, as a straight run, a bend or a junction. */
    PIPE("oil_pipe", WaterKind.PIPE),
    /** A T splitter: one input, two outputs to the left and right of it. */
    TEE("oil_splitter_t", WaterKind.SPLITTER_T),
    /** A cross splitter: one input and three outputs. */
    CROSS("oil_splitter_x", WaterKind.SPLITTER_X);

    private final String componentId;
    private final WaterKind kind;

    OilPart(String componentId, WaterKind kind) {
        this.componentId = componentId;
        this.kind = kind;
    }

    /** The component id, such as {@code oil_pipe}. */
    public String componentId() {
        return componentId;
    }

    /** The kind the part has in a network. */
    public WaterKind kind() {
        return kind;
    }

    /** True for a splitter, whose input is a face of its block. */
    public boolean splitter() {
        return kind.isSplitter();
    }

    /** The part a component id names, or empty for any other id. */
    public static Optional<OilPart> ofComponent(String componentId) {
        for (OilPart part : values()) {
            if (part.componentId.equals(componentId)) {
                return Optional.of(part);
            }
        }
        return Optional.empty();
    }
}
