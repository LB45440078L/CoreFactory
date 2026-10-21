package io.corefactory.core.turret;

/**
 * The drawing of one laser beam: its length from the muzzle to where it stops, and how many dust particles it draws along it. The
 * particles are about {@link #SPACING} blocks apart. A beam too long for its cap spreads the cap evenly along its whole length, so the
 * line stays complete rather than stopping short. A hit draws {@link #IMPACT_PARTICLES} more where the beam stops. A shot never draws
 * {@link #MAX_PARTICLES_PER_SHOT} particles or more.
 *
 * @param length    the beam's length in blocks
 * @param particles the dust particles along the beam
 */
public record BeamPlan(double length, int particles) {

    /** The distance in blocks between two particles of a beam that is short enough for the spacing. */
    public static final double SPACING = 0.6;

    /** The particles of the burst at the point where a beam hits. */
    public static final int IMPACT_PARTICLES = 6;

    /** The most dust particles one beam may draw. */
    public static final int MAX_BEAM_PARTICLES = 32;

    /** The particles one shot may draw in total, its beam and its impact together. */
    public static final int MAX_PARTICLES_PER_SHOT = 40;

    public BeamPlan {
        length = Double.isNaN(length) ? 0.0 : Math.max(0.0, length);
        particles = Math.max(0, Math.min(particles, MAX_BEAM_PARTICLES));
    }

    /**
     * The plan of a beam of a length, with a cap on its particles.
     *
     * @param length the beam's length in blocks
     * @param cap    the most particles the beam may draw; clamped to {@link #MAX_BEAM_PARTICLES}
     */
    public static BeamPlan of(double length, int cap) {
        double along = Double.isNaN(length) ? 0.0 : Math.max(0.0, length);
        int limit = Math.max(0, Math.min(cap, MAX_BEAM_PARTICLES));
        int spaced = (int) Math.floor(along / SPACING);
        return new BeamPlan(along, Math.min(limit, spaced));
    }

    /**
     * The distance from the muzzle of one particle. The particles are spread evenly along the beam, each at the middle of its share.
     *
     * @param index the particle, from 0 to {@code particles - 1}
     * @return the distance in blocks; zero when the beam has no particles
     */
    public double distanceOf(int index) {
        if (particles == 0) {
            return 0.0;
        }
        return (index + 0.5) * length / particles;
    }

    /**
     * The particles one shot draws: the beam's, and the impact's when the shot hits.
     *
     * @param hit true when the beam stops on a block or an entity
     */
    public int totalParticles(boolean hit) {
        return particles + (hit ? IMPACT_PARTICLES : 0);
    }
}
