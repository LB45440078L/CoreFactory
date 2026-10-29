package io.corefactory.paper.machine;

import io.corefactory.core.upgrade.LabLevels;

import java.util.List;
import java.util.logging.Logger;

/**
 * Reads {@code machine-upgrades.speed} from config.yml: the work pace factor of each lab machine level, level I first. A list
 * that is the wrong length, or holds a factor the level table refuses, is logged and the defaults are used.
 */
public final class LabUpgradeRates {

    /** The factors used when config.yml gives none that are valid: levels I to V. */
    static final List<Double> DEFAULTS = List.of(1.0, 1.5, 2.0, 2.75, 3.5);

    private LabUpgradeRates() {
    }

    /**
     * The level table of the lab machines.
     *
     * @param configured the list of {@code machine-upgrades.speed}, or null when the key is missing
     * @param log        receives one warning when the list is not used
     */
    public static LabLevels read(List<Double> configured, Logger log) {
        if (configured == null || configured.size() != LabLevels.MAX_LEVEL) {
            warn(log, "machine-upgrades.speed needs one factor for each of the " + LabLevels.MAX_LEVEL
                    + " machine levels; the defaults are used.");
            return LabLevels.defaults();
        }
        double[] speeds = new double[configured.size()];
        for (int i = 0; i < speeds.length; i++) {
            Double speed = configured.get(i);
            speeds[i] = speed == null ? Double.NaN : speed;
        }
        try {
            return new LabLevels(speeds);
        } catch (IllegalArgumentException e) {
            warn(log, "machine-upgrades.speed " + e.getMessage() + "; the defaults are used.");
            return LabLevels.defaults();
        }
    }

    private static void warn(Logger log, String message) {
        if (log != null) {
            log.warning("config.yml: " + message);
        }
    }
}
