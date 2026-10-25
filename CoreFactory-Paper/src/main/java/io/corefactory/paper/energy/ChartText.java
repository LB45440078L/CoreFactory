package io.corefactory.paper.energy;

import io.corefactory.core.energy.ChartHistory;
import io.corefactory.core.energy.ChartMode;
import io.corefactory.core.energy.ChartRenderer;
import io.corefactory.core.energy.Units;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/**
 * The lines of a grid chart: a header with its name and mode, the graph in the block characters, the axis, and one line with the
 * current value. A chart is at most {@link #LINES} lines, the graph at most {@link #GRAPH_ROWS} rows, and each line at most
 * {@link #WIDTH} + {@link ChartRenderer#LABEL_WIDTH} characters, not counting colour codes. Production shows the supply and the load
 * as two graphs of half the rows each when there are at least {@link #TWO_SERIES_MIN_ROWS} rows, otherwise the supply alone. Pure.
 */
public final class ChartText {

    /** The graph columns: the last thirty samples, one per second. */
    public static final int WIDTH = 30;

    /** The most graph rows. */
    public static final int GRAPH_ROWS = 8;

    /** The fewest rows at which production shows two series. */
    public static final int TWO_SERIES_MIN_ROWS = 6;

    /** The most lines of a chart: header, graph, axis and value. */
    public static final int LINES = GRAPH_ROWS + 3;

    private static final String NO_GRID = "&7no grid";

    private ChartText() {
    }

    /**
     * The lines of a chart.
     *
     * @param name    the chart's name
     * @param mode    the mode it shows
     * @param samples the grid's samples, oldest first; may be empty
     * @param current the grid's sample for this tick, or null when the grid is gone
     */
    public static List<String> lines(String name, ChartMode mode, List<ChartHistory.Sample> samples,
                                     ChartHistory.Sample current) {
        List<String> lines = new ArrayList<>(LINES);
        lines.add("&6" + name + " &7- " + title(mode));
        switch (mode) {
            case SATISFACTION -> lines.addAll(block(values(samples, s -> s.satisfaction() * 100.0), 100.0, GRAPH_ROWS, "100%"));
            case STORAGE -> lines.addAll(block(values(samples, ChartText::storagePercent), 100.0, GRAPH_ROWS, "100%"));
            case PRODUCTION -> lines.addAll(production(samples));
        }
        lines.add(ChartRenderer.axis(WIDTH));
        lines.add(valueLine(mode, current));
        return List.copyOf(lines);
    }

    private static String title(ChartMode mode) {
        return switch (mode) {
            case SATISFACTION -> "Satisfaction";
            case PRODUCTION -> "Production";
            case STORAGE -> "Storage";
        };
    }

    private static List<String> production(List<ChartHistory.Sample> samples) {
        List<Double> supply = values(samples, s -> Units.kilowatts(s.supplyW()));
        if (GRAPH_ROWS < TWO_SERIES_MIN_ROWS) {
            return block(supply, peak(supply), GRAPH_ROWS, format(peak(supply)));
        }
        List<Double> load = values(samples, s -> Units.kilowatts(s.loadW()));
        double top = Math.max(peak(supply), peak(load));
        int rows = GRAPH_ROWS / 2;
        List<String> lines = new ArrayList<>(GRAPH_ROWS);
        lines.addAll(block(supply, top, rows, "supply"));
        lines.addAll(block(load, top, rows, "load"));
        return lines;
    }

    /** One graph: the bars of the values on {@code rows} rows, with a label on the top row and {@code 0} on the bottom row. */
    private static List<String> block(List<Double> values, double top, int rows, String topLabel) {
        List<String> bars = ChartRenderer.bars(values, top, WIDTH, rows);
        List<String> lines = new ArrayList<>(rows);
        for (int row = 0; row < rows; row++) {
            String label = row == 0 ? topLabel : row == rows - 1 ? "0" : "";
            lines.add(ChartRenderer.label(label) + bars.get(row));
        }
        return lines;
    }

    private static String valueLine(ChartMode mode, ChartHistory.Sample current) {
        if (current == null) {
            return NO_GRID;
        }
        return switch (mode) {
            case SATISFACTION -> "&f" + Units.percentText(current.satisfaction()) + " satisfied";
            case PRODUCTION -> "&fsupply " + Units.powerText(current.supplyW()) + " &7load " + Units.powerText(current.loadW());
            case STORAGE -> "&f" + Units.percentText(storagePercent(current) / 100.0) + " of "
                    + Units.energyText(current.capacityWh());
        };
    }

    private static double storagePercent(ChartHistory.Sample sample) {
        return sample.capacityWh() <= 0 ? 0.0 : sample.storedWh() / sample.capacityWh() * 100.0;
    }

    private static List<Double> values(List<ChartHistory.Sample> samples, ToDoubleFunction<ChartHistory.Sample> of) {
        List<Double> values = new ArrayList<>(samples.size());
        for (ChartHistory.Sample sample : samples) {
            values.add(of.applyAsDouble(sample));
        }
        return values;
    }

    private static double peak(List<Double> values) {
        double top = 0.0;
        for (double value : values) {
            top = Math.max(top, value);
        }
        return top;
    }

    private static String format(double kilowatts) {
        return String.format(Locale.ROOT, "%.1f kW", kilowatts);
    }
}
