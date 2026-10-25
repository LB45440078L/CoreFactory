package io.corefactory.core.railgun;

import java.util.Objects;

/**
 * The cycle of one rail gun, as a state machine that a server tick advances. A rail gun is idle while it has no target or its switch is
 * off. Once a target is held and the switch is on, it charges for the level's {@link RailgunSettings.LevelStats#chargeSeconds()}, its light
 * rising with the charge ({@link RailgunRules#lightLevel}). A charge advances at the share of power the rail gun gets, so a rail gun short of
 * power charges slower, as a laser fires slower. When the charge is complete and the barrel is aimed, the rail gun fires, its light goes out,
 * and it reloads for {@link RailgunSettings.LevelStats#reloadSeconds()}. A reload takes no power and runs on even while the switch is off.
 *
 * <p>A target lost or switched off during a charge drops the charge at once: the rail gun is idle with no light, and a new charge starts
 * from nothing. The cycle decides when a shot is fired; the caller sends the shot and reads {@link Step#fired()}. Pure: the caller supplies
 * the target, the aim and the power, so the cycle is tested without a server.
 */
public final class RailgunCycle {

    /** The phases of the cycle. */
    public enum Phase {
        /** No target, or the switch is off: no charge, no light, no power drawn. */
        IDLE,
        /** A target is held: the rail gun charges, its light rises, and it draws its power demand. */
        CHARGING,
        /** A shot has been fired: the rail gun reloads, its light is out, and it draws no power for the reload. */
        RELOADING
    }

    /**
     * What one tick of the cycle leaves.
     *
     * @param phase  the phase after the tick
     * @param light  the light level the rail gun shows, from 0 to the section's {@code max-light}
     * @param charge the share of the charge done, from 0 to 1; 1 while the rail gun is charged and waits for its aim
     * @param fired  true in the tick the rail gun fired
     */
    public record Step(Phase phase, int light, double charge, boolean fired) {
    }

    /** The ticks in one second, which a charge and a reload count in. */
    private static final double TICKS = RailgunSettings.TICKS_PER_SECOND;

    private Phase phase = Phase.IDLE;
    /** The ticks of charge done in a charge, or the ticks of reload done in a reload. */
    private double progress;

    /** The phase now. */
    public Phase phase() {
        return phase;
    }

    /** True while the rail gun charges, which is when it draws its power demand. */
    public boolean charging() {
        return phase == Phase.CHARGING;
    }

    /** Drops the cycle to idle, as when the turret loses its target for good: a charge is lost, and a reload is forgotten. */
    public void reset() {
        phase = Phase.IDLE;
        progress = 0.0;
    }

    /**
     * Advances the cycle by one server tick.
     *
     * @param settings   the rail gun's settings
     * @param level      the level of the rail gun
     * @param on         whether the rail gun's switch is on
     * @param targetHeld whether a target is held and in reach
     * @param aimed      whether the barrel is on its target, so a shot would hit
     * @param satisfaction the share of its power the rail gun gets, from 0 to 1; a charge advances at this share
     * @return the step: the phase, the light, the charge and whether it fired
     */
    public Step tick(RailgunSettings settings, RailgunLevel level, boolean on, boolean targetHeld, boolean aimed, double satisfaction) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(level, "level");
        RailgunSettings.LevelStats stats = settings.level(level);
        double share = Double.isNaN(satisfaction) ? 0.0 : Math.max(0.0, Math.min(1.0, satisfaction));
        if (phase == Phase.RELOADING) {
            progress += 1.0;
            if (progress < stats.reloadSeconds() * TICKS) {
                return new Step(Phase.RELOADING, 0, 0.0, false);
            }
            // The reload ends in this tick, and the next charge begins on the next one, so a cycle is charge plus reload in ticks.
            reset();
            return new Step(Phase.IDLE, 0, 0.0, false);
        }
        boolean fired = false;
        if (!on || !targetHeld) {
            if (phase == Phase.CHARGING) {
                reset();
            }
        } else if (phase == Phase.IDLE) {
            phase = Phase.CHARGING;
            progress = 0.0;
        }
        if (phase == Phase.CHARGING) {
            progress += share;
            if (progress >= stats.chargeSeconds() * TICKS && aimed) {
                fired = true;
                phase = Phase.RELOADING;
                progress = 0.0;
            }
        }

        int light = phase == Phase.CHARGING
                ? RailgunRules.lightLevel(settings, level, progress / TICKS)
                : 0;
        return new Step(phase, light, chargeShare(stats, phase), fired);
    }

    private double chargeShare(RailgunSettings.LevelStats stats, Phase now) {
        if (now != Phase.CHARGING) {
            return 0.0;
        }
        double charge = stats.chargeSeconds() * TICKS;
        return charge <= 0.0 ? 1.0 : Math.min(1.0, progress / charge);
    }
}
