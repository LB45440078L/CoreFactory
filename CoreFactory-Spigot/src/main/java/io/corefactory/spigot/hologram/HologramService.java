package io.corefactory.spigot.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Owns the holograms and sends them to players, one viewer at a time. A hologram is a stack of lines, each its own armour
 * stand. A player sees a hologram while they are in its world and within the view distance. The viewers are checked every
 * 20 ticks, and the text is sent to them every 10 ticks. Everything runs on the main thread.
 *
 * <p>A packet that fails to send turns holograms off for the rest of the run, and logs the failure once. Nothing else in the
 * plugin depends on them.
 */
public final class HologramService implements Listener {

    /** The first entity id given out. Each new line takes the next id down, far above the ids the server gives out. */
    static final int FIRST_ENTITY_ID = Integer.MAX_VALUE / 2;

    private static final double HEIGHT_ABOVE_CENTRE = 1.6;
    private static final long VIEWER_TICKS = 20L;
    private static final long TEXT_TICKS = 10L;

    private final Plugin plugin;
    private final boolean configured;
    private final double viewDistanceSquared;
    private final List<Hologram> live = new ArrayList<>();
    private int nextEntityId = FIRST_ENTITY_ID;
    private boolean failed;
    private BukkitTask viewerTask;
    private BukkitTask textTask;

    /**
     * @param enabled      false to create no holograms
     * @param viewDistance the distance in blocks within which a player sees a hologram, at least 1
     */
    public HologramService(Plugin plugin, boolean enabled, double viewDistance) {
        this.plugin = plugin;
        this.configured = enabled;
        double distance = Math.max(1.0, viewDistance);
        this.viewDistanceSquared = distance * distance;
    }

    /** The point a machine's hologram stands at: 1.6 blocks above the centre of its host block. */
    public static Location spotAbove(Location block) {
        return block.clone().add(0.5, 0.5 + HEIGHT_ABOVE_CENTRE, 0.5);
    }

    /** True when holograms are configured on and no packet has failed. */
    public boolean enabled() {
        return configured && !failed;
    }

    /** Starts the repeating checks. Does nothing when holograms are off. */
    public void start() {
        if (!enabled() || viewerTask != null) {
            return;
        }
        viewerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkViewers, VIEWER_TICKS, VIEWER_TICKS);
        textTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sendText, TEXT_TICKS, TEXT_TICKS);
    }

    /**
     * Creates a hologram of one line per text, from the point down, and shows it to the players in range now. Empty when
     * holograms are off, the point has no world, or there are no lines.
     *
     * @param position the point the top line stands at
     * @param lines    the text of each line, top first
     */
    public Optional<Hologram> create(Location position, List<String> lines) {
        if (!enabled() || position.getWorld() == null || lines.isEmpty()) {
            return Optional.empty();
        }
        List<Integer> ids = new ArrayList<>();
        List<UUID> uuids = new ArrayList<>();
        for (int line = 0; line < lines.size(); line++) {
            ids.add(nextEntityId--);
            uuids.add(UUID.randomUUID());
        }
        Hologram hologram = new Hologram(ids, uuids, position.clone(), lines);
        live.add(hologram);
        refresh(hologram);
        return Optional.of(hologram);
    }

    /** Hides a hologram from everyone who sees it and drops it. Does nothing for one already removed. */
    public void remove(Hologram hologram) {
        if (!live.remove(hologram)) {
            return;
        }
        for (UUID viewer : Set.copyOf(hologram.viewers())) {
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) {
                guard(() -> hide(player, hologram));
            }
        }
        hologram.viewers().clear();
    }

    /** Stops the checks and removes every hologram. Called when the plugin is disabled. */
    public void shutdown() {
        cancelTasks();
        for (Hologram hologram : List.copyOf(live)) {
            remove(hologram);
        }
    }

    /** A player who logs off sees nothing more, so they are no longer a viewer of any hologram. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        for (Hologram hologram : live) {
            hologram.viewers().remove(id);
        }
    }

    private void checkViewers() {
        for (Hologram hologram : List.copyOf(live)) {
            refresh(hologram);
        }
    }

    private void refresh(Hologram hologram) {
        Set<UUID> near = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!inRange(hologram, player)) {
                continue;
            }
            near.add(player.getUniqueId());
            if (hologram.viewers().add(player.getUniqueId())) {
                guard(() -> show(player, hologram));
            }
        }
        for (UUID viewer : Set.copyOf(hologram.viewers())) {
            if (near.contains(viewer)) {
                continue;
            }
            hologram.viewers().remove(viewer);
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) {
                guard(() -> hide(player, hologram));
            }
        }
    }

    private void sendText() {
        for (Hologram hologram : List.copyOf(live)) {
            for (UUID viewer : Set.copyOf(hologram.viewers())) {
                Player player = Bukkit.getPlayer(viewer);
                if (player != null) {
                    guard(() -> updateText(player, hologram));
                }
            }
        }
    }

    private boolean inRange(Hologram hologram, Player player) {
        World world = hologram.world();
        Location where = player.getLocation();
        return world != null && world.equals(where.getWorld())
                && where.distanceSquared(hologram.position()) <= viewDistanceSquared;
    }

    /** Spawns every line of a hologram for a player, each with its text. */
    private static void show(Player player, Hologram hologram) {
        for (int line = 0; line < hologram.lineCount(); line++) {
            send(player, HologramPackets.spawn(hologram, line));
            send(player, HologramPackets.metadata(hologram, line));
        }
    }

    /** Sends the current text of every line to a player who already sees the hologram. */
    private static void updateText(Player player, Hologram hologram) {
        for (int line = 0; line < hologram.lineCount(); line++) {
            send(player, HologramPackets.metadata(hologram, line));
        }
    }

    /** Removes every line of a hologram from a player's client. */
    private static void hide(Player player, Hologram hologram) {
        for (int line = 0; line < hologram.lineCount(); line++) {
            send(player, HologramPackets.destroy(hologram, line));
        }
    }

    private static void send(Player player, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    /** Runs one packet action, unless holograms are already off. A failure turns them off for the run. */
    private void guard(Runnable action) {
        if (failed) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            cancelTasks();
            plugin.getLogger().log(Level.WARNING,
                    "Holograms could not send a packet, so they are off for the rest of this run.", e);
        }
    }

    private void cancelTasks() {
        if (viewerTask != null) {
            viewerTask.cancel();
            viewerTask = null;
        }
        if (textTask != null) {
            textTask.cancel();
            textTask = null;
        }
    }
}
