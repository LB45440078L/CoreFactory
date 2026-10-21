package io.corefactory.spigot.lock;

import io.corefactory.api.TntDefinition;
import io.corefactory.core.mechanics.NuclearTnts;
import io.corefactory.spigot.ShippedContent;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The nuclear family, read from the shipped tnts.yml: the ten nuke tiers, the neutron bomb, the antimatter bomb and the
 * four-dimensional TNT. Nothing here is a list in the code; the expected ids below are the shipped content.
 */
class NuclearTntsTest {

    private static final Set<String> NUCLEAR = Set.of(
            "nuke_little_boy", "nuke_fat_man", "nuke_davy_crockett", "nuke_joe_1", "nuke_ivy_king",
            "nuke_b83", "nuke_ivy_mike", "nuke_castle_bravo", "nuke_mk17", "nuke_tsar_bomba",
            "exotic_neutron_bomb", "exotic_antimatter_bomb", "four_dimensional_tnt");

    @Test
    void theNuclearSetIsTheTypesWhoseMechanicIsNuclearOrNeutron() {
        Map<String, TntDefinition> tnts = ShippedContent.load().definitions().tnts();

        assertThat(tnts.keySet()).containsAll(NUCLEAR);
        assertThat(NuclearTnts.idsOf(tnts.values())).containsExactlyInAnyOrderElementsOf(NUCLEAR);
    }

    @Test
    void everyNuclearTypeNamesTheNuclearOrNeutronMechanic() {
        Map<String, TntDefinition> tnts = ShippedContent.load().definitions().tnts();

        for (String id : NUCLEAR) {
            assertThat(tnts.get(id).mechanic()).as(id).isIn("nuclear", "neutron");
            assertThat(NuclearTnts.isNuclear(tnts.get(id))).as(id).isTrue();
        }
    }

    @Test
    void theConventionalTypesAreNotNuclear() {
        Map<String, TntDefinition> tnts = ShippedContent.load().definitions().tnts();

        assertThat(tnts.get("chain_reaction_tnt").mechanic()).isEqualTo("chain_reaction");
        assertThat(NuclearTnts.isNuclear(tnts.get("chain_reaction_tnt"))).isFalse();
        assertThat(NuclearTnts.idsOf(tnts.values())).doesNotContain("chain_reaction_tnt");
    }
}
