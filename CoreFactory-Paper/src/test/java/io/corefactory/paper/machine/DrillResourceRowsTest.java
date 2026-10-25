package io.corefactory.paper.machine;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.drill.DrillSelection;
import io.corefactory.paper.machine.DrillResourceRows.Line;
import io.corefactory.paper.machine.DrillResourceRows.OreLabel;
import io.corefactory.paper.machine.DrillResourceRows.Toggle;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DrillResourceRowsTest {

    private static final Map<String, OreLabel> LABELS = Map.of(
            "galena_ore", new OreLabel("&7Galena Ore", "IRON_ORE"),
            "crude_petroleum_deposit", new OreLabel("&8Crude Petroleum Deposit", "WHITE_GLAZED_TERRACOTTA"));

    private static Deposit galena(long id, long remaining) {
        return new Deposit(id, "world", 12, -7, 9, "galena_ore", 2_500_000, remaining);
    }

    @Test
    void aLineShowsEachDepositWithItsOresIconAndWhatIsLeft() {
        List<Line> lines = DrillResourceRows.of(List.of(galena(4, 1_200_000)), LABELS, new DrillSelection());

        assertThat(lines).hasSize(1);
        Line line = lines.get(0);
        assertThat(line.depositId()).isEqualTo(4);
        assertThat(line.oreId()).isEqualTo("galena_ore");
        assertThat(line.name()).isEqualTo("&7Galena Ore");
        assertThat(line.icon()).isEqualTo("IRON_ORE");
        assertThat(line.lore()).containsExactly("&7Left &f1.2M / 2.5M (48%)", "&7At &f12 -7");
        assertThat(line.toggle()).isEqualTo(Toggle.MINING);
        assertThat(line.exhausted()).isFalse();
    }

    @Test
    void theOrderOfTheDepositsIsKept() {
        List<Line> lines = DrillResourceRows.of(List.of(galena(9, 10), galena(2, 20)), LABELS, new DrillSelection());
        assertThat(lines).extracting(Line::depositId).containsExactly(9L, 2L);
    }

    @Test
    void theToggleFollowsTheSelectionOfTheOre() {
        DrillSelection selection = new DrillSelection();
        assertThat(DrillResourceRows.of(List.of(galena(1, 5)), LABELS, selection).get(0).toggle()).isEqualTo(Toggle.MINING);
        selection.toggle("galena_ore");
        assertThat(DrillResourceRows.of(List.of(galena(1, 5)), LABELS, selection).get(0).toggle()).isEqualTo(Toggle.IGNORING);
    }

    @Test
    void anEmptyDepositIsGreyGlassAndSaysSo() {
        Line line = DrillResourceRows.of(List.of(galena(1, 0)), LABELS, new DrillSelection()).get(0);

        assertThat(line.exhausted()).isTrue();
        assertThat(line.icon()).isEqualTo(DrillResourceRows.EXHAUSTED_ICON).isEqualTo("GRAY_STAINED_GLASS_PANE");
        assertThat(line.name()).isEqualTo("&7Galena Ore");
        assertThat(line.lore()).contains("&8empty");
    }

    @Test
    void anOreWithoutALabelIsShownByItsIdAndAStoneIcon() {
        Deposit zinc = new Deposit(3, "world", 0, 0, 4, "zinc_ore", 900_000, 900_000);
        Line line = DrillResourceRows.of(List.of(zinc), LABELS, new DrillSelection()).get(0);

        assertThat(line.name()).isEqualTo("zinc_ore");
        assertThat(line.icon()).isEqualTo(DrillResourceRows.DEFAULT_ICON).isEqualTo("STONE");
    }

    @Test
    void aLabelWithoutAHostKeepsTheDefaultIcon() {
        Deposit galena = galena(1, 5);
        Line line = DrillResourceRows.of(List.of(galena), Map.of("galena_ore", new OreLabel("&7Galena Ore", null)),
                new DrillSelection()).get(0);
        assertThat(line.icon()).isEqualTo(DrillResourceRows.DEFAULT_ICON);
    }

    @Test
    void noDepositsMeansNoLines() {
        assertThat(DrillResourceRows.of(List.of(), LABELS, new DrillSelection())).isEmpty();
    }

    @Test
    void plainTextDropsTheColourCodes() {
        assertThat(DrillResourceRows.plain("&8Crude &fPetroleum")).isEqualTo("Crude Petroleum");
    }
}
