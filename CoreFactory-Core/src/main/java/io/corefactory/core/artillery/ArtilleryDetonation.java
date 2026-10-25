package io.corefactory.core.artillery;

import io.corefactory.core.artillery.ArtillerySettings.ShellSpec;

import java.util.Random;

/**
 * The numbers of one detonation of a shell: the altitude an air burst is set off at, the blast radius of a small submunition, the
 * centre damage of a blast, and the megatons a nuclear warhead is run with. A blast's radius for the shell itself is
 * {@link ArtilleryFiringRules#blastRadiusBlocks}; the submunitions of a cluster use the smaller scale here.
 */
public final class ArtilleryDetonation {

    /** The least radius of a submunition's blast, in blocks: a small detonation breaks a few blocks, whatever its yield. */
    static final double SUBMUNITION_FLOOR_BLOCKS = 3.0;

    /** The blast's damage at its centre for one ton of TNT, in HP, scaled with the cube root of the yield. */
    static final double DAMAGE_PER_CUBE_ROOT_TON = 12.0;

    /** The most damage a blast deals at its centre, in HP, however large its yield. */
    static final double MAX_DAMAGE = 40.0;

    private ArtilleryDetonation() {
    }

    /**
     * The altitude an air burst is set off at: a point drawn uniformly between the shell's lowest and highest burst altitude. A shell with
     * one altitude always bursts there.
     *
     * @param spec   the shell's yield and burst altitudes
     * @param random the random source; a seeded one makes the burst repeatable
     * @return the altitude above the ground, in blocks
     */
    public static double burstAltitude(ShellSpec spec, Random random) {
        double fraction = random.nextDouble();
        return spec.burstMinBlocks() + (spec.burstMaxBlocks() - spec.burstMinBlocks()) * fraction;
    }

    /**
     * The radius of the blast of one submunition, in blocks: the blast radius scale of its yield, with the cube root, and at least the
     * floor, so a small submunition still breaks a few blocks around its point.
     *
     * @param tons          the submunition's yield, in tons of TNT, 0 or more
     * @param radiusConstant the blast radius per cube root of the yield, from the safety settings
     * @return the radius in blocks
     */
    public static double submunitionRadiusBlocks(double tons, double radiusConstant) {
        return Math.max(SUBMUNITION_FLOOR_BLOCKS, radiusConstant * Math.cbrt(Math.max(0.0, tons)));
    }

    /**
     * The damage a blast deals at its centre, in HP: it grows with the cube root of the yield and is capped, so a large blast does not
     * kill more than the cap at its centre. The falloff to the edge of the blast is the blast's own.
     *
     * @param tons the yield in tons of TNT, 0 or more
     * @return the centre damage in HP
     */
    public static double damage(double tons) {
        return Math.min(MAX_DAMAGE, DAMAGE_PER_CUBE_ROOT_TON * Math.cbrt(Math.max(0.0, tons)));
    }

    /**
     * The megatons a nuclear warhead is run with, for a yield in tons: the nuclear mechanic reads its yield in megatons.
     *
     * @param tons the yield in tons of TNT, 0 or more
     * @return the yield in megatons
     */
    public static double megatons(double tons) {
        return tons / NuclearYields.TONS_PER_MEGATON;
    }
}
