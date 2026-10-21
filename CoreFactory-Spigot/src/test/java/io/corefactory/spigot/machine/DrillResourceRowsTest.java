package io.corefactory.spigot.machine;

import io.corefactory.core.drill.DrillReport;
import io.corefactory.core.drill.DrillSelection;
import io.corefactory.spigot.machine.DrillResourceRows.Line;
import io.corefactory.spigot.machine.DrillResourceRows.OreLabel;
import io.corefactory.spigot.machine.DrillResourceRows.Toggle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DrillResourceRowsTest {

    private static final List<String> ORDER = List.of("uraninite_ore", "galena_ore", "crude_petroleum_deposit");
    private static final Map<String, OreLabel> LABELS = Map.of(
            "uraninite_ore", new OreLabel("&8Uraninite Ore", "DEEPSLATE", false),
            "galena_ore", new OreLabel("&9Galena Ore", "STONE", false),
            "crude_petroleum_deposit", new OreLabel("&8Crude Petroleum Deposit", "WHITE_GLAZED_TERRACOTTA", true));

    @Test
    void oneLinePerReportRowWithItsNameIconAndCounts() {
        List<DrillReport.Row> rows = DrillReport.build(ORDER, Map.of("uraninite_ore", 5L, "galena_ore", 2L),
                Map.of("uraninite_ore", 3L), new DrillSelection());

        List<Line> lines = DrillResourceRows.of(rows, LABELS);

        assertThat(lines).extracting(Line::oreId).containsExactly("uraninite_ore", "galena_ore");
        Line uraninite = lines.get(0);
        assertThat(uraninite.name()).isEqualTo("&8Uraninite Ore");
        assertThat(uraninite.icon()).isEqualTo("DEEPSLATE");
        assertThat(uraninite.lore()).containsExactly(
                "&7In range now: &f5", "&7Mined by this drill: &f3", "&7Total in range: &f8");
        assertThat(uraninite.toggle()).isEqualTo(Toggle.MINING);
        assertThat(uraninite.exhausted()).isFalse();
    }

    @Test
    void anIgnoredOreShowsTheIgnoringToggle() {
        DrillSelection selection = new DrillSelection(List.of("galena_ore"));
        List<DrillReport.Row> rows = DrillReport.build(ORDER, Map.of("galena_ore", 4L), Map.of(), selection);

        Line galena = DrillResourceRows.of(rows, LABELS).get(0);

        assertThat(galena.toggle()).isEqualTo(Toggle.IGNORING);
        assertThat(galena.exhausted()).isFalse();
        assertThat(galena.icon()).isEqualTo("STONE");
    }

    @Test
    void anExhaustedOreIsGreyWithTheExhaustedLine() {
        List<DrillReport.Row> rows = DrillReport.build(ORDER, Map.of(), Map.of("galena_ore", 6L), new DrillSelection());

        Line galena = DrillResourceRows.of(rows, LABELS).get(0);

        assertThat(galena.exhausted()).isTrue();
        assertThat(galena.icon()).isEqualTo(DrillResourceRows.EXHAUSTED_ICON).isEqualTo("GRAY_STAINED_GLASS_PANE");
        assertThat(galena.name()).isEqualTo("&7Galena Ore");
        assertThat(galena.lore()).last().isEqualTo("&8exhausted in range");
        assertThat(galena.lore()).contains("&7Total in range: &f6");
        assertThat(galena.toggle()).isEqualTo(Toggle.MINING);
    }

    @Test
    void aDepositIsCountedInVolumeUnits() {
        List<DrillReport.Row> rows = DrillReport.build(ORDER, Map.of("crude_petroleum_deposit", 380L),
                Map.of("crude_petroleum_deposit", 20L), new DrillSelection());

        Line deposit = DrillResourceRows.of(rows, LABELS).get(0);

        assertThat(deposit.lore()).containsExactly(
                "&7In range now: &f380 units", "&7Mined by this drill: &f20 units", "&7Total in range: &f400 units");
    }

    @Test
    void anOreWithNoLabelIsShownByItsId() {
        List<DrillReport.Row> rows = DrillReport.build(List.of(), Map.of("zinc_ore", 1L), Map.of(), new DrillSelection());

        Line zinc = DrillResourceRows.of(rows, LABELS).get(0);

        assertThat(zinc.name()).isEqualTo("zinc_ore");
        assertThat(zinc.icon()).isEqualTo(DrillResourceRows.DEFAULT_ICON);
    }

    @Test
    void theToggleFollowsTheSelectionTheReportWasBuiltWith() {
        DrillSelection selection = new DrillSelection();
        List<DrillReport.Row> rows = DrillReport.build(ORDER, Map.of("galena_ore", 1L), Map.of(), selection);
        assertThat(DrillResourceRows.of(rows, LABELS).get(0).toggle()).isEqualTo(Toggle.MINING);

        selection.toggle("galena_ore");
        List<DrillReport.Row> after = DrillReport.build(ORDER, Map.of("galena_ore", 1L), Map.of(), selection);
        assertThat(DrillResourceRows.of(after, LABELS).get(0).toggle()).isEqualTo(Toggle.IGNORING);
    }
}
