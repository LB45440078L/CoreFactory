package io.corefactory.core.deposit;

import io.corefactory.core.config.ConfigValues;

import java.util.Map;
import java.util.logging.Logger;

/**
 * The planning constants of the deposits: the cell size, the chance that a cell holds a deposit, the share of oil, the
 * radius and amount ranges, and the share of stone blocks in a deposit's footprint that show as visible ore. The last is
 * read by the world generation, so this record is the one source of truth for every deposit number.
 *
 * @param cellSize        the side of a planning cell, in blocks; at least twice {@code maxRadius}
 * @param density         the chance that a cell holds a deposit, from 0 to 1
 * @param oilShare        the chance that a deposit is crude petroleum rather than a solid ore, from 0 to 1
 * @param minRadius       the smallest radius, in blocks, from 1 to {@code maxRadius}
 * @param maxRadius       the largest radius, in blocks, from {@code minRadius} to {@value #MAX_RADIUS}
 * @param minAmount       the smallest initial amount, at least 1
 * @param maxAmount       the largest initial amount, at least {@code minAmount}
 * @param oreBlockDensity the share of the stone blocks of a deposit's footprint that show as ore, from 0 to 1
 */
public record DepositSettings(int cellSize, double density, double oilShare, int minRadius, int maxRadius,
                              long minAmount, long maxAmount, double oreBlockDensity) {

    /** The largest radius a deposit may have, in blocks. */
    public static final int MAX_RADIUS = 256;

    /** The largest cell size, in blocks. */
    public static final int MAX_CELL_SIZE = 4096;

    /** The config.yml section the values are read from. */
    public static final String SECTION = "deposits";

    public DepositSettings {
        requireRange("density", density, 0, 1);
        requireRange("oilShare", oilShare, 0, 1);
        requireRange("oreBlockDensity", oreBlockDensity, 0, 1);
        if (minRadius < 1 || minRadius > maxRadius || maxRadius > MAX_RADIUS) {
            throw new IllegalArgumentException("The radius range must be 1 to " + MAX_RADIUS + " with minRadius <= maxRadius, not "
                    + minRadius + " to " + maxRadius);
        }
        if (cellSize < 2 * maxRadius || cellSize > MAX_CELL_SIZE) {
            throw new IllegalArgumentException("The cell size must be " + (2 * maxRadius) + " to " + MAX_CELL_SIZE
                    + " for this radius, not " + cellSize);
        }
        if (minAmount < 1 || maxAmount < minAmount) {
            throw new IllegalArgumentException("The amount range must start at 1 or more and not end below its start: "
                    + minAmount + " to " + maxAmount);
        }
    }

    /** The planning constants with the default values. */
    public static DepositSettings defaults() {
        return new DepositSettings(160, 0.6, 0.25, 24, 48, 500_000L, 25_000_000L, 0.3);
    }

    /**
     * Reads the {@code deposits:} section. A missing key takes its default. When the values together break a rule, one
     * warning is logged and the defaults are used for the whole section, so the world never gets a half-applied set.
     *
     * @param config the root configuration as plain data, or any section that holds a {@code deposits} section; may be null
     * @param log    receives the warning when the section is refused
     * @return the settings read, or the defaults
     */
    public static DepositSettings from(Map<String, Object> config, Logger log) {
        Map<String, Object> section = ConfigValues.section(config, SECTION);
        DepositSettings defaults = defaults();
        try {
            return new DepositSettings(
                    ConfigValues.integer(section, "cell-size", defaults.cellSize()),
                    ConfigValues.number(section, "density", defaults.density()),
                    ConfigValues.number(section, "oil-share", defaults.oilShare()),
                    ConfigValues.integer(section, "min-radius", defaults.minRadius()),
                    ConfigValues.integer(section, "max-radius", defaults.maxRadius()),
                    ConfigValues.longValue(section, "min-amount", defaults.minAmount()),
                    ConfigValues.longValue(section, "max-amount", defaults.maxAmount()),
                    ConfigValues.number(section, "ore-block-density", defaults.oreBlockDensity()));
        } catch (IllegalArgumentException e) {
            log.warning("config.yml: '" + SECTION + "' is invalid (" + e.getMessage() + "); the defaults are used.");
            return defaults;
        }
    }

    private static void requireRange(String name, double value, double min, double max) {
        if (!(value >= min && value <= max)) {
            throw new IllegalArgumentException(name + " must be from " + min + " to " + max + ", not " + value);
        }
    }
}
