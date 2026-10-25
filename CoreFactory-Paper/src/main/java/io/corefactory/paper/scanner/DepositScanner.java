package io.corefactory.paper.scanner;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositFilter;
import io.corefactory.core.deposit.DepositScan;
import io.corefactory.core.deposit.DepositSettings;
import io.corefactory.core.deposit.DepositStore;
import io.corefactory.core.deposit.DepositText;
import io.corefactory.core.deposit.ScannerSettings;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.text.Text;
import io.corefactory.paper.TickScheduler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Ore Density Scanner: a held tool that finds the deposits near its holder.
 *
 * <ul>
 *   <li>Right-click (in air or on a block) scans the deposits within the scanner radius, with the ore filter of the player. The
 *       action bar names the nearest one, and the chat lists up to five with their direction, distance, tier and share left. The
 *       nearest eight are outlined around the player for a while.</li>
 *   <li>Sneak and left-click surveys the same radius: the chat gives the exact centre and radius of each deposit found.</li>
 *   <li>Sneak and right-click moves the ore filter on: all ores, then each ore in turn, then all again.</li>
 * </ul>
 *
 * <p>A scan waits for the scanner cooldown, and the deposit map ({@code /cf deposits}) lists the same deposits as a menu, where a
 * click sets the player's waypoint. The filter and the cooldown are kept per player, in memory, and end when the player leaves.
 */
public final class DepositScanner implements Listener {

    /** The component id of the Ore Density Scanner, from materials.yml. */
    public static final String ITEM = "ore_density_scanner";

    /** The most deposits a scan finds or a survey lists, nearest first. */
    static final int LISTED = 10;

    /** The deposits the chat lists after a plain scan. */
    static final int CHAT_NEAREST = 5;

    /** The nearest deposits that a scan outlines. */
    static final int OUTLINED = 8;

    /** How long a scan's action bar line keeps the waypoint off the action bar, in ticks. */
    static final long ACTION_BAR_HOLD_TICKS = 60L;

    /** The most deposits the map lists: three pages of eighteen. */
    static final int MAP_LIMIT = 54;

    private final CoreFactoryCore core;
    private final DepositStore store;
    private final int maxRadius;
    private final ScannerSettings settings;
    private final WaypointService waypoints;
    private final DepositOutline outline;
    private final Map<UUID, String> filters = new HashMap<>();
    private final Map<UUID, Long> lastScan = new HashMap<>();

    /**
     * @param ticks           the scheduler that runs the waypoint and outline redraws
     * @param core            the core, for the items, the ores and the tick
     * @param store           the deposits
     * @param depositSettings the deposit settings, for the largest radius a deposit may have
     * @param settings        the scanner settings
     */
    public DepositScanner(TickScheduler ticks, CoreFactoryCore core, DepositStore store, DepositSettings depositSettings,
                          ScannerSettings settings) {
        this.core = core;
        this.store = store;
        this.maxRadius = depositSettings.maxRadius();
        this.settings = settings;
        this.waypoints = new WaypointService(ticks, core);
        this.outline = new DepositOutline(ticks);
    }

    /** Opens the deposit map for a player: the nearest deposits of the scanner's filter, with the waypoint on a click. */
    public void openMap(Player player) {
        DepositMap.open(this, player, 0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || !isScanner(event.getItem())) {
            return;
        }
        Action action = event.getAction();
        boolean right = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (right && player.isSneaking()) {
            cycleFilter(player);
        } else if (right) {
            scan(player, false);
        } else if (player.isSneaking()) {
            scan(player, true);
        }
    }

