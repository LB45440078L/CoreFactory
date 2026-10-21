package io.corefactory.core.mechanics;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.DetonationContext;
import io.corefactory.api.Mechanic;
import io.corefactory.api.CoreFactoryApi;
import io.corefactory.api.PlacedTnt;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.engine.Shapes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mechanics that decide whether, when and where a blast happens.
 */
final class LogicMechanics {

    private LogicMechanics() {
    }

    static List<Mechanic> all(Toolkit kit) {
        return List.of(
                new Conditional(kit),
                new ProximityFuse(kit),
                new Pulse(kit),
                new Mirror(kit));
    }

    /**
     * A logic gate. The TNT detonates only when every enabled condition holds (or any of them, with
     * {@code mode: any}). When the check fails the TNT fizzles and drops back as an item.
     */
    static final class Conditional extends AbstractMechanic {

        Conditional(Toolkit kit) {
            super("conditional", "Only detonates if its conditions hold: a block nearby, a player near, a signal, a time.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            World world = ctx.world();
            Location origin = ctx.origin();
            List<Boolean> checks = new ArrayList<>();

            Material required = Material.matchMaterial(ctx.settings().getString("require-block", ""));
            if (required != null) {
                checks.add(hasBlock(world, origin, required, ctx.settings().getInt("block-radius", 3)));
            }
            double playerRange = ctx.settings().getDouble("require-player-within", 0.0);
            if (playerRange > 0) {
                checks.add(!kit.effects().nearby(origin, playerRange).isEmpty());
            }
            if (ctx.settings().getBoolean("require-powered", false)) {
                checks.add(origin.getBlock().isBlockIndirectlyPowered());
            }
            int timeMin = ctx.settings().getInt("time-min", -1);
            int timeMax = ctx.settings().getInt("time-max", -1);
            if (timeMin >= 0 && timeMax >= 0) {
                long time = world.getTime();
                checks.add(timeMin <= timeMax ? time >= timeMin && time <= timeMax
                        : time >= timeMin || time <= timeMax);
            }

            boolean any = "any".equalsIgnoreCase(ctx.settings().getString("mode", "all"));
            boolean pass = checks.isEmpty()
                    || (any ? checks.stream().anyMatch(check -> check) : checks.stream().allMatch(check -> check));
            if (!pass) {
                Optional<ItemStack> item = ctx.api().createTntItem(ctx.tnt().id(), 1);
                item.ifPresent(stack -> world.dropItemNaturally(origin, stack));
                kit.effects().particle(origin, Particle.SMOKE, 10, 0.3, 0.02, 32.0);
                return;
            }
            standardBlast(ctx, 14.0);
        }

        private static boolean hasBlock(World world, Location origin, Material wanted, int radius) {
            boolean[] found = {false};
            Shapes.sphere(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                    world.getMinHeight(), world.getMaxHeight() - 1, (x, y, z) -> {
                        if (!found[0] && world.getBlockAt(x, y, z).getType() == wanted) {
                            found[0] = true;
                        }
                    });
            return found[0];
        }
    }

    /** Arms after a short delay. The first player to come within range sets off the countdown. */
    static final class ProximityFuse extends AbstractMechanic {

        ProximityFuse(Toolkit kit) {
            super("proximity_tnt", "Arms after a short delay, then counts down when a player comes near.", kit);
        }

        @Override
        public int placedTickInterval() {
            return 10;
        }

        @Override
        public void onPlacedTick(PlacedTnt placed, TntDefinition tnt, CoreFactoryApi api) {
            long now = api.currentTick();
            if (placed.armedAtTick() == 0) {
                placed.setArmedAtTick(now + Math.max(0, tnt.settings().getInt("arm-ticks", 60)));
                return;
            }
            if (!placed.armed()) {
                if (now < placed.armedAtTick()) {
                    return;
                }
                placed.setArmed(true);
            }
            Location at = placed.location().clone().add(0.5, 0.5, 0.5);
            double range = tnt.settings().getDouble("trigger-radius", 3.0);
            Player nearest = null;
            double best = range * range;
            for (Player player : kit.effects().nearby(at, range)) {
                double distance = player.getLocation().distanceSquared(at);
                if (distance <= best) {
                    best = distance;
                    nearest = player;
                }
            }
            if (nearest != null) {
                api.ignite(placed, nearest);
            }
        }

