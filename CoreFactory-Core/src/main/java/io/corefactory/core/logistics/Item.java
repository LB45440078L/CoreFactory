package io.corefactory.core.logistics;

import java.util.Objects;

/**
 * An item on a belt, in a picker or in a splitter: a material or a component, identified by its id. The Paper side maps
 * its stacks to these records, so the logistics model never sees a Bukkit type.
 *
 * @param kind {@link #MATERIAL} or {@link #COMPONENT}
 * @param id   the material name or the component id, not blank
 */
public record Item(String kind, String id) {

    /** The kind of a vanilla material. */
    public static final String MATERIAL = "MATERIAL";

    /** The kind of a component from items.yml or materials.yml. */
    public static final String COMPONENT = "COMPONENT";

    public Item {
        Objects.requireNonNull(kind, "item kind");
        Objects.requireNonNull(id, "item id");
        if (!MATERIAL.equals(kind) && !COMPONENT.equals(kind)) {
            throw new IllegalArgumentException("item kind must be MATERIAL or COMPONENT: " + kind);
        }
        if (id.isBlank()) {
            throw new IllegalArgumentException("item id must not be blank");
        }
    }

    /** A vanilla material item. */
    public static Item material(String id) {
        return new Item(MATERIAL, id);
    }

    /** A component item. */
    public static Item component(String id) {
        return new Item(COMPONENT, id);
    }
}
