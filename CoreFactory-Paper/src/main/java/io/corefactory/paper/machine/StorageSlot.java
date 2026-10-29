package io.corefactory.paper.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.BooleanSupplier;

/**
 * One slot of a vanilla container's inventory, seen as a process stack, for the verified insert of {@link HopperTransfer} and the
 * crafter grid. A component item is identified by its stored id, so a renamed or merged stack of it is still that item. A vanilla
 * item holds a reference only while its stack is plain: a named or otherwise changed stack reads as a different item, so an item is
 * never merged into it and it is never replaced. Setting a slot to the item it holds adds to the stack in place.
 */
final class StorageSlot implements Stack {

    private final Inventory inventory;
    private final ItemRefs refs;
    private final int index;

    /**
     * @param inventory the live inventory of the container
     * @param refs      converts the slot's items to references
     * @param index     the slot
     */
    StorageSlot(Inventory inventory, ItemRefs refs, int index) {
        this.inventory = inventory;
        this.refs = refs;
        this.index = index;
    }

    @Override
    public ItemRef ref() {
        ItemStack item = inventory.getItem(index);
        if (ItemRefs.isEmpty(item)) {
            return null;
        }
        ItemRef ref = refs.of(item).orElse(null);
        if (ref == null) {
            return null;
        }
        return identify(ref, () -> {
            ItemStack plain = refs.create(ref, 1);
            return plain != null && item.isSimilar(plain);
        });
    }

    /**
     * The reference a stack counts as. A component item counts as its stored id whatever its display meta, so a renamed or merged stack
     * is still that item and a different id is a different item. A vanilla item counts as its material only while it is plain.
     *
     * @param ref     the reference the stack's id or material gives
     * @param isPlain whether the stack is the plain item of its material; read only for a vanilla item
     * @return the reference, or null when the stack is a changed vanilla item
     */
    static ItemRef identify(ItemRef ref, BooleanSupplier isPlain) {
        if (ref.kind() == ItemRef.Kind.ITEM) {
            return ref;
        }
        return isPlain.getAsBoolean() ? ref : null;
    }

    @Override
    public int count() {
        ItemStack item = inventory.getItem(index);
        return ItemRefs.isEmpty(item) ? 0 : item.getAmount();
    }

    @Override
    public void set(ItemRef ref, int count) {
        if (ref == null || count <= 0) {
            if (!ItemRefs.isEmpty(inventory.getItem(index))) {
                inventory.setItem(index, null);
            }
            return;
        }
        ItemStack current = inventory.getItem(index);
        if (!ItemRefs.isEmpty(current)) {
            if (!ref.equals(ref())) {
                return;
            }
            current.setAmount(count);
            inventory.setItem(index, current);
            return;
        }
        ItemStack created = refs.create(ref, count);
        if (created != null) {
            inventory.setItem(index, created);
        }
    }
}
