package io.corefactory.core.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns the samples of a grid into text rows: a title, the graph and an axis. The graph is drawn with the eighth-block
 * characters U+2581 to U+2588, so each column has {@code rows * 8} levels. A value is scaled to the top of its mode: 100 %
 * for satisfaction and storage, and the highest supply in view for production.
 *
 * <p>The result has {@code rows + 2} lines: the title, {@code rows} lines of graph and the axis. The graph shows the last
 * {@code width} samples, right-aligned; an empty history gives blank graph rows and "no samples" in the title.
 */
public final class ChartRenderer {

    /** The width of the axis label column, in characters. */
    public static final int LABEL_WIDTH = 9;

    private static final int LEVELS_PER_ROW = 8;
    private static final char BLOCK_BASE = '▀';
    private static final char AXIS_CORNER = '└';
    private static final char AXIS_LINE = '─';

    private ChartRenderer() {
    }

    /**
     * Renders one chart.
     *
     * @param mode    the chart's mode
     * @param samples the samples, oldest first
     * @param width   the number of graph columns, positive
     * @param rows    the number of graph rows, positive
     * @return the title, the graph rows from the top and the axis
     */
    public static List<String> render(ChartMode mode, List<ChartHistory.Sample> samples, int width, int rows) {
        if (width < 1 || rows < 1) {
            throw new IllegalArgumentException("width and rows must be positive: " + width + ", " + rows);
        }
        List<Double> values = new ArrayList<>(samples.size());
        for (ChartHistory.Sample sample : samples) {
            values.add(valueOf(mode, sample));
        }
        List<Double> window = values.subList(Math.max(0, values.size() - width), values.size());
        double top = topOf(mode, window);
        List<String> graph = bars(values, top, width, rows);

        List<String> lines = new ArrayList<>(rows + 2);
        lines.add(title(mode, samples.isEmpty()));
        for (int row = 0; row < rows; row++) {
            lines.add(labelFor(mode, top, row == 0, row == rows - 1) + graph.get(row));
        }
        lines.add(axis(width));
        return List.copyOf(lines);
    }

    /**
     * The bar rows of a series, without a label or an axis: the last {@code width} values, right-aligned, each scaled to
     * {@code top}. A value at or above the top fills its column. Each row is {@code width} characters, the top row first.
     *
     * @param values the values, oldest first
     * @param top    the value that fills a column, in the same unit as the values; 0 or less draws nothing
     * @param width  the number of columns, positive
     * @param rows   the number of rows, positive
     */
    public static List<String> bars(List<Double> values, double top, int width, int rows) {
        if (width < 1 || rows < 1) {
            throw new IllegalArgumentException("width and rows must be positive: " + width + ", " + rows);
        }
        List<Double> window = values.subList(Math.max(0, values.size() - width), values.size());
        int levels = rows * LEVELS_PER_ROW;
        int[] heights = new int[width];
        int offset = width - window.size();
        for (int i = 0; i < window.size(); i++) {
            heights[offset + i] = top <= 0 ? 0 : (int) Math.round(Math.min(1.0, window.get(i) / top) * levels);
        }
        List<String> rowsOut = new ArrayList<>(rows);
        for (int row = rows - 1; row >= 0; row--) {
            StringBuilder line = new StringBuilder(width);
            for (int column = 0; column < width; column++) {
                int cell = Math.min(LEVELS_PER_ROW, Math.max(0, heights[column] - row * LEVELS_PER_ROW));
                line.append(cell == 0 ? ' ' : (char) (BLOCK_BASE + cell));
            }
            rowsOut.add(line.toString());
        }
        return List.copyOf(rowsOut);
    }

    /** The axis line under a graph: the corner, then a line of {@code width} characters, under the label column. */
    public static String axis(int width) {
        return " ".repeat(LABEL_WIDTH) + AXIS_CORNER + String.valueOf(AXIS_LINE).repeat(width);
    }

    private static double valueOf(ChartMode mode, ChartHistory.Sample sample) {
        return switch (mode) {
            case SATISFACTION -> sample.satisfaction() * 100.0;
            case PRODUCTION -> Units.kilowatts(sample.supplyW());
            case STORAGE -> sample.capacityWh() <= 0 ? 0.0 : sample.storedWh() / sample.capacityWh() * 100.0;
        };
    }

    private static double topOf(ChartMode mode, List<Double> window) {
        if (mode != ChartMode.PRODUCTION) {
            return 100.0;
        }
        double top = 0.0;
        for (double value : window) {
            top = Math.max(top, value);
        }
        return top;
    }

    private static String title(ChartMode mode, boolean empty) {
        String name = switch (mode) {
            case SATISFACTION -> "Satisfaction (%)";
            case PRODUCTION -> "Production (kW)";
            case STORAGE -> "Storage (% of capacity)";
        };
        return empty ? name + " - no samples" : name;
    }

    private static String labelFor(ChartMode mode, double top, boolean topRow, boolean bottomRow) {
        String text;
        if (topRow) {
            text = switch (mode) {
                case SATISFACTION, STORAGE -> String.format(Locale.ROOT, "%.0f%%", top);
                case PRODUCTION -> String.format(Locale.ROOT, "%.1f kW", top);
            };
        } else if (bottomRow) {
            text = "0";
        } else {
            text = "";
        }
        return label(text);
    }

    /** A label padded to the label column: right-aligned in {@link #LABEL_WIDTH} - 1 characters, then a space. */
    public static String label(String text) {
        return String.format(Locale.ROOT, "%" + (LABEL_WIDTH - 1) + "s ", text);
    }
}
