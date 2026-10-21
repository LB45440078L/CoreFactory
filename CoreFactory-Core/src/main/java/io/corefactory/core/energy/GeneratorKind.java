package io.corefactory.core.energy;

/**
 * The six generator kinds. Wind and solar need no fuel. The other four burn one fuel item at a time while they run at
 * their rated output, so an item lasts its electric energy divided by that output.
 */
public enum GeneratorKind {

    /** Up to 3 kW, at an open site, from the wind speed; no fuel. */
    WIND(EnergyTable.WIND_RATED_W, 0.0),

    /** Up to 320 W at the sun's peak, 0 at night; no fuel. */
    SOLAR(EnergyTable.SOLAR_EFFICIENCY * EnergyTable.SOLAR_PANEL_AREA_M2 * EnergyTable.IRRADIANCE_PEAK_W_M2, 0.0),

    /** 25 kW while burning; one charcoal lasts 30 s. */
    CHARCOAL(EnergyTable.CHARCOAL_RATED_W, EnergyTable.CHARCOAL_FUEL_J * EnergyTable.CHARCOAL_EFFICIENCY),

    /** 80 kW while burning; one petroleum lasts about 160 s. */
    PETROLEUM(EnergyTable.PETROLEUM_RATED_W, EnergyTable.PETROLEUM_FUEL_J * EnergyTable.PETROLEUM_EFFICIENCY),

    /** 150 kW while burning; one diesel lasts about 96 s. */
    DIESEL(EnergyTable.DIESEL_RATED_W, EnergyTable.DIESEL_FUEL_J * EnergyTable.DIESEL_EFFICIENCY),

    /** 2 MW while burning underwater; one fuel rod lasts 178 h. */
    NUCLEAR(EnergyTable.NUCLEAR_RATED_W, Units.joulesFromWattHours(EnergyTable.NUCLEAR_ROD_WH));

    private final double ratedW;
    private final double electricJoulesPerItem;

    GeneratorKind(double ratedW, double electricJoulesPerItem) {
        this.ratedW = ratedW;
        this.electricJoulesPerItem = electricJoulesPerItem;
    }

    /** The output at rated power, in W: the output while the kind runs, the peak for solar. */
    public double ratedW() {
        return ratedW;
    }

    /** The electric energy one fuel item gives, in J; 0 for a kind with no fuel. */
    public double electricJoulesPerItem() {
        return electricJoulesPerItem;
    }

    /**
     * The seconds one fuel item burns for at rated output: its electric energy over the rated output.
     *
     * @return the burn time in s, or 0 for a kind with no fuel
     */
    public double burnSeconds() {
        return burnsFuel() ? electricJoulesPerItem / ratedW : 0.0;
    }

    /** True for the kinds that burn a fuel item: charcoal, petroleum, diesel and nuclear. */
    public boolean burnsFuel() {
        return electricJoulesPerItem > 0.0;
    }
}
