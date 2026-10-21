package io.corefactory.core.logistics;

import io.corefactory.core.process.ItemRef;

import java.util.Objects;

/**
 * The mapping between an item on a belt, in a picker or in a splitter and the item reference a process slot holds. A
 * component is {@link Item#COMPONENT} and {@link ItemRef.Kind#ITEM}; a vanilla material is {@link Item#MATERIAL} and
 * {@link ItemRef.Kind#MATERIAL}. Pure, so the ports are tested without a server.
 */
public final class PortItems {

    private PortItems() {
    }

    /** The reference a logistics item stands for in a process slot. */
    public static ItemRef refOf(Item item) {
        Objects.requireNonNull(item, "item");
        return Item.COMPONENT.equals(item.kind()) ? ItemRef.item(item.id()) : ItemRef.material(item.id());
    }

    /** The logistics item a process reference stands for. */
    public static Item itemOf(ItemRef ref) {
        Objects.requireNonNull(ref, "ref");
        return ref.kind() == ItemRef.Kind.ITEM ? Item.component(ref.name()) : Item.material(ref.name());
    }
}
