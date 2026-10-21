package io.corefactory.core.energy;

import io.corefactory.core.energy.ChartHistory.Sample;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChartRendererTest {

    private static Sample production(double supplyW) {
        return new Sample(supplyW, 0, 1.0, 0, 0);
    }

    private static Sample satisfaction(double satisfaction) {
        return new Sample(0, 0, satisfaction, 0, 0);
    }

    private static Sample storage(double storedWh, double capacityWh) {
        return new Sample(0, 0, 1.0, storedWh, capacityWh);
    }

    private static String graph(List<String> lines, int index) {
        return lines.get(index);
    }

    private static String cells(String line) {
        return line.substring(ChartRenderer.LABEL_WIDTH);
    }

    @Test
    void theOutputHasTheTitleTheRowsAndTheAxis() {
        for (int rows : new int[] {1, 3, 6}) {
            List<String> lines = ChartRenderer.render(ChartMode.PRODUCTION,
                    List.of(production(1_000), production(2_000)), 10, rows);
            assertThat(lines).hasSize(rows + 2);
            assertThat(lines.get(0)).startsWith("Production");
            assertThat(lines.get(lines.size() - 1)).contains("└");
        }
    }

    @Test
    void anEmptyHistoryShowsNoSamplesAndBlankRows() {
        List<String> lines = ChartRenderer.render(ChartMode.SATISFACTION, List.of(), 12, 4);
        assertThat(lines).hasSize(6);
        assertThat(lines.get(0)).contains("no samples");
        for (int row = 1; row <= 4; row++) {
            assertThat(cells(graph(lines, row)).trim()).isEmpty();
        }
    }

    @Test
    void aConstantProductionSeriesFillsEveryRowOfItsColumns() {
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            samples.add(production(5_000));
        }
        List<String> lines = ChartRenderer.render(ChartMode.PRODUCTION, samples, 4, 2);
        assertThat(cells(graph(lines, 1))).isEqualTo("████");
        assertThat(cells(graph(lines, 2))).isEqualTo("████");
        assertThat(graph(lines, 1)).startsWith("  5.0 kW ");
    }

    @Test
    void aConstantSatisfactionOfHalfFillsTheBottomHalfOfTheGraph() {
        List<Sample> samples = List.of(satisfaction(0.5), satisfaction(0.5), satisfaction(0.5));
        List<String> lines = ChartRenderer.render(ChartMode.SATISFACTION, samples, 3, 2);
        assertThat(cells(graph(lines, 1))).isEqualTo("   ");
        assertThat(cells(graph(lines, 2))).isEqualTo("███");
        assertThat(graph(lines, 1)).startsWith("    100% ");
        assertThat(graph(lines, 2)).startsWith("       0 ");
    }

    @Test
    void productionIsScaledToTheHighestSupplyInView() {
        List<String> lines = ChartRenderer.render(ChartMode.PRODUCTION,
                List.of(production(1_000), production(2_000), production(4_000)), 3, 1);
        assertThat(cells(graph(lines, 1))).isEqualTo("▂▄█");
        assertThat(graph(lines, 1)).startsWith("  4.0 kW ");
    }

    @Test
    void storageIsShownAsPercentOfCapacity() {
        List<String> lines = ChartRenderer.render(ChartMode.STORAGE,
                List.of(storage(2_500, 5_000), storage(5_000, 5_000)), 2, 1);
        assertThat(cells(graph(lines, 1))).isEqualTo("▄█");
    }

    @Test
    void aStorageWithNoCapacityShowsNothing() {
        List<String> lines = ChartRenderer.render(ChartMode.STORAGE, List.of(storage(100, 0)), 1, 1);
        assertThat(cells(graph(lines, 1))).isEqualTo(" ");
    }

    @Test
    void onlyTheLastWidthSamplesAreShownRightAligned() {
        List<Sample> samples = new ArrayList<>();
        for (double s : new double[] {0.1, 0.9, 0.2, 0.3, 0.4}) {
            samples.add(satisfaction(s));
        }
        List<String> lines = ChartRenderer.render(ChartMode.SATISFACTION, samples, 3, 1);
        assertThat(cells(graph(lines, 1))).isEqualTo("▂▂▃");
    }

    @Test
    void aShortHistoryIsRightAlignedWithBlankColumnsOnTheLeft() {
        List<String> lines = ChartRenderer.render(ChartMode.SATISFACTION, List.of(satisfaction(1.0)), 4, 1);
        assertThat(cells(graph(lines, 1))).isEqualTo("   █");
    }

    @Test
    void theGraphRowsAreCountedByHeight() {
        List<String> lines = ChartRenderer.render(ChartMode.SATISFACTION, List.of(satisfaction(0.5)), 1, 5);
        assertThat(lines).hasSize(7);
        assertThat(cells(graph(lines, 1))).isEqualTo(" ");
        assertThat(cells(graph(lines, 3))).isEqualTo("▄");
        assertThat(cells(graph(lines, 5))).isEqualTo("█");
    }

    @Test
    void aZeroOrNegativeSizeIsRefused() {
        assertThatThrownBy(() -> ChartRenderer.render(ChartMode.PRODUCTION, List.of(), 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChartRenderer.render(ChartMode.PRODUCTION, List.of(), 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
