package io.corefactory.core.energy;

/**
 * The open-air input of a wind generator: how many air blocks lie above it and the generator's y.
 *
 * @param airAbove the count of air blocks above the generator
 * @param y        the generator's y
 */
public record OpenAir(int airAbove, int y) {

    public OpenAir {
        Checks.nonNegative(airAbove, "air above");
    }

    /** True when the site is open enough for wind: at least 6 air blocks above and y of 96 or more. */
    public boolean open() {
        return airAbove >= EnergyTable.WIND_MIN_OPEN_BLOCKS && y >= EnergyTable.WIND_MIN_Y;
    }
}
