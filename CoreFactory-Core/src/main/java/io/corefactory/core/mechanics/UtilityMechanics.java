package io.corefactory.core.mechanics;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.DetonationContext;
import io.corefactory.api.Mechanic;
import io.corefactory.api.CoreFactoryApi;
import io.corefactory.api.PlacedTnt;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.engine.Shapes;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mechanics for economy and building: harvesting drops, sorting into chests, marking regions, and anti-TNT fields.
 */
final class UtilityMechanics {

    private static final ItemStack HARVEST_TOOL = new ItemStack(Material.NETHERITE_PICKAXE);

    private UtilityMechanics() {
    }

    static List<Mechanic> all(Toolkit kit) {
        return List.of(
                new Harvest(kit),
                new Sorting(kit),
                new Marker(kit),
                new Anchor(kit));
    }

    /** Destroys blocks, but their drops go to the igniter's inventory (or the ground if there is none). */
    static final class Harvest extends AbstractMechanic {

        Harvest(Toolkit kit) {
            super("harvest", "Destroys blocks but collects their drops for whoever lit it.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            int maxItems = ctx.settings().getInt("max-items", 2_000);
            Location origin = ctx.origin();
            World world = ctx.world();
            List<ItemStack> loot = new ArrayList<>();

            BlockChangeSet set = new BlockChangeSet();
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        Block block = world.getBlockAt(x, y, z);
                        if (!breakable(ctx, block) || isContainerType(block.getType())) {
                            return;
                        }
                        if (loot.size() < maxItems) {
                            loot.addAll(block.getDrops(HARVEST_TOOL));
                        }
                        set.set(x, y, z, Material.AIR.createBlockData());
                    });
            submit(ctx, set);
            deliver(origin, ctx.source(), loot);
            kit.explosions().damage(origin, radius, damage(ctx, 0.0), ctx.source());
            kit.effects().boom(origin, 2f);
        }
    }

    /**
     * Pulls chosen ore types out of the blast into the nearest chest, barrel or shulker in range. Anything that
     * does not fit, or any ore with no container nearby, drops on the ground.
     */
    static final class Sorting extends AbstractMechanic {

        Sorting(Toolkit kit) {
            super("sorting", "Pulls chosen block types into the nearest container before the blast.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            double containerRadius = ctx.settings().getDouble("container-radius", radius * 2.0);
            Set<Material> wanted = materials(ctx.settings().getStringList("sort-materials"));
            Location origin = ctx.origin();
            World world = ctx.world();

            Container target = nearestContainer(world, origin, containerRadius);
            List<ItemStack> sorted = new ArrayList<>();
            BlockChangeSet set = new BlockChangeSet();
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        Block block = world.getBlockAt(x, y, z);
                        if (!breakable(ctx, block) || isContainerType(block.getType())) {
                            return;
                        }
                        if (wanted.contains(block.getType())) {
                            sorted.addAll(block.getDrops(HARVEST_TOOL));
                        }
                        set.set(x, y, z, Material.AIR.createBlockData());
                    });
            submit(ctx, set);

            List<ItemStack> overflow = sorted;
            if (target != null && !sorted.isEmpty()) {
                overflow = new ArrayList<>(target.getInventory().addItem(sorted.toArray(ItemStack[]::new)).values());
            }
            deliver(origin, null, overflow);
            kit.explosions().damage(origin, radius, damage(ctx, 6.0), ctx.source());
            kit.effects().boom(origin, 2f);
        }

        private static Container nearestContainer(World world, Location origin, double range) {
            Container[] best = {null};
            double[] bestDistance = {Double.MAX_VALUE};
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), range,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        Block block = world.getBlockAt(x, y, z);
                        if (!isContainerType(block.getType()) || !(block.getState() instanceof Container container)) {
                            return;
                        }
                        double distance = origin.distanceSquared(block.getLocation().add(0.5, 0.5, 0.5));
                        if (distance < bestDistance[0]) {
                            bestDistance[0] = distance;
                            best[0] = container;
                        }
                    });
            return best[0];
        }

        private static Set<Material> materials(List<String> names) {
            Set<Material> out = new HashSet<>();
            for (String name : names) {
                Material material = Material.matchMaterial(name);
                if (material != null) {
                    out.add(material);
                }
            }
            return out;
        }
    }

    /**
     * Does not explode. While placed it outlines its region for nearby players. Igniting it scans the region
     * and reports what is in it to the player who lit it.
     */
    static final class Marker extends AbstractMechanic {

        Marker(Toolkit kit) {
            super("marker", "Does not explode. Outlines its region; igniting it scans the region for materials.", kit);
        }

        @Override
        public IgnitionMode ignitionMode() {
            return IgnitionMode.PERSISTENT;
        }

        @Override
        public int placedTickInterval() {
            return 20;
        }

        @Override
        public void onPlacedTick(PlacedTnt placed, TntDefinition tnt, CoreFactoryApi api) {
            outline(placed.location().clone().add(0.5, 0.5, 0.5), tnt.radius(), Color.fromRGB(0x55FFFF));
        }

        @Override
        public void detonate(DetonationContext ctx) {
            World world = ctx.world();
            Location origin = ctx.origin();
            int radius = (int) Math.round(ctx.tnt().radius());
            Map<Material, Integer> counts = new EnumMap<>(Material.class);
            int[] total = {0};
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        Material type = world.getBlockAt(x, y, z).getType();
                        if (type.isAir()) {
                            return;
                        }
                        counts.merge(type, 1, Integer::sum);
                        total[0]++;
                    });
            if (ctx.source() instanceof Player player) {
                String top = counts.entrySet().stream()
                        .sorted(Map.Entry.<Material, Integer>comparingByValue().reversed())
                        .limit(3)
                        .map(entry -> entry.getKey().name().toLowerCase().replace('_', ' '))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("nothing");
                player.sendMessage(kit.messages().get().get("scan-result", "count", total[0], "top", top));
            }
        }
    }

    /** Anti-TNT field. While placed, explosions cannot reach inside its radius. Ignition does nothing. */
    static final class Anchor extends AbstractMechanic {

        Anchor(Toolkit kit) {
            super("anchor", "Creates an anti-TNT field: explosions cannot reach inside it while it stands.", kit);
        }

        @Override
        public IgnitionMode ignitionMode() {
            return IgnitionMode.INERT;
        }

        @Override
        public int placedTickInterval() {
            return 20;
        }

        @Override
        public void onPlacedTick(PlacedTnt placed, TntDefinition tnt, CoreFactoryApi api) {
            outline(placed.location().clone().add(0.5, 0.5, 0.5), tnt.radius(), Color.AQUA);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            // Never reached: anchors are inert.
        }
    }

    /** Draws a sphere outline of points for players near it, on a Fibonacci lattice so points spread evenly. */
    static void outline(Location centre, double radius, Color color) {
        int points = 48;
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) (points - 1)) * 2;
            double ring = Math.sqrt(1 - y * y);
            double theta = golden * i;
            Location at = centre.clone().add(Math.cos(theta) * ring * radius, y * radius,
                    Math.sin(theta) * ring * radius);
            centre.getWorld().getPlayers().stream()
                    .filter(player -> player.getLocation().distanceSquared(at) < 48 * 48)
                    .forEach(player -> player.spawnParticle(org.bukkit.Particle.DUST, at, 1, 0, 0, 0, 0,
                            new org.bukkit.Particle.DustOptions(color, 0.9f)));
        }
    }
}
