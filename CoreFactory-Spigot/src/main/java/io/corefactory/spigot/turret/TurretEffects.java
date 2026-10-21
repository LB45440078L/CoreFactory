package io.corefactory.spigot.turret;

import io.corefactory.core.turret.RoundType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * The particles of a shot: a muzzle flash of three particles and a tracer of a few particles along the line of fire. Every shot costs
 * eight particles at most, and the whole server gets a budget of particles per tick for turret effects, so many turrets firing at once
 * never flood the clients. A shot past the budget shows no tracer; the flash is kept while the budget allows it.
 */
public final class TurretEffects {

    /** The particles of one shot's muzzle flash. */
    static final int FLASH_PARTICLES = 3;

    /** The points of one shot's tracer. */
    static final int TRACER_POINTS = 5;

    /** The most particles all turrets may spawn in one server tick. */
    static final int PARTICLES_PER_TICK = 60;

    private static final double MUZZLE_OFFSET = 0.6;

    private int budget = PARTICLES_PER_TICK;

    /** Starts a new server tick, with the full particle budget again. */
    public void beginTick() {
        budget = PARTICLES_PER_TICK;
    }

    /**
     * Shows one shot: a muzzle flash at the barrel, then a tracer from the muzzle to where the shot ends.
     *
     * @param world the world of the shot
     * @param barrel the point the shot is fired from
     * @param direction the unit direction of the shot
     * @param end the point where the shot stops
     * @param type the round fired; an incendiary round has a flame tracer
     */
    public void shot(World world, Location barrel, Vector direction, Location end, RoundType type) {
        Location muzzle = barrel.clone().add(direction.clone().multiply(MUZZLE_OFFSET));
        if (budget >= FLASH_PARTICLES) {
            budget -= FLASH_PARTICLES;
            world.spawnParticle(Particle.FLAME, muzzle, FLASH_PARTICLES, 0.03, 0.03, 0.03, 0.01);
        }
        if (budget < TRACER_POINTS) {
            return;
        }
        budget -= TRACER_POINTS;
        Particle tracer = type == RoundType.INCENDIARY ? Particle.FLAME : Particle.CRIT;
        for (int point = 1; point <= TRACER_POINTS; point++) {
            double fraction = point / (double) TRACER_POINTS;
            Location at = muzzle.clone().add(end.clone().subtract(muzzle).multiply(fraction));
            world.spawnParticle(tracer, at, 1, 0, 0, 0, 0);
        }
    }
}
