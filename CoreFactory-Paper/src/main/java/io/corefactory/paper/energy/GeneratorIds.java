package io.corefactory.paper.energy;

import io.corefactory.core.energy.GeneratorKind;

import java.util.Optional;

/**
 * The component ids of the six generators, each naming one kind. Solar keeps its id, solar_panel. Pure.
 */
public final class GeneratorIds {

    private GeneratorIds() {
    }

    /**
     * The component id of a kind's item.
     *
     * @param kind the generator kind
     */
    public static String componentId(GeneratorKind kind) {
        return switch (kind) {
            case WIND -> "wind_generator";
            case SOLAR -> "solar_panel";
            case CHARCOAL -> "charcoal_generator";
            case PETROLEUM -> "petroleum_generator";
            case DIESEL -> "diesel_generator";
            case NUCLEAR -> "nuclear_generator";
        };
    }

    /**
     * The kind a component id names, or empty when the id is not a generator.
     *
     * @param componentId the component id, such as charcoal_generator
     */
    public static Optional<GeneratorKind> kindOf(String componentId) {
        if (componentId == null) {
            return Optional.empty();
        }
        for (GeneratorKind kind : GeneratorKind.values()) {
            if (componentId(kind).equals(componentId)) {
                return Optional.of(kind);
            }
        }
        return Optional.empty();
    }
}
