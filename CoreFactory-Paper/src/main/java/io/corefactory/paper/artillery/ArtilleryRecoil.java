package io.corefactory.paper.artillery;

/**
 * The recoil of an artillery barrel after a shot: how far the barrel is kicked back along its axis, tick by tick. The barrel kicks back over
 * {@link #KICK_TICKS} ticks, then eases back to its rest over {@link #RETURN_TICKS} ticks, so it is back in place after {@link #TOTAL_TICKS}.
 * The curve is a fraction of the full kick, from 0 at rest to 1 at the kick's peak. Pure, so the curve is tested without a server.
 */
final class ArtilleryRecoil {

    /** The ticks the barrel takes to kick back to its full distance. */
    static final int KICK_TICKS = 2;

    /** The ticks the barrel takes to ease back from its full distance to its rest. */
    static final int RETURN_TICKS = 8;

    /** The ticks of one recoil, from the shot to the barrel's rest. */
    static final int TOTAL_TICKS = KICK_TICKS + RETURN_TICKS;

    /** The interpolation of the barrel's display while it recoils, in ticks: short, so the kick reads as a jolt. */
    static final int INTERPOLATION_TICKS = 2;

    private ArtilleryRecoil() {
    }

    /**
     * How far the barrel is kicked back at a tick of its recoil.
     *
     * @param tick      the ticks since the shot, from 0 at the shot; a tick at or past {@link #TOTAL_TICKS} is at rest
     * @param maxBlocks the full distance of the kick, in blocks
     * @return the distance back along the barrel's axis, in blocks, from 0 up to {@code maxBlocks}
     */
    static double displacement(int tick, double maxBlocks) {
        if (tick <= 0 || tick >= TOTAL_TICKS) {
            return 0.0;
        }
        if (tick <= KICK_TICKS) {
            return maxBlocks * tick / KICK_TICKS;
        }
        double t = (double) (tick - KICK_TICKS) / RETURN_TICKS;
        return maxBlocks * (1.0 + Math.cos(Math.PI * t)) / 2.0;
    }
}
