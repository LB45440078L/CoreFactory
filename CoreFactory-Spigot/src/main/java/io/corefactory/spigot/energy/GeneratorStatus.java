package io.corefactory.spigot.energy;

import io.corefactory.core.energy.GeneratorKind;

/**
 * The status word of a generator, shown in its menu and its hologram: running, no fuel, closed (wind), night (solar), or not
 * underwater (nuclear). Pure: the device reads its state and passes the flags in.
 */
public enum GeneratorStatus {

    /** Producing, or with fuel ready to burn. */
    RUNNING("running"),
    /** A fuel kind with no item burning and none kept. */
    NO_FUEL("no fuel"),
    /** A wind generator whose site is not open. */
    CLOSED("closed (wind)"),
    /** A solar panel in the dark. */
    NIGHT("night (solar)"),
    /** A nuclear generator that is not fully under water. */
    NOT_UNDERWATER("not underwater (nuclear)");

    private final String word;

    GeneratorStatus(String word) {
        this.word = word;
    }

    /** The status word, lower case, with the kind in brackets where it helps. */
    public String word() {
        return word;
    }

    /**
     * The status of a generator in its last tick.
     *
     * @param kind       the generator's kind
     * @param open       true when a wind generator's site is open
     * @param underwater true when a nuclear generator stands fully in water
     * @param producing  true when the generator gave power in its last tick
     * @param burning    true when a fuel item is burning
     * @param fuel       the fuel items kept
     */
    public static GeneratorStatus of(GeneratorKind kind, boolean open, boolean underwater, boolean producing, boolean burning,
                                     int fuel) {
        return switch (kind) {
            case WIND -> open ? RUNNING : CLOSED;
            case SOLAR -> producing ? RUNNING : NIGHT;
            case NUCLEAR -> !underwater ? NOT_UNDERWATER : burning || fuel > 0 ? RUNNING : NO_FUEL;
            case CHARCOAL, PETROLEUM, DIESEL -> burning || fuel > 0 ? RUNNING : NO_FUEL;
        };
    }
}
