package io.corefactory.spigot.machine;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.ClientEffects;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.spigot.machine.ConcealmentRules.Decision;
import io.corefactory.spigot.machine.ConcealmentRules.Viewer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hides the vanilla barrel under each lab tool and drill from the players near it, so they see the custom model instead. The
 * server keeps the barrel: only the client of a nearby player is sent air at the host block, with the same block-change packets
 * the beacon effect uses, and the real block is sent back when the player leaves range, changes world, quits or the machine goes.
 *
 * <p>Each machine also gets one invisible Interaction entity over its host block, so a client that sees air can still click the
 * machine. The entity is found again through its id on the machine, and a leftover one from an earlier run is removed when its
 * chunk loads. The pass that re-sends the air runs every {@link #PASS_TICKS} ticks, which also covers chunks the client receives
 * again and the client's own corrections. A machine is concealed only when it is settled, loaded, and its host block is still its
 * host material.
 *
 * <p>Everything runs on the main thread. When the concealment is off, nothing is hidden and no entity is made; the leftover
 * entities are still removed on chunk load.
 */
public final class BarrelConcealment implements Listener {

    /** Ticks between two passes. Every concealed view is sent again on each pass. */
    static final long PASS_TICKS = 40L;

    private static final NamespacedKey INTERACTION_MARK = new NamespacedKey(ItemFactory.NAMESPACE, "machine_interaction");
    private static final BlockData AIR = Material.AIR.createBlockData();

    private final Plugin plugin;
    private final ClientEffects effects;
    private final MachineStore store;
    private final boolean enabled;
    private final double range;
    private final Map<PlacedMachine, Set<UUID>> concealed = new HashMap<>();
    private BukkitTask task;

    /**
     * @param plugin  the plugin whose scheduler runs the passes
     * @param effects the block views of the players, from the PacketEvents client effects
     * @param store   the registry of machines
     * @param enabled false to hide nothing and make no interaction entities
     * @param range   the range in blocks from a machine's host block within which a player sees it concealed
     */
    public BarrelConcealment(Plugin plugin, ClientEffects effects, MachineStore store, boolean enabled, double range) {
        this.plugin = plugin;
        this.effects = effects;
        this.store = store;
        this.enabled = enabled;
        this.range = range;
    }

    /**
     * Removes the leftover interaction entities of every loaded chunk, starts the passes when concealment is on, and hooks the
     * removal of machines. Call once, after the machine store has loaded.
     */
    public void start() {
        store.onRemoved(this::detach);
        sweepLoaded();
        if (enabled && task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::pass, 1L, PASS_TICKS);
        }
    }

    /** Stops the passes, shows every concealed barrel again and removes the interaction entities. */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (PlacedMachine placed : store.all()) {
            release(placed);
            removeInteraction(placed);
        }
        concealed.clear();
        sweepLoaded();
    }

    /** A player who logs off sees nothing more, so they are no longer concealed from any machine. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        concealed.values().forEach(viewers -> viewers.remove(id));
        concealed.values().removeIf(Set::isEmpty);
    }

    /** A new world has its own blocks, so the views are decided again once the player is in it. */
    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        passNextTick();
    }

    /** A teleport can take a player into or out of range, so the views are decided again once it has happened. */
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        passNextTick();
    }

    /** Removes the interaction entities a previous run left in the chunk. */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        sweep(event.getChunk());
    }

    /**
     * A chunk that unloads takes its machines' interactions with it, and the client drops the chunk too, so the views are
     * forgotten without a packet.
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        for (PlacedMachine placed : store.all()) {
            if (inChunk(placed, chunk)) {
                concealed.remove(placed);
                removeInteraction(placed);
            }
        }
        sweep(chunk);
    }

    private void passNextTick() {
        Bukkit.getScheduler().runTask(plugin, this::pass);
    }

    /**
     * Makes the views of every machine match the players online: new viewers are concealed, viewers in range are refreshed, and
     * viewers out of range or gone are restored. Each player's changes are sent as one packet of block changes.
     */
    private void pass() {
        if (!enabled) {
            return;
        }
        Map<UUID, Player> online = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            online.put(player.getUniqueId(), player);
        }
        Map<UUID, BlockChangeSet> hide = new HashMap<>();
        Map<UUID, BlockChangeSet> reveal = new HashMap<>();
        for (PlacedMachine placed : store.all()) {
            if (!eligible(placed)) {
                release(placed);
                continue;
            }
            if (placed.interaction() == null || Bukkit.getEntity(placed.interaction()) == null) {
                attach(placed);
            }
            UUID world = placed.block().getWorld().getUID();
            Location centre = placed.block().clone().add(0.5, 0.5, 0.5);
            List<Viewer> viewers = new ArrayList<>();
            for (Player player : online.values()) {
                viewers.add(new Viewer(player.getUniqueId(), player.getWorld().getUID(), distanceSquared(player, centre)));
            }
            Set<UUID> before = concealed.getOrDefault(placed, Set.of());
            Decision decision = ConcealmentRules.decide(world, range, before, viewers);
            Set<UUID> now = new HashSet<>(before);
            Block block = placed.block().getBlock();
            for (UUID id : decision.restore()) {
                now.remove(id);
                Player player = online.get(id);
                if (player != null && player.getWorld().getUID().equals(world)) {
                    queue(reveal, id, block, block.getBlockData());
                }
            }
            for (UUID id : union(decision.conceal(), decision.refresh())) {
                now.add(id);
                if (online.containsKey(id)) {
                    queue(hide, id, block, AIR);
                }
            }
            if (now.isEmpty()) {
                concealed.remove(placed);
            } else {
                concealed.put(placed, now);
            }
        }
        for (Map.Entry<UUID, BlockChangeSet> entry : reveal.entrySet()) {
            Player player = online.get(entry.getKey());
            effects.restoreBlocks(player, player.getWorld(), entry.getValue());
        }
        for (Map.Entry<UUID, BlockChangeSet> entry : hide.entrySet()) {
            Player player = online.get(entry.getKey());
            effects.showBlocks(player, player.getWorld(), entry.getValue());
        }
    }

    /** The machine's host block is a real registered host, loaded and settled, and still its host material. */
    private boolean eligible(PlacedMachine placed) {
        return enabled && placed.settled() && placed.isLoaded() && placed.host() != null
                && placed.block().getBlock().getType() == placed.host();
    }

    /** Shows the real block to every player who sees it hidden, and forgets them. */
    private void release(PlacedMachine placed) {
        Set<UUID> viewers = concealed.remove(placed);
        if (viewers == null || viewers.isEmpty() || !placed.isLoaded()) {
            return;
        }
        World world = placed.block().getWorld();
        Block block = placed.block().getBlock();
        BlockChangeSet real = new BlockChangeSet(1);
        real.set(block.getX(), block.getY(), block.getZ(), block.getBlockData());
        for (UUID id : viewers) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.getWorld().equals(world)) {
                effects.restoreBlocks(player, world, real);
            }
        }
    }

    /** Removes a machine from the views and its interaction. Called when the machine leaves the store. */
    private void detach(PlacedMachine placed) {
        release(placed);
        removeInteraction(placed);
    }

    /** Makes the machine's interaction entity over its host block, and marks it so a leftover one can be found. */
    private void attach(PlacedMachine placed) {
        Location spot = placed.block().clone().add(0.5, 0.0, 0.5);
        String mark = placed.block().getBlockX() + " " + placed.block().getBlockY() + " " + placed.block().getBlockZ();
        Interaction interaction = spot.getWorld().spawn(spot, Interaction.class, entity -> {
            entity.setInteractionWidth(1.0f);
            entity.setInteractionHeight((float) Footprint.heightOf(placed.machine().id()));
            entity.setResponsive(true);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(INTERACTION_MARK, PersistentDataType.STRING, mark);
        });
        placed.setInteraction(interaction.getUniqueId());
    }

    private static void removeInteraction(PlacedMachine placed) {
        if (placed.interaction() == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(placed.interaction());
        if (entity != null) {
            entity.remove();
        }
        placed.setInteraction(null);
    }

    /** Removes every interaction entity of this plugin in the chunk. Each one is marked, so none of them is anything else's. */
    private static void sweep(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Interaction && entity.getPersistentDataContainer().has(INTERACTION_MARK, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }

    private void sweepLoaded() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                sweep(chunk);
            }
        }
    }

    private static boolean inChunk(PlacedMachine placed, Chunk chunk) {
        Location at = placed.block();
        return at.getWorld() != null && at.getWorld().equals(chunk.getWorld())
                && at.getBlockX() >> 4 == chunk.getX() && at.getBlockZ() >> 4 == chunk.getZ();
    }

    private static double distanceSquared(Player player, Location centre) {
        if (!player.getWorld().equals(centre.getWorld())) {
            return Double.POSITIVE_INFINITY;
        }
        return player.getLocation().distanceSquared(centre);
    }

    private static Set<UUID> union(Set<UUID> first, Set<UUID> second) {
        Set<UUID> all = new HashSet<>(first);
        all.addAll(second);
        return all;
    }

    /** Queues one block write for a player, in the player's own batch. */
    private static void queue(Map<UUID, BlockChangeSet> batches, UUID id, Block block, BlockData data) {
        batches.computeIfAbsent(id, key -> new BlockChangeSet(16)).set(block.getX(), block.getY(), block.getZ(), data);
    }
}
