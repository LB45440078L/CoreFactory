package io.corefactory.paper.audio;

import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.energy.GeneratorKind;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Plays the machine sounds of the resource pack, the sound events {@code corefactory:KEY} named in its sounds.json. The
 * tickers call the hooks here. A periodic sound fires at most once every few seconds per device: each device has its own
 * offset within the interval, so the devices do not all sound on the same tick. The volume and the streaming of each sound
 * are set in the pack, so the calls here play at full volume and normal pitch.
 */
public final class MachineSounds {

    private static final String NAMESPACE = "corefactory:";
    private static final int GENERATOR_HUM_TICKS = 40;
    private static final int DRILL_RUN_TICKS = 60;
    private static final int PUMP_RUN_TICKS = 40;
    private static final String PUMP_KEY = "water_pump";

    private MachineSounds() {
    }

    /** Plays a pack sound at a location, to every player who can hear it. */
    public static void play(Location at, String key, float volume, float pitch) {
        World world = at.getWorld();
        if (world != null) {
            world.playSound(at, NAMESPACE + key, SoundCategory.BLOCKS, volume, pitch);
        }
    }

    /** Plays a pack sound at the player's own position, for the player only. */
    public static void play(Player player, String key, float volume, float pitch) {
        player.playSound(player.getLocation(), NAMESPACE + key, SoundCategory.PLAYERS, volume, pitch);
    }

    /**
     * A generator that gave power this tick hums, once every two seconds, with the sound of its kind. The block is the
     * generator's anchor block.
     */
    public static void generatorHum(GeneratorKind kind, Location block, float volume) {
        play(block.clone().add(0.5, 0.5, 0.5), SoundKeys.generator(kind), volume, 1.0f);
    }

    /** True when a generator at this block hums this tick. Each generator hums once every two seconds, at its own offset. */
    public static boolean humDue(Location block, long tick) {
        return due(tick, block, GENERATOR_HUM_TICKS);
    }

    /**
     * A drill that runs this tick sounds, once every three seconds, with the sound of its kind. The block is the drill's
     * block.
     */
    public static void drillRun(boolean running, DrillKind kind, Location block, long tick) {
        if (running && due(tick, block, DRILL_RUN_TICKS)) {
            play(block.clone().add(0.5, 0.5, 0.5), SoundKeys.drill(kind), 1.0f, 1.0f);
        }
    }

    /**
     * A pump that runs sounds its water, once every two seconds, with the pump sound. Each level sounds a little higher than the one
     * below it. The block is the pump's own block.
     */
    public static void pumpRun(Location block, int level, long tick) {
        if (due(tick, block, PUMP_RUN_TICKS)) {
            play(block.clone().add(0.5, 0.5, 0.5), PUMP_KEY, 1.0f, 0.9f + 0.1f * (level - 1));
        }
    }

    /**
     * A machine that is working sounds its cycle, from the ticker's own work interval, with the sound of its machine id. A
     * machine with no sound of its own runs the generic lab sound.
     */
    public static void working(String machineId, Location at) {
        play(at, SoundKeys.process(machineId), 1.0f, 1.0f);
    }

    /** A machine that completes a pass sounds its finish, if it has one: the cartridge press stamps a finished cartridge. */
    public static void complete(String machineId, Location at) {
        SoundKeys.completion(machineId).ifPresent(key -> play(at, key, 1.0f, 1.0f));
    }

    /** True on one tick in every interval for the device at a location. The offset comes from the block position. */
    private static boolean due(long tick, Location at, int intervalTicks) {
        int offset = Math.floorMod(at.getBlockX() * 31 + at.getBlockY() * 17 + at.getBlockZ(), intervalTicks);
        return Math.floorMod(tick + offset, intervalTicks) == 0;
    }
}
