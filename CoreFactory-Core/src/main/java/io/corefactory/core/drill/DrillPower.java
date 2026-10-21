package io.corefactory.core.drill;

/**
 * The power a drill has in one cycle: the source that drives it and the efficiency that source gives, from 0 to 1.
 *
 * @param source     the source that drives the drill, {@link PowerSource#NONE} when there is none
 * @param efficiency 0 for no power, 1 at full power
 */
public record DrillPower(PowerSource source, double efficiency) {

    /** A drill with no power: no source and no efficiency. */
    public static final DrillPower NONE = new DrillPower(PowerSource.NONE, 0.0);

    /** True when the drill has any power, so its cycle advances. */
    public boolean powered() {
        return efficiency > 0;
    }
}
