package io.corefactory.core.water;

import java.util.Optional;

/**
 * The placed water blocks: the pipe, the pump and the two splitters. Each names its component id, which is also its item and its
 * device type, and the kind it has in a network.
 */
public enum WaterPart {
    /** A pipe: joins on all six faces and carries water. */
    PIPE("water_pipe", WaterKind.PIPE),
    /** A pump: sits on a water block and pushes water into its network. */
    PUMP("water_pump", WaterKind.PUMP),
    /** A T splitter: one horizontal input, two outputs to the left and right of it. */
    TEE("water_splitter_t", WaterKind.SPLITTER_T),
    /** A cross splitter: one horizontal input and three outputs. */
    CROSS("water_splitter_x", WaterKind.SPLITTER_X);

    private final String componentId;
    private final WaterKind kind;

    WaterPart(String componentId, WaterKind kind) {
        this.componentId = componentId;
        this.kind = kind;
    }

    /** The component id, such as {@code water_pipe}. */
    public String componentId() {
        return componentId;
    }

    /** The kind the part has in a water network. */
    public WaterKind kind() {
        return kind;
    }

    /** True for a splitter, which must be placed on a side face, since its input is horizontal. */
    public boolean splitter() {
        return kind.isSplitter();
    }

    /** The part a component id names, or empty for any other id. */
    public static Optional<WaterPart> ofComponent(String componentId) {
        for (WaterPart part : values()) {
            if (part.componentId.equals(componentId)) {
                return Optional.of(part);
            }
        }
        return Optional.empty();
    }
}
