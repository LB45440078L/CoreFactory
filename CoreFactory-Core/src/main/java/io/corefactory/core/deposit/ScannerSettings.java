package io.corefactory.core.deposit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.logging.Logger;

/**
 * The settings of the ore density scanner, read from the {@code deposits:} section of config.yml beside the planning constants:
 * the search radius of a scan, the cooldown between two scans, and how long a scan's outline stays drawn.
 *
 * @param scannerRadius   the radius a scan searches around the player, in blocks, from 1 to {@value #MAX_RADIUS}
 * @param scanCooldownTicks the ticks between two scans of one scanner, from 0 to {@value #MAX_COOLDOWN_TICKS}
 * @param outlineSeconds  how long a scan's outline stays drawn, in seconds, from 1 to {@value #MAX_OUTLINE_SECONDS}
 */
public record ScannerSettings(int scannerRadius, int scanCooldownTicks, int outlineSeconds) {

    /** The largest scan radius, in blocks. */
    public static final int MAX_RADIUS = 512;

    /** The largest cooldown, in ticks: one minute. */
    public static final int MAX_COOLDOWN_TICKS = 1_200;

    /** The largest outline time, in seconds. */
    public static final int MAX_OUTLINE_SECONDS = 120;

    public ScannerSettings {
        if (scannerRadius < 1 || scannerRadius > MAX_RADIUS) {
            throw new IllegalArgumentException("The scanner radius must be 1 to " + MAX_RADIUS + ", not " + scannerRadius);
        }
        if (scanCooldownTicks < 0 || scanCooldownTicks > MAX_COOLDOWN_TICKS) {
            throw new IllegalArgumentException("The scan cooldown must be 0 to " + MAX_COOLDOWN_TICKS + " ticks, not "
                    + scanCooldownTicks);
        }
        if (outlineSeconds < 1 || outlineSeconds > MAX_OUTLINE_SECONDS) {
            throw new IllegalArgumentException("The outline time must be 1 to " + MAX_OUTLINE_SECONDS + " seconds, not "
                    + outlineSeconds);
        }
    }

    /** The settings with the default values: a 96 block radius, a cooldown of 60 ticks and an outline of 20 seconds. */
    public static ScannerSettings defaults() {
        return new ScannerSettings(96, 60, 20);
    }

    /**
     * Reads the scanner keys of the {@code deposits:} section. A missing key takes its default. When the values break a rule, one
     * warning is logged and the defaults are used.
     *
     * @param config the root configuration, or any section that holds a {@code deposits} section; may be null
     * @param log    receives the warning when the values are refused
     * @return the settings read, or the defaults
     */
    public static ScannerSettings from(ConfigurationSection config, Logger log) {
        ConfigurationSection section = config == null ? null : config.getConfigurationSection(DepositSettings.SECTION);
        if (section == null) {
            section = new MemoryConfiguration();
        }
        ScannerSettings defaults = defaults();
        try {
            return new ScannerSettings(
                    section.getInt("scanner-radius", defaults.scannerRadius()),
                    section.getInt("scan-cooldown-ticks", defaults.scanCooldownTicks()),
                    section.getInt("outline-seconds", defaults.outlineSeconds()));
        } catch (IllegalArgumentException e) {
            log.warning("config.yml: the scanner keys of '" + DepositSettings.SECTION + "' are invalid (" + e.getMessage()
                    + "); the defaults are used.");
            return defaults;
        }
    }
}
