package io.moretntplus.core.mechanics;

import io.moretntplus.api.DetonationContext;
import io.moretntplus.api.Mechanic;
import io.moretntplus.api.MoreTntPlusApi;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mechanics that act on creatures and players instead of blocks. None of them destroys terrain.
 */
final class EntityMechanics {

    private EntityMechanics() {
    }

    static List<Mechanic> all(Toolkit kit) {
        return List.of(
                new Concussion(kit),
                new Launch(kit),
                new Teleport(kit),
                new GravityFlip(kit));
    }

    /** Non-lethal crowd control: slowness, nausea and blindness for everything in range. */
    static final class Concussion extends AbstractMechanic {

        Concussion(Toolkit kit) {
            super("concussion", "No block damage. Slows, nauseates and blinds everything in range.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            int slow = Math.max(1, ctx.settings().getInt("slowness-seconds", 8)) * 20;
            int nausea = Math.max(1, ctx.settings().getInt("nausea-seconds", 12)) * 20;
            int blind = Math.max(1, ctx.settings().getInt("blindness-seconds", 5)) * 20;
            Location origin = ctx.origin();
            for (Entity entity : nearby(origin, radius)) {
                if (entity instanceof LivingEntity living) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slow, 2));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nausea, 0));
                    living.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blind, 0));
                }
            }
            kit.effects().boom(origin, 2f);
            kit.effects().particle(origin, Particle.SONIC_BOOM, 1, 0.0, 0.0, 64.0);
        }
    }

    /** Throws entities up and away from the centre with a configurable vector, without fall damage. */
    static final class Launch extends AbstractMechanic {

        Launch(Toolkit kit) {
            super("launch", "Knocks everything in range upward and outward. Fall damage is cancelled.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            double up = ctx.settings().getDouble("up", 1.4);
            double radial = ctx.settings().getDouble("radial", 1.6);
            Location origin = ctx.origin();
            kit.explosions().push(origin, radius, radial, new Vector(0, up, 0));
            for (Entity entity : nearby(origin, radius)) {
                if (entity instanceof LivingEntity living) {
                    living.setFallDistance(0f);
                }
            }
            kit.explosions().damage(origin, radius, damage(ctx, 0.0), ctx.source());
            kit.effects().boom(origin, 2f);
        }
    }

    /**
     * Moves everything in range to a destination. A placed teleport TNT with a target sends entities there,
     * keeping their formation; otherwise they land at a random spot nearby. Only entities move, not blocks.
     */
    static final class Teleport extends AbstractMechanic {

        Teleport(Toolkit kit) {
            super("teleport", "Teleports entities in range to a set target, or to a random spot nearby.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            double offset = ctx.settings().getDouble("random-offset", 10.0);
            Location origin = ctx.origin();
            World world = ctx.world();

            Location destination = null;
            if (ctx.placed() != null && ctx.placed().target() != null) {
                destination = ctx.placed().target().clone().add(0.5, 0.0, 0.5);
            }
            if (destination == null) {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                int x = origin.getBlockX() + (int) Math.round((random.nextDouble() * 2 - 1) * offset);
                int z = origin.getBlockZ() + (int) Math.round((random.nextDouble() * 2 - 1) * offset);
                int y = world.getHighestBlockYAt(x, z) + 1;
                destination = new Location(world, x + 0.5, y, z + 0.5);
            }

            kit.effects().particle(origin, Particle.PORTAL, 30, 0.5, 0.4, 64.0);
            for (Entity entity : nearby(origin, radius)) {
                if (!(entity instanceof LivingEntity) || entity instanceof TNTPrimed) {
                    continue;
                }
                Location from = entity.getLocation();
                Location to = destination.clone().add(from.getX() - origin.getX(), from.getY() - origin.getY(),
                        from.getZ() - origin.getZ());
                to.setYaw(from.getYaw());
                to.setPitch(from.getPitch());
                entity.teleport(to);
            }
            kit.effects().particle(destination, Particle.PORTAL, 30, 0.5, 0.4, 64.0);
        }
    }

    /**
     * Makes everything in range float upward for a while, the nearest thing this server can do to an inverted
     * gravity field: players get levitation, other mobs have gravity switched off and restored afterwards.
     */
    static final class GravityFlip extends AbstractMechanic {

        GravityFlip(Toolkit kit) {
            super("gravity_flip", "Makes everything in range float upward for a while, then fall again.", kit);
        }

        @Override
        public void detonate(DetonationContext ctx) {
            double radius = ctx.tnt().radius();
            int duration = Math.max(10, ctx.settings().getInt("duration-ticks", 100));
            double lift = ctx.settings().getDouble("lift", 0.9);
            MoreTntPlusApi api = ctx.api();
            Location origin = ctx.origin();
            for (Entity entity : nearby(origin, radius)) {
                if (!(entity instanceof LivingEntity living) || entity instanceof TNTPrimed) {
                    continue;
                }
                if (living instanceof Player player) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, 0));
                    player.setVelocity(new Vector(0, lift, 0));
                    continue;
                }
                boolean original = living.hasGravity();
                living.setGravity(false);
                living.setVelocity(new Vector(0, lift, 0));
                api.scheduleLater(duration, () -> {
                    if (living.isValid()) {
                        living.setGravity(original);
                    }
                });
            }
            kit.effects().boom(origin, 2f);
            kit.effects().particle(origin, Particle.END_ROD, 40, radius * 0.5, 0.3, 64.0);
        }
    }

    /** Entities within {@code radius} of the origin, in the same world. */
    static List<Entity> nearby(Location origin, double radius) {
        List<Entity> out = new ArrayList<>();
        World world = origin.getWorld();
        if (world == null) {
            return out;
        }
        for (Entity entity : world.getNearbyEntities(origin, radius, radius, radius)) {
            if (entity.getLocation().distanceSquared(origin) <= radius * radius) {
                out.add(entity);
            }
        }
        return out;
    }
}
