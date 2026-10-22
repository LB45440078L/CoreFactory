package io.corefactory.core.deposit;

/**
 * The rule that decides which blocks of a deposit's footprint show as ore when its chunk is laid. A block shows when a
 * 64-bit hash of the deposit id and the block's position falls below the ore block density. The hash is a pure function
 * of its inputs, so a chunk laid again gets the same blocks, and no random source is involved.
 */
public final class OreBlocks {

    private static final long GAMMA = 0x9E3779B97F4A7C15L;
    private static final long PRIME_X = 0x632BE59BD9B4E019L;
    private static final long PRIME_Y = 0x85EBCA77C2B2AE63L;
    private static final long PRIME_Z = 0xC2B2AE3D27D4EB4FL;

    private OreBlocks() {
    }

    /**
     * @param depositId the id of the deposit the block lies in
     * @param x         the block's x
     * @param y         the block's y
     * @param z         the block's z
     * @param density   the chance that a block shows, from 0 to 1
     * @return true when the block shows as ore; over many blocks, the share is close to {@code density}
     */
    public static boolean shows(long depositId, int x, int y, int z, double density) {
        long hash = mix(depositId + GAMMA + (long) x * PRIME_X);
        hash = mix(hash + (long) y * PRIME_Y);
        hash = mix(hash + (long) z * PRIME_Z);
        return (hash >>> 11) * 0x1.0p-53 < density;
    }

    private static long mix(long value) {
        long z = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
