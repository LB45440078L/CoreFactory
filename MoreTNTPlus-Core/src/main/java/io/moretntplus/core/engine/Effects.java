package io.moretntplus.core.engine;

import io.moretntplus.core.config.CoreSettings;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Sounds and particles, sent only to players near the effect and capped per tick. Nothing here is
 * broadcast world-wide, which keeps large blasts from flooding clients.
 */
public final class Effects {

    private CoreSettings settings;
    private int particlesThisTick;

    public Effects(CoreSettings settings) {
        this.settings = settings;
    }

    public void reconfigure(CoreSettings settings) {
        this.settings = settings;
    }

    /** Resets the particle budget. Called once per server tick by the core. */
    public void beginTick() {
        particlesThisTick = 0;
    }

    /** Players in the same world within {@code range} blocks of {@code at}. */
    public List<Player> nearby(Location at, double range) {
        World world = at.getWorld();
        List<Player> out = new ArrayList<>();
        if (world == null) {
            return out;
        }
        double range2 = range * range;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(at) <= range2) {
                out.add(player);
            }
        }
        return out;
    }

    /** An explosion's sound and a puff of smoke, for players within the effect view distance. */
    public void boom(Location at, float size) {
        for (Player player : nearby(at, settings.effectViewDistance())) {
            player.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, Math.min(4f, size), 0.9f + size * 0.02f);
        }
        particle(at, Particle.EXPLOSION, 1, 0.4, 0.0, settings.effectViewDistance());
    }

    /** A particle burst near {@code at}. Dropped silently once the per-tick budget is spent. */
    public void particle(Location at, Particle particle, int count, double spread, double extra, double range) {
        if (particlesThisTick >= settings.particlesPerTick()) {
            return;
        }
        for (Player player : nearby(at, range)) {
            player.spawnParticle(particle, at, count, spread, spread, spread, extra);
        }
        particlesThisTick += count;
    }

    /** A coloured dust particle, for outlines and fuse lines. */
    public void dust(Location at, Color color, float size, double range) {
        if (particlesThisTick >= settings.particlesPerTick()) {
            return;
        }
        Particle.DustOptions options = new Particle.DustOptions(color, size);
        for (Player player : nearby(at, range)) {
            player.spawnParticle(Particle.DUST, at, 1, 0.0, 0.0, 0.0, 0.0, options);
        }
        particlesThisTick++;
    }

    /** Draws a dust line between two points, one particle every {@code step} blocks. */
    public void line(Location from, Location to, Color color, double step, double range) {
        double length = from.distance(to);
        if (length < 0.01) {
            return;
        }
        double dx = (to.getX() - from.getX()) / length;
        double dy = (to.getY() - from.getY()) / length;
        double dz = (to.getZ() - from.getZ()) / length;
        for (double d = 0; d <= length; d += step) {
            dust(from.clone().add(dx * d, dy * d, dz * d), color, 0.8f, range);
        }
    }
}
