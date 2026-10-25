package io.corefactory.paper.artillery;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.artillery.ArtillerySettings;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.paper.chunks.ChunkKey;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
 * Keeps the artillery pieces by id, by block and by interaction, and owns their entities: the item displays of their models, and an
 * interaction for the clicks. A piece has a body display and a barrel display, and a level VII piece has an upper display too. The body
 * turns to the aim and the barrel tilts to the elevation, both shown by {@link #refresh}. Every entity carries a marker with the piece's
 * id, so entities left by an earlier run are found and removed. A piece whose chunk unloads keeps its data, and its entity handles are
 * forgotten until the chunk loads again. A piece whose world is not loaded waits in the pending records until that world loads.
 */
public final class ArtilleryRegistry {

    /** How long a display takes to turn to a new pose, in ticks: its transformation is interpolated over this time. */
    static final int TURN_TICKS = 20;

    private final CoreFactoryCore core;
    private final ArtillerySettings settings;
    private final Logger log;
    private final Map<String, ArtilleryDevice> byId = new LinkedHashMap<>();
    private final Map<String, ArtilleryDevice> byBlock = new HashMap<>();
    private final Map<UUID, ArtilleryDevice> byInteraction = new HashMap<>();
    private final Set<String> reportedMissingItem = new HashSet<>();
    private final Set<String> reloading = new HashSet<>();
    /** The ticks since the last shot of each piece whose barrel is still kicking back. A piece is absent once its barrel is at rest. */
    private final Map<String, Integer> recoilTicks = new HashMap<>();
    private List<ArtilleryDevice> pending = List.of();

    /**
     * @param core     the core, for the items and the display scale
     * @param settings the settings, for the height of each level
     * @param log      receives the warnings
     */
    public ArtilleryRegistry(CoreFactoryCore core, ArtillerySettings settings, Logger log) {
        this.core = core;
        this.settings = settings;
        this.log = log;
    }

    /**
     * Reads the saved pieces. Those in a loaded world are added; the rest wait.
     *
     * @param saved the saved pieces
     */
    public void load(List<ArtilleryDevice> saved) {
        pending = List.copyOf(saved);
        materializePending();
    }

    /** Adds the pending pieces whose world has loaded since. */
    public void materializePending() {
        List<ArtilleryDevice> waiting = new ArrayList<>();
        for (ArtilleryDevice piece : pending) {
            if (Bukkit.getWorld(piece.world()) == null) {
                waiting.add(piece);
            } else if (!byId.containsKey(piece.id())) {
                addDevice(piece);
            }
        }
        pending = List.copyOf(waiting);
    }

    /**
     * Registers a new piece and shows it, when its chunk is loaded.
     *
     * @return the piece
     */
    public ArtilleryDevice add(ArtilleryDevice piece) {
        addDevice(piece);
        spawnEntities(piece);
        return piece;
    }

    /** Removes a piece from the registry and removes its entities. The caller drops the items it gives back. */
    public void remove(ArtilleryDevice piece) {
        byId.remove(piece.id());
        reloading.remove(piece.id());
        recoilTicks.remove(piece.id());
        byBlock.remove(key(piece.world(), piece.pos().x(), piece.pos().y(), piece.pos().z()));
        destroyEntities(piece);
    }

    /** Every piece, in the order they were added. */
    public List<ArtilleryDevice> all() {
        return List.copyOf(byId.values());
    }

    /** The piece with the given id, if any. */
    public Optional<ArtilleryDevice> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** The piece standing on a block, if any. */
    public Optional<ArtilleryDevice> at(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byBlock.get(key(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ())));
    }

    /** The piece whose interaction entity this is, if any. */
    public Optional<ArtilleryDevice> byInteraction(UUID entity) {
        return Optional.ofNullable(byInteraction.get(entity));
    }

    /** True when the piece's world and chunk are loaded, so it can act and its entities exist. */
    public boolean isLoaded(ArtilleryDevice piece) {
        World world = Bukkit.getWorld(piece.world());
        return world != null && world.isChunkLoaded(piece.pos().x() >> 4, piece.pos().z() >> 4);
    }

    /** The block a piece stands on, or null when its world is not loaded. */
    public Location blockOf(ArtilleryDevice piece) {
        World world = Bukkit.getWorld(piece.world());
        return world == null ? null : new Location(world, piece.pos().x(), piece.pos().y(), piece.pos().z());
    }

    /** The chunk of every piece, for the chunk keeper. */
    public Set<ChunkKey> chunkKeys() {
        Set<ChunkKey> keys = new HashSet<>();
        for (ArtilleryDevice piece : byId.values()) {
            keys.add(ChunkKey.of(piece.world(), piece.pos().x(), piece.pos().z()));
        }
        return keys;
    }

    /** Makes the missing display or interaction of every piece whose chunk is loaded. */
    public void ensureEntities() {
        for (ArtilleryDevice piece : byId.values()) {
            if (isLoaded(piece)) {
                spawnEntities(piece);
            }
        }
    }

    /**
     * Starts the recoil of a piece's barrel at a shot: the barrel kicks back along its axis and eases back to rest over
     * {@link ArtilleryRecoil#TOTAL_TICKS} ticks, one step of which each {@link #refresh} takes. The aim of the barrel does not change.
     *
     * @param piece the piece that fired
     */
    public void recoil(ArtilleryDevice piece) {
        recoilTicks.put(piece.id(), 0);
        applyPose(piece);
    }

    /**
     * Shows a piece's pose: its body at its yaw, and its barrel at its pitch, and steps the barrel's recoil. A barrel that was reloading and is now
     * idle returns to horizontal, and the body keeps its yaw. A barrel laid at an aim stays at its elevation until the piece has fired and finished
     * reloading. Called every tick for every piece, so a display is sent its transformation only when the pose or the recoil has changed.
     *
     * @param piece the piece
     * @param busy  true while the piece reloads, or while a shot of it is in the air
     * @return true when the piece has turned ready: it was reloading or in the air, and now it is neither
     */
    public boolean refresh(ArtilleryDevice piece, boolean busy) {
        boolean wasBusy = reloading.contains(piece.id());
        if (busy) {
            reloading.add(piece.id());
        } else {
            reloading.remove(piece.id());
        }
        boolean ready = wasBusy && !busy;
        if (ready) {
            piece.setBarrelPitch(ArtilleryPose.IDLE_PITCH);
        }
        boolean poseChanged = piece.shownYaw() != piece.bodyYaw() || piece.shownPitch() != piece.barrelPitch();
        Integer kick = recoilTicks.get(piece.id());
        if (kick != null) {
            int next = kick + 1;
            if (next >= ArtilleryRecoil.TOTAL_TICKS) {
                recoilTicks.remove(piece.id());
            } else {
                recoilTicks.put(piece.id(), next);
            }
            if (poseChanged) {
                applyPose(piece);
            } else {
                applyBarrel(piece, ArtilleryRecoil.INTERPOLATION_TICKS);
            }
        } else if (poseChanged) {
            applyPose(piece);
        }
        return ready;
    }

    /** Removes the marked entities of a chunk that no piece owns. Called when a chunk loads. */
    public void sweep(Chunk chunk) {
        Set<UUID> owned = ownedEntities();
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(ArtilleryIds.MARK, PersistentDataType.STRING)
                    && !owned.contains(entity.getUniqueId())) {
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

    /** A chunk is unloading: the entity handles of its pieces are forgotten. The data stays. */
    public void unloaded(Chunk chunk) {
        for (ArtilleryDevice piece : byId.values()) {
            if (piece.world().equals(chunk.getWorld().getUID())
                    && piece.pos().x() >> 4 == chunk.getX() && piece.pos().z() >> 4 == chunk.getZ()) {
                releaseEntities(piece);
            }
        }
    }

    /** Removes the entities of every piece. The data stays. */
    public void releaseAll() {
        for (ArtilleryDevice piece : byId.values()) {
            destroyEntities(piece);
        }
    }

    /** The saved form of every piece, with the pending records kept. */
    public List<ArtilleryDevice> snapshot() {
        List<ArtilleryDevice> pieces = new ArrayList<>(pending);
        pieces.addAll(byId.values());
        return pieces;
    }

    private void addDevice(ArtilleryDevice piece) {
        byId.put(piece.id(), piece);
        byBlock.put(key(piece.world(), piece.pos().x(), piece.pos().y(), piece.pos().z()), piece);
    }

    /** Makes the displays and the interaction of a piece that lack them, and shows its pose on the displays made. */
    private void spawnEntities(ArtilleryDevice piece) {
        Location block = blockOf(piece);
        if (block == null || !isLoaded(piece)) {
            return;
        }
        World world = block.getWorld();
        boolean made = false;
        for (ArtilleryDisplays.Layer layer : ArtilleryDisplays.layers(piece.level())) {
            UUID current = displayOf(piece, layer.part());
            if (current != null && Bukkit.getEntity(current) != null) {
                continue;
            }
            Optional<ItemStack> item = modelOf(piece.level(), layer.model());
            if (item.isPresent()) {
                setDisplayOf(piece, layer.part(), spawnDisplay(world, block, piece, item.get(), layer));
                made = true;
            }
        }
        if (made) {
            applyPose(piece);
        }
        if (piece.interaction() == null || Bukkit.getEntity(piece.interaction()) == null) {
            if (piece.interaction() != null) {
                byInteraction.remove(piece.interaction());
            }
            UUID interaction = spawnInteraction(world, block, piece);
            piece.setInteraction(interaction);
            byInteraction.put(interaction, piece);
        }
    }

    /**
     * The item that shows a model: the material of the piece's level, with the model key of the model. The models of a piece are keyed by their
     * part's id; the upper model of level VII has no item of its own, so it takes the material of level VII's item.
     */
    private Optional<ItemStack> modelOf(ArtilleryLevel level, String model) {
        ComponentDefinition definition = core.componentDefinitions().get(ArtilleryIds.itemOf(level));
        if (definition == null) {
            if (reportedMissingItem.add(ArtilleryIds.itemOf(level))) {
                log.warning("No item is defined for " + ArtilleryIds.itemOf(level) + ", so no display is shown for the piece.");
            }
            return Optional.empty();
        }
        ItemStack item = core.items().machine(definition);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setItemModel(new NamespacedKey(ItemFactory.NAMESPACE, ItemFactory.displayKey(model)));
            item.setItemMeta(meta);
        }
        return Optional.of(item);
    }

    /**
     * The display of one model, at the centre of the piece's block, with the transformation of its pose. The scale is the display scale on every
     * axis: no axis is scaled by the height of the level, since each model is already as tall as its level.
     */
    private UUID spawnDisplay(World world, Location block, ArtilleryDevice piece, ItemStack item, ArtilleryDisplays.Layer layer) {
        ItemDisplay display = world.spawn(block.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(transformationOf(piece, layer));
            entity.setInterpolationDuration(TURN_TICKS);
            entity.setInterpolationDelay(0);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(ArtilleryIds.MARK, PersistentDataType.STRING, piece.id());
        });
        return display.getUniqueId();
    }

    /**
     * The transformation of a display for a piece's pose. The body and the upper model turn about the vertical axis through the centre of the
     * block, by {@link ArtilleryPose#displayYaw}, and stay on their rest translation. The barrel turns the same way, then tilts about its own
     * X axis by its pitch, and is translated so that its trunnion stays where it rests: see {@link ArtilleryPose#barrelTranslation}.
     */
    private Transformation transformationOf(ArtilleryDevice piece, ArtilleryDisplays.Layer layer) {
        float scale = core.settings().displayScale();
        Quaternionf left = new Quaternionf().rotationY((float) Math.toRadians(ArtilleryPose.displayYaw(piece.bodyYaw())));
        Vector3f translation;
        if (layer.part() == ArtilleryDisplays.Part.BARREL) {
            left.rotateX((float) Math.toRadians(piece.barrelPitch()));
            double[] moved = ArtilleryPose.barrelTranslation(layer.translationBlocks(), layer.trunnionBlocks() - 0.5, scale,
                    piece.bodyYaw(), piece.barrelPitch());
            double back = ArtilleryRecoil.displacement(recoilTicks.getOrDefault(piece.id(), 0),
                    settings.presentation().recoilBlocks());
            double[] kick = ArtilleryPose.recoil(piece.bodyYaw(), piece.barrelPitch(), back);
            translation = new Vector3f((float) (moved[0] + kick[0]), (float) (moved[1] + kick[1]), (float) (moved[2] + kick[2]));
        } else {
            translation = new Vector3f(0f, (float) layer.translationBlocks(), 0f);
        }
        return new Transformation(translation, left, new Vector3f(scale, scale, scale), new Quaternionf());
    }

    /**
     * Sets the transformation of every display of a piece to its pose, and records the pose the displays show. The turn is interpolated over
     * {@link #TURN_TICKS}.
     */
    private void applyPose(ArtilleryDevice piece) {
        for (ArtilleryDisplays.Layer layer : ArtilleryDisplays.layers(piece.level())) {
            showLayer(piece, layer, TURN_TICKS);
        }
        piece.setShown(piece.bodyYaw(), piece.barrelPitch());
    }

    /**
     * Sets the transformation of a piece's barrel only, over the given interpolation: the barrel's recoil steps with it, while the body keeps the
     * turn it is making. The pose the displays show is not recorded here, so a pose that differs is still applied in full by {@link #refresh}.
     */
    private void applyBarrel(ArtilleryDevice piece, int interpolationTicks) {
        for (ArtilleryDisplays.Layer layer : ArtilleryDisplays.layers(piece.level())) {
            if (layer.part() == ArtilleryDisplays.Part.BARREL) {
                showLayer(piece, layer, interpolationTicks);
            }
        }
    }

    private void showLayer(ArtilleryDevice piece, ArtilleryDisplays.Layer layer, int interpolationTicks) {
        UUID id = displayOf(piece, layer.part());
        Entity entity = id == null ? null : Bukkit.getEntity(id);
        if (entity instanceof ItemDisplay display) {
            display.setInterpolationDuration(interpolationTicks);
            display.setTransformation(transformationOf(piece, layer));
        }
    }

    private static UUID displayOf(ArtilleryDevice piece, ArtilleryDisplays.Part part) {
        return switch (part) {
            case BODY -> piece.display();
            case BARREL -> piece.barrelDisplay();
            case TOP -> piece.topDisplay();
        };
    }

    private static void setDisplayOf(ArtilleryDevice piece, ArtilleryDisplays.Part part, UUID display) {
        switch (part) {
            case BODY -> piece.setDisplay(display);
            case BARREL -> piece.setBarrelDisplay(display);
            case TOP -> piece.setTopDisplay(display);
        }
    }

    /** One interaction over the piece, as tall as the piece, so a click on any part of the model reaches it. */
    private UUID spawnInteraction(World world, Location block, ArtilleryDevice piece) {
        float height = (float) Math.max(1.0, settings.heightOf(piece.level()));
        Interaction interaction = world.spawn(block.clone().add(0.5, 0.0, 0.5), Interaction.class, entity -> {
            entity.setInteractionWidth(1.0f);
            entity.setInteractionHeight(height);
            entity.setResponsive(true);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(ArtilleryIds.MARK, PersistentDataType.STRING, piece.id());
        });
        return interaction.getUniqueId();
    }

    private void releaseEntities(ArtilleryDevice piece) {
        if (piece.interaction() != null) {
            byInteraction.remove(piece.interaction());
        }
        piece.setDisplay(null);
        piece.setBarrelDisplay(null);
        piece.setTopDisplay(null);
        piece.setInteraction(null);
    }

    private void destroyEntities(ArtilleryDevice piece) {
        removeEntity(piece.display());
        removeEntity(piece.barrelDisplay());
        removeEntity(piece.topDisplay());
        removeEntity(piece.interaction());
        releaseEntities(piece);
    }

    private static void removeEntity(UUID id) {
        if (id == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(id);
        if (entity != null) {
            entity.remove();
        }
    }

    private Set<UUID> ownedEntities() {
        Set<UUID> owned = new HashSet<>();
        for (ArtilleryDevice piece : byId.values()) {
            if (piece.display() != null) {
                owned.add(piece.display());
            }
            if (piece.barrelDisplay() != null) {
                owned.add(piece.barrelDisplay());
            }
            if (piece.topDisplay() != null) {
                owned.add(piece.topDisplay());
            }
            if (piece.interaction() != null) {
                owned.add(piece.interaction());
            }
        }
        return owned;
    }

    private static String key(UUID world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
