package io.corefactory.spigot.research;

import io.corefactory.api.PlacedTnt;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.research.ResearchService;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * Enforces the research gates. A gated item cannot be crafted, placed or lit until a player has finished the node that
 * unlocks it. It also loads each player's research when they join and forgets it when they leave.
 */
public final class ResearchGateListener implements Listener {

    private final CoreFactoryCore core;
    private final ResearchService research;

    public ResearchGateListener(CoreFactoryCore core, ResearchService research) {
        this.core = core;
        this.research = research;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        research.load(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        research.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        Optional<Player> player = firstPlayer(event.getViewers());
        if (result == null || player.isEmpty()) {
            return;
        }
        if (locked(player.get(), result)) {
            event.getInventory().setResult(null);
            notice(player.get(), result);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player && locked(player, event.getRecipe().getResult())) {
            event.setCancelled(true);
            notice(player, event.getRecipe().getResult());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (locked(player, event.getItemInHand())) {
            event.setCancelled(true);
            notice(player, event.getItemInHand());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Material held = event.getMaterial();
        if (held != Material.FLINT_AND_STEEL && held != Material.FIRE_CHARGE) {
            return;
        }
        Optional<PlacedTnt> placed = core.placedAt(event.getClickedBlock().getLocation());
        if (placed.isEmpty()) {
            return;
        }
        String id = placed.get().tntId();
        if (!research.isUnlocked(event.getPlayer().getUniqueId(), id)) {
            event.setCancelled(true);
            notice(event.getPlayer(), id);
        }
    }

    private boolean locked(Player player, ItemStack item) {
        return idOf(item).map(id -> !research.isUnlocked(player.getUniqueId(), id)).orElse(false);
    }

    private Optional<String> idOf(ItemStack item) {
        if (item == null) {
            return Optional.empty();
        }
        Optional<String> tnt = core.tntIdOf(item);
        return tnt.isPresent() ? tnt : core.items().componentId(item);
    }

    private void notice(Player player, ItemStack item) {
        idOf(item).ifPresent(id -> notice(player, id));
    }

    private void notice(Player player, String id) {
        research.nodeFor(player.getUniqueId(), id).ifPresent(node -> player.sendMessage(
                Messages.color("&cRequires research: " + node.name() + "&c. Finish the earlier nodes at a Research Console.")));
    }

    private static Optional<Player> firstPlayer(java.util.List<HumanEntity> viewers) {
        return viewers.stream().filter(Player.class::isInstance).map(Player.class::cast).findFirst();
    }
}
