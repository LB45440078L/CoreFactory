package io.corefactory.core.mechanics;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.DetonationContext;
import io.corefactory.api.Mechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;

/**
 * Common plumbing for the built-in mechanics: id and summary, the standard blast, queued writes, and the
 * rules for what a blast may touch.
 */
public abstract class AbstractMechanic implements Mechanic {

    private final String id;
    private final String summary;
    protected final Toolkit kit;

    protected AbstractMechanic(String id, String summary, Toolkit kit) {
        this.id = id;
        this.summary = summary;
        this.kit = kit;
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final String summary() {
        return summary;
    }

    /** Runs the standard blast with the TNT's radius, the {@code damage} setting, and the default chain step. */
    protected void standardBlast(DetonationContext ctx, double defaultDamage) {
        kit.blast().run(ctx, ctx.tnt().radius(), damage(ctx, defaultDamage),
                ctx.settings().getDouble("chain-delay-per-block", 2.0), set -> {
                });
    }

    protected static double damage(DetonationContext ctx, double fallback) {
        return ctx.settings().getDouble("damage", fallback);
    }

    /** Queues a set of writes through the staged pipeline. */
    protected static void submit(DetonationContext ctx, BlockChangeSet set) {
        ctx.api().submitBlockChanges(ctx.world(), set, () -> {
        });
    }

    /**
     * True when a blast may destroy this block: it is not air, not TNT (TNT is never destroyed by these helpers;
     * the standard blast ignites it instead), and not protected by the configured list or an anchor field.
     */
    protected static boolean breakable(DetonationContext ctx, Block block) {
        Material type = block.getType();
        return !type.isAir() && type != Material.TNT && !ctx.api().protection().isProtected(block);
    }

    /** Looks up a material by name, falling back when the name is missing or wrong. */
    protected static Material material(String name, Material fallback) {
        Material material = name == null ? null : Material.matchMaterial(name);
        return material == null ? fallback : material;
    }

    protected static BlockData data(String name, Material fallback) {
        return material(name, fallback).createBlockData();
    }

    /** Hands items to a player, dropping whatever does not fit at the given location. */
    protected static void deliver(Location at, Entity recipient, Collection<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }
        World world = at.getWorld();
        if (recipient instanceof org.bukkit.entity.Player player) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(items.toArray(ItemStack[]::new));
            leftovers.values().forEach(item -> world.dropItemNaturally(at, item));
            return;
        }
        items.forEach(item -> world.dropItemNaturally(at, item));
    }

    /** Whether a material is a block that stores items. */
    protected static boolean isContainerType(Material material) {
        String name = material.name();
        return name.endsWith("CHEST") || name.endsWith("BARREL") || name.endsWith("SHULKER_BOX");
    }
}
