package io.corefactory.core.survey;

import io.corefactory.core.geology.GeologyRegistry;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.geology.Placement;
import io.corefactory.core.survey.ChunkSurvey.Cell;
import io.corefactory.core.survey.ChunkSurvey.OreTotal;
import io.corefactory.core.survey.ChunkSurvey.Survey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkSurveyTest {

    private static OreDefinition ore(String id) {
        return new OreDefinition(id, id, "SMOOTH_BASALT", "raw_" + id, 1, 2, Set.of("STONE"), -64, 320, 1.0, 1, 1);
    }

    private static final List<OreDefinition> ORES = List.of(ore("first"), ore("second"), ore("third"));

    private static final int CHUNK_X = 0;
    private static final int CHUNK_Z = 0;

    private static Survey survey(List<Placement> placements) {
        return ChunkSurvey.survey(CHUNK_X, CHUNK_Z, placements, ORES);
    }

    @Test
    void theOreWithMostBlocksInAColumnDominatesIt() {
        Survey survey = survey(List.of(
                new Placement(2, 10, 3, 0),
                new Placement(2, 11, 3, 1),
                new Placement(2, 12, 3, 1),
                new Placement(2, 13, 3, 1)));

        assertThat(survey.cell(2, 3)).isEqualTo(new Cell(1, 3));
    }

    @Test
    void theCountIsTheDominantOresBlocksInThatColumnOnly() {
        Survey survey = survey(List.of(
                new Placement(5, 0, 5, 2),
                new Placement(5, 1, 5, 2),
                new Placement(6, 0, 5, 0),
                new Placement(5, 0, 6, 0)));

        assertThat(survey.cell(5, 5)).isEqualTo(new Cell(2, 2));
        assertThat(survey.cell(6, 5)).isEqualTo(new Cell(0, 1));
        assertThat(survey.cell(5, 6)).isEqualTo(new Cell(0, 1));
    }

    @Test
    void aTieGoesToTheOreEarlierInFileOrder() {
        Survey survey = survey(List.of(
                new Placement(1, 0, 1, 2),
                new Placement(1, 1, 1, 2),
                new Placement(1, 2, 1, 0),
                new Placement(1, 3, 1, 0)));

        assertThat(survey.cell(1, 1)).isEqualTo(new Cell(0, 2));
    }

    @Test
    void aTieIsBrokenByFileOrderNotByPlacementOrder() {
        Survey survey = survey(List.of(
                new Placement(4, 0, 4, 1),
                new Placement(4, 1, 4, 0)));

        assertThat(survey.cell(4, 4)).isEqualTo(new Cell(0, 1));
    }

    @Test
    void anEmptyChunkHasEveryColumnEmpty() {
        Survey survey = survey(List.of());

        assertThat(survey.cells()).hasSize(256).containsOnly(new Cell(ChunkSurvey.NONE, 0));
        assertThat(survey.totals()).isEmpty();
        assertThat(survey.placements()).isZero();
    }

    @Test
    void aColumnWithNoPlannedBlockIsEmpty() {
        Survey survey = survey(List.of(new Placement(0, 7, 15, 1)));

        assertThat(survey.cell(0, 15)).isEqualTo(new Cell(1, 1));
        assertThat(survey.cell(0, 14)).isEqualTo(new Cell(ChunkSurvey.NONE, 0));
        assertThat(survey.cell(1, 15)).isEqualTo(new Cell(ChunkSurvey.NONE, 0));
    }

    @Test
    void theCellsAreInRowMajorOrderOfZThenX() {
        Survey survey = survey(List.of(new Placement(9, 0, 2, 0)));

        assertThat(survey.cells().get(2 * 16 + 9)).isEqualTo(new Cell(0, 1));
    }

    @Test
    void totalsGiveThePlannedCountAndDepthRangeOfEachOre() {
        Survey survey = survey(List.of(
                new Placement(0, -10, 0, 0),
                new Placement(3, 40, 9, 0),
                new Placement(7, 5, 2, 0),
                new Placement(8, 12, 2, 2)));

        assertThat(survey.totals()).containsExactly(
                new OreTotal(0, 3, -10, 40),
                new OreTotal(2, 1, 12, 12));
        assertThat(survey.placements()).isEqualTo(4);
    }

    @Test
    void oresWithNoPlannedBlockAreLeftOutOfTheTotals() {
        Survey survey = survey(List.of(new Placement(0, 0, 0, 1)));

        assertThat(survey.totals()).containsExactly(new OreTotal(1, 1, 0, 0));
    }

    @Test
    void localCoordinatesFollowTheChunkForNegativeChunks() {
        Survey survey = ChunkSurvey.survey(-1, 2, List.of(new Placement(-1, 3, 39, 0)), ORES);

        assertThat(survey.cell(15, 7)).isEqualTo(new Cell(0, 1));
        assertThat(survey.totals()).containsExactly(new OreTotal(0, 1, 3, 3));
    }

    @Test
    void aPlacementOutsideTheChunkIsRejected() {
        assertThatThrownBy(() -> survey(List.of(new Placement(16, 0, 0, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPlacementNamingNoOreIsRejected() {
        assertThatThrownBy(() -> survey(List.of(new Placement(0, 0, 0, 3))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stillInGroundCountsThePlannedBlocksThatAreRegistered() {
        List<Placement> planned = List.of(
                new Placement(1, 10, 1, 0),
                new Placement(2, 10, 1, 0),
                new Placement(3, 10, 1, 1));
        int[] registry = GeologyRegistry.encode(List.of(
                new Placement(1, 10, 1, 0),
                new Placement(3, 10, 1, 1)));

        assertThat(ChunkSurvey.stillInGround(planned, registry, CHUNK_X, CHUNK_Z))
                .containsExactlyInAnyOrderEntriesOf(Map.of(0, 1, 1, 1));
    }

    @Test
    void aMinedBlockIsNoLongerCountedAsStillInTheGround() {
        List<Placement> planned = List.of(
                new Placement(1, 10, 1, 0),
                new Placement(2, 10, 1, 0));
        int[] registry = GeologyRegistry.encode(List.of(new Placement(2, 10, 1, 0)));

        assertThat(ChunkSurvey.stillInGround(planned, registry, CHUNK_X, CHUNK_Z))
                .containsExactlyEntriesOf(Map.of(0, 1));
    }

    @Test
    void aRegisteredBlockOfAnotherOreIsNotCountedForThePlannedOne() {
        List<Placement> planned = List.of(new Placement(1, 10, 1, 0));
        int[] registry = GeologyRegistry.encode(List.of(new Placement(1, 10, 1, 2)));

        assertThat(ChunkSurvey.stillInGround(planned, registry, CHUNK_X, CHUNK_Z)).isEmpty();
    }

    @Test
    void aChunkWithNoRegistryHasNothingStillInTheGround() {
        assertThat(ChunkSurvey.stillInGround(List.of(new Placement(1, 10, 1, 0)), null, CHUNK_X, CHUNK_Z)).isEmpty();
    }
}
