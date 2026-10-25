package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryBallistics;
import io.corefactory.core.artillery.ArtilleryCloud;
import io.corefactory.core.artillery.ArtilleryEffects.Column;
import io.corefactory.core.artillery.ArtillerySettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Light;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The effects that stay after a shell lands: the white phosphorus clouds and the napalm fires. A cloud sits on the ground of its burst. Its
 * particles fall from the burst, a few light blocks stand at the burst so the cloud gives light, and each step a creature inside its radius
 * takes the damage of its strength. Its strength halves every half-life and it clears after its duration, when its light blocks go. A napalm
 * burst sets fire to the air over the ground it covers, and the fire is put out after {@link #FIRE_SECONDS}, so no fire is left for good.
 *
 * <p>The tick runs every {@link #STEP_TICKS} ticks. The damage of a step is the damage of one tick times the step, so a creature takes its
 * damage in whole steps: a server gives a creature a short window after each hit, and a hit every tick would be ignored in it.
 *
 * <p>A cloud hisses when it is laid and then every few seconds while it hangs, and a fire crackles when it is lit and then every few seconds while
 * it burns. The interval and the volume are the presentation settings.
 */
public final class ArtilleryHazards {

    /** The ticks between two steps of the tick action: the damage and the check of the clouds and fires. */
    public static final int STEP_TICKS = 10;

    /** How long the fire of a napalm burst burns, in seconds, before it is put out. */
    static final int FIRE_SECONDS = 20;

    /** The light blocks a cloud stands, at the burst. */
    static final int LIGHT_BLOCKS = 3;

    /** The light level of a light block, the brightest there is. */
    private static final int LIGHT_LEVEL = 15;

    /** The height of the box a cloud's creatures are looked for in, above and below its ground, in blocks. */
    private static final double CLOUD_HEIGHT = 4.0;

    /** The side of a light block's display, in blocks: small, so the light block reads as a light, not a wall. */
    private static final float LIGHT_SIZE = 0.25f;

    private final ArtillerySettings.Presentation presentation;
    private long ticks;
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Fire> fires = new ArrayList<>();

    /**
     * @param presentation the interval and the volume of the hisses and the crackles
     */
    public ArtilleryHazards(ArtillerySettings.Presentation presentation) {
        this.presentation = presentation;
    }

    /** A cloud on the ground of its burst: its place, its settings, its start, and the light blocks that stand for it. */
    private record Cloud(World world, Location ground, ArtillerySettings.Phosphorus settings, long startTicks, List<UUID> lights) {
    }

    /** The fire of a napalm burst: its middle, the blocks it set alight, and the ticks it starts and is put out at. */
    private record Fire(World world, Location center, List<Location> blocks, long startTicks, long endTicks) {
    }

    /**
     * Starts a white phosphorus cloud: its particles, and its light blocks at the burst.
     *
     * @param world      the world of the burst
     * @param ground     the ground under the burst, where the cloud lies and its damage is dealt
     * @param burst      the point of the burst, in the air over the ground
     * @param settings   the cloud's radius, damage, duration, half-life and particles
     */
    public void addCloud(World world, Location ground, Location burst, ArtillerySettings.Phosphorus settings) {
        world.spawnParticle(Particle.END_ROD, burst, settings.lightParticles(), settings.cloudRadiusBlocks(),
                settings.cloudRadiusBlocks() / 2.0, settings.cloudRadiusBlocks(), 0.05);
        List<UUID> lights = new ArrayList<>(LIGHT_BLOCKS);
        for (int i = 0; i < LIGHT_BLOCKS; i++) {
            Location at = burst.clone().add(i - 1.0, 0.0, 0.0);
            Light light = (Light) Material.LIGHT.createBlockData();
            light.setLevel(LIGHT_LEVEL);
            BlockDisplay display = world.spawn(at, BlockDisplay.class, entity -> {
                entity.setBlock(light);
                entity.setTransformation(new Transformation(new Vector3f(-LIGHT_SIZE / 2, -LIGHT_SIZE / 2, -LIGHT_SIZE / 2),
                        new Quaternionf(), new Vector3f(LIGHT_SIZE, LIGHT_SIZE, LIGHT_SIZE), new Quaternionf()));
                entity.setPersistent(false);
            });
            lights.add(display.getUniqueId());
        }
        clouds.add(new Cloud(world, ground, settings, ticks, List.copyOf(lights)));
        ArtilleryCues.hiss(ground, presentation.hazardVolume());
    }

    /**
     * Sets fire to the air over the ground of the given columns, where a solid block lies under it and the air is free. The fire is put out
     * after {@link #FIRE_SECONDS}. Columns in unloaded chunks are left alone.
     *
     * @param world   the world of the burst
     * @param columns the columns the burst covers
     */
    public void addFire(World world, List<Column> columns) {
        List<Location> blocks = new ArrayList<>();
        for (Column column : columns) {
            if (!world.isChunkLoaded(column.x() >> 4, column.z() >> 4)) {
                continue;
            }
            int top = world.getHighestBlockYAt(column.x(), column.z());
            Block ground = world.getBlockAt(column.x(), top, column.z());
            Block above = ground.getRelative(BlockFace.UP);
            if (ground.getType().isSolid() && above.getType().isAir()) {
                above.setType(Material.FIRE);
                blocks.add(above.getLocation());
            }
        }
        if (blocks.isEmpty()) {
            return;
        }
        Location center = blocks.get(blocks.size() / 2).clone().add(0.5, 0.5, 0.5);
        fires.add(new Fire(world, center, List.copyOf(blocks), ticks, ticks + (long) FIRE_SECONDS * 20L));
        ArtilleryCues.crackle(center, presentation.hazardVolume());
    }

    /**
     * Runs one step: the damage of the clouds, their particles, the clearing of the clouds that have run their duration, and the putting out
     * of the fires that have burned long enough. Called by the tick action every {@link #STEP_TICKS} ticks.
     */
    public void tick() {
        ticks += STEP_TICKS;
        int interval = presentation.hazardIntervalTicks();
        Iterator<Cloud> cloudIterator = clouds.iterator();
        while (cloudIterator.hasNext()) {
            Cloud cloud = cloudIterator.next();
            long elapsed = ticks - cloud.startTicks();
            double seconds = elapsed * ArtilleryBallistics.SECONDS_PER_TICK;
            if (!ArtilleryCloud.standing(seconds, cloud.settings().durationSeconds())) {
                clearLights(cloud.lights());
                cloudIterator.remove();
                continue;
            }
            if (ArtilleryTrail.crossed(elapsed - STEP_TICKS, elapsed, interval)) {
                ArtilleryCues.hiss(cloud.ground(), presentation.hazardVolume());
            }
            hurt(cloud, ArtilleryCloud.strength(seconds, cloud.settings().halfLifeSeconds()));
        }
        Iterator<Fire> fireIterator = fires.iterator();
        while (fireIterator.hasNext()) {
            Fire fire = fireIterator.next();
            if (ticks >= fire.endTicks()) {
                putOut(fire);
                fireIterator.remove();
                continue;
            }
            long elapsed = ticks - fire.startTicks();
            if (ArtilleryTrail.crossed(elapsed - STEP_TICKS, elapsed, interval)) {
                ArtilleryCues.crackle(fire.center(), presentation.hazardVolume());
            }
        }
    }

    /** Removes the light blocks of every cloud. Called when the plugin stops; the clouds are not kept. */
    public void shutdown() {
        for (Cloud cloud : clouds) {
            clearLights(cloud.lights());
        }
        clouds.clear();
    }

    /** Deals a cloud's damage for one step to every living creature inside its radius, and draws its particles around its ground. */
    private void hurt(Cloud cloud, double strength) {
        World world = cloud.world();
        Location ground = cloud.ground();
        double radius = cloud.settings().cloudRadiusBlocks();
        DamageSource source = DamageSource.builder(DamageType.GENERIC).withDamageLocation(ground).build();
        for (LivingEntity living : world.getNearbyLivingEntities(ground, radius, CLOUD_HEIGHT, radius)) {
            double distance = Math.hypot(living.getLocation().getX() - ground.getX(), living.getLocation().getZ() - ground.getZ());
            double damage = ArtilleryCloud.damagePerTick(cloud.settings().damagePerTick(), strength, distance, radius)
                    * STEP_TICKS;
            if (damage > 0.0) {
                living.setNoDamageTicks(0);
                living.damage(damage, source);
            }
        }
        world.spawnParticle(Particle.END_ROD, ground.clone().add(0.0, 1.0, 0.0), 12, radius / 2.0, 1.0, radius / 2.0, 0.01);
    }

    /** Removes the light blocks of a cloud that has cleared, when they still stand. */
    private static void clearLights(List<UUID> lights) {
        for (UUID id : lights) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    /** Puts out the fire of a napalm burst: each block it set alight is turned back to air, when it is still fire. */
    private static void putOut(Fire fire) {
        for (Location at : fire.blocks()) {
            if (fire.world().isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
                Block block = at.getBlock();
                if (block.getType() == Material.FIRE) {
                    block.setType(Material.AIR);
                }
            }
        }
    }
}
