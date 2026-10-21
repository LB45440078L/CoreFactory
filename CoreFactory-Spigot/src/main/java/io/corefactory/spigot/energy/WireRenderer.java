package io.corefactory.spigot.energy;

import io.corefactory.core.energy.EnergyTable;
import io.corefactory.core.energy.WirePath;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Draws the wires of the energy network as thin, live conductors. A wire is a gentle catenary of dust in its grid's colour, one point
 * every {@link #SPACING} blocks, with a bright dust pulse for each third of the power it carries. The pulses move from the generator
 * end toward the pylon end, faster and brighter as the flow grows; a wire that carries no power has none.
 *
 * <p>Each redraw, a player is sent the wires within the view range, nearest first, and only as many particles as the cap allows. A wire
 * costs its points plus its pulses, and is sent whole or not at all, so the nearest wires are always complete. Pure geometry and budget
 * are in {@link WirePath}.
 */
final class WireRenderer {

    /** The distance between two points of a wire, in blocks. */
    static final double SPACING = 0.5;

    private static final float WIRE_SIZE = 0.7f;
    private static final float PULSE_SIZE = 0.8f;

    /** How far a pulse is lightened toward white at no flow; a full flow adds {@link #PULSE_FLOW_LIGHT} more. */
    private static final double PULSE_BASE_LIGHT = 0.3;
    private static final double PULSE_FLOW_LIGHT = 0.5;

    private final double viewBlocks;
    private final int cap;

    /**
     * @param viewBlocks the distance from a player within which a wire is sent to them, in blocks
     * @param cap        the most particles a player is sent for the wires in one redraw
     */
    WireRenderer(double viewBlocks, int cap) {
        this.viewBlocks = viewBlocks;
        this.cap = Math.max(0, cap);
    }

    /** A wire ready to send: its points and pulses for this redraw, its middle, and the dust of each. */
    private record Drawn(double[] middle, List<double[]> points, List<double[]> pulses, Particle.DustOptions wire,
                         Particle.DustOptions pulse) {

        /** The particles this wire costs a player: its points and its pulses. */
        int cost() {
            return points.size() + pulses.size();
        }
    }

    /** A wire within view of a player, with its distance from the player. */
    private record Near(Drawn wire, double distance) {
    }

    /**
     * Draws the wires to the players of their worlds, for the given server tick.
     *
     * @param segments the wires to draw
     * @param tick     the server tick, which places the pulses
     */
    void draw(List<EnergyEffects.Segment> segments, long tick) {
        if (cap == 0 || segments.isEmpty()) {
            return;
        }
        double seconds = tick * EnergyTable.SECONDS_PER_TICK;
        Map<UUID, List<Drawn>> byWorld = new HashMap<>();
        for (EnergyEffects.Segment segment : segments) {
            byWorld.computeIfAbsent(segment.world(), key -> new ArrayList<>()).add(prepare(segment, seconds));
        }
        for (Map.Entry<UUID, List<Drawn>> entry : byWorld.entrySet()) {
            World world = Bukkit.getWorld(entry.getKey());
            if (world == null) {
                continue;
            }
            for (Player player : world.getPlayers()) {
                sendTo(player, entry.getValue());
            }
        }
    }

    private static Drawn prepare(EnergyEffects.Segment segment, double seconds) {
        double[] from = segment.from();
        double[] to = segment.to();
        double length = WirePath.length(from, to);
        double flow = segment.flow();
        int count = WirePath.pulseCount(flow);
        double speed = WirePath.pulseSpeed(flow);
        List<double[]> pulses = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pulses.add(WirePath.point(from, to, WirePath.pulseFraction(seconds, speed, length, i, count)));
        }
        Particle.DustOptions wire = new Particle.DustOptions(Color.fromRGB(segment.colour()), WIRE_SIZE);
        int bright = WirePath.brighten(segment.colour(), PULSE_BASE_LIGHT + PULSE_FLOW_LIGHT * flow);
        Particle.DustOptions pulse = new Particle.DustOptions(Color.fromRGB(bright), PULSE_SIZE);
        double[] middle = {(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2};
        return new Drawn(middle, WirePath.path(from, to, SPACING), pulses, wire, pulse);
    }

    private void sendTo(Player player, List<Drawn> wires) {
        Location at = player.getLocation();
        List<Near> near = new ArrayList<>();
        for (Drawn wire : wires) {
            double dx = wire.middle()[0] - at.getX();
            double dy = wire.middle()[1] - at.getY();
            double dz = wire.middle()[2] - at.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance <= viewBlocks) {
                near.add(new Near(wire, distance));
            }
        }
        if (near.isEmpty()) {
            return;
        }
        near.sort(Comparator.comparingDouble(Near::distance));
        int[] costs = new int[near.size()];
        for (int i = 0; i < costs.length; i++) {
            costs[i] = near.get(i).wire().cost();
        }
        boolean[] drawn = WirePath.fit(costs, cap);
        for (int i = 0; i < drawn.length; i++) {
            if (drawn[i]) {
                send(player, near.get(i).wire());
            }
        }
    }

    private static void send(Player player, Drawn wire) {
        for (double[] point : wire.points()) {
            player.spawnParticle(Particle.DUST, point[0], point[1], point[2], 1, 0, 0, 0, 0, wire.wire());
        }
        for (double[] pulse : wire.pulses()) {
            player.spawnParticle(Particle.DUST, pulse[0], pulse[1], pulse[2], 1, 0, 0, 0, 0, wire.pulse());
        }
    }
}
