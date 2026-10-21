package io.corefactory.core.drill;

import io.corefactory.core.geology.Placement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TargetChoiceTest {

    private static final DrillBox BOX = DrillBox.around(0, 60, 0, DrillTable.level(1));

    private static Placement at(int x, int y, int z) {
        return new Placement(x, y, z, 0);
    }

    @Test
    void chooseTakesTheNearestBlockOfASelectedOre() {
        Placement far = at(4, 58, 4);
        Placement near = at(1, 60, 0);
        Map<String, List<Placement>> byOre = Map.of("galena_ore", List.of(far, near));

        assertThat(TargetChoice.choose(byOre, BOX, 0, 60, 0, new DrillSelection(), ore -> 2)).contains(near);
    }

    @Test
    void anIgnoredOreIsSkippedEvenWhenItIsNearest() {
        Placement ignoredNear = at(1, 60, 0);
        Placement mineFar = at(-3, 50, -3);
        Map<String, List<Placement>> byOre = Map.of(
                "galena_ore", List.of(ignoredNear),
                "thorite_ore", List.of(mineFar));
        DrillSelection selection = new DrillSelection(List.of("galena_ore"));

        assertThat(TargetChoice.choose(byOre, BOX, 0, 60, 0, selection, ore -> 1)).contains(mineFar);
    }

    @Test
    void anOreWithNoBudgetLeftIsSkipped() {
        Placement exhausted = at(1, 60, 0);
        Placement spare = at(-3, 50, -3);
        Map<String, List<Placement>> byOre = Map.of(
                "galena_ore", List.of(exhausted),
                "thorite_ore", List.of(spare));

        assertThat(TargetChoice.choose(byOre, BOX, 0, 60, 0, new DrillSelection(),
                ore -> ore.equals("galena_ore") ? 0 : 3)).contains(spare);
    }

    @Test
    void chooseIsEmptyWhenNoSelectedOreHasBudgetLeft() {
        Map<String, List<Placement>> byOre = Map.of("galena_ore", List.of(at(1, 60, 0)));

        assertThat(TargetChoice.choose(byOre, BOX, 0, 60, 0, new DrillSelection(List.of("galena_ore")), ore -> 5))
                .isEmpty();
        assertThat(TargetChoice.choose(byOre, BOX, 0, 60, 0, new DrillSelection(), ore -> 0)).isEmpty();
        assertThat(TargetChoice.choose(Map.of(), BOX, 0, 60, 0, new DrillSelection(), ore -> 5)).isEmpty();
    }

    @Test
    void aBlockOutsideTheBoxIsNeverChosen() {
        Map<String, List<Placement>> byOre = Map.of("galena_ore", List.of(at(9, 60, 0)));

        assertThat(TargetChoice.choose(byOre, BOX, 0, 60, 0, new DrillSelection(), ore -> 1)).isEmpty();
    }

    @Test
    void tiesGoToTheLowerYThenXThenZ() {
        Placement highY = at(1, 61, 0);
        Placement lowY = at(1, 59, 0);
        Map<String, List<Placement>> byOre = Map.of("galena_ore", List.of(highY, lowY));
        assertThat(TargetChoice.choose(byOre, BOX, 0, 60, 0, new DrillSelection(), ore -> 2)).contains(lowY);

        Placement xHigh = at(1, 60, 0);
        Placement xLow = at(-1, 60, 0);
        Map<String, List<Placement>> sameDistance = Map.of("galena_ore", List.of(xHigh, xLow));
        assertThat(TargetChoice.choose(sameDistance, BOX, 0, 60, 0, new DrillSelection(), ore -> 2)).contains(xLow);
    }
}
