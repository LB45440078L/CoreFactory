package io.corefactory.core.turret;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntPredicate;

/**
 * A test-only model of one laser cannon, ticked in the order of {@code TurretService.tickOne}: the cooldown counts down first, a cannon with
 * no power does nothing more, and a cannon with a target, off cooldown and on the aim fires and sets its cooldown to the interval. Ticks are
 * numbered from 1. The aim is taken as true whenever the cannon holds a target; the aim is tested on its own in {@link LaserSuspectsTest}.
 */
final class LaserCannonModel {

    /** Ticks in one second of the server clock. */
    static final int TICKS_PER_SECOND = 20;

    /** The level V laser's damage per hit, with its boost: 12 base, 50 per cent boost. */
    static final double LEVEL_FIVE_DAMAGE = TurretLevels.LASER.level(5).effectiveDamage();

    /** The level V laser's interval at full power, in ticks. */
    static final int LEVEL_FIVE_INTERVAL = TurretLevels.LASER.level(5).fireIntervalTicks();

    private LaserCannonModel() {
    }

    /** The ticks on which the cannon fires, from 1 to {@code ticks}, with the target held on every tick. */
    static List<Integer> firingTicks(int baseInterval, double satisfaction, int ticks) {
        return firingTicks(baseInterval, satisfaction, ticks, tick -> true);
    }

    /**
     * The ticks on which the cannon fires. The cooldown counts down on every tick, as in the server, and the cannon fires only on a tick it
     * holds a target.
     *
     * @param hasTarget true on the ticks the cannon holds a target
     */
    static List<Integer> firingTicks(int baseInterval, double satisfaction, int ticks, IntPredicate hasTarget) {
        List<Integer> fired = new ArrayList<>();
        int cooldown = 0;
        for (int tick = 1; tick <= ticks; tick++) {
            if (cooldown > 0) {
                cooldown--;
            }
            OptionalInt interval = FireRate.intervalTicks(baseInterval, satisfaction);
            if (interval.isPresent() && cooldown == 0 && hasTarget.test(tick)) {
                fired.add(tick);
                cooldown = interval.getAsInt();
            }
        }
        return fired;
    }

    /** The damage a cannon deals over {@code ticks} ticks when every shot hits for {@code damagePerHit}. */
    static double damage(int baseInterval, double satisfaction, int ticks, double damagePerHit) {
        return firingTicks(baseInterval, satisfaction, ticks).size() * damagePerHit;
    }

    /** Damage per second, averaged over a window of whole seconds. */
    static double damagePerSecond(int baseInterval, double satisfaction, int seconds, double damagePerHit) {
        return damage(baseInterval, satisfaction, seconds * TICKS_PER_SECOND, damagePerHit) / seconds;
    }
}
