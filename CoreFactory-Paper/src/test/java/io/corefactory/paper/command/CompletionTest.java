package io.corefactory.paper.command;

import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Product;
import io.corefactory.paper.ShippedContent;
import io.corefactory.paper.command.Completion.Candidate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The completion rules, with the ids the shipped YAML defines. Nothing here starts a server.
 */
class CompletionTest {

    private static final String GIVE = "corefactory.give";
    private static final String RECIPES = "corefactory.recipes";

    private static ShippedContent.Loaded content;
    private static List<String> giveIds;
    private static List<String> recipeIds;

    @BeforeAll
    static void load() {
        content = ShippedContent.load();
        Set<String> tntIds = content.definitions().tnts().keySet();
        List<String> ores = content.ores().stream().map(OreDefinition::id).toList();
        giveIds = Completion.giveIds(tntIds, content.definitions().components().keySet(), ores);
        recipeIds = Completion.recipeIds(tntIds, content.definitions().tntRecipes(), content.definitions().components(),
                processOutputs(content));
    }

    @Test
    void theTypedPrefixMatchesIgnoringCase() {
        List<String> offered = Completion.complete(Completion.all(giveIds, GIVE), "NUKE_B", anyone());

        assertThat(offered).containsExactly("nuke_b83");
    }

    @Test
    void aPrefixOfAComponentIdOffersEveryIdThatStartsWithIt() {
        List<String> offered = Completion.complete(Completion.all(giveIds, GIVE), "ore_drill_", anyone());

        assertThat(offered).containsExactly("ore_drill_1", "ore_drill_2", "ore_drill_3", "ore_drill_4", "ore_drill_5");
    }

    @Test
    void anEmptyPrefixOffersEveryIdInAlphabeticalOrder() {
        List<String> offered = Completion.complete(Completion.all(giveIds, GIVE), "", anyone());

        assertThat(offered).hasSameSizeAs(giveIds);
        assertThat(offered).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        assertThat(offered).contains("nuke_little_boy", "pylon_1", "uraninite_ore", "crude_petroleum_deposit");
    }

    @Test
    void aSuggestionListedTwiceIsOfferedOnce() {
        List<Candidate> twice = List.of(new Candidate("pylon_1", null), new Candidate("pylon_1", null));

        assertThat(Completion.complete(twice, "p", anyone())).containsExactly("pylon_1");
    }

    @Test
    void anIdNeedingAPermissionIsNotOfferedWithoutIt() {
        List<Candidate> ids = Completion.all(giveIds, GIVE);

        assertThat(Completion.complete(ids, "", permissions())).isEmpty();
        assertThat(Completion.complete(ids, "pylon_", permissions(GIVE))).containsExactly(
                "pylon_1", "pylon_2", "pylon_3", "pylon_4", "pylon_5");
    }

    @Test
    void aSuggestionWithNoPermissionIsOfferedToEveryone() {
        List<Candidate> mixed = List.of(new Candidate("reload", "corefactory.reload"), new Candidate("list", null));

        assertThat(Completion.complete(mixed, "", permissions())).containsExactly("list");
        assertThat(Completion.complete(mixed, "", permissions("corefactory.reload")))
                .containsExactly("list", "reload");
    }

    @Test
    void giveOffersEveryTntComponentAndOreBlock() {
        assertThat(giveIds).contains("nuke_little_boy", "petroleum_drill_1", "research_console", "crude_oil_refinery",
                "uraninite_ore", "energy_wire", "railgun_turret");
        assertThat(giveIds).hasSize(content.definitions().tnts().size()
                + content.definitions().components().size() + content.ores().size());
    }

    @Test
    void recipeOffersOnlyTheIdsThatHaveARecipe() {
        assertThat(recipeIds).contains("research_console", "particle_accelerator", "pylon_1", "nuke_little_boy", "energy_wire");
        assertThat(recipeIds).doesNotContain("raw_uraninite", "uraninite_ore", "crude_petroleum_deposit");
        assertThat(giveIds).containsAll(recipeIds);
    }

    @Test
    void recipeOffersTheItemsAProcessMakes() {
        assertThat(recipeIds).contains("railgun_turret", "laser_turret", "machine_gun_turret", "crushed_uraninite");
    }

    @Test
    void recipeOffersAProcessOutputOnlyWhenItIsAComponent() {
        Map<String, ComponentDefinition> pylon = Map.of("pylon_1", content.definitions().components().get("pylon_1"));
        List<ItemRef> outputs = List.of(ItemRef.material("IRON_INGOT"), ItemRef.item("no_such_part"), ItemRef.item("pylon_1"));

        assertThat(Completion.recipeIds(Set.of(), Map.of(), pylon, outputs)).containsExactly("pylon_1");
    }

    @Test
    void recipeOffersAProcessOutputOnceEvenWhenItHasACraftingRecipe() {
        List<ItemRef> outputs = List.of(ItemRef.item("pylon_1"), ItemRef.item("pylon_1"));

        assertThat(Completion.recipeIds(Set.of(), Map.of(), content.definitions().components(), outputs))
                .containsOnlyOnce("pylon_1");
    }

    @Test
    void recipeIsNotOfferedWithoutItsPermission() {
        List<Candidate> ids = Completion.all(recipeIds, RECIPES);

        assertThat(Completion.complete(ids, "", permissions())).isEmpty();
        assertThat(Completion.complete(ids, "research_", permissions(RECIPES))).contains("research_console");
    }

    @Test
    void theGrantableResearchNodesAreTheTreesNodes() {
        List<String> nodes = content.tree().nodes().stream().map(node -> node.id()).toList();
        List<String> offered = Completion.complete(Completion.all(nodes, "corefactory.research.admin"), "", anyone());

        assertThat(offered).hasSameSizeAs(nodes);
        assertThat(offered).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    void theOfferedRecipeIdsAreDistinct() {
        assertThat(recipeIds.stream().distinct().count()).isEqualTo(recipeIds.size());
    }

    /** The reference of every item a process makes, in the order of processes.yml. */
    private static List<ItemRef> processOutputs(ShippedContent.Loaded loaded) {
        return loaded.processes().processes().stream()
                .flatMap(process -> process.outputs().stream())
                .map(Product::ref)
                .toList();
    }

    private static Predicate<String> anyone() {
        return permission -> true;
    }

    private static Predicate<String> permissions(String... granted) {
        Set<String> has = Set.of(granted);
        return has::contains;
    }
}
