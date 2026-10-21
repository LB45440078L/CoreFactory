package io.corefactory.spigot.survey;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.geology.BlockSampler;
import io.corefactory.core.geology.GeologyService;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.geology.Placement;
import io.corefactory.core.survey.ChunkSurvey;
import io.corefactory.spigot.geology.GeologyWorker;
import io.corefactory.spigot.geology.OreChunks;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Right-click with the Ore Density Scanner: a filled map of the chunk the player stands in, showing the planned ore of
 * each column and how dense it is, and a chat summary of every ore planned there. Each use makes a new map, so earlier
 * maps stay valid.
 */
public final class ScannerListener implements Listener {

    /** The component id of the Ore Density Scanner, from materials.yml. */
    public static final String SCANNER = "ore_density_scanner";

    private final CoreFactoryCore core;
    private final GeologyWorker worker;
    private final Map<UUID, Long> lastScan = new HashMap<>();

    public ScannerListener(CoreFactoryCore core, GeologyWorker worker) {
        this.core = core;
        this.worker = worker;
    }

    /**
     * Scans on a main-hand right-click in air or on a block. A click can arrive twice in one tick (once for the block,
     * once for the air), so a player's second scan in the same tick is ignored.
     */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!SCANNER.equals(core.items().componentId(event.getItem()).orElse(null))) {
            return;
        }
        Player player = event.getPlayer();
        long tick = core.currentTick();
        Long previous = lastScan.put(player.getUniqueId(), tick);
        if (previous != null && previous == tick) {
            return;
        }
        scan(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastScan.remove(event.getPlayer().getUniqueId());
    }

    private void scan(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        GeologyService service = worker.service();
        // Runs on the main thread, once per use, against the loaded chunk. The planner asks for only the blocks its
        // veins' random walks reach, not every block of the chunk. Including hosts keeps the ore already laid here in
        // the plan, so the still-in-the-ground count can be taken from the registry.
        List<Placement> planned = service.plan(world.getSeed(), chunkX, chunkZ, sampler(world), true);
        ChunkSurvey.Survey survey = ChunkSurvey.survey(chunkX, chunkZ, planned, service.ores());
        Map<Integer, Integer> stillInGround = ChunkSurvey.stillInGround(planned, OreChunks.registry(chunk), chunkX,
                chunkZ);
        int[] oreRgb = new int[service.ores().size()];
        for (int index = 0; index < oreRgb.length; index++) {
            oreRgb[index] = OreColours.of(service.ore(index).id());
        }
        Map<Integer, ItemStack> left = player.getInventory().addItem(mapItem(world, survey, oreRgb));
        for (ItemStack rest : left.values()) {
            world.dropItemNaturally(player.getLocation(), rest);
        }
        report(player, survey, stillInGround);
    }

    private static ItemStack mapItem(World world, ChunkSurvey.Survey survey, int[] oreRgb) {
        MapView view = Bukkit.createMap(world);
        for (MapRenderer existing : List.copyOf(view.getRenderers())) {
            view.removeRenderer(existing);
        }
        view.setScale(MapView.Scale.NORMAL);
        view.setCenterX(SurveyMap.centre(survey.chunkX()));
        view.setCenterZ(SurveyMap.centre(survey.chunkZ()));
        view.setTrackingPosition(false);
        view.setLocked(true);
        view.addRenderer(new SurveyRenderer(survey, oreRgb));
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        if (item.getItemMeta() instanceof MapMeta meta) {
            meta.setMapView(view);
            meta.setDisplayName("Chunk survey (" + survey.chunkX() + ", " + survey.chunkZ() + ")");
            item.setItemMeta(meta);
        }
        return item;
    }

    private void report(Player player, ChunkSurvey.Survey survey, Map<Integer, Integer> stillInGround) {
        player.sendMessage(Messages.color("&6Chunk survey (" + survey.chunkX() + ", " + survey.chunkZ() + ") &7- &f"
                + survey.placements() + " &7planned blocks"));
        if (survey.totals().isEmpty()) {
            player.sendMessage(Messages.color("&7No special ore is planned in this chunk."));
            return;
        }
        for (ChunkSurvey.OreTotal total : survey.totals()) {
            OreDefinition ore = worker.service().ore(total.ore());
            player.sendMessage(Messages.color("&8 - " + ore.name() + "&7: &f" + total.planned() + " &7planned, &f"
                    + stillInGround.getOrDefault(total.ore(), 0) + " &7still in the ground, y &f" + total.minY()
                    + " &7to &f" + total.maxY()));
        }
    }

    private static BlockSampler sampler(World world) {
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        return (x, y, z) -> y < minY || y >= maxY ? "AIR" : world.getBlockAt(x, y, z).getType().name();
    }
}
