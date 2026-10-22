package io.corefactory.spigot.machine;

import io.corefactory.core.deposit.DrillYield;
import io.corefactory.core.drill.DrillTable;

import java.util.List;
import java.util.logging.Logger;

/**
 * Reads {@code drill-rates} from config.yml: the items a drill of each level yields per second at full power, level 1 first. A
 * list that is the wrong length, or holds a rate the yield table refuses, is logged and the defaults are used.
 */
public final class DrillRates {

    /** The rates used when config.yml gives none that are valid, items per second at full power for levels 1 to 5. */
    static final List<Double> DEFAULTS = List.of(1.0, 1.5, 2.5, 4.0, 6.0);

    private DrillRates() {
    }

    /**
     * The yield table of the drills.
     *
     * @param configured the list of {@code drill-rates}, or null when the key is missing
     * @param log        receives one warning when the list is not used
     */
    public static DrillYield read(List<Double> configured, Logger log) {
        if (configured == null || configured.size() != DrillTable.MAX_LEVEL) {
            warn(log, "drill-rates needs one rate for each of the " + DrillTable.MAX_LEVEL + " drill levels; the defaults are used.");
            return defaults();
        }
        double[] rates = new double[configured.size()];
        for (int i = 0; i < rates.length; i++) {
            Double rate = configured.get(i);
            rates[i] = rate == null ? Double.NaN : rate;
        }
        try {
            return new DrillYield(rates);
        } catch (IllegalArgumentException e) {
            warn(log, "drill-rates " + e.getMessage() + "; the defaults are used.");
            return defaults();
        }
    }

    /** The yield table of the default rates. */
    static DrillYield defaults() {
        return new DrillYield(DEFAULTS.stream().mapToDouble(Double::doubleValue).toArray());
    }

    private static void warn(Logger log, String message) {
        if (log != null) {
            log.warning("config.yml: " + message);
        }
    }
}
