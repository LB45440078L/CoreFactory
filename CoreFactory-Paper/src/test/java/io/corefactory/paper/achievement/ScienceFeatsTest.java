package io.corefactory.paper.achievement;

import io.corefactory.api.TntDefinition;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.paper.ShippedContent;
import io.corefactory.paper.achievement.Achievement.Category;
import io.corefactory.paper.achievement.Achievement.Trigger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ScienceFeatsTest {

    private static List<Achievement> feats;
    private static ShippedContent.Loaded data;

    @BeforeAll
    static void build() {
        data = ShippedContent.load();
        Map<String, String> names = new HashMap<>();
        for (ComponentDefinition component : data.definitions().components().values()) {
            names.put(component.id(), component.displayName());
        }
        for (TntDefinition tnt : data.definitions().tnts().values()) {
            names.put(tnt.id(), tnt.displayName());
        }
        feats = ScienceFeats.build(new ScienceFeats.Sources(data.tree(), data.processes().machines(), data.ores(),
                data.definitions().tnts().values(), names, id -> null));
    }

    @Test
    void everyScienceFeatHasAUniqueId() {
        List<String> ids = feats.stream().map(Achievement::id).toList();

        assertThat(ids).doesNotHaveDuplicates();
        assertThat(ids).allSatisfy(id -> assertThat(id).matches("[a-z0-9_]+"));
    }

    @Test
    void everyScienceFeatIsInTheScienceCategoryAndHasATitleAndDescription() {
        assertThat(feats).allSatisfy(feat -> {
            assertThat(feat.category()).isEqualTo(Category.SCIENCE);
            assertThat(feat.title()).isNotBlank();
            assertThat(feat.description()).isNotBlank().doesNotContain("&");
            assertThat(feat.goal()).isPositive();
        });
    }

    @Test
    void theRequiredFeatsArePresent() {
        assertThat(ids()).contains("mine_ore", "crush_ore", "first_research_pack", "make_yellowcake",
                "make_uf6_cylinder", "make_enriched_uranium", "make_u233_oxide", "make_plutonium_oxide",
                "make_deuterium_cell", "place_machine", "place_every_machine", "make_antimatter_condensate",
                "blast_exotic_neutron_bomb", "blast_exotic_antimatter_bomb", "blast_four_dimensional_tnt");
    }

    @Test
    void eachResearchNodeHasOneFeatNamingIt() {
        List<Achievement> research = feats.stream().filter(feat -> feat.trigger() == Trigger.RESEARCH).toList();

        assertThat(research).hasSize(data.tree().nodes().size());
        for (ResearchTree.Node node : data.tree().nodes()) {
            assertThat(research).filteredOn(feat -> feat.param().equals(node.id())).hasSize(1);
        }
    }

    @Test
    void theEveryMachineFeatCountsEveryKindOfMachine() {
        Achievement every = feats.stream().filter(feat -> feat.id().equals("place_every_machine")).findFirst()
                .orElseThrow();

        assertThat(every.trigger()).isEqualTo(Trigger.PLACE_EACH);
        assertThat(every.goal()).isEqualTo(data.processes().machines().size());
    }

    @Test
    void everyTriggerAFeatUsesIsHookedByAGameEvent() {
        assertThat(feats).allSatisfy(feat -> assertThat(AchievementTracker.HOOKED).contains(feat.trigger()));
    }

    @Test
    void everyParameterNamesARealMachineItemOrTntType() {
        Set<String> machines = data.processes().machines().stream().map(machine -> machine.id())
                .collect(Collectors.toSet());
        Set<String> components = data.definitions().components().keySet();
        Set<String> tnts = data.definitions().tnts().keySet();
        for (Achievement feat : feats) {
            switch (feat.trigger()) {
                case PRODUCE -> assertThat(machines).as(feat.id()).contains(feat.param());
                case MAKE -> assertThat(components).as(feat.id()).contains(feat.param());
                case DETONATE -> assertThat(tnts).as(feat.id()).contains(feat.param());
                default -> {
                }
            }
        }
    }

    private List<String> ids() {
        return feats.stream().map(Achievement::id).toList();
    }

}
