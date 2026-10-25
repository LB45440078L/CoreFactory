package io.corefactory.paper.artillery;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.text.Text;
import io.corefactory.paper.chunks.ChunkKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Keeps the Command Centers by id, by block and by interaction, and owns their entities: the model's item display, the screen's text display,
 * the interaction over the screen that takes the clicks, and the map wall in front of the screen. The entities carry their own marker, so the
 * artillery's sweep never touches them. A center whose chunk unloads keeps its data, and its entity handles are forgotten until the chunk
 * loads again; its map wall is taken away, and is placed again when a target is set and the chunks are loaded.
 */
public final class CommandCenterRegistry {

    /** The marker on the entities of a command center. It is not the artillery's marker, so each registry sweeps only its own. */
    private static final NamespacedKey MARK = new NamespacedKey(ItemFactory.NAMESPACE, "command_center");

    private final CoreFactoryCore core;
    private final Logger log;
    private final int wallTiles;
    private final Map<String, CommandCenter> byId = new LinkedHashMap<>();
    private final Map<String, CommandCenter> byBlock = new HashMap<>();
    private final Map<UUID, CommandCenter> byInteraction = new HashMap<>();
    private final Map<String, MapWall> walls = new HashMap<>();
    private List<CommandCenter> pending = List.of();
    private boolean reportedMissingItem;

    /**
     * @param core      the core, for the items and the display scale
     * @param log       receives the warnings
     * @param wallTiles the side of the map walls, in item frames
     */
    public CommandCenterRegistry(CoreFactoryCore core, Logger log, int wallTiles) {
        this.core = core;
        this.log = log;
        this.wallTiles = wallTiles;
    }

    /**
     * Reads the saved centers. Those in a loaded world are added; the rest wait.
     *
     * @param saved the saved centers
     */
    public void load(List<CommandCenter> saved) {
        pending = List.copyOf(saved);
        materializePending();
    }

    /** Adds the pending centers whose world has loaded since. */
    public void materializePending() {
        List<CommandCenter> waiting = new ArrayList<>();
        for (CommandCenter center : pending) {
            if (Bukkit.getWorld(center.world()) == null) {
                waiting.add(center);
            } else if (!byId.containsKey(center.id())) {
                addCenter(center);
            }
        }
        pending = List.copyOf(waiting);
    }

    /**
     * Registers a new center and shows it, when its chunk is loaded.
     *
     * @return the center
     */
    public CommandCenter add(CommandCenter center) {
        addCenter(center);
        spawnEntities(center);
        return center;
    }

    /** Removes a center from the registry and removes its entities. The caller drops the item it gives back. */
    public void remove(CommandCenter center) {
        byId.remove(center.id());
        byBlock.remove(key(center.world(), center.pos().x(), center.pos().y(), center.pos().z()));
        destroyEntities(center);
    }

    /** Every center, in the order they were added. */
    public List<CommandCenter> all() {
        return List.copyOf(byId.values());
    }

