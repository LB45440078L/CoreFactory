package io.corefactory.core.deposit;

import io.corefactory.core.geology.OreDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PatchBlocksTest {

    private static final long ID = 0x5EED_0042L;
    private static final long OTHER_ID = 0x5EED_0043L;
    private static final String HOST = "BLACK_GLAZED_TERRACOTTA";
    private static final OreDefinition GALENA = new OreDefinition("galena_ore", "Galena", HOST, "raw_galena", 2, 5,
            Set.of("STONE"), -32, 40);
    private static final Deposit DEPOSIT = new Deposit(ID, "world", 0, 0, 5, "galena_ore", 1000, 1000);

    @Test
    void aBlockInsideTheCircleOfTheHostInTheBandIsASite() {
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, 0, 0, 0, 1.0)).isTrue();
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, -3, 10, 4, 1.0)).isTrue();
    }

    @Test
    void theEdgeOfTheCircleAndTheEdgesOfTheBandAreSites() {
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, 5, 0, 0, 1.0)).isTrue();
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, 0, -32, 0, 1.0)).isTrue();
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, 0, 40, 0, 1.0)).isTrue();
    }

    @Test
    void aBlockOutsideTheCircleIsNotASite() {
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, 6, 0, 0, 1.0)).isFalse();
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, 4, 0, 4, 1.0)).isFalse();
    }

    @Test
    void aBlockBelowOrAboveTheBandIsNotASite() {
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, 0, -33, 0, 1.0)).isFalse();
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, 0, 41, 0, 1.0)).isFalse();
    }

    @Test
    void aBlockOfAnotherHostIsNotASite() {
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, "STONE", 0, 0, 0, 1.0)).isFalse();
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, "YELLOW_GLAZED_TERRACOTTA", 0, 0, 0, 1.0)).isFalse();
    }

    @Test
    void aDepositOfAnotherOreIsNotASiteForThatOre() {
        Deposit cinnabar = new Deposit(ID, "world", 0, 0, 5, "cinnabar_ore", 1000, 1000);
        OreDefinition otherOre = new OreDefinition("cinnabar_ore", "Cinnabar", HOST, "raw_cinnabar", 2, 4,
                Set.of("STONE"), -32, 40);

        assertThat(PatchBlocks.isSite(cinnabar, GALENA, HOST, 0, 0, 0, 1.0)).isFalse();
        assertThat(PatchBlocks.isSite(cinnabar, otherOre, HOST, 0, 0, 0, 1.0)).isTrue();
    }

    @Test
    void aBlockTheDensityDoesNotShowIsNotASite() {
        Integer hidden = null;
        for (int x = -5; x <= 5 && hidden == null; x++) {
            if (!OreBlocks.shows(ID, x, 0, 0, 0.3)) {
                hidden = x;
            }
        }
        assertThat(hidden).isNotNull();

        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, hidden, 0, 0, 0.3)).isFalse();
        assertThat(PatchBlocks.isSite(DEPOSIT, GALENA, HOST, 0, 0, 0, 0.0)).isFalse();
    }

    @Test
    void theFirstDepositThatCoversTheBlockIsTheOneItBelongsTo() {
        Deposit first = new Deposit(ID, "world", 0, 0, 5, "galena_ore", 1000, 1000);
        Deposit second = new Deposit(OTHER_ID, "world", 3, 0, 5, "galena_ore", 1000, 1000);
        Map<String, OreDefinition> ores = Map.of("galena_ore", GALENA);

        Optional<Deposit> found = PatchBlocks.siteOf(List.of(first, second), ores::get, HOST, 4, 0, 0, 1.0);

        assertThat(found).contains(first);
    }

    @Test
    void aBlockOnlyTheSecondDepositCoversBelongsToTheSecond() {
        Deposit first = new Deposit(ID, "world", 0, 0, 5, "galena_ore", 1000, 1000);
        Deposit second = new Deposit(OTHER_ID, "world", 12, 0, 5, "galena_ore", 1000, 1000);
        Map<String, OreDefinition> ores = Map.of("galena_ore", GALENA);

        Optional<Deposit> found = PatchBlocks.siteOf(List.of(first, second), ores::get, HOST, 11, 0, 0, 1.0);

        assertThat(found).contains(second);
    }

    @Test
    void aDepositWithAnUnknownOreNeverMatches() {
        Map<String, OreDefinition> none = Map.of();

        assertThat(PatchBlocks.siteOf(List.of(DEPOSIT), none::get, HOST, 0, 0, 0, 1.0)).isEmpty();
    }

    @Test
    void noSiteWhenNothingCoversTheBlock() {
        Map<String, OreDefinition> ores = Map.of("galena_ore", GALENA);

        assertThat(PatchBlocks.siteOf(List.of(DEPOSIT), ores::get, HOST, 50, 0, 50, 1.0)).isEmpty();
        assertThat(PatchBlocks.siteOf(List.of(), ores::get, HOST, 0, 0, 0, 1.0)).isEmpty();
    }
}
