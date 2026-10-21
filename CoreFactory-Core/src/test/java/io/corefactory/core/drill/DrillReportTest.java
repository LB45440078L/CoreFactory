package io.corefactory.core.drill;

import io.corefactory.core.drill.DrillReport.Row;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DrillReportTest {

    private static final List<String> ORDER = List.of("cinnabar_ore", "galena_ore", "thorite_ore");

    @Test
    void totalIsWhatIsInRangePlusWhatThisDrillMined() {
        List<Row> rows = DrillReport.build(ORDER, Map.of("galena_ore", 7L), Map.of("galena_ore", 3L),
                new DrillSelection());

        assertThat(rows).containsExactly(new Row("galena_ore", 7, 3, 10, true, false));
    }

    @Test
    void rowsOnlyForOresInRangeOrMinedAndInOrderOfTheList() {
        List<Row> rows = DrillReport.build(ORDER, Map.of("thorite_ore", 2L, "cinnabar_ore", 1L),
                Map.of("galena_ore", 4L), new DrillSelection());

        assertThat(rows).extracting(Row::oreId).containsExactly("cinnabar_ore", "galena_ore", "thorite_ore");
        assertThat(rows.get(1)).isEqualTo(new Row("galena_ore", 0, 4, 4, true, true));
    }

    @Test
    void anOreWithNothingInRangeAndNothingMinedHasNoRow() {
        assertThat(DrillReport.build(ORDER, Map.of("galena_ore", 0L), Map.of(), new DrillSelection())).isEmpty();
    }

    @Test
    void aSelectedOreWithNothingLeftInRangeIsExhausted() {
        List<Row> rows = DrillReport.build(ORDER, Map.of(), Map.of("galena_ore", 6L), new DrillSelection());

        assertThat(rows).containsExactly(new Row("galena_ore", 0, 6, 6, true, true));
    }

    @Test
    void anIgnoredOreIsNeverExhaustedAndIsMarkedNotSelected() {
        DrillSelection selection = new DrillSelection(List.of("galena_ore", "thorite_ore"));

        List<Row> rows = DrillReport.build(ORDER, Map.of("galena_ore", 2L), Map.of("thorite_ore", 1L), selection);

        assertThat(rows).containsExactly(
                new Row("galena_ore", 2, 0, 2, false, false),
                new Row("thorite_ore", 0, 1, 1, false, false));
    }

    @Test
    void oresOutsideTheListStillShowAfterTheListedOnes() {
        List<Row> rows = DrillReport.build(ORDER, Map.of("zinc_ore", 1L, "thorite_ore", 1L), Map.of(),
                new DrillSelection());

        assertThat(rows).extracting(Row::oreId).containsExactly("thorite_ore", "zinc_ore");
    }

    @Test
    void negativeCountsReadAsZero() {
        assertThat(DrillReport.build(ORDER, Map.of("galena_ore", -4L), Map.of(), new DrillSelection())).isEmpty();
    }
}