        @Override
        public void detonate(DetonationContext ctx) {
            standardBlast(ctx, 14.0);
        }
    }

    /**
     * Emits a redstone pulse: a shell of redstone blocks at a set radius that powers neighbouring components,
     * then removes itself. Optionally also explodes.
     */
    static final class Pulse extends AbstractMechanic {

        private static final int MAX_PULSE_BLOCKS = 4_000;

        Pulse(Toolkit kit) {
            super("pulse_tnt", "Sends a redstone pulse through a shell around it, optionally also exploding.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            if (ctx.settings().getBoolean("explode", true)) {
                standardBlast(ctx, 6.0);
            }
            double radius = ctx.settings().getDouble("pulse-radius", ctx.tnt().radius() * 1.5);
            int ticks = Math.max(2, ctx.settings().getInt("pulse-ticks", 40));
            Material material = material(ctx.settings().getString("pulse-material", "REDSTONE_BLOCK"),
                    Material.REDSTONE_BLOCK);
            Location origin = ctx.origin();
            World world = ctx.world();

            List<Block> pulsed = new ArrayList<>();
            Shapes.scan(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(), radius,
                    world.getMinHeight(), world.getMaxHeight() - 1,
                    (dx, dy, dz) -> {
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        return distance <= radius && distance > radius - 1.2;
                    },
                    (x, y, z) -> {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType().isAir() && pulsed.size() < MAX_PULSE_BLOCKS) {
                            block.setType(material, true);
                            pulsed.add(block);
                        }
                    });
            ctx.api().scheduleLater(ticks, () -> {
                for (Block block : pulsed) {
                    if (block.getType() == material) {
                        block.setType(Material.AIR, false);
                    }
                }
            });
            kit.effects().particle(origin, Particle.END_ROD, 20, radius * 0.2, 0.2, 64.0);
        }
    }

    /**
     * Repeats the blast at a mirrored position. The centre of the blast is reflected across a plane set
     * {@code plane-offset} blocks away along {@code axis}, and every block the blast removed is removed again
     * at its reflection.
     */
    static final class Mirror extends AbstractMechanic {

        Mirror(Toolkit kit) {
            super("mirror_tnt", "Carves the same blast again, reflected across a plane: symmetry builds.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            boolean zAxis = "z".equalsIgnoreCase(ctx.settings().getString("axis", "x"));
            double offset = ctx.settings().getDouble("plane-offset", 24.0);
            double radius = ctx.tnt().radius();
            Location origin = ctx.origin();
            World world = ctx.world();

            BlockChangeSet original = kit.blast().carve(ctx, radius, 2.0);
            submit(ctx, original);

            double plane = (zAxis ? origin.getZ() : origin.getX()) + offset;
            BlockChangeSet mirrored = new BlockChangeSet(original.size());
            for (int i = 0; i < original.size(); i++) {
                int x = original.x(i);
                int y = original.y(i);
                int z = original.z(i);
                int mx = zAxis ? x : (int) Math.floor(2 * plane - (x + 0.5));
                int mz = zAxis ? (int) Math.floor(2 * plane - (z + 0.5)) : z;
                if (breakable(ctx, world.getBlockAt(mx, y, mz))) {
                    mirrored.set(mx, y, mz, Material.AIR.createBlockData());
                }
            }
            submit(ctx, mirrored);

            Location mirrorOrigin = origin.clone();
            if (zAxis) {
                mirrorOrigin.setZ(2 * plane - origin.getZ());
            } else {
                mirrorOrigin.setX(2 * plane - origin.getX());
            }
            kit.explosions().damage(mirrorOrigin, radius, damage(ctx, 10.0), ctx.source());
            kit.effects().boom(mirrorOrigin, 3f);
            kit.effects().line(origin, mirrorOrigin, org.bukkit.Color.fromRGB(0x9CE8FF), 1.0, 96.0);
        }
    }
}
