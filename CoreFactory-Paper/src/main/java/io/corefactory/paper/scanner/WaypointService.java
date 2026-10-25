package io.corefactory.paper.scanner;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositScan;
import io.corefactory.core.deposit.DepositText;
import io.corefactory.core.text.Text;
import io.corefactory.paper.TickScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The waypoints of the players: a beam of dust from the player toward the centre of a deposit, and its direction and distance on
 * the action bar. A waypoint lasts two minutes, and ends early when the player stands inside the deposit's circle. One waypoint
 * per player; setting a new one replaces the old.
 *
 * <p>A timer redraws every waypoint every {@value #PERIOD_TICKS} ticks. The beam is at most {@value #BEAM_POINTS} points, two
 * blocks apart, so a waypoint sends a few particles a tick to its player alone.
 */
public final class WaypointService {

    /** How long a waypoint lasts, in ticks: two minutes. */
    static final long LIFETIME_TICKS = 120L * 20L;

    /** The ticks between two redraws of the beams and the action bar. */
    static final long PERIOD_TICKS = 5L;

    /** The most points of a beam, which reaches about 40 blocks. */
    static final int BEAM_POINTS = 20;

    /** The blocks between two points of a beam. */
    static final double BEAM_STEP = 2.0;

    private static final Particle.DustOptions BEAM = new Particle.DustOptions(Color.fromRGB(0xFFAA00), 1.0f);

    private final CoreFactoryCore core;
    private final Map<UUID, Waypoint> waypoints = new HashMap<>();
    private final Map<UUID, Long> actionBarHeld = new HashMap<>();

    WaypointService(TickScheduler ticks, CoreFactoryCore core) {
        this.core = core;
        ticks.register("waypoints", (int) PERIOD_TICKS, this::tick);
    }

    /**
     * Sets the waypoint of a player, replacing any they had.
     *
     * @param player the player
     * @param deposit the deposit to lead to
     * @param name    the ore's name, with colour codes
     */
    void set(UUID player, Deposit deposit, String name) {
        waypoints.put(player, new Waypoint(deposit, name, core.currentTick() + LIFETIME_TICKS));
    }

    /** The waypoint of a player, if they have one. */
    Optional<Waypoint> get(UUID player) {
        return Optional.ofNullable(waypoints.get(player));
    }

    /** Removes the waypoint of a player. */
    void clear(UUID player) {
        waypoints.remove(player);
        actionBarHeld.remove(player);
    }

    /**
     * Keeps the action bar free of the waypoint until a tick: a scan shows its own line there, which must not be overwritten.
     *
     * @param player the player
     * @param untilTick the server tick until which the waypoint keeps off the action bar
     */
    void holdActionBar(UUID player, long untilTick) {
        actionBarHeld.put(player, untilTick);
    }

    private void tick() {
        long now = core.currentTick();
        Iterator<Map.Entry<UUID, Waypoint>> entries = waypoints.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Waypoint> entry = entries.next();
            UUID id = entry.getKey();
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                entries.remove();
                actionBarHeld.remove(id);
                continue;
            }
            Waypoint waypoint = entry.getValue();
            Deposit deposit = waypoint.deposit();
            if (now >= waypoint.expiresTick()) {
                entries.remove();
                actionBarHeld.remove(id);
                player.sendMessage(Text.parse("&7The waypoint to " + waypoint.name() + " &7has expired."));
                continue;
            }
            Location at = player.getLocation();
            if (!at.getWorld().getName().equals(deposit.world())) {
                holdOrShow(player, now, "&7Waypoint " + waypoint.name() + "&7: in another world");
                continue;
            }
            int x = at.getBlockX();
            int z = at.getBlockZ();
            if (DepositScan.arrived(deposit, x, z)) {
                entries.remove();
                actionBarHeld.remove(id);
                player.sendMessage(Text.parse("&aYou are inside " + waypoint.name() + "&a."));
                continue;
            }
            beam(player, deposit);
            holdOrShow(player, now, "&6Waypoint " + waypoint.name() + "&7: &f" + DepositText.place(deposit, x, z));
        }
    }

    private void holdOrShow(Player player, long now, String text) {
        if (now >= actionBarHeld.getOrDefault(player.getUniqueId(), 0L)) {
            ActionBars.send(player, text);
        }
    }

    /** The dust from the player's chest toward the centre of the deposit, at most {@link #BEAM_POINTS} points long. */
    private static void beam(Player player, Deposit deposit) {
        Location at = player.getLocation();
        double sx = at.getX();
        double sy = at.getY() + 1.0;
        double sz = at.getZ();
        double dx = deposit.centreX() + 0.5 - sx;
        double dz = deposit.centreZ() + 0.5 - sz;
        double length = Math.hypot(dx, dz);
        if (length < BEAM_STEP) {
            return;
        }
        double ux = dx / length;
        double uz = dz / length;
        int points = (int) Math.min(BEAM_POINTS, length / BEAM_STEP);
        for (int i = 1; i <= points; i++) {
            double d = i * BEAM_STEP;
            player.spawnParticle(Particle.DUST, sx + ux * d, sy, sz + uz * d, 1, 0, 0, 0, 0, BEAM);
        }
    }
}
