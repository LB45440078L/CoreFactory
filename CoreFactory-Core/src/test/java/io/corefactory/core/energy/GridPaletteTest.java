package io.corefactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GridPaletteTest {

    @Test
    void thePaletteHasSixteenDistinctColours() {
        Set<Integer> colours = new HashSet<>();
        for (int id = 0; id < GridPalette.SIZE; id++) {
            colours.add(GridPalette.colour(id));
        }
        assertThat(GridPalette.SIZE).isEqualTo(16);
        assertThat(colours).hasSize(16);
    }

    @Test
    void everyColourIsAPlainRgbValue() {
        for (int id = 0; id < 64; id++) {
            assertThat(GridPalette.colour(id)).isBetween(0, 0xFFFFFF);
        }
    }

    @Test
    void theColourOfAnIdRepeatsEverySixteenIds() {
        for (int id = 0; id < 100; id++) {
            assertThat(GridPalette.colour(id + GridPalette.SIZE)).isEqualTo(GridPalette.colour(id));
        }
    }

    @Test
    void theColourIsStableForAnId() {
        assertThat(GridPalette.colour(5)).isEqualTo(GridPalette.colour(5));
        assertThat(GridPalette.colour(21)).isEqualTo(GridPalette.colour(5));
    }
}
