package io.corefactory.core.oil;

import io.corefactory.core.config.ConfigValues;

import java.util.Map;

/**
 * The oil settings of the {@code oil} section of config.yml: the litres one oil tube holds, and the litres per second an oil network moves
 * from its sources to its sinks.
 *
 * @param tubeCapacityLitres       the litres each oil tube holds, finite and above 0
 * @param pipeFlowLitresPerSecond  the litres per second each network moves, finite and 0 or more
 * @throws IllegalArgumentException when the capacity is not finite or not above 0, or the flow is negative or not finite
 */
public record OilSettings(double tubeCapacityLitres, double pipeFlowLitresPerSecond) {

    /** The section's name in config.yml. */
    public static final String SECTION = "oil";

    /** The default litres one oil tube holds. */
    public static final double DEFAULT_TUBE_CAPACITY_LITRES = 100.0;

    /** The default litres per second an oil network moves. */
    public static final double DEFAULT_PIPE_FLOW_LITRES_PER_SECOND = 4.0;

    public OilSettings {
        if (!Double.isFinite(tubeCapacityLitres) || tubeCapacityLitres <= 0) {
            throw new IllegalArgumentException("tube-capacity-litres must be finite and above 0, got " + tubeCapacityLitres);
        }
        if (!Double.isFinite(pipeFlowLitresPerSecond) || pipeFlowLitresPerSecond < 0) {
            throw new IllegalArgumentException("pipe-flow-litres-per-second must be finite and 0 or more, got " + pipeFlowLitresPerSecond);
        }
    }

    /** The settings the code runs with when config.yml gives none. */
    public static OilSettings defaults() {
        return new OilSettings(DEFAULT_TUBE_CAPACITY_LITRES, DEFAULT_PIPE_FLOW_LITRES_PER_SECOND);
    }

    /**
     * Reads the oil section. A missing section or key takes its default, and so does a value that is out of range or not finite.
     *
     * @param config the root of the configuration as plain data, or null for the defaults
     */
    public static OilSettings from(Map<String, Object> config) {
        Map<String, Object> oil = ConfigValues.optionalSection(config, SECTION);
        if (oil == null) {
            return defaults();
        }
        double capacity = ConfigValues.number(oil, "tube-capacity-litres", DEFAULT_TUBE_CAPACITY_LITRES);
        double flow = ConfigValues.number(oil, "pipe-flow-litres-per-second", DEFAULT_PIPE_FLOW_LITRES_PER_SECOND);
        return new OilSettings(
                Double.isFinite(capacity) && capacity > 0 ? capacity : DEFAULT_TUBE_CAPACITY_LITRES,
                Double.isFinite(flow) && flow >= 0 ? flow : DEFAULT_PIPE_FLOW_LITRES_PER_SECOND);
    }
}