    /** A held scanner never breaks a block: its left-click is for the survey, never for mining. */
    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (isScanner(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        filters.remove(id);
        lastScan.remove(id);
        waypoints.clear(id);
        outline.cancel(id);
    }

    /** Runs one scan or survey for a player, unless the scanner is still cooling down. */
    void scan(Player player, boolean survey) {
        UUID id = player.getUniqueId();
        long now = core.currentTick();
        Long last = lastScan.get(id);
        if (last != null && now - last < settings.scanCooldownTicks()) {
            ActionBars.send(player, "&7Scanner recharging");
            return;
        }
        lastScan.put(id, now);
        int radius = settings.scannerRadius();
        Location at = player.getLocation();
        int x = at.getBlockX();
        int z = at.getBlockZ();
        String filter = filterOf(id);
        String label = filterLabel(filter);
        List<Deposit> near = nearby(player, filter, LISTED);
        waypoints.holdActionBar(id, now + ACTION_BAR_HOLD_TICKS);

        player.sendMessage(Text.parse("&6Deposits within &f" + radius + " &6blocks&7, filter: &f" + label));
        if (near.isEmpty()) {
            String none = filter.isEmpty() ? "No deposits within " + radius + " blocks"
                    : "No " + label + " deposits within " + radius + " blocks";
            ActionBars.send(player, "&7" + none);
            player.sendMessage(Text.parse("&7" + none + "."));
            outline.cancel(id);
            return;
        }
        Deposit nearest = near.get(0);
        ActionBars.send(player, "&7Filter: &f" + label + " &8| " + DepositText.actionLine(oreName(nearest.ore()), nearest, x, z));
        if (survey) {
            for (Deposit deposit : near) {
                player.sendMessage(Text.parse(DepositText.surveyLine(oreName(deposit.ore()), deposit)));
            }
            if (near.size() == LISTED) {
                player.sendMessage(Text.parse("&7Showing the nearest " + LISTED + "."));
            }
        } else {
            for (Deposit deposit : near.subList(0, Math.min(CHAT_NEAREST, near.size()))) {
                player.sendMessage(Text.parse(DepositText.listLine(oreName(deposit.ore()), deposit, x, z)));
            }
            if (near.size() > CHAT_NEAREST) {
                player.sendMessage(Text.parse("&8Sneak and left-click for the centre and radius of each."));
            }
        }
        List<Deposit> outlined = near.subList(0, Math.min(OUTLINED, near.size()));
        outline.show(player, outlined.stream()
                .map(deposit -> new DepositOutline.Ring(deposit.world(), deposit.centreX(), deposit.centreZ(), deposit.radius(),
                        DepositOutline.colourOf(oreName(deposit.ore()))))
                .toList(), settings.outlineSeconds());
    }

    /** Moves the player's ore filter on one step and says so on the action bar and in chat. */
    void cycleFilter(Player player) {
        UUID id = player.getUniqueId();
        String filter = DepositFilter.next(filterOf(id), core.ores().stream().map(OreDefinition::id).toList());
        filters.put(id, filter);
        String label = filterLabel(filter);
        ActionBars.send(player, "&7Scanner filter: &f" + label);
        player.sendMessage(Text.parse("&7Scanner filter set to &f" + label + "&7."));
    }

    /** Sets the player's waypoint to a deposit and says where it is. */
    void setWaypoint(Player player, Deposit deposit) {
        String name = oreName(deposit.ore());
        waypoints.set(player.getUniqueId(), deposit, name);
        Location at = player.getLocation();
        player.sendMessage(Text.parse("&6Waypoint set to &f" + name + "&6, " + DepositText.place(deposit, at.getBlockX(),
                at.getBlockZ()) + "&6. It lasts two minutes."));
    }

    /** Removes the player's waypoint, if they have one. */
    void clearWaypoint(Player player) {
        waypoints.clear(player.getUniqueId());
        player.sendMessage(Text.parse("&7Waypoint cleared."));
    }

    /** The deposits near a player that pass a filter, nearest first, at most {@code limit}. */
    List<Deposit> nearby(Player player, String filter, int limit) {
        Location at = player.getLocation();
        int radius = settings.scannerRadius();
        List<Deposit> found = store.overlapping(at.getWorld().getName(), at.getBlockX(), at.getBlockZ(),
                Math.max(radius, maxRadius));
        return DepositScan.nearest(found, at.getBlockX(), at.getBlockZ(), radius, filter, limit);
    }

    /** The filter a player has now: {@link DepositFilter#ALL} or an ore id. */
    String filterOf(UUID player) {
        return filters.getOrDefault(player, DepositFilter.ALL);
    }

    /** The name of a filter for display: {@code all ores} or the ore's name. */
    String filterLabel(String filter) {
        return filter.isEmpty() ? "all ores" : oreName(filter);
    }

    /** The ore's name with colour codes, or its id when no ore has that id. */
    String oreName(String id) {
        for (OreDefinition ore : core.ores()) {
            if (ore.id().equals(id)) {
                return ore.name();
            }
        }
        return id;
    }

    /** The radius of a scan, in blocks. */
    int radius() {
        return settings.scannerRadius();
    }

    /** The core, for the map's icons. */
    CoreFactoryCore core() {
        return core;
    }

    /** The waypoints, for the map. */
    WaypointService waypoints() {
        return waypoints;
    }

    private boolean isScanner(ItemStack item) {
        return item != null && core.items().componentId(item).filter(ITEM::equals).isPresent();
    }
}
