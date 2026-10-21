package io.corefactory.core.mechanics;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.DetonationContext;
import io.corefactory.api.Mechanic;
import io.corefactory.api.CoreFactoryApi;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.engine.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Military mechanics: a biological cloud that withers life, and napalm that bursts into fire where it lands.
 */
final class MilitaryMechanics {

    private MilitaryMechanics() {
    }

    static List<Mechanic> all(Toolkit kit) {
        return List.of(new Biological(kit), new Napalm(kit));
    }

    /**
     * A small burst, then a lingering cloud. Inside the cloud, living things take damage and are poisoned or
     * withered, grass turns to path, leaves and plants die, and the cloud is drawn in green.
     */
    static final class Biological extends AbstractMechanic {

        Biological(Toolkit kit) {
            super("biological", "A small burst and a lingering cloud: sickens creatures, withers plants.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double zone = ctx.tnt().radius();
            double burst = ctx.settings().getDouble("burst-radius", Math.max(2.0, zone * 0.35));
            kit.blast().run(ctx, burst, 4.0, 2.0, set -> {
            });

            int seconds = Math.max(5, ctx.settings().getInt("duration-seconds", 30));
            int interval = Math.max(10, ctx.settings().getInt("tick-interval", 20));
            int samples = Math.max(20, ctx.settings().getInt("samples", 180));
            double playerDamage = ctx.settings().getDouble("player-damage", 1.0);
            double mobDamage = ctx.settings().getDouble("mob-damage", 2.0);
            Location origin = ctx.origin();
            World world = ctx.world();
            CoreFactoryApi api = ctx.api();
            int rounds = seconds * 20 / interval;

            Tasks.repeat(api.plugin(), interval, rounds, step -> {
                for (Entity entity : world.getNearbyEntities(origin, zone, zone, zone)) {
                    if (!(entity instanceof LivingEntity living) || living.getLocation().distance(origin) > zone) {
                        continue;
                    }
                    if (living instanceof Player) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
                        living.damage(playerDamage);
                    } else {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0));
                        living.damage(mobDamage);
                    }
                }

                ThreadLocalRandom random = ThreadLocalRandom.current();
                BlockChangeSet set = new BlockChangeSet(samples);
                for (int i = 0; i < samples; i++) {
                    double dx;
                    double dy;
                    double dz;
                    do {
                        dx = (random.nextDouble() * 2 - 1) * zone;
                        dy = (random.nextDouble() * 2 - 1) * zone;
                        dz = (random.nextDouble() * 2 - 1) * zone;
                    } while (dx * dx + dy * dy + dz * dz > zone * zone);
                    int x = origin.getBlockX() + (int) Math.floor(dx);
                    int y = origin.getBlockY() + (int) Math.floor(dy);
                    int z = origin.getBlockZ() + (int) Math.floor(dz);
                    Block block = world.getBlockAt(x, y, z);
                    BlockData withered = wither(block.getType());
                    if (withered != null && !api.protection().isProtected(block)) {
                        set.set(x, y, z, withered);
                    }
                }
                submit(ctx, set);
                kit.effects().particle(origin, Particle.COMPOSTER, 30, zone * 0.5, 0.05, zone * 2 + 16);
            }, () -> {
            });
        }

        /** What a withering cloud turns a block into, or {@code null} if it is unaffected. */
        private static BlockData wither(Material type) {
            String name = type.name();
            if (type == Material.GRASS_BLOCK) {
                return Material.DIRT_PATH.createBlockData();
            }
            if (name.endsWith("_LEAVES") || name.contains("FLOWER") || name.contains("SAPLING")
                    || name.contains("MUSHROOM") || name.equals("SHORT_GRASS") || name.equals("TALL_GRASS")
                    || name.equals("FERN") || name.equals("LARGE_FERN") || name.equals("GRASS")) {
                return Material.AIR.createBlockData();
            }
            return null;
        }
    }

    /**
     * Napalm. Dropped, it detonates the moment it lands and spreads fire in an ellipse stretched along the way
     * it was falling. Placed and lit, it spreads fire in a circle. Blocks that catch fire may turn to magma.
     */
    static final class Napalm extends AbstractMechanic {

        Napalm(Toolkit kit) {
            super("napalm", "Detonates where it lands, spreading fire along its fall. Throw it to use it.", kit);
        }

        @Override
        public void onDropped(Item item, TntDefinition tnt, CoreFactoryApi api) {
            Vector[] fall = {item.getVelocity()};
            new BukkitRunnable() {
                private int age;

                @Override
                public void run() {
                    age++;
                    if (!item.isValid()) {
                        cancel();
                        return;
                    }
                    if ((item.isOnGround() && age > 2) || age > 200) {
                        cancel();
                        Location at = item.getLocation();
                        Vector horizontal = new Vector(fall[0].getX(), 0, fall[0].getZ());
                        item.remove();
                        api.detonate(tnt.id(), at, null, horizontal, 0);
                        return;
                    }
                    fall[0] = item.getVelocity();
                }
            }.runTaskTimer(api.plugin(), 1L, 1L);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            double stretch = ctx.settings().getDouble("stretch", 1.6);
            Vector fall = ctx.placed() == null ? ctx.direction() : null;
            Vector horizontal = fall == null ? new Vector() : new Vector(fall.getX(), 0, fall.getZ());
            double speed = horizontal.length();

            double ux = speed > 0.05 ? horizontal.getX() / speed : 1;
            double uz = speed > 0.05 ? horizontal.getZ() / speed : 0;
            double major = speed > 0.05 ? radius * (1 + stretch * Math.min(1.0, speed / 0.6)) : radius;
            double minor = radius;
            double vx = -uz;
            double vz = ux;

            Location origin = ctx.origin();
            World world = ctx.world();
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int reach = (int) Math.ceil(major);
            BlockChangeSet set = new BlockChangeSet();
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double along = dx * ux + dz * uz;
                    double across = dx * vx + dz * vz;
                    if ((along * along) / (major * major) + (across * across) / (minor * minor) > 1) {
                        continue;
                    }
                    if (random.nextDouble() < 0.08) {
                        continue;
                    }
                    int x = origin.getBlockX() + dx;
                    int z = origin.getBlockZ() + dz;
                    int ground = groundAt(world, x, origin.getBlockY(), z);
                    if (ground == Integer.MIN_VALUE) {
                        continue;
                    }
                    Block surface = world.getBlockAt(x, ground, z);
                    double roll = random.nextDouble();
                    if (roll < 0.05 && !ctx.api().protection().isProtected(surface)) {
                        set.set(x, ground, z, Material.MAGMA_BLOCK.createBlockData());
                    }
                    if (world.getBlockAt(x, ground + 1, z).getType().isAir()) {
                        set.set(x, ground + 1, z, roll < 0.012 ? Material.LAVA.createBlockData()
                                : Material.FIRE.createBlockData());
                    }
                }
            }
            submit(ctx, set);

            world.createExplosion(origin, 1.2f, false, false);
            kit.explosions().damage(origin, radius, damage(ctx, 4.0), ctx.source());
            kit.effects().boom(origin, 2f);
            kit.effects().particle(origin, Particle.FLAME, 40, radius * 0.5, 0.1, 64.0);
        }

        /** The y of the highest solid block near {@code y} at this column, or {@code MIN_VALUE} if none. */
        private static int groundAt(World world, int x, int y, int z) {
            for (int probe = y + 3; probe >= y - 6; probe--) {
                Block block = world.getBlockAt(x, probe, z);
                if (block.getType().isSolid() && !block.getType().isAir()) {
                    return probe;
                }
            }
            return Integer.MIN_VALUE;
        }
    }
}
