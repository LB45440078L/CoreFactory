package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.Position;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

/**
 * The sounds and the particles of the artillery's shots, each one a single call to the world: the launch, the whistle of a shell in the air, the
 * trail it leaves, the landing, the hiss of a phosphorus cloud, the crackle of a napalm fire, and the reload. The shot's state decides when a cue
 * plays; the volumes and the counts come from the presentation settings. The sounds are vanilla ones, and each is named here so it is found in one
 * place. A blast's own explosion sound is not here: the core's blast plays it.
 */
final class ArtilleryCues {

    /** The launch's report: the vanilla explosion, played low so it sounds like a heavy gun. */
    static final Sound LAUNCH = Sound.ENTITY_GENERIC_EXPLODE;

    /** The whistle of a shell in the air: the breeze's whirl, played high and faint. */
    static final Sound WHISTLE = Sound.ENTITY_BREEZE_WHIRL;

    /** The pitch of the whistle. */
    static final float WHISTLE_PITCH = 1.5f;

    /** The sound of a piece that is ready to fire again: a mechanical piston. */
    static final Sound RELOAD = Sound.BLOCK_PISTON_CONTRACT;

    /** The thud of a shell that lands without a blast: a heavy fall. */
    static final Sound THUD = Sound.ENTITY_GENERIC_BIG_FALL;

    /** The pitch of the thud. */
    static final float THUD_PITCH = 0.8f;

    /** The hiss of a phosphorus cloud: the fire going out. */
    static final Sound HISS = Sound.BLOCK_FIRE_EXTINGUISH;

    /** The crackle of a napalm fire: the ambient fire. */
    static final Sound CRACKLE = Sound.BLOCK_FIRE_AMBIENT;

    /** The pitch of the reload, the hiss and the crackle: the vanilla pitch. The launch report takes its pitch from the settings. */
    static final float NORMAL_PITCH = 1.0f;

    /** The part of a flame burst of the launch that flashes. */
    private static final int MUZZLE_FLAME = 8;

    /** The smoke that drifts from the barrel at a launch. */
    private static final int MUZZLE_SMOKE = 16;

    /** The dust a blast on the ground throws up, as a share of its debris count. */
    private static final double DUST_SHARE = 0.5;

    private ArtilleryCues() {
    }

    /**
     * The launch of a shot: the report, heard about the volume's distance, and a muzzle flash and a puff of smoke at the barrel's tip.
     *
     * @param tip    the point at the barrel's tip, where the shell leaves the barrel
     * @param volume the volume of the report
     * @param pitch  the pitch of the report
     */
    static void launch(Location tip, double volume, double pitch) {
        World world = tip.getWorld();
        world.playSound(tip, LAUNCH, SoundCategory.BLOCKS, (float) volume, (float) pitch);
        world.spawnParticle(Particle.FLAME, tip, MUZZLE_FLAME, 0.15, 0.15, 0.15, 0.08);
        world.spawnParticle(Particle.LARGE_SMOKE, tip, MUZZLE_SMOKE, 0.4, 0.4, 0.4, 0.02);
    }

    /**
     * The whistle of a shell in the air: a faint sound at the shell, so only those near it hear it.
     *
     * @param at     the shell's position
     * @param volume the volume of the whistle
     */
    static void whistle(Location at, double volume) {
        at.getWorld().playSound(at, WHISTLE, SoundCategory.AMBIENT, (float) volume, WHISTLE_PITCH);
    }

    /**
     * The trail a shell leaves: smoke puffs spread between its position of the last tick and its position now, at most {@code maxPuffs} of them.
     *
     * @param world    the world of the shell
     * @param from     the shell's position of the last tick
     * @param to       the shell's position now
     * @param maxPuffs the most puffs of the trail this tick
     */
    static void trail(World world, Position from, Position to, int maxPuffs) {
        for (Position puff : ArtilleryTrail.puffs(from, to, maxPuffs)) {
            world.spawnParticle(Particle.SMOKE, puff.x(), puff.y(), puff.z(), 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    /**
     * The mechanical sound of a piece that is ready to fire again.
     *
     * @param at     the piece's block
     * @param volume the volume of the sound
     */
    static void reloaded(Location at, double volume) {
        at.getWorld().playSound(at, RELOAD, SoundCategory.BLOCKS, (float) volume, NORMAL_PITCH);
    }

    /**
     * The thud of a shell that lands without a blast.
     *
     * @param at     the landing point
     * @param volume the volume of the thud
     */
    static void thud(Location at, double volume) {
        at.getWorld().playSound(at, THUD, SoundCategory.BLOCKS, (float) volume, THUD_PITCH);
    }

    /**
     * The hiss of a phosphorus cloud, at its ground.
     *
     * @param at     the ground under the cloud
     * @param volume the volume of the hiss
     */
    static void hiss(Location at, double volume) {
        at.getWorld().playSound(at, HISS, SoundCategory.BLOCKS, (float) volume, NORMAL_PITCH);
    }

    /**
     * The crackle of a napalm fire.
     *
     * @param at     the middle of the fire
     * @param volume the volume of the crackle
     */
    static void crackle(Location at, double volume) {
        at.getWorld().playSound(at, CRACKLE, SoundCategory.BLOCKS, (float) volume, NORMAL_PITCH);
    }

    /**
     * The debris of a blast on the ground: pieces of the ground the blast throws up, and a cloud of its dust. Only a blast on the ground throws debris;
     * one in the air throws none.
     *
     * @param ground the ground the blast lands on, at the middle of its block
     * @param count  the debris particles; the dust is half of this
     */
    static void debris(Location ground, int count) {
        World world = ground.getWorld();
        BlockData block = world.getBlockData(ground);
        world.spawnParticle(Particle.BLOCK, ground.clone().add(0.0, 0.5, 0.0), count, 1.0, 0.5, 1.0, 0.1, block);
        world.spawnParticle(Particle.CLOUD, ground.clone().add(0.0, 1.0, 0.0), (int) (count * DUST_SHARE), 2.0, 0.5, 2.0, 0.02);
    }
}
