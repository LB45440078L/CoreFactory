package io.corefactory.spigot.turret;

import io.corefactory.core.turret.BeamPlan;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * The particles of a laser shot: a beam of small dust from the muzzle to where it stops, and a burst where it hits. A level 1 to 4 laser is red;
 * a level 5 laser is blue, the beam and the burst alike, since the END_ROD burst cannot take a colour. A shot draws fewer than forty particles
 * in all (see {@link BeamPlan}). The whole server gets a budget of laser particles per tick, so many lasers firing at once never flood the
 * clients; a shot past the budget draws nothing.
 */
public final class LaserEffects {

    /** The most laser particles all turrets may draw in one server tick. */
    static final int PARTICLES_PER_TICK = 160;

    private static final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(255, 40, 40), 0.6f);
    private static final Particle.DustOptions BLUE = new Particle.DustOptions(Color.fromRGB(79, 182, 255), 0.6f);
    private static final double IMPACT_SPREAD = 0.15;
    private static final double IMPACT_SPEED = 0.05;

    private int budget = PARTICLES_PER_TICK;

    /** Starts a new server tick, with the full particle budget again. */
    public void beginTick() {
        budget = PARTICLES_PER_TICK;
    }

    /**
     * Shows one shot: the beam from the muzzle along its direction, and a burst where it hits.
     *
     * @param world     the world of the shot
     * @param muzzle    the point the beam starts from
     * @param direction the unit direction of the beam
     * @param plan      the beam's length and particles
     * @param impact    where the beam stops on a block or an entity, or null when it stops at its range
     * @param blue      true for the blue beam of a level 5 laser, false for the red beam of the others
     */
    public void shot(World world, Location muzzle, Vector direction, BeamPlan plan, Location impact, boolean blue) {
        boolean hit = impact != null;
        int cost = plan.totalParticles(hit);
        if (budget < cost) {
            return;
        }
        budget -= cost;
        Particle.DustOptions colour = blue ? BLUE : RED;
        for (int index = 0; index < plan.particles(); index++) {
            Location at = muzzle.clone().add(direction.clone().multiply(plan.distanceOf(index)));
            world.spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, colour);
        }
        if (hit) {
            if (blue) {
                world.spawnParticle(Particle.DUST, impact, BeamPlan.IMPACT_PARTICLES, IMPACT_SPREAD, IMPACT_SPREAD, IMPACT_SPREAD,
                        0, BLUE);
            } else {
                world.spawnParticle(Particle.END_ROD, impact, BeamPlan.IMPACT_PARTICLES, IMPACT_SPREAD, IMPACT_SPREAD, IMPACT_SPREAD,
                        IMPACT_SPEED);
            }
        }
    }
}
