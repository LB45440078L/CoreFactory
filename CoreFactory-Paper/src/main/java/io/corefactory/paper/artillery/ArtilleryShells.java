package io.corefactory.paper.artillery;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.artillery.ArtillerySettings;
import io.corefactory.core.artillery.Position;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The shells in the air, drawn: one item display per shot in flight, which is the shell's own item model, moved along its arc each tick, with a
 * trail of smoke and a whistle. A display exists only while its shot is in the air and its chunk is loaded. It is made when the shot is first drawn,
 * moved by {@link #fly}, and removed by {@link #hide}: when the shot lands, when its chunk unloads, when its world goes, and when the plugin stops.
 * Its entity is not persistent, so no display is saved, and {@link #sweep} removes any marked display a run left in a loaded chunk.
 */
final class ArtilleryShells {

    /** The scale of a shell's display: its sprite at the size of an item. */
    private static final float SCALE = 1.0f;

    /** The ticks a display takes to move from one tick's point to the next: the shell moves once a tick, so one tick smooths it. */
    private static final int MOVE_TICKS = 1;

    /** The mark on a shell's display, so a display left by an earlier run is found. */
    private static final NamespacedKey MARK = new NamespacedKey(ItemFactory.NAMESPACE, "artillery-shell");

    private final CoreFactoryCore core;
    private final ArtillerySettings.Presentation presentation;
    private final Map<ArtilleryShot, UUID> displays = new IdentityHashMap<>();

    /**
     * @param core         the core, for the item of each shell
     * @param presentation the trail and the whistle of a shell in the air
     */
    ArtilleryShells(CoreFactoryCore core, ArtillerySettings.Presentation presentation) {
        this.core = core;
        this.presentation = presentation;
    }

    /**
     * Draws a shot for one tick, in a loaded chunk: its display is made or moved to the shell's position, its trail is laid from the position of the
     * last tick, and its whistle sounds when it is due. The caller has checked that the chunk is loaded.
     *
     * @param shot  the shot in the air
     * @param world the world of the shot
     * @param from  the shell's position of the last tick
     * @param at    the shell's position now
     */
    void fly(ArtilleryShot shot, World world, Position from, Position at) {
        Location where = new Location(world, at.x(), at.y(), at.z());
        Entity display = displayOf(shot);
        if (display == null) {
            display = spawn(shot, where);
            if (display != null) {
                displays.put(shot, display.getUniqueId());
            }
        } else {
            display.teleport(where);
        }
        world.spawnParticle(Particle.FLAME, where, 1, 0.0, 0.0, 0.0, 0.0);
        ArtilleryCues.trail(world, from, at, presentation.trailPuffsPerTick());
        if (ArtilleryTrail.whistleDue(shot.tick(), presentation.whistleIntervalTicks())) {
            ArtilleryCues.whistle(where, presentation.whistleVolume());
        }
    }

    /**
     * Removes the display of a shot, when it has one. The shot itself is not changed: a display that is hidden while the shot is still in the air is
     * made again when its chunk is drawn.
     *
     * @param shot the shot
     */
    void hide(ArtilleryShot shot) {
        UUID id = displays.remove(shot);
        if (id != null) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    /** Removes every display. Called when the plugin stops. */
    void releaseAll() {
        for (UUID id : displays.values()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        displays.clear();
    }

    /** Removes the marked displays of a chunk that no shot in the air owns. Called when a chunk loads. */
    void sweep(Chunk chunk) {
        Set<UUID> owned = new HashSet<>(displays.values());
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(MARK, PersistentDataType.STRING) && !owned.contains(entity.getUniqueId())) {
                entity.remove();
            }
        }
    }

    /** Sweeps every loaded chunk. Called once when the plugin starts, before any shot is in the air. */
    void sweepLoaded() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                sweep(chunk);
            }
        }
    }

    /** The display of a shot that is on the server, or null when it has none, or when the display is gone. */
    private Entity displayOf(ArtilleryShot shot) {
        UUID id = displays.get(shot);
        return id == null ? null : Bukkit.getEntity(id);
    }

    /**
     * Makes the display of a shell at a point: its item model, upright and turned to the viewer, not persisted. Null when the shell has no item
     * defined, in which case the shot is still drawn by its trail and its whistle.
     */
    private Entity spawn(ArtilleryShot shot, Location where) {
        ComponentDefinition definition = core.componentDefinitions().get(ArtilleryIds.shellItemOf(shot.shell()));
        if (definition == null) {
            return null;
        }
        ItemStack item = core.items().component(definition, 1);
        return where.getWorld().spawn(where, ItemDisplay.class, display -> {
            display.setItemStack(item);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setBillboard(Display.Billboard.CENTER);
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                    new Vector3f(SCALE, SCALE, SCALE), new Quaternionf()));
            display.setTeleportDuration(MOVE_TICKS);
            display.setPersistent(false);
            display.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, shot.pieceId());
        });
    }
}
