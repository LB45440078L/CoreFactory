package io.corefactory.core.water;

import io.corefactory.core.hologram.Tone;

import java.util.List;

/**
 * The lines of a pump's hologram, and the status they name: the title, the status, the flow, the source, and the power or the fuel. Pure,
 * so it is tested with plain numbers; the service reads the pump and fills in a {@link Reading}.
 */
public final class PumpHologram {

    /** The lines of a pump's hologram. The same count at every level, so the hologram is made once. */
    public static final int LINES = 5;

    private PumpHologram() {
    }

    /**
     * The pump's state a hologram is built from.
     *
     * @param level        the pump's level, 1 to 5
     * @param delivered    units per second the pump puts into its network now
     * @param capacity     units per second the pump can give at its level
     * @param source       the water blocks its source counted, up to the cap
     * @param minimum      the water blocks a source needs
     * @param running      true when the pump runs in this tick
     * @param gridConnected true when a grid covers a powered pump; false for a pump that burns fuel
     * @param satisfaction the share of its demand the grid gives a powered pump, 0 to 1
     * @param demandW      the power the pump asks for, in W
     * @param fuelItems    the fuel items it holds, the one burning now included
     */
    public record Reading(int level, double delivered, double capacity, int source, int minimum, boolean running,
                          boolean gridConnected, double satisfaction, int demandW, int fuelItems) {
    }

    /**
     * The status of a pump: pumping while it runs. Otherwise the first thing that stops it, in the order the power line reads: for a
     * powered pump the grid, then the source, then the power; for a pump that burns fuel the source, then the fuel or the diesel.
     */
    public static PumpStatus status(Reading reading) {
        if (reading.running()) {
            return PumpStatus.PUMPING;
        }
        boolean hasSource = reading.source() >= reading.minimum();
        if (PumpLevels.powered(reading.level())) {
            if (!reading.gridConnected()) {
                return PumpStatus.NO_GRID;
            }
            if (!hasSource) {
                return PumpStatus.NO_SOURCE;
            }
            return PumpStatus.NO_POWER;
        }
        if (!hasSource) {
            return PumpStatus.NO_SOURCE;
        }
        return PumpFuel.drawsFluid(reading.level()) ? PumpStatus.NO_DIESEL : PumpStatus.NO_FUEL;
    }

    /** The background of a pump's hologram: green while it pumps, red while it is stopped. */
    public static Tone tone(Reading reading) {
        return status(reading).tone();
    }

    /**
     * The lines of a pump's hologram, top first: the title, the status, the flow, the source, and the power line of a powered pump or the
     * fuel line of another.
     */
    public static List<String> lines(Reading reading) {
        String powerOrFuel = PumpLevels.powered(reading.level())
                ? powerLine(reading)
                : WaterText.fuel(reading.level(), reading.fuelItems(), reading.running());
        return List.of(
                WaterText.title(reading.level()),
                WaterText.status(status(reading)),
                WaterText.flow(reading.delivered(), reading.capacity()),
                WaterText.source(reading.source(), reading.minimum()),
                powerOrFuel);
    }

    /** The power line of a powered pump: no grid when none covers it, waiting for a source when it has none, else its share of its demand. */
    static String powerLine(Reading reading) {
        if (!reading.gridConnected()) {
            return WaterText.noGrid();
        }
        if (reading.source() < reading.minimum()) {
            return WaterText.noSource();
        }
        return WaterText.power(reading.satisfaction(), reading.demandW());
    }
}
