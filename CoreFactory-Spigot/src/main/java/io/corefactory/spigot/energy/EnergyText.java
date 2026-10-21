package io.corefactory.spigot.energy;

import io.corefactory.core.energy.GridResult;
import io.corefactory.core.energy.Units;

import java.util.ArrayList;
import java.util.List;

/**
 * The text of the energy holograms, in real units, with ampersand colour codes. An accumulator shows two lines: its name, what it
 * holds and its state, and a flash line that reads only while a discharge has just started. A pylon shows the voltage and current of
 * its grid, and the power its grid delivers of its capacity. A lab tool or drill gets one more line at the bottom for its grid, so
 * each hologram keeps the same number of lines. Pure.
 */
public final class EnergyText {

    /** The lines of an accumulator's hologram. */
    public static final int ACCUMULATOR_LINES = 2;

    /** The lines of a pylon's hologram. */
    public static final int PYLON_LINES = 2;

    private static final String FLASH = "&6&lDischarge started";

    private EnergyText() {
    }

    /**
     * The lines of an accumulator's hologram: {@code Accumulator I  4.2 kWh / 20 kWh  21 %  charging}, and the flash line.
     *
     * @param level       the level, 1 to 3
     * @param storedWh    the energy held, in Wh
     * @param capacityWh  the most it holds, in Wh
     * @param charging    true when it took energy in the last tick
     * @param discharging true when it gave energy out in the last tick
     * @param flashing    true while the flash line should read
     */
    public static List<String> accumulator(int level, double storedWh, double capacityWh, boolean charging, boolean discharging,
                                           boolean flashing) {
        double fraction = capacityWh <= 0 ? 0.0 : Math.min(1.0, storedWh / capacityWh);
        return List.of(
                "&7Accumulator " + EnergyLevels.roman(level) + "  &f" + Units.energyText(storedWh) + " &7/ "
                        + Units.energyText(capacityWh) + "  " + Units.percentText(fraction) + "  " + state(charging, discharging),
                flashing ? FLASH : "");
    }

    /** The state word of an accumulator, coloured: discharging, charging or idle. */
    public static String state(boolean charging, boolean discharging) {
        if (discharging) {
            return "&cdischarging";
        }
        return charging ? "&acharging" : "&7idle";
    }

    /** The lines of a pylon's hologram before its grid has been solved once. */
    public static List<String> pylonWaiting() {
        return List.of("&7Pylon  &8waiting for the first tick", "");
    }

    /**
     * The lines of a pylon's hologram: {@code Grid #3  230.0 V  12.4 A}, and {@code 2.8 kW of 23.0 kW}, the power its grid delivers
     * of the grid's capacity.
     *
     * @param gridId  the grid's id
     * @param reading the grid's reading of the last tick, or null when it has none yet
     */
    public static List<String> pylon(int gridId, GridResult reading) {
        if (reading == null) {
            return pylonWaiting();
        }
        return List.of(
                "&7Grid #" + gridId + "  &f" + Units.voltageText(reading.voltageV()) + "  "
                        + Units.currentText(reading.currentA()),
                "&f" + Units.powerText(reading.deliveredW()) + " &7of " + Units.powerText(reading.grid().capacityW()));
    }

    /**
     * The network line of a lab tool or drill: {@code Grid 230.0 V  4.0 kW  100 %}, its voltage, its demand and its satisfaction.
     *
     * @param voltageV     the voltage of its grid, in V
     * @param demandW      the power it asks for, in W
     * @param satisfaction the share of the demand it receives, 0 to 1
     */
    public static String grid(double voltageV, double demandW, double satisfaction) {
        String colour = satisfaction >= 1.0 ? "&a" : satisfaction > 0 ? "&e" : "&c";
        return colour + "Grid " + Units.voltageText(voltageV) + "  " + Units.powerText(demandW) + "  "
                + Units.percentText(satisfaction);
    }

    /**
     * The lines of a machine or drill hologram with the network line added at the bottom. The count grows by one whatever the
     * network, so the hologram keeps the lines it was made with.
     *
     * @param lines        the lines the hologram already shows
     * @param voltageV     the voltage of its grid, in V
     * @param demandW      the power it asks for, in W
     * @param satisfaction the share of the demand it receives, 0 to 1
     */
    public static List<String> withGrid(List<String> lines, double voltageV, double demandW, double satisfaction) {
        List<String> all = new ArrayList<>(lines);
        all.add(grid(voltageV, demandW, satisfaction));
        return List.copyOf(all);
    }
}
