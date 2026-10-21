package io.corefactory.spigot.energy;

import io.corefactory.core.energy.ChartMode;
import io.corefactory.spigot.energy.ChartRules.Verdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChartRulesTest {

    private static final int PER_GRID = 1;
    private static final int TOTAL = 5;

    @Test
    void aNameIsOneToTwentyFourLettersDigitsOrUnderscores() {
        assertThat(ChartRules.validName("a")).isTrue();
        assertThat(ChartRules.validName("Grid_01")).isTrue();
        assertThat(ChartRules.validName("A".repeat(24))).isTrue();
    }

    @Test
    void aNameOutsideTheRulesIsRefused() {
        assertThat(ChartRules.validName(null)).isFalse();
        assertThat(ChartRules.validName("")).isFalse();
        assertThat(ChartRules.validName("A".repeat(25))).isFalse();
        assertThat(ChartRules.validName("two words")).isFalse();
        assertThat(ChartRules.validName("dash-name")).isFalse();
        assertThat(ChartRules.validName("café")).isFalse();
    }

    @Test
    void aGridWithAChartIsFullWhateverTheServerHolds() {
        assertThat(ChartRules.canCreate(1, 0, PER_GRID, TOTAL)).isEqualTo(Verdict.GRID_FULL);
        assertThat(ChartRules.canCreate(1, 5, PER_GRID, TOTAL)).isEqualTo(Verdict.GRID_FULL);
    }

    @Test
    void aServerWithFiveChartsIsFullForAGridWithNone() {
        assertThat(ChartRules.canCreate(0, 5, PER_GRID, TOTAL)).isEqualTo(Verdict.SERVER_FULL);
        assertThat(ChartRules.canCreate(0, 6, PER_GRID, TOTAL)).isEqualTo(Verdict.SERVER_FULL);
    }

    @Test
    void aChartIsAllowedOnAnEmptyGridWhileTheServerHasRoom() {
        assertThat(ChartRules.canCreate(0, 0, PER_GRID, TOTAL)).isEqualTo(Verdict.OK);
        assertThat(ChartRules.canCreate(0, 4, PER_GRID, TOTAL)).isEqualTo(Verdict.OK);
    }

    @Test
    void theGridLimitIsCheckedBeforeTheServerLimit() {
        assertThat(ChartRules.canCreate(1, 4, PER_GRID, TOTAL)).isEqualTo(Verdict.GRID_FULL);
    }

    @Test
    void theModeCyclesSatisfactionProductionStorage() {
        assertThat(ChartRules.next(ChartMode.SATISFACTION)).isEqualTo(ChartMode.PRODUCTION);
        assertThat(ChartRules.next(ChartMode.PRODUCTION)).isEqualTo(ChartMode.STORAGE);
        assertThat(ChartRules.next(ChartMode.STORAGE)).isEqualTo(ChartMode.SATISFACTION);
    }

    @Test
    void threeStepsReturnToTheStart() {
        for (ChartMode start : ChartMode.values()) {
            ChartMode mode = start;
            for (int i = 0; i < 3; i++) {
                mode = ChartRules.next(mode);
            }
            assertThat(mode).isEqualTo(start);
        }
    }

    @Test
    void aModeWordParsesInAnyCase() {
        assertThat(ChartRules.parseMode("satisfaction")).contains(ChartMode.SATISFACTION);
        assertThat(ChartRules.parseMode("PRODUCTION")).contains(ChartMode.PRODUCTION);
        assertThat(ChartRules.parseMode("Storage")).contains(ChartMode.STORAGE);
        assertThat(ChartRules.parseMode("sToRaGe")).contains(ChartMode.STORAGE);
    }

    @Test
    void aBadModeWordParsesToNothing() {
        assertThat(ChartRules.parseMode("power")).isEmpty();
        assertThat(ChartRules.parseMode("")).isEmpty();
        assertThat(ChartRules.parseMode(null)).isEmpty();
    }

    @Test
    void theWordOfAModeIsItsLowerCaseName() {
        assertThat(ChartRules.word(ChartMode.SATISFACTION)).isEqualTo("satisfaction");
        assertThat(ChartRules.word(ChartMode.PRODUCTION)).isEqualTo("production");
        assertThat(ChartRules.word(ChartMode.STORAGE)).isEqualTo("storage");
    }
}
