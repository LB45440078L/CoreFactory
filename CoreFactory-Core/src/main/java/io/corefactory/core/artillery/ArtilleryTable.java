package io.corefactory.core.artillery;

/**
 * The formulas of an artillery piece, over the coefficients of {@link ArtillerySettings}. Each level's range, reload and scatter
 * is a smooth curve from level I to level VII, and the charge tier multiplies each one.
 *
 * <p>With level number {@code n} from 1 to 7, the level's position is {@code p = (n - 1) / 6}, so level I is at 0 and level VII at 1:
 * <ul>
 *   <li>range = range I x (range VII / range I)^p, times the tier's range multiplier. The growth is geometric, so each
 *       level reaches about 1.35 times further than the one below it;</li>
 *   <li>reload = (reload I + (reload VII - reload I) x p) x the tier's reload multiplier, in whole ticks. The reload grows in
 *       equal steps, so the top levels are not much slower to reload than the middle ones;</li>
 *   <li>scatter sigma at 1000 blocks = scatter I x (scatter VII / scatter I)^p, times the tier's scatter multiplier. It falls
 *       by about 0.7 times a level, so a level VII piece lands about 7.5 times closer to its aim than a level I piece;</li>
 *   <li>scatter sigma at a range = the scatter sigma at 1000 blocks x range / 1000, which is linear in the range.</li>
 * </ul>
 */
public final class ArtilleryTable {

    private final ArtillerySettings settings;

    /**
     * @param settings the settings whose coefficients the formulas use
     */
    public ArtilleryTable(ArtillerySettings settings) {
        this.settings = settings;
    }

    /** The level's position between level I (0) and level VII (1). */
    static double position(ArtilleryLevel level) {
        return (level.number() - 1) / 6.0;
    }

    /**
     * The range of a level with a charge, in blocks.
     *
     * @param level the artillery level
     * @param tier  the charge tier
     * @return the range in blocks
     */
    public double rangeBlocks(ArtilleryLevel level, ChargeTier tier) {
        double base = settings.rangeLevelOneBlocks()
                * Math.pow(settings.rangeLevelSevenBlocks() / settings.rangeLevelOneBlocks(), position(level));
        return base * settings.charge(tier).range();
    }

    /**
     * The reload of a level with a charge, in ticks, rounded to a whole tick.
     *
     * @param level the artillery level
     * @param tier  the charge tier
     * @return the reload in ticks
     */
    public int reloadTicks(ArtilleryLevel level, ChargeTier tier) {
        double base = settings.reloadLevelOneTicks()
                + (settings.reloadLevelSevenTicks() - settings.reloadLevelOneTicks()) * position(level);
        return (int) Math.round(base * settings.charge(tier).reload());
    }

    /**
     * The standard deviation of the landing scatter at 1000 blocks of range, with a charge, in blocks.
     *
     * @param level the artillery level
     * @param tier  the charge tier
     * @return the standard deviation in blocks at 1000 blocks of range
     */
    public double scatterSigmaAt1000Blocks(ArtilleryLevel level, ChargeTier tier) {
        double base = settings.scatterLevelOneBlocks()
                * Math.pow(settings.scatterLevelSevenBlocks() / settings.scatterLevelOneBlocks(), position(level));
        return base * settings.charge(tier).scatter();
    }

    /**
     * The standard deviation of the landing scatter for one shot, in blocks: linear in the range of the shot.
     *
     * @param level      the artillery level
     * @param tier       the charge tier
     * @param rangeBlocks the range of the shot, in blocks
     * @return the standard deviation in blocks
     */
    public double scatterSigmaBlocks(ArtilleryLevel level, ChargeTier tier, double rangeBlocks) {
        return scatterSigmaAt1000Blocks(level, tier) * rangeBlocks / 1000.0;
    }
}
