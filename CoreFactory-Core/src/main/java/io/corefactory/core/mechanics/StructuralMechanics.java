package io.corefactory.core.mechanics;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.DetonationContext;
import io.corefactory.api.Mechanic;
import io.corefactory.api.CoreFactoryApi;
import io.corefactory.api.PlacedTnt;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.engine.Shapes;
import io.corefactory.core.engine.Tasks;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mechanics that change how a blast is shaped, timed or chained.
 */
final class StructuralMechanics {

    private StructuralMechanics() {
    }

    static List<Mechanic> all(Toolkit kit) {
        return List.of(
                new ChainReaction(kit),
                new ShapedCharge(kit),
                new GravityWell(kit),
                new Implosion(kit),
                new FuseLinked(kit),
                new DelayedCascade(kit),
                new FourDimensional(kit));
    }

    /** Distance from a block's centre to the blast centre, given block coordinates. */
    static double distance(int x, int y, int z, Location origin) {
        double dx = x + 0.5 - origin.getX();
        double dy = y + 0.5 - origin.getY();
        double dz = z + 0.5 - origin.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Detonates nearby TNT in a rolling wave: each block of distance adds a little delay. */
    static final class ChainReaction extends AbstractMechanic {

        ChainReaction(Toolkit kit) {
            super("chain_reaction", "Detonates nearby TNT in a rolling wave, one delay step per block.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            double perBlock = ctx.settings().getDouble("delay-per-block", 3.0);
            double chainRadius = ctx.settings().getDouble("chain-radius", radius * 1.6);
            kit.blast().run(ctx, radius, damage(ctx, 10.0), perBlock, set -> {
            });

            World world = ctx.world();
            Location origin = ctx.origin();
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), chainRadius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        double distance = distance(x, y, z, origin);
                        if (distance <= radius || world.getBlockAt(x, y, z).getType() != Material.TNT) {
                            return;
                        }
                        kit.blast().igniteAt(ctx, world, x, y, z, distance, perBlock);
                    });
        }
    }

    /** Only destroys blocks inside a cone along its direction, like a real shaped charge. */
    static final class ShapedCharge extends AbstractMechanic {

        ShapedCharge(Toolkit kit) {
            super("shaped_charge", "Blasts a cone along the direction it was placed facing.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            Vector axis = axis(ctx);
            double length = ctx.settings().getDouble("length", ctx.tnt().radius());
            double cosine = Math.cos(Math.toRadians(ctx.settings().getDouble("cone-degrees", 25.0)));
            double damage = damage(ctx, 14.0);
            Location origin = ctx.origin();
            World world = ctx.world();

            BlockChangeSet set = new BlockChangeSet();
            Shapes.scan(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), length,
                    world.getMinHeight(), world.getMaxHeight() - 1,
                    (dx, dy, dz) -> inCone(dx, dy, dz, axis, length, cosine),
                    (x, y, z) -> {
                        if (breakable(ctx, world.getBlockAt(x, y, z))) {
                            set.set(x, y, z, Material.AIR.createBlockData());
                        }
                    });
            submit(ctx, set);

            kit.explosions().damage(origin, length, damage, ctx.source(),
                    living -> inCone(living.getLocation().getX() - origin.getX(),
                            living.getLocation().getY() - origin.getY(),
                            living.getLocation().getZ() - origin.getZ(), axis, length, cosine));
            kit.effects().line(origin, origin.clone().add(axis.clone().multiply(length)),
                    Color.fromRGB(0xFF8A2B), 1.0, 96.0);
            kit.effects().boom(origin, 3f);
        }

        private static Vector axis(DetonationContext ctx) {
            Vector direction = ctx.direction();
            if (direction == null) {
                double[] fallback = ctx.settings().getDoubleArray("default-direction", new double[]{1, 0, 0});
                direction = new Vector(fallback[0], fallback.length > 1 ? fallback[1] : 0,
                        fallback.length > 2 ? fallback[2] : 0);
            }
            return direction.lengthSquared() < 0.0001 ? new Vector(1, 0, 0) : direction.clone().normalize();
        }

        private static boolean inCone(double dx, double dy, double dz, Vector axis, double length, double cosine) {
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 0.5 || distance > length) {
                return false;
            }
            double alignment = (dx * axis.getX() + dy * axis.getY() + dz * axis.getZ()) / distance;
            return alignment >= cosine;
        }
    }

    /** Pulls blocks and entities inward, then carves a smaller crater at the centre. */
    static final class GravityWell extends AbstractMechanic {

        GravityWell(Toolkit kit) {
            super("gravity_well", "Sucks blocks and entities into the centre before the crater forms.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            int pullTicks = Math.max(1, ctx.settings().getInt("pull-ticks", 40));
            double strength = ctx.settings().getDouble("pull-strength", 0.45);
            int maxDebris = ctx.settings().getInt("max-debris", 300);
            double crater = ctx.settings().getDouble("crater-fraction", 0.5);
            double damage = damage(ctx, 6.0);
            Location origin = ctx.origin();
            World world = ctx.world();

            List<FallingBlock> floating = new ArrayList<>();
            BlockChangeSet set = new BlockChangeSet();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        if (floating.size() >= maxDebris || random.nextBoolean()) {
                            return;
                        }
                        Block block = world.getBlockAt(x, y, z);
                        if (!breakable(ctx, block)) {
                            return;
                        }
                        FallingBlock falling = world.spawnFallingBlock(
                                new Location(world, x + 0.5, y + 0.5, z + 0.5), block.getBlockData());
                        falling.setGravity(false);
                        falling.setDropItem(false);
                        falling.setHurtEntities(false);
                        floating.add(falling);
                        set.set(x, y, z, Material.AIR.createBlockData());
                    });
            submit(ctx, set);

            Tasks.repeat(ctx.api().plugin(), 1L, pullTicks, step -> {
                for (FallingBlock falling : floating) {
                    if (!falling.isValid()) {
                        continue;
                    }
                    Vector toward = origin.toVector().subtract(falling.getLocation().toVector());
                    double distance = toward.length();
                    if (distance < 0.5) {
                        continue;
                    }
                    falling.setVelocity(toward.normalize().multiply(strength * (1 + 2 * (1 - Math.min(1, distance / radius)))));
                }
                kit.explosions().push(origin, radius, -strength * 0.3, new Vector());
                kit.effects().particle(origin, Particle.PORTAL, 6, radius * 0.4, 0.2, 64.0);
            }, () -> {
                floating.forEach(FallingBlock::remove);
                kit.blast().run(ctx, radius * crater, damage, 2.0, set2 -> {
                });
            });
        }
    }

    /** Collapses everything in the radius into a dense core of one material at the centre. */
    static final class Implosion extends AbstractMechanic {

        Implosion(Toolkit kit) {
            super("implosion", "Collapses blocks into a dense core at the centre instead of scattering them.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            double coreRadius = ctx.settings().getDouble("core-radius", radius * 0.25);
            BlockData coreData = data(ctx.settings().getString("core-material", "BLACK_CONCRETE"),
                    Material.BLACK_CONCRETE);
            Location origin = ctx.origin();
            World world = ctx.world();

            BlockChangeSet set = kit.blast().carve(ctx, radius, 2.0);
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), coreRadius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        Block block = world.getBlockAt(x, y, z);
                        if (ctx.api().protection().isProtected(block) || block.getType() == Material.TNT) {
                            return;
                        }
                        set.set(x, y, z, coreData);
                    });
            submit(ctx, set);

            kit.explosions().push(origin, radius, -ctx.settings().getDouble("pull-strength", 1.2), new Vector());
            kit.explosions().damage(origin, radius, damage(ctx, 4.0), ctx.source());
            kit.effects().boom(origin, 2f);
            kit.effects().particle(origin, Particle.SMOKE, 40, 0.5, 0.05, 64.0);
        }
    }

    /** Does not explode. Igniting it relays the signal along its fuse links, one link after another. */
    static final class FuseLinked extends AbstractMechanic {

        FuseLinked(Toolkit kit) {
            super("fuse_linked", "Does not explode. Ignition relays along its fuse links to every linked TNT.", kit);
        }

        @Override
        public IgnitionMode ignitionMode() {
            return IgnitionMode.PERSISTENT;
        }

        @Override
        public int placedTickInterval() {
            return 10;
        }

        @Override
        public void onPlacedTick(PlacedTnt placed, TntDefinition tnt, CoreFactoryApi api) {
            Location from = placed.location().clone().add(0.5, 0.5, 0.5);
            for (Location link : placed.links()) {
                kit.effects().line(from, link.clone().add(0.5, 0.5, 0.5), Color.RED, 0.75, 48.0);
            }
        }

        @Override
        public void detonate(DetonationContext ctx) {
            int step = Math.max(1, ctx.settings().getInt("link-delay", 4));
            if (ctx.placed() != null) {
                List<Location> links = List.copyOf(ctx.placed().links());
                for (int i = 0; i < links.size(); i++) {
                    Optional<PlacedTnt> target = ctx.api().placedAt(links.get(i));
                    if (target.isEmpty()) {
                        continue;
                    }
                    PlacedTnt linked = target.get();
                    ctx.api().scheduleLater((long) (i + 1) * step, () -> ctx.api().ignite(linked, ctx.source()));
                }
            }
            kit.effects().particle(ctx.origin(), Particle.ELECTRIC_SPARK, 12, 0.3, 0.1, 32.0);
        }
    }

    /** Explodes in several waves of small submunitions scattered around the target, like a cluster bomb. */
    static final class DelayedCascade extends AbstractMechanic {

        DelayedCascade(Toolkit kit) {
            super("delayed_cascade", "Explodes in waves of small submunitions, spaced by ticks.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            int waves = Math.max(1, ctx.settings().getInt("waves", 3));
            int interval = Math.max(1, ctx.settings().getInt("wave-interval", 6));
            int submunitions = Math.max(1, ctx.settings().getInt("submunitions", 6));
            double spread = ctx.settings().getDouble("spread", ctx.tnt().radius());
            double subRadius = ctx.settings().getDouble("submunition-radius", ctx.tnt().radius() * 0.35);
            double damage = damage(ctx, 6.0);
            for (int wave = 0; wave < waves; wave++) {
                ctx.api().scheduleLater((long) wave * interval, () -> {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    for (int i = 0; i < submunitions; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double distance = Math.sqrt(random.nextDouble()) * spread;
                        Location centre = ctx.origin().clone().add(Math.cos(angle) * distance, 0,
                                Math.sin(angle) * distance);
                        DetonationContext sub = new DetonationContext(ctx.tnt(), centre, ctx.source(),
                                ctx.direction(), null, ctx.depth() + 1, ctx.api());
                        kit.blast().run(sub, subRadius, damage, 2.0, set -> {
                        });
                    }
                });
            }
        }
    }

    /**
     * Carves the shell where x^2 + y^2 + z^2 + w^2 = R^2 for a fourth coordinate w that sweeps through a random
     * phase. Every step shows a different 3D slice of the hypersphere, so the pattern is hard to predict.
     */
    static final class FourDimensional extends AbstractMechanic {

        FourDimensional(Toolkit kit) {
            super("four_dimensional", "Carves 3D slices of a four-dimensional sphere: unpredictable and dangerous.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            int steps = Math.max(2, ctx.settings().getInt("steps", 14));
            int stepTicks = Math.max(1, ctx.settings().getInt("step-ticks", 2));
            double band = ctx.settings().getDouble("thickness", 1.4) * radius;
            double damage = damage(ctx, 10.0);
            double phase = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
            Location origin = ctx.origin();
            World world = ctx.world();

            Tasks.repeat(ctx.api().plugin(), stepTicks, steps, step -> {
                double w = radius * Math.cos(phase + step * 0.45);
                double w2 = w * w;
                BlockChangeSet set = new BlockChangeSet();
                Shapes.scan(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                        world.getMinHeight(), world.getMaxHeight() - 1,
                        (dx, dy, dz) -> Math.abs(dx * dx + dy * dy + dz * dz + w2 - radius * radius) < band,
                        (x, y, z) -> {
                            if (breakable(ctx, world.getBlockAt(x, y, z))) {
                                set.set(x, y, z, Material.AIR.createBlockData());
                            }
                        });
                submit(ctx, set);
                kit.explosions().damage(origin, radius, damage * 0.4, ctx.source());
                kit.effects().particle(origin, Particle.PORTAL, 8, radius * 0.3, 0.3, 64.0);
            }, () -> kit.effects().boom(origin, 3f));
        }
    }

}
