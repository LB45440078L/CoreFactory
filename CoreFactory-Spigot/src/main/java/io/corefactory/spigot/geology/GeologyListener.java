package io.corefactory.spigot.geology;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositSettings;
import io.corefactory.core.deposit.DepositStore;
import io.corefactory.core.deposit.PatchBlocks;
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
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Connects the placed ore blocks and the deposits' patch blocks to the game. Mining a registered host block gives the ore's
 * raw items instead of the vanilla drop, or the ore block item when the tool has silk touch. Placing an ore block item
 * registers the block, and explosions remove the blocks they destroy from the registry.
 *
 * <p>A patch block is a host block of a deposit's ore, laid into the world by {@link DepositGenerator} (see
 * {@link PatchBlocks}). Mining one by hand takes one item from its deposit and gives the ore's component; the vanilla drop is
 * cancelled and the block breaks as usual. The take happens only when the break is not cancelled by a later plugin. A host
 * block that a player, a piston or a dispenser puts on a patch site is excluded (see {@link PatchExclusions}), so it gives
 * nothing. An explosion removes a patch block with no drop and no change to its deposit.
 */
public final class GeologyListener implements Listener {

    private static final Logger LOG = Logger.getLogger(GeologyListener.class.getName());

    private final CoreFactoryCore core;
    private final GeologyWorker worker;
    private final DepositStore deposits;
    private final DepositSettings settings;
    private final AchievementTracker achievements;
    private final Visuals visuals;
    private final Set<Material> hosts;
    private final Map<String, OreDefinition> oreById;
    private PendingPatch pending;

    /** A patch block whose break is not cancelled yet, with the deposit and the ore it gives. */
    private record PendingPatch(BlockBreakEvent event, long deposit, OreDefinition ore) {
    }

    /**
     * @param core         the core, which creates the ore items and the components
     * @param worker       the ores of the server and their host blocks
     * @param deposits     where the deposits are kept; a patch block takes one unit from its deposit
     * @param settings     the ore block density and the largest radius, which bounds the deposit lookup
     * @param achievements receives each ore mined by hand
     * @param visuals      plays the break effect
     */
    public GeologyListener(CoreFactoryCore core, GeologyWorker worker, DepositStore deposits, DepositSettings settings,
                           AchievementTracker achievements, Visuals visuals) {
        this.core = core;
        this.worker = worker;
        this.deposits = deposits;
        this.settings = settings;
        this.achievements = achievements;
        this.visuals = visuals;
        this.hosts = EnumSet.noneOf(Material.class);
        this.oreById = new HashMap<>();
        List<OreDefinition> ores = worker.service().ores();
        for (int index = 0; index < ores.size(); index++) {
            Material host = worker.host(index);
            if (host != null) {
                hosts.add(host);
            }
            oreById.putIfAbsent(ores.get(index).id(), ores.get(index));
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

    /** A host block a player places on a patch site is excluded, so breaking it gives no ore. */
    @EventHandler(ignoreCancelled = true)
    public void onHostPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        excludeSite(block, block.getType());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block moved : event.getBlocks()) {
            if (hosts.contains(moved.getType())) {
                excludeSite(moved.getRelative(event.getDirection()), moved.getType());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block moved : event.getBlocks()) {
            if (hosts.contains(moved.getType())) {
                excludeSite(moved.getRelative(event.getDirection().getOppositeFace()), moved.getType());
            }
        }
    }

    /** A host block a dispenser puts into air is excluded, as a placement by a player is. */
    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        Material material = event.getItem().getType();
        if (!hosts.contains(material)) {
            return;
        }
        if (!(event.getBlock().getBlockData() instanceof Directional facing)) {
            return;
        }
        Block target = event.getBlock().getRelative(facing.getFacing());
        if (target.isEmpty()) {
            excludeSite(target, material);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        pending = null;
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        int[] registry = OreChunks.registry(chunk);
        int index = GeologyRegistry.oreIndex(registry, x, y, z);
        if (index < 0) {
            planPatchBreak(event, block);
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

    /**
     * Decides whether a broken block that is not a registered ore is a patch block. When it is, the vanilla drop is
     * cancelled here, and the deposit is taken at monitor priority by {@link #givePatchOre}, so that a later cancel by
     * another plugin takes nothing. A store that cannot be read leaves the block as vanilla, which gives no ore.
     */
    private void planPatchBreak(BlockBreakEvent event, Block block) {
        Material material = block.getType();
        if (!hosts.contains(material)) {
            return;
        }
        Chunk chunk = block.getChunk();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        if (PatchExclusions.pack(x, y, z) < 0) {
            return;
        }
        List<Deposit> found;
        try {
            found = deposits.overlapping(block.getWorld().getName(), x, z, settings.maxRadius());
        } catch (RuntimeException e) {
            LOG.warning("Could not search the deposits at " + x + " " + y + " " + z + ": " + e.getMessage());
            return;
        }
        Optional<Deposit> site = PatchBlocks.siteOf(found, oreById::get, material.name(), x, y, z,
                settings.oreBlockDensity());
        if (site.isEmpty() || PatchExclusions.contains(chunk, x, y, z)) {
            return;
        }
        event.setDropItems(false);
        pending = new PendingPatch(event, site.get().id(), oreById.get(site.get().ore()));
    }

    /**
     * Gives the ore of a patch block that broke. The item is made first, so a missing component takes nothing; then one
     * unit is taken from the deposit, and the item is given, with any overflow dropped at the player. An empty deposit
     * gives nothing, and the block is gone either way.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void givePatchOre(BlockBreakEvent event) {
        PendingPatch patch = pending;
        if (patch == null || patch.event() != event) {
            return;
        }
        pending = null;
        Optional<ItemStack> item = core.createComponentItem(patch.ore().drop(), 1);
        if (item.isEmpty()) {
            return;
        }
        long taken;
        try {
            taken = deposits.take(patch.deposit(), 1);
        } catch (RuntimeException e) {
            LOG.warning("Could not take from deposit " + patch.deposit() + ": " + e.getMessage());
            return;
        }
        if (taken <= 0) {
            return;
        }
        Player player = event.getPlayer();
        for (ItemStack left : player.getInventory().addItem(item.get()).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
        visuals.play("ore_break", event.getBlock().getLocation().add(0.5, 0.5, 0.5), 1.0);
        achievements.mined(player, patch.ore().id());
    }

    /**
     * Excludes a position from being a patch block when a block of the material would be there and the position is a patch
     * site. A failed lookup excludes it too, so the record errs on the side of no ore.
     *
     * @param block    the position the block is at, or will be at
     * @param material the material that is or will be there
     */
    private void excludeSite(Block block, Material material) {
        if (!hosts.contains(material)) {
            return;
        }
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        if (PatchExclusions.pack(x, y, z) < 0) {
            return;
        }
        boolean site;
        try {
            site = PatchBlocks.siteOf(deposits.overlapping(block.getWorld().getName(), x, z, settings.maxRadius()),
                    oreById::get, material.name(), x, y, z, settings.oreBlockDensity()).isPresent();
        } catch (RuntimeException e) {
            LOG.warning("Could not search the deposits at " + x + " " + y + " " + z + ": " + e.getMessage());
            site = true;
        }
        if (site) {
            PatchExclusions.add(block.getChunk(), x, y, z);
        }
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
