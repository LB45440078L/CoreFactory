package io.corefactory.core.energy;

/** The five accumulator levels: how much energy they hold and how fast they charge and discharge. */
public enum AccumulatorLevel {

    /** Holds 5 kWh; charges and discharges at up to 5 kW. */
    I(EnergyTable.ACCUMULATOR_I_CAPACITY_WH, EnergyTable.ACCUMULATOR_I_POWER_W),

    /** Holds 20 kWh; charges and discharges at up to 20 kW. */
    II(EnergyTable.ACCUMULATOR_II_CAPACITY_WH, EnergyTable.ACCUMULATOR_II_POWER_W),

    /** Holds 80 kWh; charges and discharges at up to 60 kW. */
    III(EnergyTable.ACCUMULATOR_III_CAPACITY_WH, EnergyTable.ACCUMULATOR_III_POWER_W),

    /** Holds 320 kWh; charges and discharges at up to 200 kW. */
    IV(EnergyTable.ACCUMULATOR_IV_CAPACITY_WH, EnergyTable.ACCUMULATOR_IV_POWER_W),

    /** Holds 1.28 MWh; charges and discharges at up to 800 kW. */
    V(EnergyTable.ACCUMULATOR_V_CAPACITY_WH, EnergyTable.ACCUMULATOR_V_POWER_W);

    private final double capacityWh;
    private final double powerW;

    AccumulatorLevel(double capacityWh, double powerW) {
        this.capacityWh = capacityWh;
        this.powerW = powerW;
    }

    /** The most energy the accumulator holds, in Wh. */
    public double capacityWh() {
        return capacityWh;
    }

    /** The most power the accumulator takes in, and the most it gives out, in W. */
    public double powerW() {
        return powerW;
    }
}
