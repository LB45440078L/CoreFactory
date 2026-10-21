package io.corefactory.spigot.energy;

import io.corefactory.core.energy.GeneratorKind;
import org.bukkit.Particle;

import java.util.Optional;

/**
 * Which particle a generator gives off, and how often. A charcoal generator smokes while it burns, a petroleum or diesel one
 * gives a sparse cosy smoke while it burns, a nuclear one sparkles while it runs underwater, and wind gives none. Players farther
 * than {@link #VIEW_BLOCKS} blocks see nothing. Pure: the choice is tested without a server.
 */
public final class GeneratorLook {

    /** Ticks between two puffs of a generator. */
    public static final int PERIOD_TICKS = 10;

    /** The distance in blocks within which a player sees the particles of a generator. */
    public static final double VIEW_BLOCKS = 48.0;

    /**
     * One puff: the particle and how many are sent together.
     *
     * @param particle the particle
     * @param count    the number sent, positive
     */
    public record Puff(Particle particle, int count) {
    }

    private GeneratorLook() {
    }

    /**
     * The puff a generator gives this period, or empty when it gives none.
     *
     * @param kind      the generator's kind
     * @param burning   true when a fuel item is burning
     * @param producing true when the generator gave power in its last tick
     */
    public static Optional<Puff> puffFor(GeneratorKind kind, boolean burning, boolean producing) {
        return switch (kind) {
            case CHARCOAL -> burning ? Optional.of(new Puff(Particle.SMOKE, 3)) : Optional.empty();
            case PETROLEUM, DIESEL -> burning ? Optional.of(new Puff(Particle.CAMPFIRE_COSY_SMOKE, 1)) : Optional.empty();
            case NUCLEAR -> producing ? Optional.of(new Puff(Particle.END_ROD, 1)) : Optional.empty();
            case WIND, SOLAR -> Optional.empty();
        };
    }
}
