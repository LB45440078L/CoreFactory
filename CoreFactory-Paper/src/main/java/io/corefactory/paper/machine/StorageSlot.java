package io.corefactory.paper.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * One slot of a vanilla container's inventory, seen as a process stack, for the verified insert of {@link HopperTransfer}. A slot
 * holds a reference only when its stack is the plain item of that reference: a named or otherwise changed stack reads as a different
 * item, so an item is never merged into it and it is never replaced. Setting a slot to the item it holds adds to the stack in place.
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
        ItemStack plain = refs.create(ref, 1);
        return plain != null && item.isSimilar(plain) ? ref : null;
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
