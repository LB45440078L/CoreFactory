package io.corefactory.paper.energy;

import io.corefactory.core.energy.ChartHistory.Sample;
import io.corefactory.core.energy.ChartMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChartTextTest {

    private static final String NAME = "Grid_One";
    private static final int MAX_VISIBLE = 40;

    private static List<Sample> samples(int count) {
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double supply = 1_000.0 * (i % 7);
            double load = 800.0 * ((i + 3) % 5);
            samples.add(new Sample(supply, load, (i % 10) / 10.0, 500.0 * (i % 9), 4_000.0));
        }
        return samples;
    }

    private static String visible(String line) {
        return line.replaceAll("&[0-9a-fk-or]", "");
    }

    private static Sample current() {
        return new Sample(2_000.0, 1_000.0, 0.5, 1_000.0, 4_000.0);
    }

    @Test
    void everyModeFitsTheChartSizeWithEmptyAndFullSamples() {
        for (ChartMode mode : ChartMode.values()) {
            for (List<Sample> samples : List.of(List.<Sample>of(), samples(40))) {
                List<String> lines = ChartText.lines(NAME, mode, samples, current());
                assertThat(lines).hasSize(ChartText.LINES);
                assertThat(lines.subList(1, lines.size() - 2)).hasSizeLessThanOrEqualTo(ChartText.GRAPH_ROWS);
                assertThat(lines).allSatisfy(line -> assertThat(visible(line).length()).isLessThanOrEqualTo(MAX_VISIBLE));
            }
        }
    }

    @Test
    void theHeaderNamesTheChartAndItsMode() {
        for (ChartMode mode : ChartMode.values()) {
            String header = ChartText.lines(NAME, mode, samples(10), current()).get(0);
            assertThat(visible(header)).contains(NAME).containsIgnoringCase(ChartRules.word(mode));
        }
    }

    @Test
    void theValueLineSaysNoGridWhenTheGridIsGone() {
        for (ChartMode mode : ChartMode.values()) {
            List<String> lines = ChartText.lines(NAME, mode, samples(10), null);
            assertThat(visible(lines.get(lines.size() - 1))).isEqualTo("no grid");
        }
    }

    @Test
    void productionShowsTheSupplyAndTheLoadAsTwoGraphs() {
        List<String> lines = ChartText.lines(NAME, ChartMode.PRODUCTION, samples(40), current());
        List<String> visible = lines.stream().map(ChartTextTest::visible).toList();
        assertThat(visible).anyMatch(line -> line.startsWith(" ") && line.contains("supply"));
        assertThat(visible).anyMatch(line -> line.startsWith(" ") && line.contains("load"));
        assertThat(lines).hasSize(ChartText.LINES);
    }

    @Test
    void theStorageValueLineShowsThePercentAndTheCapacity() {
        List<String> lines = ChartText.lines(NAME, ChartMode.STORAGE, samples(10), current());
        assertThat(visible(lines.get(lines.size() - 1))).isEqualTo("25 % of 4.0 kWh");
    }

    @Test
    void theSatisfactionValueLineShowsThePercentSatisfied() {
        List<String> lines = ChartText.lines(NAME, ChartMode.SATISFACTION, samples(10), current());
        assertThat(visible(lines.get(lines.size() - 1))).isEqualTo("50 % satisfied");
    }
}
