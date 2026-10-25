package io.corefactory.paper.listener;

import io.corefactory.api.PlacedTnt;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.lock.LockRules;
import io.corefactory.core.mechanics.NuclearTnts;
import io.corefactory.paper.lock.TntLocks;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * The PIN lock of a nuclear TNT, on the right-click. A sneak-right-click with an empty hand is the owner's lock, or an admin's
 * remove. A right-click without sneaking on a locked one, with any item or none, opens the unlock keypad. Both are taken at the
 * lowest priority, before the item uses that would light the TNT, so a right-click never lights a locked TNT.
 */
public final class LockListener implements Listener {

    private final CoreFactoryCore core;
    private final TntLocks locks;

    public LockListener(CoreFactoryCore core, TntLocks locks) {
        this.core = core;
        this.locks = locks;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() != Material.TNT) {
            return;
        }
        Optional<PlacedTnt> placed = core.placedAt(block.getLocation());
        if (placed.isEmpty() || !isNuclear(placed.get())) {
            return;
        }
        Player player = event.getPlayer();
        boolean manage = player.isSneaking() && mainHandEmpty(player);
        boolean unlock = !player.isSneaking() && LockRules.refusesTriggers(placed.get().lock());
        if (!manage && !unlock) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (manage) {
            locks.manage(player, placed.get());
        } else {
            locks.promptUnlock(player, placed.get());
        }
    }

    private boolean isNuclear(PlacedTnt placed) {
        return core.definition(placed.tntId()).map(NuclearTnts::isNuclear).orElse(false);
    }

    private static boolean mainHandEmpty(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return item == null || item.getType().isAir();
    }
}
