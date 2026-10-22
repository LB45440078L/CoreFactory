package io.corefactory.spigot.vertical;

import io.corefactory.core.vertical.VerticalLevels;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.logging.Logger;

/**
 * The settings of the vertical belts from the config.yml section {@code vertical-belt}: the per-level speed and capacity, and the most
 * item displays on towers at once. A list that is missing or not valid falls back to the defaults, with one warning.
 *
 * @param levels      the per-level speed and capacity
 * @param maxDisplays the most item displays on towers at once
 */
public record VerticalSettings(VerticalLevels levels, int maxDisplays) {

    /** The shipped carry speeds of levels I to III, in blocks per second: the regular belt speeds. */
    public static final double[] DEFAULT_SPEEDS = {1.875, 3.75, 7.5};

    /** The shipped capacities of levels I to III, in items per block. */
    public static final int[] DEFAULT_CAPACITIES = {4, 8, 12};

    /** The shipped cap on item displays on towers. */
    public static final int DEFAULT_MAX_DISPLAYS = 600;

    /**
     * Reads the section.
     *
     * @param config the plugin's configuration
     * @param log    receives the warning for a list that is not valid
     */
    public static VerticalSettings from(FileConfiguration config, Logger log) {
        List<Double> speedList = config.getDoubleList("vertical-belt.speed");
        List<Integer> capacityList = config.getIntegerList("vertical-belt.capacity");
        double[] speeds = speedList.stream().mapToDouble(Double::doubleValue).toArray();
        int[] capacities = capacityList.stream().mapToInt(Integer::intValue).toArray();
        VerticalLevels levels;
        try {
            levels = VerticalLevels.of(speeds, capacities);
        } catch (IllegalArgumentException e) {
            log.warning("vertical-belt in config.yml is not valid and the defaults are used: " + e.getMessage());
            levels = VerticalLevels.of(DEFAULT_SPEEDS, DEFAULT_CAPACITIES);
        }
        return new VerticalSettings(levels, Math.max(0, config.getInt("vertical-belt.max-item-displays", DEFAULT_MAX_DISPLAYS)));
    }
}
