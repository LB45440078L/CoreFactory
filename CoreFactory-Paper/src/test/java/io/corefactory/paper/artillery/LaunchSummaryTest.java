package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.FiringVerdict;
import io.corefactory.core.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the launch summary's counts and lines for given shot results, without a server. */
class LaunchSummaryTest {

    private static final String ELEVATION = ArtilleryVerdicts.message(FiringVerdict.ELEVATION_TOO_LOW);
    private static final String RANGE = ArtilleryVerdicts.message(FiringVerdict.OUT_OF_RANGE);

    @Test
    void allFiredCountsEveryPieceAndHasOnlyTheSummaryLine() {
        LaunchSummary.Summary summary = LaunchSummary.of(List.of(
                attempt("Artillery at 1, 64, 2", ArtilleryService.ShotResult.accepted()),
                attempt("Artillery at 5, 64, 2", ArtilleryService.ShotResult.accepted()),
                attempt("Artillery at 9, 64, 2", ArtilleryService.ShotResult.accepted())));

        assertThat(summary.fired()).isEqualTo(3);
        assertThat(summary.refused()).isZero();
        assertThat(summary.lines()).containsExactly("<accent>Launch:</accent> <good>3 fired</good>, <muted>0 refused</muted>");
    }

    @Test
    void aRefusedPieceGetsItsReasonAfterTheSummaryLine() {
        LaunchSummary.Summary summary = LaunchSummary.of(List.of(
                attempt("Artillery at 1, 64, 2", ArtilleryService.ShotResult.accepted()),
                attempt("Artillery at 5, 64, 2", ArtilleryService.ShotResult.refused(ELEVATION)),
                attempt("Artillery at 9, 64, 2", ArtilleryService.ShotResult.refused(RANGE))));

        assertThat(summary.fired()).isEqualTo(1);
        assertThat(summary.refused()).isEqualTo(2);
        assertThat(summary.lines()).containsExactly(
                "<accent>Launch:</accent> <good>1 fired</good>, <bad>2 refused</bad>",
                "<muted>Artillery at 5, 64, 2:</muted> " + ELEVATION,
                "<muted>Artillery at 9, 64, 2:</muted> " + RANGE);
    }

    @Test
    void nothingFiredShowsZeroFiredInMuted() {
        LaunchSummary.Summary summary = LaunchSummary.of(List.of(
                attempt("Artillery at 1, 64, 2", ArtilleryService.ShotResult.refused(ELEVATION))));

        assertThat(summary.fired()).isZero();
        assertThat(summary.lines().get(0)).isEqualTo("<accent>Launch:</accent> <muted>0 fired</muted>, <bad>1 refused</bad>");
    }

    @Test
    void aPieceNameIsShownAsTyped() {
        LaunchSummary.Summary summary = LaunchSummary.of(List.of(
                attempt("<red>Gun</red> at 1, 64, 2", ArtilleryService.ShotResult.refused(ELEVATION))));

        assertThat(Text.plain(Text.parse(summary.lines().get(1)))).startsWith("<red>Gun</red> at 1, 64, 2: ");
    }

    private static LaunchSummary.Attempt attempt(String piece, ArtilleryService.ShotResult result) {
        return new LaunchSummary.Attempt(piece, result);
    }
}
