package io.corefactory.paper.railgun;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.railgun.RailgunBeam;
import io.corefactory.core.railgun.RailgunLevel;
import io.corefactory.core.railgun.RailgunRules;
import io.corefactory.core.railgun.RailgunSettings;
import io.corefactory.core.turret.BeamPlan;
import io.corefactory.paper.turret.TurretStrike;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * The rail gun's shots in flight. Each shot is one {@link RailgunBeam}: its frontier moves toward the target's centre each server tick, and
 * the beam's points are drawn at once. A shot ends in one of two ways. When its frontier arrives at the target, the target takes the shot's
 * damage, after the armour the shot does not ignore, through the same strike as the laser, and a blue burst is drawn where it hit. When a
 * condition ends it first, the shot ends without damage: the target has died or been removed, the target has left the world, or the chunk
 * under the frontier is not loaded.
 *
 * <p>The points are drawn with one {@code spawnParticle} call each. Paper places every particle of one call at one location, so a point at
 * its own position needs a call of its own; a count of one with no offset puts the particle exactly on the point. The beam's budget bounds
 * the calls: at the default budget of 96 points, one shot draws at most 95 particles a tick, the flame frontier as a flame, the strands as
 * a crimson-to-ember colour transition and the spiral as a blue-to-ice one. The shots in flight share the server-wide cap, see {@link #tick}.
 *
 * <p>A shot holds its target only until it ends, and the list holds the shot only until then, so a dead or removed target is not kept.
 * Nothing here loads a chunk: the frontier's chunk is only checked, and the target is only read. The class is used on the main thread.
 */
public final class RailgunShots {

    /** The size of the dust of the beam's points, which the laser's dust uses too. */
    private static final float DUST_SIZE = 0.6f;
    /** The strands: a crimson that warms to ember along each particle's life, so the beam reads as heated rather than as a flat red line. */
    private static final Particle.DustTransition STRAND = new Particle.DustTransition(
            Color.fromRGB(255, 20, 60), Color.fromRGB(255, 150, 40), DUST_SIZE);
    /** The blue spiral: a deep blue that cools to ice white, wrapping the strands. */
    private static final Particle.DustTransition SPIRAL = new Particle.DustTransition(
            Color.fromRGB(40, 150, 255), Color.fromRGB(210, 245, 255), DUST_SIZE);
    /** The blue of the hit burst: the laser's blue for its level 5. */
    private static final Particle.DustOptions BURST = new Particle.DustOptions(Color.fromRGB(79, 182, 255), DUST_SIZE);
    /** How far the burst of a hit spreads from the target's centre, in blocks, as the laser's burst does. */
    private static final double IMPACT_SPREAD = 0.15;

    private final RailgunSettings settings;
    private final List<Shot> shots = new ArrayList<>();

    /**
     * @param core     the core of the plugin; the shots use none of its services yet, so it is only checked to be present
     * @param settings the rail gun's settings: the helix, the strands, the spiral, the hit distance and the particle budget
     */
    public RailgunShots(CoreFactoryCore core, RailgunSettings settings) {
        Objects.requireNonNull(core, "core");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /**
     * Starts one shot from a source toward a target's centre. Does nothing when the target is dead or removed, when the source has no
     * loaded world, when the target is in another world, or when the chunk of the source is not loaded.
     *
     * @param source the rail gun's muzzle, a point in the world
     * @param target the entity the shot flies toward
     * @param level  the level of the rail gun that fires, which sets the frontier's speed and the damage
     */
    public void fire(Location source, LivingEntity target, RailgunLevel level) {
        Objects.requireNonNull(level, "level");
        World world = source == null ? null : source.getWorld();
        if (world == null || target == null) {
            return;
        }
        if (!isAlive(target) || !world.equals(target.getWorld()) || !isLoaded(world)
                || !world.isChunkLoaded(chunkOf(source.getX()), chunkOf(source.getZ()))) {
            return;
        }
        shots.add(new Shot(world, target, level,
                new RailgunBeam(settings, level, source.getX(), source.getY(), source.getZ())));
    }

    /**
     * Advances every shot one server tick, draws its points, and ends the shots that have arrived or that a condition has ended. The shots
     * in flight share the settings' cap on points a tick: each draws an equal share of it, and its own budget bounds it further. Run once
     * each server tick, from the plugin's tick scheduler.
     */
    public void tick() {
        int share = shots.isEmpty() ? 0 : settings.maxParticlesPerTick() / shots.size();
        Iterator<Shot> iterator = shots.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().step(settings, share)) {
                iterator.remove();
            }
        }
    }

    /** Removes every shot, with no damage. Called when the plugin stops. */
    public void release() {
        shots.clear();
    }

    /**
     * Whether a shot may advance this tick, given the facts about its target and its frontier. A shot that may not advance ends without
     * damage.
     *
     * @param targetAlive    true when the target is valid and not dead
     * @param targetInWorld  true when the target is still in the shot's world
     * @param frontierLoaded true when the chunk under the frontier is loaded
     * @return true when the shot continues
     */
    static boolean mayAdvance(boolean targetAlive, boolean targetInWorld, boolean frontierLoaded) {
        return targetAlive && targetInWorld && frontierLoaded;
    }

    /**
     * The damage a shot leaves in its target: the level's damage, less the armour the shot does not ignore, from the rail gun's rules.
     *
     * @param settings  the rail gun's settings
     * @param level     the level of the rail gun
     * @param armour    the target's armour value
     * @param toughness the target's armour toughness
     * @return the damage in HP, never negative
     */
    static double damageOf(RailgunSettings settings, RailgunLevel level, double armour, double toughness) {
        return RailgunRules.damageAfterArmour(settings, level, armour, toughness);
    }

    /**
     * The chunk coordinate of a block coordinate: the block's floor, divided by 16.
     *
     * @param blockCoordinate an x or a z, in blocks
     * @return the chunk's x or z
     */
    static int chunkOf(double blockCoordinate) {
        return (int) Math.floor(blockCoordinate) >> 4;
    }

    /** True when an entity is spawned and not dead. */
    private static boolean isAlive(LivingEntity target) {
        return target.isValid() && !target.isDead();
    }

    /** True when the world is the one the server still holds, so it is loaded. */
    private static boolean isLoaded(World world) {
        return Bukkit.getWorld(world.getUID()) == world;
    }

    /** Draws the points of one tick: a flame at the frontier, the red strands and the blue spiral as dust. */
    private static void draw(World world, List<RailgunBeam.Sample> points) {
        for (RailgunBeam.Sample point : points) {
            switch (point.kind()) {
                case RED_STRAND -> world.spawnParticle(Particle.DUST_COLOR_TRANSITION, point.x(), point.y(), point.z(), 1, 0.0, 0.0,
                        0.0, 0.0, STRAND);
                case FLAME_FRONTIER -> world.spawnParticle(Particle.FLAME, point.x(), point.y(), point.z(), 1, 0.0, 0.0, 0.0, 0.0);
                case BLUE_SPIRAL -> world.spawnParticle(Particle.DUST_COLOR_TRANSITION, point.x(), point.y(), point.z(), 1, 0.0, 0.0,
                        0.0, 0.0, SPIRAL);
            }
        }
    }

    /** A shot in flight: its world, its target and its beam. The target is held only while the shot is in the list. */
    private static final class Shot {
        private final World world;
        private final LivingEntity target;
        private final RailgunLevel level;
        private final RailgunBeam beam;

        Shot(World world, LivingEntity target, RailgunLevel level, RailgunBeam beam) {
            this.world = world;
            this.target = target;
            this.level = level;
            this.beam = beam;
        }

        /**
         * Advances the shot one tick. Draws its points while it flies, or applies its damage and draws the burst when it arrives.
         *
         * @param settings the rail gun's settings
         * @param share    the most points this shot may draw this tick, its share of the cap on all the shots
         * @return true when the shot has ended and must be removed
         */
        boolean step(RailgunSettings settings, int share) {
            boolean inWorld = world.equals(target.getWorld());
            boolean loaded = world.isChunkLoaded(chunkOf(beam.frontierX()), chunkOf(beam.frontierZ()));
            if (!mayAdvance(isAlive(target), inWorld, loaded)) {
                return true;
            }
            BoundingBox box = target.getBoundingBox();
            double cx = box.getCenterX();
            double cy = box.getCenterY();
            double cz = box.getCenterZ();
            if (!beam.advance(cx, cy, cz)) {
                draw(world, beam.points(share));
                return false;
            }
            double damage = damageOf(settings, level, TurretStrike.armourOf(target), TurretStrike.toughnessOf(target));
            TurretStrike.strike(target, damage);
            world.spawnParticle(Particle.DUST, cx, cy, cz, BeamPlan.IMPACT_PARTICLES,
                    IMPACT_SPREAD, IMPACT_SPREAD, IMPACT_SPREAD, 0.0, BURST);
            return true;
        }
    }
}
