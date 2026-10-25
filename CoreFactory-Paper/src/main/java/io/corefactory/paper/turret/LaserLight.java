package io.corefactory.paper.turret;

import io.corefactory.core.turret.LaserRules;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Display;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The light of a laser shot. The turret's model glows at full brightness, and a light block stands in the air in front of the barrel, both
 * for {@link LaserRules#GLOW_TICKS} ticks, then go back as they were. Vanilla light has no colour, so the light is white: the red comes from
 * the glowing model and the beam. The light block is placed only on air, outside the claims, in a loaded chunk, and never over another
 * block. A shot that comes while a light or glow still stands restarts its time.
 */
final class LaserLight {

    private static final double FRONT_DISTANCE = 1.0;

    private final TurretProtection protection;
    private final Map<String, Placed> lights = new HashMap<>();
    private final Map<UUID, Glow> glows = new HashMap<>();
    private final Map<String, Held> held = new HashMap<>();

    /**
     * A light block a shot placed: where it stands, what it was before, and the tick it goes back at.
     *
     * @param block    the block's location
     * @param original the block's data before the light
     * @param until    the tick at which it is set back
     */
    private record Placed(Location block, BlockData original, long until) {
    }

    /**
     * A light a rail gun holds: where it stands, what the block was before, and the level it shows now. It stays until the rail gun sets it
     * back to 0.
     *
     * @param block    the block's location
     * @param original the block's data before the light
     * @param level    the light level it shows, from 1 to 15
     */
    private record Held(Location block, BlockData original, int level) {
    }

    /**
     * A model that glows: its display, its brightness before the shot, and the tick it goes back at.
     *
     * @param display the display's id
     * @param before  the brightness it had, or null when it followed the light of its place
     * @param until   the tick at which it is set back
     */
    private record Glow(UUID display, Display.Brightness before, long until) {
    }

    /**
     * @param protection the claim systems; a light is never placed where one protects
     */
    LaserLight(TurretProtection protection) {
        this.protection = protection;
    }

    /**
     * Sets back the lights and glows whose time is up at this tick.
     *
     * @param now the current tick
     */
    void tick(long now) {
        for (Iterator<Placed> it = lights.values().iterator(); it.hasNext(); ) {
            Placed placed = it.next();
            if (placed.until() <= now) {
                it.remove();
                restore(placed);
            }
        }
        for (Iterator<Glow> it = glows.values().iterator(); it.hasNext(); ) {
            Glow glow = it.next();
            if (glow.until() <= now) {
                it.remove();
                restore(glow);
            }
        }
    }

    /**
     * Lights the air block in front of a barrel, when that block is air outside the claims in a loaded chunk, and is not the turret's own
     * block. A block already lit by an earlier shot only has its time restarted.
     *
     * @param barrel   the point the shot is fired from
     * @param heading  the unit direction of the shot
     * @param turretAt the block the turret stands in
     * @param now      the current tick
     */
    void flash(Location barrel, Vector heading, Location turretAt, long now) {
        Location front = barrel.clone().add(heading.clone().multiply(FRONT_DISTANCE));
        World world = front.getWorld();
        if (world == null) {
            return;
        }
        int x = front.getBlockX();
        int y = front.getBlockY();
        int z = front.getBlockZ();
        if (x == turretAt.getBlockX() && y == turretAt.getBlockY() && z == turretAt.getBlockZ()) {
            return;
        }
        String key = key(world, x, y, z);
        Placed lit = lights.get(key);
        if (lit != null) {
            lights.put(key, new Placed(lit.block(), lit.original(), now + LaserRules.GLOW_TICKS));
            return;
        }
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }
        Block block = world.getBlockAt(x, y, z);
        if (!block.getType().isAir() || protection.protects(block.getLocation())) {
            return;
        }
        lights.put(key, new Placed(block.getLocation(), block.getBlockData(), now + LaserRules.GLOW_TICKS));
        block.setBlockData(lightData(LaserRules.LIGHT_LEVEL), false);
    }

    /**
     * Makes a model glow at full brightness. A model already glowing only has its time restarted.
     *
     * @param display the item display of the turret
     * @param now     the current tick
     */
    void glow(Display display, long now) {
        UUID id = display.getUniqueId();
        Glow glowing = glows.get(id);
        Display.Brightness before = glowing != null ? glowing.before() : display.getBrightness();
        glows.put(id, new Glow(id, before, now + LaserRules.GLOW_TICKS));
        display.setBrightness(new Display.Brightness(LaserRules.FULL_LIGHT, LaserRules.FULL_LIGHT));
    }

    /**
     * Holds a light level at an air block until it is set again, as a rail gun does while it charges: the block takes the level, and a level
     * change sets it again. A level of 0 sets the block back as it was. A block that is not air, is protected or is in an unloaded chunk takes
     * no light.
     *
     * @param block the block the light stands in
     * @param level the light level, from 0 to 15; 0 sets the block back
     */
    void hold(Location block, int level) {
        World world = block.getWorld();
        if (world == null) {
            return;
        }
        String key = key(world, block.getBlockX(), block.getBlockY(), block.getBlockZ());
        Held lit = held.get(key);
        if (level <= 0) {
            if (lit != null) {
                held.remove(key);
                restore(lit.block(), lit.original());
            }
            return;
        }
        if (lit != null) {
            if (lit.level() != level && world.getBlockAt(block).getType() == Material.LIGHT) {
                held.put(key, new Held(lit.block(), lit.original(), level));
                world.getBlockAt(block).setBlockData(lightData(level), false);
            }
            return;
        }
        if (!world.isChunkLoaded(block.getBlockX() >> 4, block.getBlockZ() >> 4)) {
            return;
        }
        Block at = world.getBlockAt(block);
        if (!at.getType().isAir() || protection.protects(at.getLocation())) {
            return;
        }
        held.put(key, new Held(at.getLocation(), at.getBlockData(), level));
        at.setBlockData(lightData(level), false);
    }

    /** Sets back every light in a chunk that is unloading, now, so no light is left in the saved chunk. */
    void restoreIn(Chunk chunk) {
        for (Iterator<Placed> it = lights.values().iterator(); it.hasNext(); ) {
            Placed placed = it.next();
            Location at = placed.block();
            if (at.getWorld() != null && at.getWorld().equals(chunk.getWorld())
                    && (at.getBlockX() >> 4) == chunk.getX() && (at.getBlockZ() >> 4) == chunk.getZ()) {
                it.remove();
                restore(placed);
            }
        }
        for (Iterator<Held> it = held.values().iterator(); it.hasNext(); ) {
            Held light = it.next();
            Location at = light.block();
            if (at.getWorld() != null && at.getWorld().equals(chunk.getWorld())
                    && (at.getBlockX() >> 4) == chunk.getX() && (at.getBlockZ() >> 4) == chunk.getZ()) {
                it.remove();
                restore(light.block(), light.original());
            }
        }
    }

    /** Sets back every light and glow at once. Called when the plugin stops. */
    void restoreAll() {
        for (Placed placed : lights.values()) {
            restore(placed);
        }
        lights.clear();
        for (Glow glow : glows.values()) {
            restore(glow);
        }
        glows.clear();
        for (Held light : held.values()) {
            restore(light.block(), light.original());
        }
        held.clear();
    }

    private static void restore(Placed placed) {
        restore(placed.block(), placed.original());
    }

    private static void restore(Location at, BlockData original) {
        World world = at.getWorld();
        if (world == null || !world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
            return;
        }
        Block block = world.getBlockAt(at);
        if (block.getType() == Material.LIGHT) {
            block.setBlockData(original, false);
        }
    }

    private static void restore(Glow glow) {
        if (Bukkit.getEntity(glow.display()) instanceof Display display) {
            display.setBrightness(glow.before());
        }
    }

    private static BlockData lightData(int level) {
        Light light = (Light) Material.LIGHT.createBlockData();
        light.setLevel(Math.max(0, Math.min(level, 15)));
        return light;
    }

    private static String key(World world, int x, int y, int z) {
        return world.getUID() + ":" + x + ":" + y + ":" + z;
    }
}
