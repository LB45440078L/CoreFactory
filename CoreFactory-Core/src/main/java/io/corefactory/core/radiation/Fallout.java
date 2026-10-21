package io.corefactory.core.radiation;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * The fallout left by nuclear blasts, and the dose each player has taken from it.
 *
 * <p>A zone is a disc on the ground. Its rate falls linearly from the centre to the edge and halves every
 * {@link #ZONE_HALF_LIFE_SECONDS}. Once a second every player's dose is updated: the player takes in the rate at their
 * position, scaled by their {@code exposure} (a full suit leaves none), and the dose clears a little. Nausea follows
 * the dose, and so does health. Zones that have decayed to nothing are dropped.
 */
public final class Fallout {

    /** How often doses are updated, in ticks: one second. */
    public static final int INTERVAL_TICKS = 20;

    /** How long a zone's rate takes to halve: 90 minutes. */
    public static final double ZONE_HALF_LIFE_SECONDS = 90 * 60;

    /** Zone rates below this are gone, in Sv/s. */
    static final double NEGLIGIBLE_SV_PER_SECOND = 1e-7;

    /** Doses below this are dropped from the table, in Sv. */
    private static final double NEGLIGIBLE_DOSE = 1e-9;

    /** How long nausea lasts after each update, in ticks. */
    private static final int NAUSEA_TICKS = 120;

    /**
     * One zone of fallout.
     *
     * @param world       the world it lies in
     * @param x           the centre's x
     * @param z           the centre's z
     * @param radius      the radius in blocks
     * @param centreRate  the rate at the centre, in Sv/s
     * @param startTick   the server tick the blast happened on
     */
    public record Zone(World world, double x, double z, double radius, double centreRate, long startTick) {

        /** The rate at a horizontal point, at {@code tick}. Zero outside the zone. */
        double rateAt(double px, double pz, long tick) {
            return RadiationModel.rateAt(centreRate, radius, Math.hypot(px - x, pz - z), ageSeconds(tick),
                    ZONE_HALF_LIFE_SECONDS);
        }

        /** True when the zone has decayed below {@link #NEGLIGIBLE_SV_PER_SECOND} at its centre. */
        boolean spent(long tick) {
            return centreRate * RadiationModel.decay(ageSeconds(tick), ZONE_HALF_LIFE_SECONDS)
                    < NEGLIGIBLE_SV_PER_SECOND;
        }

        private double ageSeconds(long tick) {
            return Math.max(0L, tick - startTick) / 20.0;
        }
    }

    private final List<Zone> zones = new ArrayList<>();
    private final Map<UUID, Double> doses = new HashMap<>();

    /**
     * Adds the fallout of a blast of {@code megatons} at a point. Nothing is added for a blast with no yield.
     *
     * @param tick the server tick of the blast
     */
    public void add(World world, double x, double z, double radius, double megatons, long tick) {
        double rate = RadiationModel.centreRate(megatons);
        if (rate <= 0 || radius <= 0) {
            return;
        }
        zones.add(new Zone(world, x, z, radius, rate, tick));
    }

    /**
     * Updates every player's dose, once per {@link #INTERVAL_TICKS}. Creative and spectator players are not affected.
     *
     * @param exposure the share of the fallout a player takes in, from 0 (fully shielded) to 1 (none shielded)
     */
    public void tick(long tick, ToDoubleFunction<Player> exposure) {
        if (tick % INTERVAL_TICKS != 0) {
            return;
        }
        zones.removeIf(zone -> zone.spent(tick));
        double seconds = INTERVAL_TICKS / 20.0;
        Set<UUID> online = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            online.add(id);
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double share = Math.clamp(exposure.applyAsDouble(player), 0.0, 1.0);
            double dose = doseOf(id);
            dose = RadiationModel.absorb(dose, rateAt(player.getLocation(), tick) * share, seconds);
            dose = RadiationModel.recover(dose, seconds);
            store(id, dose);
            apply(player, dose, seconds);
        }
        doses.keySet().retainAll(online);
    }

    /**
     * Gives a player an instant dose, such as from a neutron burst. The share of it that reaches the player is scaled by
     * {@code exposure}: 1 with no shielding, 0 with the full suit.
     */
    public void irradiate(Player player, double sievert, double exposure) {
        double share = Math.clamp(exposure, 0.0, 1.0);
        UUID id = player.getUniqueId();
        store(id, doseOf(id) + Math.max(0.0, sievert) * share);
    }

    /** The fallout rate at a location, in Sv/s, summed over every zone in its world. */
    public double rateAt(Location at, long tick) {
        double rate = 0.0;
        for (Zone zone : zones) {
            if (zone.world() == at.getWorld()) {
                rate += zone.rateAt(at.getX(), at.getZ(), tick);
            }
        }
        return rate;
    }

    /** The dose a player has accumulated, in Sv. */
    public double doseOf(UUID player) {
        return doses.getOrDefault(player, 0.0);
    }

    /** The zones still active, oldest first. */
    public List<Zone> zones() {
        return List.copyOf(zones);
    }

    private void store(UUID id, double dose) {
        if (dose < NEGLIGIBLE_DOSE) {
            doses.remove(id);
        } else {
            doses.put(id, dose);
        }
    }

    private static void apply(Player player, double dose, double seconds) {
        if (RadiationModel.causesNausea(dose)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, NAUSEA_TICKS, 0));
        }
        double damage = RadiationModel.damagePerSecond(dose) * seconds;
        if (damage > 0 && !player.isDead()) {
            player.damage(damage);
        }
    }
}
