package io.corefactory.paper.logistics;

import io.corefactory.core.logistics.LogisticsLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogisticsTextTest {

    @Test
    void aPickerStatusSaysWhatItIsDoing() {
        assertThat(LogisticsText.pickerStatus(true, false, 2.0)).isEqualTo("&amoving 2.0/s");
        assertThat(LogisticsText.pickerStatus(false, true, 2.0)).isEqualTo("&cwaiting: output full");
        assertThat(LogisticsText.pickerStatus(false, false, 2.0)).isEqualTo("&7waiting: no item");
    }

    @Test
    void aBlockedPickerSaysOutputFullEvenIfItMovedRecently() {
        assertThat(LogisticsText.pickerStatus(true, true, 1.0)).isEqualTo("&cwaiting: output full");
    }

    @Test
    void aSplitterStatusIsFlowingBlockedOrIdle() {
        assertThat(LogisticsText.splitterStatus(true, false)).isEqualTo("&aflowing");
        assertThat(LogisticsText.splitterStatus(true, true)).isEqualTo("&cblocked");
        assertThat(LogisticsText.splitterStatus(false, false)).isEqualTo("&7idle");
    }

    @Test
    void thePickerLinesNameTheLevel() {
        assertThat(LogisticsText.pickerLines(LogisticsLevel.II, 200.0, "&7idle"))
                .containsExactly("&6Picker II  &7200 W", "&7idle");
    }

    @Test
    void theSplitterLinesCountBothLists() {
        assertThat(LogisticsText.splitterLines(LogisticsLevel.III, 1_500.0, 2, 5, "&aflowing"))
                .containsExactly("&6Splitter III  &71.5 kW", "&7Left: &f2 &7Right: &f5", "&aflowing");
    }

    @Test
    void aBeltNameIsItsKindAndRoman() {
        assertThat(LogisticsText.name(LogisticsIds.Kind.BELT, LogisticsLevel.I)).isEqualTo("Belt I");
    }
}
