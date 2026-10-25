package io.corefactory.paper.command;

import io.corefactory.paper.ShippedContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The close-id rule of the unknown-id message, with small id lists and with the ids the shipped YAML defines.
 */
class IdSuggestionsTest {

    private static List<String> recipeIds;
    private static List<String> giveIds;

    @BeforeAll
    static void load() {
        ShippedContent.Loaded content = ShippedContent.load();
        giveIds = Completion.giveIds(content.definitions().tnts().keySet(), content.definitions().components().keySet(),
                content.ores().stream().map(ore -> ore.id()).toList());
        recipeIds = Completion.recipeIds(content.definitions().tnts().keySet(), content.definitions().tntRecipes(),
                content.definitions().components(), content.processes().processes().stream()
                        .flatMap(process -> process.outputs().stream())
                        .map(product -> product.ref())
                        .toList());
    }

    @Test
    void railgunSuggestsRailgunTurret() {
        assertThat(IdSuggestions.closest("railgun", List.of("railgun_turret", "laser_turret", "pylon_1")))
                .containsExactly("railgun_turret");
    }

    @Test
    void railgunSuggestsRailgunTurretInTheShippedRecipeIds() {
        assertThat(IdSuggestions.closest("railgun", recipeIds)).contains("railgun_turret");
    }

    @Test
    void aDisplayNameFindsItsIdWithUnderscores() {
        assertThat(IdSuggestions.closest("Energy Wire", giveIds)).first().isEqualTo("energy_wire");
        assertThat(IdSuggestions.closest("energy-wire", giveIds)).first().isEqualTo("energy_wire");
    }

    @Test
    void theTypedTextIsReadWithoutCase() {
        assertThat(IdSuggestions.closest("RAILGUN", List.of("railgun_turret"))).containsExactly("railgun_turret");
    }

    @Test
    void aMistypedIdSharingItsFirstThreeLettersIsSuggested() {
        assertThat(IdSuggestions.closest("railgn", List.of("railgun_turret", "pylon_1"))).containsExactly("railgun_turret");
    }

    @Test
    void theRankingPutsPrefixMatchesFirstThenContainsThenSharedLetters() {
        List<String> ids = List.of("fail_rail", "rainbow", "rail_gun", "railgun_turret");

        assertThat(IdSuggestions.closest("rail", ids))
                .containsExactly("rail_gun", "railgun_turret", "fail_rail", "rainbow");
    }

    @Test
    void atMostFiveIdsAreListed() {
        List<String> ids = List.of("x1", "x2", "x3", "x4", "x5", "x6", "x7");

        assertThat(IdSuggestions.closest("x", ids)).hasSize(IdSuggestions.LIMIT).containsExactly("x1", "x2", "x3", "x4", "x5");
    }

    @Test
    void anIdListedTwiceIsSuggestedOnce() {
        assertThat(IdSuggestions.closest("pylon", List.of("pylon_1", "pylon_1", "pylon_2"))).containsExactly("pylon_1", "pylon_2");
    }

    @Test
    void nothingCloseGivesNoSuggestion() {
        assertThat(IdSuggestions.closest("zzz", List.of("railgun_turret", "pylon_1"))).isEmpty();
    }

    @Test
    void aBlankOrMissingTypedTextGivesNoSuggestion() {
        assertThat(IdSuggestions.closest("", giveIds)).isEmpty();
        assertThat(IdSuggestions.closest("   ", giveIds)).isEmpty();
        assertThat(IdSuggestions.closest(null, giveIds)).isEmpty();
    }
}
