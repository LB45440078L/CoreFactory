package io.corefactory.core.mechanics;

import io.corefactory.api.DetonationContext;
import io.corefactory.api.CoreFactoryApi;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

/**
 * The exotic neutron bomb. Its blast is small, a few kilotons, but its neutrons reach far. Within the blast radius a
 * player takes the full dose; the dose falls off linearly to nothing at {@code bio-radius}. A radioactive suit scales the
 * dose down. Living creatures in the same area are withered. The blast leaves only a thin fallout.
 *
 * <p>Settings: {@code dose-sv} (the dose at the blast's edge, default 4), {@code bio-radius} (where the dose ends,
 * default 90), and the usual {@code damage}.
 */
final class NeutronMechanic extends AbstractMechanic {

    /** The mechanic id in tnts.yml. */
    static final String ID = "neutron";

    /** How long a creature stays withered, at the blast's edge and beyond it, in ticks. */
    private static final int WITHER_NEAR_TICKS = 20 * 30;
    private static final int WITHER_FAR_TICKS = 20 * 10;

    NeutronMechanic(Toolkit kit) {
        super(ID, "A small blast whose neutrons deliver a lethal radiation dose far beyond it.", kit);
    }

    @Override
    public void detonate(DetonationContext ctx) {
        double radius = ctx.tnt().radius();
        Location origin = ctx.origin();
        World world = ctx.world();
        CoreFactoryApi api = ctx.api();
        double dose = ctx.settings().getDouble("dose-sv", 4.0);
        double bioRadius = Math.max(radius, ctx.settings().getDouble("bio-radius", radius * 2));
        double megatons = ctx.settings().getDouble("yield-megatons", 0.0);

        kit.explosions().damage(origin, radius, damage(ctx, 20.0), ctx.source());

        for (Player player : world.getPlayers()) {
            double distance = player.getLocation().distance(origin);
            if (distance > bioRadius) {
                continue;
            }
            double share = bioRadius == radius ? 1.0
                    : Math.clamp((bioRadius - distance) / (bioRadius - radius), 0.0, 1.0);
            kit.radiation().fallout().irradiate(player, dose * share, kit.radiation().suit().exposure(player));
        }

        withers(world, origin, radius, bioRadius);

        double fallout = radius * 3.0;
        kit.radiation().fallout().add(world, origin.getX(), origin.getZ(), fallout, megatons, api.currentTick());

        kit.effects().particle(origin, org.bukkit.Particle.FLASH, 3, radius * 0.2, 0.2, radius * 0.2);
        kit.effects().visual("neutron_burst", origin, bioRadius);
        for (Player player : kit.effects().nearby(origin, bioRadius)) {
            player.playSound(origin, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 6.0f, 1.4f);
        }
    }

    /** Withers every creature in the bio radius: harder near the blast, lighter at the edge. */
    private static void withers(World world, Location origin, double radius, double bioRadius) {
        Collection<org.bukkit.entity.Entity> nearby = world.getNearbyEntities(origin, bioRadius, bioRadius, bioRadius);
        for (org.bukkit.entity.Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living) || entity instanceof Player) {
                continue;
            }
            boolean near = entity.getLocation().distance(origin) <= radius;
            living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, near ? WITHER_NEAR_TICKS : WITHER_FAR_TICKS,
                    near ? 1 : 0));
        }
    }
}
