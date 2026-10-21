package io.corefactory.core.drill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DrillSelectionTest {

    @Test
    void everyOreIsMinedByDefault() {
        DrillSelection selection = new DrillSelection();
        assertThat(selection.isMining("uraninite_ore")).isTrue();
        assertThat(selection.serialise()).isEmpty();
    }

    @Test
    void toggleIgnoresThenMinesAgain() {
        DrillSelection selection = new DrillSelection();

        assertThat(selection.toggle("galena_ore")).isFalse();
        assertThat(selection.isMining("galena_ore")).isFalse();
        assertThat(selection.isMining("thorite_ore")).isTrue();

        assertThat(selection.toggle("galena_ore")).isTrue();
        assertThat(selection.isMining("galena_ore")).isTrue();
    }

    @Test
    void serialisesOnlyTheIgnoredOresInAlphabeticalOrder() {
        DrillSelection selection = new DrillSelection();
        selection.toggle("thorite_ore");
        selection.toggle("cinnabar_ore");
        selection.toggle("galena_ore");
        selection.toggle("galena_ore");

        assertThat(selection.serialise()).containsExactly("cinnabar_ore", "thorite_ore");
    }

    @Test
    void readsASavedListAndDropsBlankAndRepeatedIds() {
        DrillSelection selection = new DrillSelection(List.of("galena_ore", "", "galena_ore", " "));

        assertThat(selection.serialise()).containsExactly("galena_ore");
        assertThat(new DrillSelection(selection.serialise()).serialise()).isEqualTo(selection.serialise());
    }
}
