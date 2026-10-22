package io.corefactory.core.turret;

import java.util.OptionalInt;

/**
 * The interval between two shots of a turret. A turret at full power fires at its configured interval. With less power the interval
 * grows in proportion, so a turret at half power fires half as often. A turret with no power does not fire at all.
 */
public final class FireRate {

    /**
     * The slack allowed before a quotient is rounded up. The share comes from a sum of floating-point draws, so a full-power turret can read
     * one ulp below 1; without the slack that adds a whole tick (4 divided by 1 - 2^-52 rounds up to 5).
     */
    private static final double ROUNDING_SLACK = 1e-9;

    private FireRate() {
    }

    /**
     * The ticks between two shots.
     *
     * @param baseTicks    the configured interval at full power
     * @param satisfaction the share of the power demand the turret received, 0 to 1
     * @return the interval in ticks, rounded up; empty when the turret has no power
     */
    public static OptionalInt intervalTicks(int baseTicks, double satisfaction) {
        if (!(satisfaction > 0.0)) {
            return OptionalInt.empty();
        }
        double share = Math.min(1.0, satisfaction);
        return OptionalInt.of((int) Math.ceil(Math.max(1, baseTicks) / share - ROUNDING_SLACK));
    }
}
