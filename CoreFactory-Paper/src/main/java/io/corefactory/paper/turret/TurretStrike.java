package io.corefactory.paper.turret;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

/**
 * The one way a turret's beam or round removes its damage from a mob. The hit is already the damage left after the armour the shot does not
 * ignore, so it comes off the mob's absorption first and then its health. A killing blow goes through the server's damage path, so the mob
 * dies and drops its loot as it does to any other damage. The machine gun, the laser and the rail gun all strike through here, so a kill is
 * the same whichever turret made it.
 */
public final class TurretStrike {

    /** The damage of a killing blow: far above any mob's health, so armour cannot save it. */
    private static final double KILL_BLOW_DAMAGE = 1000.0;

    private TurretStrike() {
    }

    /**
     * Removes one turret hit from an entity's health: the absorption first, then the health. The hit is already the damage left after the
     * entity's armour, so no hurt animation, invulnerability window, damage event or armour acts on it. A health of 0 is dead, as the
     * Bukkit API documents for {@code setHealth}.
     *
     * @return the damage removed, which is less than the hit when the entity has less absorption and health than the hit, or when the
     *         server's damage path left the entity standing
     */
    public static double strike(LivingEntity struck, double amount) {
        if (!(amount > 0.0)) {
            return 0.0;
        }
        double absorption = Math.max(0.0, struck.getAbsorptionAmount());
        double fromAbsorption = Math.min(absorption, amount);
        if (fromAbsorption > 0.0) {
            struck.setAbsorptionAmount(absorption - fromAbsorption);
        }
        double remaining = amount - fromAbsorption;
        double health = struck.getHealth();
        if (remaining <= 0.0 || health <= 0.0) {
            return fromAbsorption;
        }
        if (health - remaining > 0.0) {
            struck.setHealth(Math.min(maxHealthOf(struck), health - remaining));
            return fromAbsorption + Math.max(0.0, health - struck.getHealth());
        }
        return fromAbsorption + killBlow(struck, health);
    }

    /**
     * The blow that ends a mob goes through the server's own damage path. Setting health to zero only writes the value: the mob would not
     * fire its death event or drop its loot. So the mob is left at one health and a blow far above its health is dealt, which armour can
     * reduce by at most 80 per cent, and the server kills it normally. A cancelled event or a resistance can leave the mob standing, so the
     * damage counted is the health it lost, read after the blow.
     *
     * @param health the health the mob had before the blow
     * @return the health the mob lost: all of it when the blow killed the mob, less when it is still standing
     */
    private static double killBlow(LivingEntity struck, double health) {
        struck.setHealth(1.0);
        struck.setNoDamageTicks(0);
        struck.damage(KILL_BLOW_DAMAGE);
        return Math.max(0.0, health - Math.max(0.0, struck.getHealth()));
    }

    /** The armour value of an entity, which the armour formula reads; 0 when the entity has no armour attribute. */
    public static double armourOf(LivingEntity living) {
        AttributeInstance armour = living.getAttribute(Attribute.ARMOR);
        return armour == null ? 0.0 : armour.getValue();
    }

    /** The armour toughness of an entity, which the server's armour formula reads with its armour. */
    public static double toughnessOf(LivingEntity living) {
        AttributeInstance toughness = living.getAttribute(Attribute.ARMOR_TOUGHNESS);
        return toughness == null ? 0.0 : toughness.getValue();
    }

    /** The maximum health of an entity, which its health attribute holds. */
    private static double maxHealthOf(LivingEntity living) {
        AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? living.getHealth() : maxHealth.getValue();
    }
}
