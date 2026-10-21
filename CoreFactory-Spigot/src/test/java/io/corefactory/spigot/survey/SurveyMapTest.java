package io.corefactory.spigot.survey;

import io.corefactory.core.survey.ChunkSurvey;
import io.corefactory.core.survey.ChunkSurvey.Cell;
import io.corefactory.core.survey.ChunkSurvey.Survey;
import io.corefactory.spigot.survey.SurveyMap.Rect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SurveyMapTest {

    private static final int RED = 0xFF0000;
    private static final int GREEN = 0x00FF00;
    private static final int[] ORE_RGB = {RED, GREEN};

    /** Builds a survey whose columns are all empty, except the ones set. */
    private static final class Grid {

        private final List<Cell> cells = new ArrayList<>(Collections.nCopies(256, new Cell(ChunkSurvey.NONE, 0)));

        Grid set(int column, int row, int ore, int count) {
            cells.set(row * 16 + column, new Cell(ore, count));
            return this;
        }

        Survey build() {
            return new Survey(0, 0, cells, List.of(), 0);
        }
    }

    private static int pixel(int[] pixels, int x, int y) {
        return pixels[y * SurveyMap.SIZE + x];
    }

    @Test
    void eachCellIsItsOwnEightPixelSquare() {
        assertThat(SurveyMap.cellRect(0, 0)).isEqualTo(new Rect(0, 0, 8));
        assertThat(SurveyMap.cellRect(3, 1)).isEqualTo(new Rect(24, 8, 8));
        assertThat(SurveyMap.cellRect(15, 15)).isEqualTo(new Rect(120, 120, 8));
    }

    @Test
    void theCellsCoverTheWholeMap() {
        int[] pixels = SurveyMap.paint(new Grid().build(), ORE_RGB);

        assertThat(pixels).hasSize(SurveyMap.SIZE * SurveyMap.SIZE);
        assertThat(pixels).containsOnly(SurveyMap.EMPTY_RGB);
    }

    @Test
    void aPixelInsideACellTakesThatCellsShade() {
        int[] pixels = SurveyMap.paint(new Grid().set(3, 1, 0, ChunkSurvey.SIZE * 4).build(), ORE_RGB);

        assertThat(pixel(pixels, 3 * 8 + 4, 1 * 8 + 4)).isEqualTo(RED);
    }

    @Test
    void aFullCountGivesTheFullColourAndNoBlocksTheDarkest() {
        assertThat(SurveyMap.shade(0xFFFFFF, SurveyMap.FULL_COUNT)).isEqualTo(0xFFFFFF);
        assertThat(SurveyMap.shade(0xFFFFFF, 500)).isEqualTo(0xFFFFFF);
        assertThat(SurveyMap.shade(0xFFFFFF, 0)).isEqualTo(0x676767);
        assertThat(SurveyMap.shade(0xFFFFFF, -5)).isEqualTo(SurveyMap.shade(0xFFFFFF, 0));
    }

    @Test
    void moreBlocksGiveABrighterCell() {
        int previous = -1;
        for (int count = 0; count <= SurveyMap.FULL_COUNT; count++) {
            int red = (SurveyMap.shade(RED, count) >> 16) & 0xFF;
            assertThat(red).isGreaterThanOrEqualTo(previous);
            previous = red;
        }
    }

    @Test
    void aBorderIsDrawnBetweenCellsOfDifferentOres() {
        int[] pixels = SurveyMap.paint(new Grid().set(2, 0, 0, 9).set(3, 0, 1, 5).build(), ORE_RGB);

        assertThat(pixel(pixels, 24, 4)).isEqualTo(SurveyMap.BORDER_RGB);
    }

    @Test
    void noBorderIsDrawnBetweenCellsOfTheSameOre() {
        int[] pixels = SurveyMap.paint(new Grid().set(1, 0, 0, 5).set(2, 0, 0, 9).build(), ORE_RGB);

        assertThat(pixel(pixels, 16, 4)).isEqualTo(SurveyMap.shade(RED, 9));
    }

    @Test
    void aBorderIsDrawnBetweenAnOreCellAndAnEmptyOne() {
        int[] pixels = SurveyMap.paint(new Grid().set(4, 0, 0, 3).build(), ORE_RGB);

        assertThat(pixel(pixels, 40, 4)).isEqualTo(SurveyMap.BORDER_RGB);
        assertThat(pixel(pixels, 32, 4)).isEqualTo(SurveyMap.BORDER_RGB);
    }

    @Test
    void noBorderIsDrawnBetweenTwoEmptyCells() {
        int[] pixels = SurveyMap.paint(new Grid().build(), ORE_RGB);

        assertThat(pixel(pixels, 8, 4)).isEqualTo(SurveyMap.EMPTY_RGB);
    }

    @Test
    void aBorderIsDrawnOnTheTopOfACellWhoseUpperNeighbourDiffers() {
        int[] pixels = SurveyMap.paint(new Grid().set(0, 1, 1, 4).build(), ORE_RGB);

        assertThat(pixel(pixels, 4, 8)).isEqualTo(SurveyMap.BORDER_RGB);
        assertThat(pixel(pixels, 4, 7)).isEqualTo(SurveyMap.EMPTY_RGB);
    }

    @Test
    void theMapIsCentredOnTheMiddleBlockOfTheChunk() {
        assertThat(SurveyMap.centre(0)).isEqualTo(8);
        assertThat(SurveyMap.centre(2)).isEqualTo(40);
        assertThat(SurveyMap.centre(-1)).isEqualTo(-8);
    }
}
