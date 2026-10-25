package io.corefactory.paper.artillery;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.item.ComponentDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The shells and the charges of a magazine as items: counts them in a player's inventory, takes them out of it, and makes the stacks that give
 * them back. A shell and a charge are components, identified by their component ids, so an item of another kind is never taken.
 */
final class MagazineItems {

    private final CoreFactoryCore core;

    /**
     * @param core the core, whose items and component definitions identify and make the ammunition
     */
    MagazineItems(CoreFactoryCore core) {
        this.core = core;
    }

    /**
     * How many items of a component a player carries.
     *
     * @param player      the player
     * @param componentId the component id, such as {@code explosive_shell}
     * @return the count, 0 or more
     */
    int count(Player player, String componentId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isComponent(item, componentId)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Takes items of a component out of a player's inventory, from the first slots that hold them.
     *
     * @param player      the player
     * @param componentId the component id
     * @param amount      how many to take, 1 or more
     * @return how many were taken: the amount, or less when the player carries fewer
     */
    int take(Player player, String componentId, int amount) {
        int left = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && left > 0; slot++) {
            ItemStack item = contents[slot];
            if (!isComponent(item, componentId)) {
                continue;
            }
            int taken = Math.min(left, item.getAmount());
            left -= taken;
            int remaining = item.getAmount() - taken;
            if (remaining == 0) {
                player.getInventory().setItem(slot, null);
            } else {
                ItemStack rest = item.clone();
                rest.setAmount(remaining);
                player.getInventory().setItem(slot, rest);
            }
        }
        return amount - left;
    }

    /**
     * The stacks that hold a number of items of a component, each at most one stack's worth.
     *
     * @param componentId the component id
     * @param amount      how many items, 0 or more
     * @return the stacks, in order; empty when the amount is 0 or the component is unknown
     */
    List<ItemStack> stacks(String componentId, int amount) {
        List<ItemStack> stacks = new ArrayList<>();
        ComponentDefinition definition = core.componentDefinitions().get(componentId);
        if (definition == null || amount <= 0) {
            return stacks;
        }
        int stackSize = core.items().component(definition, 1).getMaxStackSize();
        int left = amount;
        while (left > 0) {
            int size = Math.min(left, stackSize);
            stacks.add(core.items().component(definition, size));
            left -= size;
        }
        return stacks;
    }

    private boolean isComponent(ItemStack item, String componentId) {
        return item != null && componentId.equals(core.items().componentId(item).orElse(null));
    }
}
