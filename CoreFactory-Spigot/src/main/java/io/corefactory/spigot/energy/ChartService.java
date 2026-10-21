package io.corefactory.spigot.energy;

import io.corefactory.core.config.Messages;
import io.corefactory.core.energy.ChartHistory;
import io.corefactory.core.energy.ChartMode;
import io.corefactory.core.energy.GridResult;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.energy.Units;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.spigot.energy.StoredEnergy.StoredChart;
import io.corefactory.spigot.hologram.Hologram;
import io.corefactory.spigot.hologram.HologramService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The grid charts: a multi-line hologram one block above a player's head, which shows the satisfaction, the production or the storage
 * of one grid. A chart is saved in {@code energy.yml} by {@link EnergyService}; this service makes its hologram and the interaction
 * entity that a right-click on the chart uses to cycle its mode. A chart's text is refreshed every {@code refresh-ticks} ticks, and only
 * while a player is within {@link #VIEW_BLOCKS} blocks of it. The charts' history is kept in memory per grid, so it starts again
 * after a restart; the charts themselves and their grid ids are saved.
 */
public final class ChartService implements Listener {

    /** How close a player must be for a chart to refresh and to count as a viewer, in blocks. */
    public static final double VIEW_BLOCKS = 48.0;

    private static final NamespacedKey MARK = new NamespacedKey(ItemFactory.NAMESPACE, "energy_chart");
    private static final int ABOVE_HEAD_BLOCKS = 2;
    private static final double INTERACTION_WIDTH = 2.0;
    private static final double INTERACTION_HEIGHT = 3.0;

    private final EnergyService energy;
    private final HologramService holograms;
    private final Plugin plugin;
    private final Logger log;
    private final int perGridLimit;
    private final int totalLimit;
    private final long refreshTicks;
    private final Map<String, Hologram> texts = new HashMap<>();
    private final Map<String, UUID> interactions = new HashMap<>();
    private BukkitTask task;

    /**
     * @param energy       the energy service, which holds the charts and the grids
     * @param holograms    the hologram service
     * @param plugin       the plugin the refresh task runs under
     * @param perGridLimit the most charts one grid may have
     * @param totalLimit   the most charts the server may have
     * @param refreshTicks ticks between refreshes, at least 1
     * @param log          receives warnings
     */
    public ChartService(EnergyService energy, HologramService holograms, Plugin plugin, int perGridLimit, int totalLimit,
                        int refreshTicks, Logger log) {
        this.energy = energy;
        this.holograms = holograms;
        this.plugin = plugin;
        this.perGridLimit = Math.max(0, perGridLimit);
        this.totalLimit = Math.max(0, totalLimit);
        this.refreshTicks = Math.max(1, refreshTicks);
        this.log = log;
    }

    /** Removes the interaction entities an earlier run left, shows every chart, and starts the refresh. Call once, after the load. */
    public void start() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (entity.getPersistentDataContainer().has(MARK, PersistentDataType.STRING)) {
                        entity.remove();
                    }
                }
            }
        }
        for (StoredChart chart : energy.charts()) {
            spawn(chart);
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, refreshTicks, refreshTicks);
    }

    /** Stops the refresh and removes every chart's hologram and interaction. The charts stay saved. Called when the plugin is disabled. */
    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (String name : List.copyOf(texts.keySet())) {
            release(name);
        }
        for (String name : List.copyOf(interactions.keySet())) {
            removeInteraction(name);
        }
    }

    /** The most charts one grid may have. */
    public int perGridLimit() {
        return perGridLimit;
    }

    /** The most charts the server may have. */
    public int totalLimit() {
        return totalLimit;
    }

    /**
     * Makes a chart on the grid covering the player's block. The chart is one block above the player's head.
     *
     * @param player the player, who must stand inside a pylon's square
     * @param name   the name, checked against {@link ChartRules#validName(String)}
     * @return the message to show the player
     */
    public String create(Player player, String name) {
        if (!ChartRules.validName(name)) {
            return "&cA chart name is 1 to " + ChartRules.MAX_NAME_LENGTH + " letters, digits or underscores.";
        }
        if (energy.chart(name).isPresent()) {
            return "&cA chart named " + name + " already exists.";
        }
        if (player.getWorld() == null) {
            return "&cNo world.";
        }
        Pos feet = new Pos(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
        OptionalInt grid = energy.gridCovering(player.getWorld().getUID(), feet);
        if (grid.isEmpty()) {
            return "&cStand inside the square of a pylon of a grid to make a chart.";
        }
        ChartRules.Verdict verdict = ChartRules.canCreate(energy.chartsOn(grid.getAsInt()), energy.charts().size(),
                perGridLimit, totalLimit);
        switch (verdict) {
            case GRID_FULL -> {
                return "&cGrid #" + grid.getAsInt() + " already has " + perGridLimit + " chart"
                        + (perGridLimit == 1 ? "" : "s") + ".";
            }
            case SERVER_FULL -> {
                return "&cThe server already has " + totalLimit + " charts.";
            }
            case OK -> {
            }
        }
        Pos above = new Pos(feet.x(), feet.y() + ABOVE_HEAD_BLOCKS, feet.z());
        StoredChart chart = new StoredChart(name, player.getWorld().getUID(), above, grid.getAsInt(), ChartMode.SATISFACTION);
        energy.addChart(chart);
        spawn(chart);
        return "&aChart " + name + " made on grid #" + grid.getAsInt() + ".";
    }

    /**
     * Removes a chart with its hologram and interaction.
     *
     * @return the message to show
     */
    public String remove(String name) {
        Optional<StoredChart> removed = energy.removeChart(name);
        if (removed.isEmpty()) {
            return "&cNo chart named " + name + ".";
        }
        release(name);
        removeInteraction(name);
        return "&7Chart " + name + " removed.";
    }

    /**
     * Sets the mode of a chart at once.
     *
     * @return the message to show
     */
    public String setMode(String name, ChartMode mode) {
        Optional<StoredChart> changed = energy.setChartMode(name, mode);
        if (changed.isEmpty()) {
            return "&cNo chart named " + name + ".";
        }
        update(changed.get());
        return "&7Chart " + name + " shows " + ChartRules.word(mode) + ".";
    }

    /** The charts as lines for the list command: name, grid, mode and the players who can see each. */
    public List<String> list() {
        List<String> lines = new ArrayList<>();
        for (StoredChart chart : energy.charts()) {
            lines.add("&f" + chart.name() + " &7grid #" + chart.grid() + " " + ChartRules.word(chart.mode())
                    + " &8" + viewers(chart) + " viewing");
        }
        return lines;
    }

    /** The lines of the info command: the grid of the player's block, or a note when no grid covers it. */
    public List<String> info(Player player) {
        OptionalInt grid = player.getWorld() == null ? OptionalInt.empty() : energy.gridCovering(player.getWorld().getUID(),
                new Pos(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ()));
        if (grid.isEmpty()) {
            return List.of("&cNo grid covers your block.");
        }
        int id = grid.getAsInt();
        Optional<GridResult> reading = energy.grid(id);
        Optional<ChartSamples.Storage> stored = energy.storage(id);
        if (reading.isEmpty() || stored.isEmpty()) {
            return List.of("&7Grid #" + id + " &8(" + GridNames.colourName(id) + ") has no reading yet.");
        }
        GridResult result = reading.get();
        return List.of(
                "&7Grid #" + id + " &f" + GridNames.colourName(id),
                "&7Voltage &f" + Units.voltageText(result.voltageV()) + "  &7Current &f"
                        + Units.currentText(result.currentA()),
                "&7Supply &f" + Units.powerText(result.grid().supplyW()) + "  &7Capacity &f"
                        + Units.powerText(result.grid().capacityW()),
                "&7Satisfaction &f" + Units.percentText(result.satisfaction()),
                "&7Stored &f" + Units.energyText(stored.get().storedWh()) + " &7of "
                        + Units.energyText(stored.get().capacityWh()));
    }

    /** A right-click on a chart's interaction cycles its mode: satisfaction, production, storage, then satisfaction again. */
    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        String name = nameOf(event.getRightClicked().getUniqueId());
        if (name == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission("corefactory.grid")) {
            player.sendMessage(Messages.color("&cYou do not have permission to change a chart."));
            return;
        }
        energy.chart(name).ifPresent(chart -> {
            ChartMode next = ChartRules.next(chart.mode());
            Optional<StoredChart> changed = energy.setChartMode(name, next);
            changed.ifPresent(this::update);
            player.sendMessage(Messages.color("&7Chart " + name + " shows &f" + ChartRules.word(next) + "&7."));
        });
    }

    /** Refreshes every chart a player is near enough to see, and makes what is missing. */
    private void refresh() {
        for (StoredChart chart : energy.charts()) {
            World world = Bukkit.getWorld(chart.world());
            if (world == null) {
                continue;
            }
            if (!interactions.containsKey(chart.name()) && world.isChunkLoaded(chart.pos().x() >> 4, chart.pos().z() >> 4)) {
                spawnInteraction(chart, world);
            }
            if (viewers(chart) == 0) {
                continue;
            }
            update(chart);
        }
    }

    /** Makes the chart's hologram, if it has none, and the interaction, and writes its text. */
    private void spawn(StoredChart chart) {
        World world = Bukkit.getWorld(chart.world());
        if (world == null) {
            return;
        }
        if (!texts.containsKey(chart.name())) {
            holograms.create(topOf(chart), text(chart)).ifPresent(hologram -> texts.put(chart.name(), hologram));
        }
        if (world.isChunkLoaded(chart.pos().x() >> 4, chart.pos().z() >> 4) && !interactions.containsKey(chart.name())) {
            spawnInteraction(chart, world);
        }
    }

    private void update(StoredChart chart) {
        Hologram hologram = texts.get(chart.name());
        if (hologram == null) {
            spawn(chart);
            return;
        }
        hologram.setLines(text(chart));
    }

    private List<String> text(StoredChart chart) {
        return ChartText.lines(chart.name(), chart.mode(), energy.samples(chart.grid()), energy.liveSample(chart.grid())
                .orElse(null));
    }

    private void spawnInteraction(StoredChart chart, World world) {
        Pos pos = chart.pos();
        Location at = new Location(world, pos.x() + 0.5, pos.y(), pos.z() + 0.5);
        try {
            Interaction interaction = world.spawn(at, Interaction.class, entity -> {
                entity.setInteractionWidth((float) INTERACTION_WIDTH);
                entity.setInteractionHeight((float) INTERACTION_HEIGHT);
                entity.setResponsive(true);
                entity.setPersistent(true);
                entity.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, chart.name());
            });
            interactions.put(chart.name(), interaction.getUniqueId());
        } catch (RuntimeException e) {
            log.warning("The interaction of chart " + chart.name() + " could not be made: " + e.getMessage());
        }
    }

    private void release(String name) {
        Hologram hologram = texts.remove(name);
        if (hologram != null) {
            holograms.remove(hologram);
        }
    }

    private void removeInteraction(String name) {
        UUID id = interactions.remove(name);
        if (id == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(id);
        if (entity != null) {
            entity.remove();
        }
    }

    private String nameOf(UUID entity) {
        for (Map.Entry<String, UUID> entry : interactions.entrySet()) {
            if (entry.getValue().equals(entity)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private int viewers(StoredChart chart) {
        World world = Bukkit.getWorld(chart.world());
        if (world == null) {
            return 0;
        }
        Location at = topOf(chart);
        int count = 0;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(at) <= VIEW_BLOCKS) {
                count++;
            }
        }
        return count;
    }

    /** The point the top line stands at: the chart's block is where its bottom line sits, and the lines go down from the top. */
    private Location topOf(StoredChart chart) {
        World world = Bukkit.getWorld(chart.world());
        Pos pos = chart.pos();
        double top = pos.y() + 0.5 + (ChartText.LINES - 1) * Hologram.LINE_SPACING;
        return new Location(world, pos.x() + 0.5, top, pos.z() + 0.5);
    }
}
