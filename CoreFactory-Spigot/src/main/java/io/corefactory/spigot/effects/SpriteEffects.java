package io.corefactory.spigot.effects;

import io.corefactory.core.engine.VisualCatalog;
import io.corefactory.core.engine.VisualCatalog.Placement;
import io.corefactory.core.engine.Visuals;
import io.corefactory.core.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Draws the named visual effects as sprites. Each sprite is an item display that shows one of the particle sprites of
 * the resource pack, moved and scaled by the display interpolation, and billboarded so it faces the viewer. Displays
 * are not saved with the world, so a restart clears any that were alive.
 *
 * <p>Spawning and removal run on the main thread, in one repeating task. At most {@code maxSprites} sprites are alive
 * at once; an effect that would take the count over the cap is dropped whole, and one warning is logged.
 */
public final class SpriteEffects implements Visuals {

    private final Plugin plugin;
    private final boolean enabled;
    private final int maxSprites;
    private final Map<String, ItemStack> items = new HashMap<>();
    private final List<Sprite> live = new ArrayList<>();
    private int reserved;
    private boolean warnedFull;
    private BukkitTask task;

    /**
     * @param enabled    false to show nothing: every {@link #play} call returns at once
     * @param maxSprites the most sprites alive at once, at least 1
     */
    public SpriteEffects(Plugin plugin, boolean enabled, int maxSprites) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.maxSprites = Math.max(1, maxSprites);
    }

    /** Starts the repeating task that moves and removes sprites. Does nothing when disabled. */
    public void start() {
        if (enabled && task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    /** Stops the task and removes every live sprite. */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Sprite sprite : live) {
            if (sprite.display != null) {
                sprite.display.remove();
            }
        }
        live.clear();
        reserved = 0;
    }

    /**
     * Starts a named effect at {@code at}. Off the main thread the call is handed to the main thread. An effect in an
     * unloaded chunk, an unknown name, or one that would take the sprite count over its cap, is skipped.
     */
    @Override
    public void play(String effect, Location at, double scale) {
        if (!enabled) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Location copy = at.clone();
            Bukkit.getScheduler().runTask(plugin, () -> play(effect, copy, scale));
            return;
        }
        Optional<VisualCatalog.Effect> found = VisualCatalog.find(effect);
        World world = at.getWorld();
        if (found.isEmpty() || world == null || !world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
            return;
        }
        List<Placement> placements = VisualCatalog.placements(found.get(), scale);
        if (reserved + placements.size() > maxSprites) {
            if (!warnedFull) {
                warnedFull = true;
                plugin.getLogger().warning("Sprite effects reached effects.max-sprites (" + maxSprites
                        + "). Newer effects are dropped until some finish. Raise the cap to show more.");
            }
            return;
        }
        Location origin = at.clone();
        for (Placement placement : placements) {
            live.add(new Sprite(origin, placement));
        }
        reserved += placements.size();
    }

    private void tick() {
        Iterator<Sprite> it = live.iterator();
        while (it.hasNext()) {
            Sprite sprite = it.next();
            sprite.age++;
            Placement placement = sprite.placement;
            if (sprite.display != null && !sprite.display.isValid()) {
                it.remove();
                reserved--;
                continue;
            }
            if (sprite.display == null && sprite.age >= placement.delay()) {
                sprite.display = spawn(sprite);
                sprite.spawnedAge = sprite.age;
            } else if (sprite.display != null && !sprite.grown && sprite.age > sprite.spawnedAge) {
                grow(sprite.display, placement);
                sprite.grown = true;
            } else if (sprite.display != null && sprite.grown && !sprite.shrinking && placement.fade() > 0
                    && sprite.age >= placement.delay() + placement.lifetime() - placement.fade()) {
                shrink(sprite.display, placement);
                sprite.shrinking = true;
            }
            if (sprite.age >= placement.delay() + placement.lifetime()) {
                if (sprite.display != null) {
                    sprite.display.remove();
                }
                it.remove();
                reserved--;
            }
        }
    }

    /** Spawns a sprite at its start, or returns null when its chunk is not loaded. */
    private ItemDisplay spawn(Sprite sprite) {
        Location origin = sprite.origin;
        World world = origin.getWorld();
        if (world == null || !world.isChunkLoaded(origin.getBlockX() >> 4, origin.getBlockZ() >> 4)) {
            return null;
        }
        ItemStack item = item(sprite.placement.sprite());
        Placement placement = sprite.placement;
        return world.spawn(origin, ItemDisplay.class, display -> {
            display.setItemStack(item);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
            display.setBillboard(Display.Billboard.CENTER);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(0);
            display.setTransformation(transform(placement.from(), placement.startScale()));
            display.setPersistent(false);
        });
    }

    /** Moves a sprite from its start to its end over the rest of its life, growing or shrinking as it goes. */
    private static void grow(ItemDisplay display, Placement placement) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(Math.max(1, placement.lifetime() - 1));
        display.setTransformation(transform(placement.offset(), placement.endScale()));
    }

    /** Shrinks a sprite to nothing over its fade ticks, from wherever it has reached. */
    private static void shrink(ItemDisplay display, Placement placement) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(placement.fade());
        display.setTransformation(transform(placement.offset(), 0.0));
    }

    private ItemStack item(String sprite) {
        return items.computeIfAbsent(sprite, id -> {
            ItemStack stack = new ItemStack(Material.PAPER);
            ItemMeta meta = stack.getItemMeta();
            meta.setItemModel(new NamespacedKey(ItemFactory.NAMESPACE,
                    ItemFactory.modelKey(ItemFactory.PARTICLE, id)));
            stack.setItemMeta(meta);
            return stack;
        });
    }

    private static Transformation transform(Vector at, double size) {
        float s = (float) size;
        return new Transformation(new Vector3f((float) at.getX(), (float) at.getY(), (float) at.getZ()),
                new Quaternionf(), new Vector3f(s, s, s), new Quaternionf());
    }

    /** One sprite of an effect: where it belongs, how far it has run, and its display once it is spawned. */
    private static final class Sprite {

        private final Location origin;
        private final Placement placement;
        private int age;
        private int spawnedAge = -1;
        private boolean grown;
        private boolean shrinking;
        private ItemDisplay display;

        private Sprite(Location origin, Placement placement) {
            this.origin = origin;
            this.placement = placement;
        }
    }
}
