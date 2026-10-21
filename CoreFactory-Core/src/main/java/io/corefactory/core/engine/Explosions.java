package io.corefactory.core.engine;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.function.Predicate;

/**
 * Entity damage and knockback shared by the mechanics.
 */
public final class Explosions {

    private final Effects effects;

    public Explosions(Effects effects) {
        this.effects = effects;
    }

    public Effects effects() {
        return effects;
    }

    /**
     * Damages living entities within {@code radius}. Damage falls off linearly to zero at the edge.
     * {@code source} is credited as the attacker when it is not {@code null}.
     */
    public void damage(Location centre, double radius, double maxDamage, Entity source) {
        damage(centre, radius, maxDamage, source, entity -> true);
    }

    public void damage(Location centre, double radius, double maxDamage, Entity source,
                       Predicate<LivingEntity> filter) {
        World world = centre.getWorld();
        if (world == null || maxDamage <= 0) {
            return;
        }
        for (Entity entity : world.getNearbyEntities(centre, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || !filter.test(living)) {
                continue;
            }
            double distance = centre.distance(living.getLocation());
            if (distance > radius) {
                continue;
            }
            double amount = maxDamage * (1.0 - distance / radius);
            if (amount <= 0) {
                continue;
            }
            if (source != null) {
                living.damage(amount, source);
            } else {
                living.damage(amount);
            }
        }
    }

    /** Pushes entities within {@code radius} away from the centre, strongest near it. */
    public void push(Location centre, double radius, double strength, Vector extra) {
        World world = centre.getWorld();
        if (world == null) {
            return;
        }
        for (Entity entity : world.getNearbyEntities(centre, radius, radius, radius)) {
            Vector away = entity.getLocation().toVector().subtract(centre.toVector());
            double distance = away.length();
            if (distance > radius) {
                continue;
            }
            Vector impulse = distance < 0.01 ? new Vector(0, 1, 0) : away.normalize();
            double falloff = 1.0 - distance / radius;
            entity.setVelocity(entity.getVelocity().add(impulse.multiply(strength * falloff)).add(extra));
        }
    }
}