    /** The center with the given id, if any. */
    public Optional<CommandCenter> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** The center standing on a block, if any. */
    public Optional<CommandCenter> at(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byBlock.get(key(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ())));
    }

    /** The center whose interaction entity this is, if any. */
    public Optional<CommandCenter> byInteraction(UUID entity) {
        return Optional.ofNullable(byInteraction.get(entity));
    }

    /** True when the center's world and chunk are loaded, so it can act and its entities exist. */
    public boolean isLoaded(CommandCenter center) {
        World world = Bukkit.getWorld(center.world());
        return world != null && world.isChunkLoaded(center.pos().x() >> 4, center.pos().z() >> 4);
    }

    /** The block a center stands on, or null when its world is not loaded. */
    public Location blockOf(CommandCenter center) {
        World world = Bukkit.getWorld(center.world());
        return world == null ? null : new Location(world, center.pos().x(), center.pos().y(), center.pos().z());
    }

    /** The chunk of every center, for the chunk keeper. */
    public Set<ChunkKey> chunkKeys() {
        Set<ChunkKey> keys = new HashSet<>();
        for (CommandCenter center : byId.values()) {
            keys.add(ChunkKey.of(center.world(), center.pos().x(), center.pos().z()));
        }
        return keys;
    }

    /** Makes the missing entities of every center whose chunk is loaded. */
    public void ensureEntities() {
        for (CommandCenter center : byId.values()) {
            if (isLoaded(center)) {
                spawnEntities(center);
            }
        }
    }

    /** Removes the marked entities of a chunk that no center owns. Called when a chunk loads. */
    public void sweep(Chunk chunk) {
        Set<UUID> owned = ownedEntities();
        for (Entity entity : chunk.getEntities()) {
            boolean marked = entity.getPersistentDataContainer().has(MARK, PersistentDataType.STRING) || isWallEntity(entity);
            if (marked && !owned.contains(entity.getUniqueId())) {
                entity.remove();
            }
        }
    }

    /** Sweeps every loaded chunk. Called once when the plugin starts. */
    public void sweepLoaded() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                sweep(chunk);
            }
        }
    }

    /**
     * A chunk is unloading: the entity handles of its centers are forgotten, and the map walls that reach into the chunk are taken away while
     * the chunk is still loaded, so their barriers give back their blocks. The data stays.
     */
    public void unloaded(Chunk chunk) {
        for (CommandCenter center : byId.values()) {
            if (center.world().equals(chunk.getWorld().getUID())
                    && center.pos().x() >> 4 == chunk.getX() && center.pos().z() >> 4 == chunk.getZ()) {
                releaseEntities(center);
            }
        }
        for (MapWall wall : walls.values()) {
            if (wall.center().world().equals(chunk.getWorld().getUID()) && wall.layout().touchesChunk(chunk.getX(), chunk.getZ())) {
                wall.remove();
            }
        }
    }

    /** Removes the entities of every center. The data stays. */
    public void releaseAll() {
        for (CommandCenter center : byId.values()) {
            destroyEntities(center);
        }
    }

    /** The saved form of every center, with the pending records kept. */
    public List<CommandCenter> snapshot() {
        List<CommandCenter> centers = new ArrayList<>(pending);
        centers.addAll(byId.values());
        return centers;
    }

    /**
     * Shows lines on a center's screen. The text is written only when the lines differ from the ones the screen shows, and nothing is
     * written while the screen's text is not in the world.
     *
     * @param center the center
     * @param lines  the lines, top first, each read by {@code Text.parse}
     */
    public void show(CommandCenter center, List<String> lines) {
        if (lines.equals(center.shown())) {
            return;
        }
        Entity entity = live(center.screen());
        if (!(entity instanceof TextDisplay display)) {
            return;
        }
        List<Component> parts = lines.stream().map(Text::parse).toList();
        display.text(Component.join(JoinConfiguration.separator(Component.newline()), parts));
        center.setShown(lines);
    }

    /** The map wall of a center, made on first use. Its frames are placed by {@link #placeWall}, and kept while the center exists. */
    MapWall wall(CommandCenter center) {
        return walls.computeIfAbsent(center.id(), id -> new MapWall(center,
                WallLayout.of(CommandCenterGeometry.screen(), center.pos().x(), center.pos().y(), center.pos().z(), wallTiles)));
    }

    /**
     * Places a center's map wall, when the center is registered, its world and chunks are loaded, and the wall is not placed already.
     *
     * @param center the center
     */
    void placeWall(CommandCenter center) {
        if (!byId.containsKey(center.id()) || !isLoaded(center)) {
            return;
        }
        World world = Bukkit.getWorld(center.world());
        if (world != null) {
            wall(center).place(world);
        }
    }

    /** Takes a center's map wall away, if it is placed: its frames go and its barriers give back their blocks. The wall's maps stay. */
    void removeWall(CommandCenter center) {
        MapWall wall = walls.get(center.id());
        if (wall != null) {
            wall.remove();
        }
    }

    /** Shows a map on a center's wall. The wall keeps the map, so it is shown when the wall is placed. */
    void showWall(CommandCenter center, byte[] frame) {
        if (byId.containsKey(center.id())) {
            wall(center).show(frame);
        }
    }

    /** True when an entity is a frame of a map wall, which the clicks and the breaks of the frames are cancelled for. */
    boolean isWallEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(MapWall.MARK, PersistentDataType.STRING);
    }

    /** True when a block is a block of a placed map wall: one of its frames' places or one of its barriers. */
    boolean wallCovers(Location at) {
        if (at.getWorld() == null) {
            return false;
        }
        for (MapWall wall : walls.values()) {
            if (wall.isPlaced() && wall.center().world().equals(at.getWorld().getUID())
                    && wall.layout().covers(at.getBlockX(), at.getBlockY(), at.getBlockZ())) {
                return true;
            }
        }
        return false;
    }

    private void addCenter(CommandCenter center) {
        byId.put(center.id(), center);
        byBlock.put(key(center.world(), center.pos().x(), center.pos().y(), center.pos().z()), center);
    }

    private void spawnEntities(CommandCenter center) {
        Location block = blockOf(center);
        if (block == null || !isLoaded(center)) {
            return;
        }
        World world = block.getWorld();
        if (center.display() == null || Bukkit.getEntity(center.display()) == null) {
            Optional<ItemStack> item = modelItem();
            if (item.isPresent()) {
                center.setDisplay(spawnModel(world, block, center, item.get()));
            }
        }
        if (center.screen() == null || Bukkit.getEntity(center.screen()) == null) {
            center.setScreen(spawnScreen(world, block, center));
        }
        if (center.interaction() == null || Bukkit.getEntity(center.interaction()) == null) {
            if (center.interaction() != null) {
                byInteraction.remove(center.interaction());
            }
            UUID interaction = spawnInteraction(world, block, center);
            center.setInteraction(interaction);
            byInteraction.put(interaction, center);
        }
        applyStrikeView(center);
    }

    /**
     * Shows the screen the center has now: the black text, with the readout of its target when it has one. The map wall is not part of the
     * screen: it is placed only while a targeting session is open on the center.
     *
     * @param center the center
     */
    void applyStrikeView(CommandCenter center) {
        Entity text = live(center.screen());
        if (text != null) {
            text.setVisibleByDefault(true);
        }
    }

    /** The item that shows the command center's model, keyed by its component id. */
    private Optional<ItemStack> modelItem() {
        ComponentDefinition definition = core.componentDefinitions().get(ArtilleryIds.COMMAND_CENTER);
        if (definition == null) {
            if (!reportedMissingItem) {
                reportedMissingItem = true;
                log.warning("No item is defined for " + ArtilleryIds.COMMAND_CENTER + ", so no model is shown for the command center.");
            }
            return Optional.empty();
        }
        return Optional.of(core.items().machine(definition));
    }

    /** The model, at the centre of its block, unlifted: its model is written from the block's floor. */
    private UUID spawnModel(World world, Location block, CommandCenter center, ItemStack item) {
        float scale = core.settings().displayScale();
        ItemDisplay display = world.spawn(block.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                    new Vector3f(scale, scale, scale), new Quaternionf()));
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, center.id());
        });
        return display.getUniqueId();
    }

    /** The screen: a black text display on the screen's plane, turned to the north like the model. */
    private UUID spawnScreen(World world, Location block, CommandCenter center) {
        float scale = CommandCenterGeometry.textScale();
        TextDisplay text = world.spawn(block.clone().add(0.5, CommandCenterGeometry.textY(), CommandCenterGeometry.textZ()),
                TextDisplay.class, entity -> {
                    entity.setBillboard(Display.Billboard.FIXED);
                    entity.setAlignment(TextDisplay.TextAlignment.CENTER);
                    entity.setShadowed(false);
                    entity.setSeeThrough(false);
                    entity.setDefaultBackground(false);
                    entity.setBackgroundColor(Color.fromARGB(255, 0, 0, 0));
                    entity.setLineWidth(CommandCenterGeometry.LINE_WIDTH);
                    entity.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotationY((float) Math.PI),
                            new Vector3f(scale, scale, scale), new Quaternionf()));
                    entity.setPersistent(true);
                    entity.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, center.id());
                });
        return text.getUniqueId();
    }

    /** The click target: an interaction over the screen, so a click on the screen reaches the center. */
    private UUID spawnInteraction(World world, Location block, CommandCenter center) {
        Interaction interaction = world.spawn(block.clone().add(0.5, CommandCenterGeometry.clickBottomY(),
                CommandCenterGeometry.clickZ()), Interaction.class, entity -> {
            entity.setInteractionWidth((float) CommandCenterGeometry.clickWidth());
            entity.setInteractionHeight((float) CommandCenterGeometry.clickHeight());
            entity.setResponsive(true);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, center.id());
        });
        return interaction.getUniqueId();
    }

    private void releaseEntities(CommandCenter center) {
        if (center.interaction() != null) {
            byInteraction.remove(center.interaction());
        }
        center.setDisplay(null);
        center.setScreen(null);
        center.setInteraction(null);
    }

    /** Removes a center's entities and its map wall, which is then forgotten. */
    private void destroyEntities(CommandCenter center) {
        removeEntity(center.display());
        removeEntity(center.screen());
        removeEntity(center.interaction());
        removeWall(center);
        walls.remove(center.id());
        releaseEntities(center);
    }

    private static Entity live(UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(id);
        return entity != null && entity.isValid() ? entity : null;
    }

    private static void removeEntity(UUID id) {
        Entity entity = live(id);
        if (entity != null) {
            entity.remove();
        }
    }

    private Set<UUID> ownedEntities() {
        Set<UUID> owned = new HashSet<>();
        for (CommandCenter center : byId.values()) {
            if (center.display() != null) {
                owned.add(center.display());
            }
            if (center.screen() != null) {
                owned.add(center.screen());
            }
            if (center.interaction() != null) {
                owned.add(center.interaction());
            }
        }
        for (MapWall wall : walls.values()) {
            owned.addAll(wall.frames());
        }
        return owned;
    }

    private static String key(UUID world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
