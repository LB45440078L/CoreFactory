package io.corefactory.paper.scanner;

import io.corefactory.paper.TickScheduler;
import io.corefactory.paper.energy.EnergyGeometry;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * The outline of a scan: a ring of dust around each deposit found, at the player's feet height, in the ore's colour. The rings
 * are sent to the scanning player alone, one ring per deposit every {@value #PERIOD_TICKS} ticks, for the time the scan gives.
 * A new scan replaces the outline of the last one. Each ring has about one point per {@value #SPACING} blocks of its edge, so a
 * scan of eight deposits sends a few dozen particles a tick.
 */
final class DepositOutline {

    /** The ticks between two redraws of the rings. */
    static final long PERIOD_TICKS = 10L;

    /** The blocks between two points of a ring. */
    static final double SPACING = 4.0;

    private static final int MIN_POINTS = 12;
    private static final int MAX_POINTS = 48;
    private static final int WHITE = 0xFFFFFF;

    /** One ring to draw: the deposit's world and centre, its radius in blocks, and the ore's colour as 0xRRGGBB. */
    record Ring(String world, int centreX, int centreZ, int radius, int rgb) {
    }

    private final TickScheduler ticks;

    DepositOutline(TickScheduler ticks) {
        this.ticks = ticks;
    }

    /**
     * Draws the rings to one player for the given seconds, replacing any outline that player already has.
     *
     * @param player  the player who scanned
     * @param rings   the rings to draw; none cancels the outline
     * @param seconds how long the outline stays, at least 1
     */
    void show(Player player, List<Ring> rings, int seconds) {
        UUID id = player.getUniqueId();
        cancel(id);
        if (rings.isEmpty()) {
            return;
        }
        int[] passes = {Math.max(1, seconds) * (int) (20 / PERIOD_TICKS)};
        String name = nameOf(id);
        ticks.register(name, 0, (int) PERIOD_TICKS, () -> {
            if (passes[0]-- <= 0 || !player.isOnline()) {
                ticks.remove(name);
                return;
            }
            draw(player, rings);
        });
    }

    /** Stops the outline of a player, if one is drawn. */
    void cancel(UUID player) {
        ticks.remove(nameOf(player));
    }

    private static String nameOf(UUID player) {
        return "deposit-outline-" + player;
    }

    private static void draw(Player player, List<Ring> rings) {
        String world = player.getWorld().getName();
        double y = player.getLocation().getY() + 0.1;
        for (Ring ring : rings) {
            if (!ring.world().equals(world)) {
                continue;
            }
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(ring.rgb()), 1.0f);
            int count = points(ring.radius());
            double cx = ring.centreX() + 0.5;
            double cz = ring.centreZ() + 0.5;
            for (double[] point : EnergyGeometry.ring(cx, y, cz, ring.radius(), count)) {
                player.spawnParticle(Particle.DUST, point[0], point[1], point[2], 1, 0, 0, 0, 0, dust);
            }
        }
    }

    /** The points of a ring of this radius: about one per {@link #SPACING} blocks, within the minimum and maximum. */
    static int points(int radius) {
        long wanted = Math.round(2 * Math.PI * radius / SPACING);
        return (int) Math.max(MIN_POINTS, Math.min(MAX_POINTS, wanted));
    }

    /**
     * The colour of an ore's name: the first colour code in it, as 0xRRGGBB. A name with no colour code is white.
     *
     * @param name the ore's name with ampersand colour codes
     */
    static int colourOf(String name) {
        for (int i = 0; i + 1 < name.length(); i++) {
            if (name.charAt(i) == '&') {
                int rgb = legacy(Character.toLowerCase(name.charAt(i + 1)));
                if (rgb >= 0) {
                    return rgb;
                }
            }
        }
        return WHITE;
    }

    /** The colour of a legacy colour code, or -1 when the code is not one of the sixteen colours. */
    private static int legacy(char code) {
        return switch (code) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> WHITE;
            default -> -1;
        };
    }
}
