package io.corefactory.spigot.geology;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.drill.DrillRules;
import io.corefactory.core.engine.Visuals;
import io.corefactory.core.geology.DepositVolumes;
import io.corefactory.core.geology.GeologyRegistry;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.geology.Placement;
import io.corefactory.spigot.achievement.AchievementTracker;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Connects the ore veins to the game. A chunk that generates is queued for its veins; a chunk that loads from disk is
 * left alone (only retrofit changes it). Mining a registered host block gives the ore's raw items instead of the
 * vanilla drop, or the ore block item when the tool has silk touch. Placing an ore block item registers the block, and
 * explosions remove the blocks they destroy from the registry.
 */
public final class GeologyListener implements Listener {

    private final CoreFactoryCore core;
    private final GeologyWorker worker;
    private final AchievementTracker achievements;
    private final Visuals visuals;

    public GeologyListener(CoreFactoryCore core, GeologyWorker worker, AchievementTracker achievements,
                           Visuals visuals) {
        this.core = core;
        this.worker = worker;
        this.achievements = achievements;
        this.visuals = visuals;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) {
            Chunk chunk = event.getChunk();
            worker.enqueue(chunk.getWorld(), chunk.getX(), chunk.getZ(), false);
        }
    }

    /**
     * Registers a placed ore block item as its ore. The vanilla placement already put the host block down; it is set
     * again only when the type differs. An unknown ore id, or a height the registry cannot store, cancels the placement.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Optional<String> oreId = core.oreIdOf(event.getItemInHand());
        if (oreId.isEmpty()) {
            return;
        }
        Block block = event.getBlock();
        int index = worker.service().indexOf(oreId.get());
        Material host = index < 0 ? null : worker.host(index);
        if (host == null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(core.messages().get("unknown-id", "id", oreId.get()));
            return;
        }
        if (block.getY() < OreDefinition.MIN_Y || block.getY() > OreDefinition.MAX_Y) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Messages.color("&cOres can only be placed from y " + OreDefinition.MIN_Y
                    + " to y " + OreDefinition.MAX_Y + "."));
            return;
        }
        if (block.getType() != host) {
            block.setType(host, false);
        }
        Chunk chunk = block.getChunk();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        int[] registry = GeologyRegistry.remove(OreChunks.registry(chunk), x, y, z);
        OreChunks.storeRegistry(chunk, GeologyRegistry.merge(registry, List.of(new Placement(x, y, z, index))));
        if (oreId.get().equals(DrillRules.DEPOSIT)) {
            OreChunks.storeVolumes(chunk, DepositVolumes.remove(OreChunks.volumes(chunk), x, y, z));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        int[] registry = OreChunks.registry(chunk);
        if (registry == null) {
            return;
        }
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        int index = GeologyRegistry.oreIndex(registry, x, y, z);
        if (index < 0) {
            return;
        }
        if (!worker.hostMatches(index, block.getType())) {
            OreChunks.storeRegistry(chunk, GeologyRegistry.remove(registry, x, y, z));
            return;
        }
        OreDefinition ore = worker.service().ore(index);
        event.setCancelled(true);
        block.setType(Material.AIR, false);
        OreChunks.storeRegistry(chunk, GeologyRegistry.remove(registry, x, y, z));
        Optional<ItemStack> oreItem = silkTouch(event.getPlayer())
                ? core.createOreItem(ore.id(), 1) : Optional.empty();
        if (oreItem.isPresent()) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), oreItem.get());
        } else {
            int amount = ThreadLocalRandom.current().nextInt(ore.dropMin(), ore.dropMax() + 1);
            worker.dropFor(block, ore, amount);
        }
        visuals.play("ore_break", block.getLocation().add(0.5, 0.5, 0.5), 1.0);
        achievements.mined(event.getPlayer(), ore.id());
    }

    private static boolean silkTouch(Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        return tool != null && tool.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        forget(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        forget(event.blockList());
    }

    /** Drops the destroyed blocks from their chunks' registries. The blocks are destroyed as usual, without a drop. */
    private static void forget(List<Block> destroyed) {
        for (Block block : destroyed) {
            Chunk chunk = block.getChunk();
            int[] registry = OreChunks.registry(chunk);
            if (registry == null) {
                continue;
            }
            int[] after = GeologyRegistry.remove(registry, block.getX(), block.getY(), block.getZ());
            if (after != registry) {
                OreChunks.storeRegistry(chunk, after);
            }
        }
    }
}
