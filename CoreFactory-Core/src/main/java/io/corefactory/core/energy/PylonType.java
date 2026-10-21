package io.corefactory.core.energy;

/**
 * The three pylon sizes. Each has a square range of a given half-side and a maximum current. Its capacity is the most
 * power a grid can carry through it at the nominal voltage.
 */
public enum PylonType {

    /** Half-side 5 blocks, 100 A, 23 kW. */
    SMALL(EnergyTable.SMALL_HALF_SIDE, EnergyTable.SMALL_MAX_CURRENT_A),

    /** Half-side 9 blocks, 400 A, 92 kW. */
    MEDIUM(EnergyTable.MEDIUM_HALF_SIDE, EnergyTable.MEDIUM_MAX_CURRENT_A),

    /** Half-side 15 blocks, 1600 A, 368 kW. */
    LARGE(EnergyTable.LARGE_HALF_SIDE, EnergyTable.LARGE_MAX_CURRENT_A);

    private final int halfSide;
    private final double maxCurrentA;

    PylonType(int halfSide, double maxCurrentA) {
        this.halfSide = halfSide;
        this.maxCurrentA = maxCurrentA;
    }

    /** The half-side of the range square, in blocks. */
    public int halfSide() {
        return halfSide;
    }

    /** The maximum current of a pylon of this type, in A. */
    public double maxCurrentA() {
        return maxCurrentA;
    }

    /** The capacity of a pylon of this type, in W: the maximum current at the nominal voltage. */
    public double capacityW() {
        return Units.NOMINAL_VOLTAGE_V * maxCurrentA;
    }
}
