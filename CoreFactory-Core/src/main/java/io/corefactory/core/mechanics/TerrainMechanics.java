package io.corefactory.core.mechanics;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.DetonationContext;
import io.corefactory.api.Mechanic;
import io.corefactory.api.MechanicSettings;
import io.corefactory.core.engine.Shapes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mechanics that rewrite terrain instead of simply removing it.
 */
final class TerrainMechanics {

    private TerrainMechanics() {
    }

    static List<Mechanic> all(Toolkit kit) {
        return List.of(
                new Vitrify(kit),
                new Liquefy(kit),
                new Terraform(kit),
                new Randomize(kit),
                new Sculpt(kit));
    }

    /** Turns a sphere of blocks into glass, with a core of obsidian. Nothing is destroyed or dropped. */
    static final class Vitrify extends AbstractMechanic {

        Vitrify(Toolkit kit) {
            super("vitrify", "Fuses a sphere of blocks into glass, with an obsidian core. Nothing is lost.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            double innerRadius = radius * ctx.settings().getDouble("inner-fraction", 0.45);
            BlockData outer = data(ctx.settings().getString("outer-material", "GLASS"), Material.GLASS);
            BlockData inner = data(ctx.settings().getString("inner-material", "OBSIDIAN"), Material.OBSIDIAN);
            Location origin = ctx.origin();
            World world = ctx.world();

            BlockChangeSet set = new BlockChangeSet();
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        if (!breakable(ctx, world.getBlockAt(x, y, z))) {
                            return;
                        }
                        boolean core = StructuralMechanics.distance(x, y, z, origin) <= innerRadius;
                        set.set(x, y, z, core ? inner : outer);
                    });
            submit(ctx, set);
            kit.explosions().damage(origin, radius, damage(ctx, 3.0), ctx.source());
            kit.effects().particle(origin, Particle.END_ROD, 20, 1.0, 0.1, 64.0);
            kit.effects().boom(origin, 2f);
        }
    }

    /** Turns solid blocks in the sphere into a fluid, for instant lava or water landscapes. */
    static final class Liquefy extends AbstractMechanic {

        Liquefy(Toolkit kit) {
            super("liquefy", "Converts solid blocks in the sphere into lava, water or slime.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            BlockData fluid = data(ctx.settings().getString("fluid", "LAVA"), Material.LAVA);
            Location origin = ctx.origin();
            World world = ctx.world();

            BlockChangeSet set = new BlockChangeSet();
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        if (breakable(ctx, world.getBlockAt(x, y, z))) {
                            set.set(x, y, z, fluid);
                        }
                    });
            submit(ctx, set);
            kit.explosions().damage(origin, radius, damage(ctx, 4.0), ctx.source());
            kit.effects().boom(origin, 2f);
            kit.effects().particle(origin, Particle.LAVA, 20, radius * 0.4, 0.1, 64.0);
        }
    }

    /** Blasts a crater and then refills it with a chosen material up to the original ground level. */
    static final class Terraform extends AbstractMechanic {

        Terraform(Toolkit kit) {
            super("terraform", "Blasts a crater and refills it with dirt, stone or sand up to the old ground level.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            BlockData fill = data(ctx.settings().getString("fill-material", "DIRT"), Material.DIRT);
            BlockData air = Material.AIR.createBlockData();
            Location origin = ctx.origin();
            World world = ctx.world();
            Map<Long, Integer> surface = new HashMap<>();

            BlockChangeSet set = new BlockChangeSet();
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        if (!breakable(ctx, world.getBlockAt(x, y, z))) {
                            return;
                        }
                        int top = surface.computeIfAbsent(column(x, z), key -> world.getHighestBlockYAt(x, z));
                        set.set(x, y, z, y <= top ? fill : air);
                    });
            submit(ctx, set);
            kit.explosions().damage(origin, radius, damage(ctx, 6.0), ctx.source());
            kit.effects().boom(origin, 3f);
        }

        private static long column(int x, int z) {
            return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
        }
    }

    /** Replaces the blocks in the sphere with random ones drawn from a weighted pool, like a loot table. */
    static final class Randomize extends AbstractMechanic {

        private static final List<Weighted> DEFAULT_POOL = List.of(
                new Weighted(Material.STONE.createBlockData(), 6),
                new Weighted(Material.DEEPSLATE.createBlockData(), 2),
                new Weighted(Material.ANDESITE.createBlockData(), 3),
                new Weighted(Material.GRAVEL.createBlockData(), 2),
                new Weighted(Material.MOSSY_COBBLESTONE.createBlockData(), 1));

        Randomize(Toolkit kit) {
            super("randomize", "Swaps blocks in the sphere for random ones from a weighted pool.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            List<Weighted> pool = pool(ctx.settings());
            int total = pool.stream().mapToInt(Weighted::weight).sum();
            Location origin = ctx.origin();
            World world = ctx.world();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            BlockChangeSet set = new BlockChangeSet();
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        if (!breakable(ctx, world.getBlockAt(x, y, z))) {
                            return;
                        }
                        int roll = random.nextInt(total);
                        for (Weighted entry : pool) {
                            roll -= entry.weight();
                            if (roll < 0) {
                                set.set(x, y, z, entry.data());
                                return;
                            }
                        }
                    });
            submit(ctx, set);
            kit.explosions().damage(origin, radius, damage(ctx, 5.0), ctx.source());
            kit.effects().boom(origin, 2f);
        }

        private static List<Weighted> pool(MechanicSettings settings) {
            List<MechanicSettings> entries = settings.getSectionList("pool");
            if (entries.isEmpty()) {
                return DEFAULT_POOL;
            }
            List<Weighted> out = new ArrayList<>();
            for (MechanicSettings entry : entries) {
                Material material = Material.matchMaterial(entry.getString("material", ""));
                int weight = Math.max(1, entry.getInt("weight", 1));
                if (material != null && material.isBlock()) {
                    out.add(new Weighted(material.createBlockData(), weight));
                }
            }
            return out.isEmpty() ? DEFAULT_POOL : List.copyOf(out);
        }

        private record Weighted(BlockData data, int weight) {
        }
    }

    /** Removes only a thin shell along a sphere, cylinder or torus, for precise demolition. */
    static final class Sculpt extends AbstractMechanic {

        Sculpt(Toolkit kit) {
            super("sculpt", "Removes only a thin shell along a chosen sphere, cylinder or torus.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            String shape = ctx.settings().getString("shape", "sphere").toLowerCase();
            double radius = ctx.tnt().radius();
            double half = ctx.settings().getDouble("thickness", 1.5) / 2.0;
            double height = ctx.settings().getDouble("height", radius);
            double tube = ctx.settings().getDouble("tube-radius", radius * 0.3);
            double reach = switch (shape) {
                case "torus" -> radius + tube + half;
                case "cylinder" -> Math.max(radius, height) + half;
                default -> radius + half;
            };
            Location origin = ctx.origin();
            World world = ctx.world();

            BlockChangeSet set = new BlockChangeSet();
            Shapes.scan(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), reach,
                    world.getMinHeight(), world.getMaxHeight() - 1, shell(shape, radius, height, tube, half),
                    (x, y, z) -> {
                        if (breakable(ctx, world.getBlockAt(x, y, z))) {
                            set.set(x, y, z, Material.AIR.createBlockData());
                        }
                    });
            submit(ctx, set);
            kit.explosions().damage(origin, radius, damage(ctx, 4.0), ctx.source());
            kit.effects().boom(origin, 2f);
        }

        private static Shapes.Offset shell(String shape, double radius, double height, double tube, double half) {
            return switch (shape) {
                case "cylinder" -> (dx, dy, dz) ->
                        Math.abs(Math.sqrt(dx * dx + dz * dz) - radius) <= half && Math.abs(dy) <= height;
                case "torus" -> (dx, dy, dz) -> {
                    double q = Math.sqrt(dx * dx + dz * dz) - radius;
                    return Math.abs(Math.sqrt(q * q + dy * dy) - tube) <= half;
                };
                default -> (dx, dy, dz) -> Math.abs(Math.sqrt(dx * dx + dy * dy + dz * dz) - radius) <= half;
            };
        }
    }
}
