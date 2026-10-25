package io.corefactory.core.artillery;

import java.util.Random;

/**
 * Where a shell lands around its aim point. The landing is a two-dimensional Gaussian on the ground plane, with the same standard
 * deviation on both axes: a shell is as likely to fall short as long, and left as right, and the spread grows with the range of the
 * shot, as a real shell's does with its angular error.
 */
public final class ArtilleryDispersion {

    private final ArtilleryTable table;

    /**
     * @param table the formulas of the artillery levels and charges
     */
    public ArtilleryDispersion(ArtilleryTable table) {
        this.table = table;
    }

    /**
     * The standard deviation of the landing on each axis for one shot, in blocks.
     *
     * @param level      the artillery level
     * @param tier       the charge tier
     * @param rangeBlocks the range of the shot, in blocks
     * @return the standard deviation in blocks
     */
    public double sigmaBlocks(ArtilleryLevel level, ChargeTier tier, double rangeBlocks) {
        return table.scatterSigmaBlocks(level, tier, rangeBlocks);
    }

    /**
     * Draws the landing point of one shot.
     *
     * @param aim        the point the piece is aimed at, on the ground plane
     * @param rangeBlocks the range of the shot, in blocks; the scatter grows with it
     * @param level      the artillery level
     * @param tier       the charge tier
     * @param random     the random source; a seeded one makes the shots repeatable
     * @return the landing point
     */
    public GroundPoint landing(GroundPoint aim, double rangeBlocks, ArtilleryLevel level, ChargeTier tier, Random random) {
        double sigma = sigmaBlocks(level, tier, rangeBlocks);
        return new GroundPoint(
                aim.x() + random.nextGaussian() * sigma,
                aim.z() + random.nextGaussian() * sigma);
    }
}
