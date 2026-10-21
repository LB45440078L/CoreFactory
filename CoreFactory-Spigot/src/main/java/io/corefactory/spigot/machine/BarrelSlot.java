package io.corefactory.spigot.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * One slot of a machine's barrel, seen as a process stack. The barrel is the storage: the engine reads and writes it
 * through this view, and the menu, the hoppers and the saves all see the same items. A slot the live inventory does not
 * have is absent: it reads as empty and is never written.
 */
final class BarrelSlot implements Stack {

    private final Supplier<Optional<Inventory>> held;
    private final ItemRefs refs;
    private final int index;

    /**
     * @param held  the live inventory of the barrel, empty while its chunk is not loaded
     * @param refs  converts the slot's items to references
     * @param index the barrel slot
     */
    BarrelSlot(Supplier<Optional<Inventory>> held, ItemRefs refs, int index) {
        this.held = held;
        this.refs = refs;
        this.index = index;
    }

    @Override
    public ItemRef ref() {
        return slot().flatMap(inventory -> refs.of(inventory.getItem(index))).orElse(null);
    }

    @Override
    public int count() {
        return slot().map(inventory -> {
            ItemStack item = inventory.getItem(index);
            return ItemRefs.isEmpty(item) ? 0 : item.getAmount();
        }).orElse(0);
    }

    @Override
    public void set(ItemRef ref, int count) {
        slot().ifPresent(inventory -> {
            if (ref == null || count <= 0) {
                inventory.setItem(index, null);
                return;
            }
            ItemStack current = inventory.getItem(index);
            if (!ItemRefs.isEmpty(current) && ref.equals(refs.of(current).orElse(null))) {
                current.setAmount(count);
                inventory.setItem(index, current);
                return;
            }
            inventory.setItem(index, refs.create(ref, count));
        });
    }

    /** The live inventory, when it has this slot. */
    private Optional<Inventory> slot() {
        return held.get().filter(inventory -> Barrel.has(inventory, index));
    }
}
