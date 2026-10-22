package io.corefactory.core.water;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.List;

/**
 * The water settings of the {@code water} section of config.yml: the source minimum of a pump, the source check and solve rates,
 * the flow and power of each pump level, and the water a process run takes.
 *
 * @param sourceMinBlocks    the water blocks a pump's source needs, at least 1
 * @param sourceCheckTicks   ticks between two checks of a pump's source, at least 1
 * @param solveTicks         ticks between two solves of the water network, at least 1
 * @param pumpFlow           units per second each pump level delivers, index 0 is level 1; five values, each finite and 0 or more
 * @param pumpPowerW         power each pump level draws in W, index 0 is level 1; five values, each 0 or more
 * @param unitPerProcessRun  water units a run of a process takes from the network, at least 1
 */
public record WaterSettings(int sourceMinBlocks, int sourceCheckTicks, int solveTicks, List<Double> pumpFlow,
                            List<Integer> pumpPowerW, int unitPerProcessRun) {

    /** The section's name in config.yml. */
    public static final String SECTION = "water";

    /** The default water blocks a pump's source needs. */
    public static final int DEFAULT_SOURCE_MIN_BLOCKS = 100;

    /** The default ticks between two checks of a pump's source. */
    public static final int DEFAULT_SOURCE_CHECK_TICKS = 200;

    /** The default ticks between two solves of the water network. */
    public static final int DEFAULT_SOLVE_TICKS = 5;

    /** The default water units a process run takes. */
    public static final int DEFAULT_UNIT_PER_PROCESS_RUN = 1;

    private static final List<Double> DEFAULT_FLOW = List.of(2.0, 4.0, 8.0, 12.0, 16.0);
    private static final List<Integer> DEFAULT_POWER = List.of(0, 0, 60000, 120000, 240000);

    public WaterSettings {
        if (sourceMinBlocks < 1) {
            throw new IllegalArgumentException("source-min-blocks must be at least 1, got " + sourceMinBlocks);
        }
        if (sourceCheckTicks < 1 || solveTicks < 1) {
            throw new IllegalArgumentException("the water tick intervals must be at least 1");
        }
        if (unitPerProcessRun < 1) {
            throw new IllegalArgumentException("unit-per-process-run must be at least 1, got " + unitPerProcessRun);
        }
        if (pumpFlow.size() != PumpLevels.MAX_LEVEL || pumpPowerW.size() != PumpLevels.MAX_LEVEL) {
            throw new IllegalArgumentException("pump-flow and pump-power-w need " + PumpLevels.MAX_LEVEL + " values");
        }
        for (double flow : pumpFlow) {
            if (!(flow >= 0) || Double.isInfinite(flow)) {
                throw new IllegalArgumentException("pump-flow must be finite and 0 or more, got " + flow);
            }
        }
        for (int power : pumpPowerW) {
            if (power < 0) {
                throw new IllegalArgumentException("pump-power-w must be 0 or more, got " + power);
            }
        }
        pumpFlow = List.copyOf(pumpFlow);
        pumpPowerW = List.copyOf(pumpPowerW);
    }

    /** The settings the code runs with when config.yml gives none. */
    public static WaterSettings defaults() {
        return new WaterSettings(DEFAULT_SOURCE_MIN_BLOCKS, DEFAULT_SOURCE_CHECK_TICKS, DEFAULT_SOLVE_TICKS, DEFAULT_FLOW,
                DEFAULT_POWER, DEFAULT_UNIT_PER_PROCESS_RUN);
    }

    /**
     * Reads the water section. A missing key takes its default. A list that is not five values long, or holds a value out of
     * range, takes the defaults, and a value below its minimum is raised to it.
     *
     * @param config the root of the configuration, or null for the defaults
     */
    public static WaterSettings from(ConfigurationSection config) {
        ConfigurationSection water = config == null ? null : config.getConfigurationSection(SECTION);
        if (water == null) {
            water = new MemoryConfiguration();
        }
        WaterSettings fallback = defaults();
        List<Double> flow = water.getDoubleList("pump-flow");
        List<Integer> power = water.getIntegerList("pump-power-w");
        boolean flowOk = flow.size() == PumpLevels.MAX_LEVEL
                && flow.stream().allMatch(value -> value >= 0 && Double.isFinite(value));
        boolean powerOk = power.size() == PumpLevels.MAX_LEVEL && power.stream().allMatch(value -> value >= 0);
        return new WaterSettings(
                Math.max(1, water.getInt("source-min-blocks", DEFAULT_SOURCE_MIN_BLOCKS)),
                Math.max(1, water.getInt("source-check-ticks", DEFAULT_SOURCE_CHECK_TICKS)),
                Math.max(1, water.getInt("solve-ticks", DEFAULT_SOLVE_TICKS)),
                flowOk ? flow : fallback.pumpFlow(),
                powerOk ? power : fallback.pumpPowerW(),
                Math.max(1, water.getInt("unit-per-process-run", DEFAULT_UNIT_PER_PROCESS_RUN)));
    }

    /** The flow of each pump level as an array, index 0 is level 1: the form the solver takes. */
    public double[] flowTable() {
        double[] table = new double[pumpFlow.size()];
        for (int i = 0; i < table.length; i++) {
            table[i] = pumpFlow.get(i);
        }
        return table;
    }
}
