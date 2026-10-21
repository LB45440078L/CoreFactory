package io.corefactory.spigot.energy;

import io.corefactory.core.energy.GridPalette;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class GridNamesTest {

    @Test
    void thereAreSixteenDistinctNamesOneForEachColour() {
        List<String> names = IntStream.range(0, GridPalette.SIZE).mapToObj(GridNames::colourName).toList();
        assertThat(names).hasSize(GridPalette.SIZE).doesNotHaveDuplicates();
    }

    @Test
    void theNameRepeatsEverySixteenGrids() {
        assertThat(GridNames.colourName(16)).isEqualTo(GridNames.colourName(0));
        assertThat(GridNames.colourName(37)).isEqualTo(GridNames.colourName(5));
    }

    @Test
    void aNegativeIdGivesTheSameNameAsItsPositionInTheCycle() {
        assertThat(GridNames.colourName(-1)).isEqualTo(GridNames.colourName(15));
        assertThat(GridNames.colourName(-16)).isEqualTo(GridNames.colourName(0));
    }

    @Test
    void theFirstGridIsRed() {
        assertThat(GridNames.colourName(0)).isEqualTo("red");
    }
}
