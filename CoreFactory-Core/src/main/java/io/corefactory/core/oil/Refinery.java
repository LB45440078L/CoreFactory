package io.corefactory.core.oil;

/**
 * The crude oil refinery: it takes petroleum from the oil network and gives diesel back to it. A pass takes 3 litres of petroleum and gives 2
 * litres of diesel, over 600 ticks at full speed, so the refinery draws 0.1 litres of petroleum a second and makes two thirds of a litre of
 * diesel for each litre it takes. Pure: the caller draws the petroleum and puts the diesel into the network.
 */
public final class Refinery {

    /** The petroleum one full pass takes, in litres. */
    static final double PASS_PETROLEUM_LITRES = 3.0;

    /** The diesel one full pass gives, in litres. */
    static final double PASS_DIESEL_LITRES = 2.0;

    /** The ticks one full pass takes, at 20 ticks a second. */
    static final double PASS_TICKS = 600.0;

    /** The server's ticks per second. */
    static final double TICKS_PER_SECOND = 20.0;

    /** The petroleum drawn each second at full speed, in litres. */
    public static final double PETROLEUM_PER_SECOND = PASS_PETROLEUM_LITRES / (PASS_TICKS / TICKS_PER_SECOND);

    /** The diesel made from each litre of petroleum. */
    public static final double DIESEL_PER_PETROLEUM = PASS_DIESEL_LITRES / PASS_PETROLEUM_LITRES;

    private Refinery() {
    }

    /**
     * The petroleum a refinery at full speed draws in a tick.
     *
     * @param dtSeconds the length of the tick in seconds, positive and finite
     * @return the litres of petroleum wanted
     */
    public static double petroleumWanted(double dtSeconds) {
        if (!Double.isFinite(dtSeconds) || dtSeconds <= 0) {
            throw new IllegalArgumentException("tick length must be finite and above 0, got " + dtSeconds);
        }
        return PETROLEUM_PER_SECOND * dtSeconds;
    }

    /**
     * The diesel made from the petroleum a tick drew.
     *
     * @param petroleumDrawn the litres of petroleum taken in, finite and 0 or more
     * @return the litres of diesel made
     */
    public static double dieselFrom(double petroleumDrawn) {
        if (!Double.isFinite(petroleumDrawn) || petroleumDrawn < 0) {
            throw new IllegalArgumentException("petroleum must be finite and 0 or more, got " + petroleumDrawn);
        }
        return petroleumDrawn * DIESEL_PER_PETROLEUM;
    }
}
