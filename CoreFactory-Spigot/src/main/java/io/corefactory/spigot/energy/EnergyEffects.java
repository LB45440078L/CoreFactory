package io.corefactory.spigot.energy;

import io.corefactory.core.energy.GeneratorKind;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The particles and sounds of the energy devices: the wires, drawn as thin conductors in their grid's colour with a pulse of power, the
 * activation burst of an accumulator, the puffs of a running generator, and the outlines of the grids of the pylon command, in each
 * grid's colour. Each is drawn with vanilla particles, sent to the players near it.
 */
public final class EnergyEffects {

    /**
     * A wire to draw: the world it is in, its two ends as {x, y, z}, its grid's colour, and its flow.
     *
     * @param world  the id of the wire's world
     * @param from   the end the pulses leave from: the generator end when the wire has one, otherwise either end
     * @param to     the other end
     * @param colour the colour of the wire's grid, as 0xRRGGBB
     * @param flow   the share of its power the wire carries, from 0 to 1; 0 draws no pulse
     */
    public record Segment(UUID world, double[] from, double[] to, int colour, double flow) {
    }

    /**
     * The outline of a grid to draw: the world it is in, its colour as 0xRRGGBB, and the points of its loops as {x, y, z}.
     *
     * @param world  the id of the grid's world
     * @param colour the grid's colour
     * @param points the points of the outline
     */
    public record Outline(UUID world, int colour, List<double[]> points) {
    }

    /**
     * A generator that may give off particles: the world it is in, the point its puffs leave from, its kind, and its state.
     *
     * @param world     the id of the generator's world
     * @param at        the point {x, y, z} the puffs leave from
     * @param kind      the generator's kind
     * @param burning   true when a fuel item is burning
     * @param producing true when it gave power in its last tick
     */
    public record GeneratorVisual(UUID world, double[] at, GeneratorKind kind, boolean burning, boolean producing) {
    }

    private static final double OUTLINE_VIEW = 96.0;
    private static final long OUTLINE_TICKS = 10L;
    private static final float OUTLINE_SIZE = 1.0f;
    /** The default of energy.wire-view-blocks: the wires within this many blocks of a player are sent to them. */
    private static final double WIRE_VIEW_BLOCKS = 24.0;
    /** The default of energy.wire-particle-cap: the most particles a player is sent for the wires in one redraw. */
    private static final int WIRE_PARTICLE_CAP = 320;
    private static final int RING_POINTS = 40;
    private static final double RING_RADIUS = 0.6;
    private static final double RING_HEIGHT = 1.2;

    private final Plugin plugin;
    private final WireRenderer wires;

    public EnergyEffects(Plugin plugin) {
        this.plugin = plugin;
        this.wires = new WireRenderer(plugin.getConfig().getDouble("energy.wire-view-blocks", WIRE_VIEW_BLOCKS),
                plugin.getConfig().getInt("energy.wire-particle-cap", WIRE_PARTICLE_CAP));
    }

    /** The activation burst of an accumulator: a ring of electric sparks over its top, and a chime. */
    public void burst(Location block) {
        World world = block.getWorld();
        if (world == null) {
            return;
        }
        double cx = block.getBlockX() + 0.5;
        double cy = block.getBlockY() + RING_HEIGHT;
        double cz = block.getBlockZ() + 0.5;
        for (double[] point : EnergyGeometry.ring(cx, cy, cz, RING_RADIUS, RING_POINTS)) {
            world.spawnParticle(Particle.ELECTRIC_SPARK, point[0], point[1], point[2], 1);
        }
        world.playSound(new Location(world, cx, cy, cz), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    /**
     * Draws the wires: a catenary of dust in each grid's colour, with a pulse of power on each wire that carries any. Each player is sent
     * the wires within the view range, nearest first, within the particle cap. Called every few ticks; the pulses are placed at the tick.
     *
     * @param segments the wires to draw
     * @param tick     the server tick
     */
    public void drawWires(List<Segment> segments, long tick) {
        wires.draw(segments, tick);
    }

    /**
     * Sends the puffs of the generators: a smoke trail while a charcoal generator burns, a sparse cosy smoke while a petroleum or
     * diesel one burns, and a faint sparkle while a nuclear one runs. Each puff goes only to the players within
     * {@link GeneratorLook#VIEW_BLOCKS} blocks of it. Called every {@link GeneratorLook#PERIOD_TICKS} ticks.
     */
    public void drawGenerators(List<GeneratorVisual> visuals) {
        for (GeneratorVisual visual : visuals) {
            Optional<GeneratorLook.Puff> puff = GeneratorLook.puffFor(visual.kind(), visual.burning(), visual.producing());
            World world = Bukkit.getWorld(visual.world());
            if (puff.isEmpty() || world == null) {
                continue;
            }
            Location at = new Location(world, visual.at()[0], visual.at()[1], visual.at()[2]);
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(at) > GeneratorLook.VIEW_BLOCKS) {
                    continue;
                }
                player.spawnParticle(puff.get().particle(), at.getX(), at.getY(), at.getZ(), puff.get().count(),
                        0.1, 0.1, 0.1, 0.01);
            }
        }
    }

    /**
     * Draws the outlines of the grids to the players within {@link #OUTLINE_VIEW} blocks of each point, every {@link #OUTLINE_TICKS}
     * ticks for the given number of seconds. The outlines are read again on every pass, so a pylon placed meanwhile shows.
     *
     * @param player   the player who asked for them
     * @param seconds  how long they show, at least 1
     * @param outlines the outlines to show now, read on every pass
     */
    public void showOutlines(Player player, int seconds, Supplier<List<Outline>> outlines) {
        int[] passes = {Math.max(1, seconds) * (int) (20 / OUTLINE_TICKS)};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (passes[0]-- <= 0 || !player.isOnline()) {
                task[0].cancel();
                return;
            }
            drawOutlines(outlines.get());
        }, 0L, OUTLINE_TICKS);
    }

    /** Sends the points of each outline, in its colour, to the players within {@link #OUTLINE_VIEW} blocks of each point. */
    public void drawOutlines(List<Outline> outlines) {
        for (Outline outline : outlines) {
            World world = Bukkit.getWorld(outline.world());
            if (world == null) {
                continue;
            }
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(outline.colour()), OUTLINE_SIZE);
            List<Player> players = world.getPlayers();
            for (double[] point : outline.points()) {
                for (Player player : players) {
                    if (player.getLocation().distance(new Location(world, point[0], point[1], point[2])) > OUTLINE_VIEW) {
                        continue;
                    }
                    player.spawnParticle(Particle.DUST, point[0], point[1], point[2], 1, 0, 0, 0, 0, dust);
                }
            }
        }
    }
}
