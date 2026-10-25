package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.paper.ShippedContent;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks that the piece items map to their levels, and that nothing else does. */
class ArtilleryIdsTest {

    @Test
    void eachLevelHasItsItemAndTheItemGivesBackTheLevel() {
        for (ArtilleryLevel level : ArtilleryLevel.values()) {
            String item = ArtilleryIds.itemOf(level);
            assertThat(item).isEqualTo("artillery_" + level.number());
            assertThat(ArtilleryIds.levelOfItem(item)).contains(level);
            assertThat(ArtilleryIds.isItem(item)).isTrue();
        }
    }

    @Test
    void eachShellAndChargeHasItsComponentId() {
        assertThat(ArtilleryIds.shellItemOf(ArtilleryShell.EXPLOSIVE)).isEqualTo("explosive_shell");
        assertThat(ArtilleryIds.shellItemOf(ArtilleryShell.CLUSTER_ENRICHED_URANIUM)).isEqualTo("cluster_enriched_uranium_shell");
        assertThat(ArtilleryIds.shellItemOf(ArtilleryShell.CONDENSED_ANTIMATTER)).isEqualTo("condensed_antimatter_shell");
        assertThat(ArtilleryIds.chargeItemOf(ChargeTier.TIER_I)).isEqualTo("tier1_explosive_charge");
        assertThat(ArtilleryIds.chargeItemOf(ChargeTier.TIER_III)).isEqualTo("tier3_explosive_charge");
    }

    @Test
    void theShellAndChargeItemsAreDefinedInMaterials() {
        Set<String> components = ShippedContent.load().definitions().components().keySet();
        for (ArtilleryShell shell : ArtilleryShell.values()) {
            assertThat(components).contains(ArtilleryIds.shellItemOf(shell));
        }
        for (ChargeTier tier : ChargeTier.values()) {
            assertThat(components).contains(ArtilleryIds.chargeItemOf(tier));
        }
    }

    @Test
    void otherIdsAreNoPiece() {
        assertThat(ArtilleryIds.levelOfItem("artillery_8")).isEmpty();
        assertThat(ArtilleryIds.levelOfItem("artillery_0")).isEmpty();
        assertThat(ArtilleryIds.levelOfItem("artillery_01")).isEmpty();
        assertThat(ArtilleryIds.levelOfItem("artillery_")).isEmpty();
        assertThat(ArtilleryIds.levelOfItem("machine_gun_turret")).isEmpty();
        assertThat(ArtilleryIds.levelOfItem("explosive_shell")).isEmpty();
        assertThat(ArtilleryIds.levelOfItem(null)).isEmpty();
        assertThat(ArtilleryIds.isItem("tier1_explosive_charge")).isFalse();
    }
}
