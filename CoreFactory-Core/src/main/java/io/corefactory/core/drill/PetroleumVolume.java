package io.corefactory.core.drill;

/**
 * The volume rule of a crude petroleum deposit. A deposit holds a volume of petroleum, one unit per pump. Its volume is kept
 * per block in the chunk's data (see {@link io.corefactory.core.geology.DepositVolumes}); a deposit with no stored volume
 * still holds the full volume. At zero the block is removed, so petroleum is finite. Pure.
 */
public final class PetroleumVolume {

    /** The volume of a deposit when ores.yml does not give one. */
    public static final int DEFAULT_VOLUME = 400;

    private PetroleumVolume() {
    }

    /**
     * The full volume of a deposit from its configured volume.
     *
     * @param configured the {@code volume} of the deposit in ores.yml, or 0 when it has none
     * @return the configured volume, or {@link #DEFAULT_VOLUME} when it is not positive
     */
    public static int volumeOf(int configured) {
        return configured > 0 ? configured : DEFAULT_VOLUME;
    }

    /**
     * The volume left after one pump.
     *
     * @param remaining the volume before the pump
     * @return one less, never below zero
     */
    public static int afterPump(int remaining) {
        return Math.max(0, remaining - 1);
    }

    /** True when a deposit has no petroleum left, so its block is removed. */
    public static boolean depleted(int remaining) {
        return remaining <= 0;
    }
}
