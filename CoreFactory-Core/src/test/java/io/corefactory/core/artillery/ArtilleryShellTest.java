package io.corefactory.core.artillery;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ArtilleryShellTest {

    @Test
    void theNuclearShellsAreExactlyTheFourWarheads() {
        assertThat(Arrays.stream(ArtilleryShell.values()).filter(ArtilleryShell::nuclear))
                .containsExactly(ArtilleryShell.ENRICHED_URANIUM, ArtilleryShell.HYDROGEN_THERMONUCLEAR,
                        ArtilleryShell.CLUSTER_ENRICHED_URANIUM, ArtilleryShell.CONDENSED_ANTIMATTER);
    }

    @Test
    void theTierAndLevelOfTheConventionalShellsAreTheLowest() {
        assertThat(ArtilleryShell.EXPLOSIVE.requiredTier()).isEqualTo(ChargeTier.TIER_I);
        assertThat(ArtilleryShell.EXPLOSIVE.minimumLevel()).isEqualTo(ArtilleryLevel.I);
        assertThat(ArtilleryShell.WHITE_PHOSPHORUS.minimumLevel()).isEqualTo(ArtilleryLevel.I);
        assertThat(ArtilleryShell.CLUSTER.requiredTier()).isEqualTo(ChargeTier.TIER_II);
        assertThat(ArtilleryShell.CLUSTER.minimumLevel()).isEqualTo(ArtilleryLevel.II);
    }

    @Test
    void theUraniumAndHydrogenShellsNeedLevelSixAndTheHeavierShellsTierThree() {
        assertThat(ArtilleryShell.ENRICHED_URANIUM.minimumLevel()).isEqualTo(ArtilleryLevel.VI);
        assertThat(ArtilleryShell.ENRICHED_URANIUM.requiredTier()).isEqualTo(ChargeTier.TIER_II);
        assertThat(ArtilleryShell.HYDROGEN_THERMONUCLEAR.minimumLevel()).isEqualTo(ArtilleryLevel.VI);
        assertThat(ArtilleryShell.HYDROGEN_THERMONUCLEAR.requiredTier()).isEqualTo(ChargeTier.TIER_III);
        assertThat(ArtilleryShell.CLUSTER_ENRICHED_URANIUM.minimumLevel()).isEqualTo(ArtilleryLevel.VII);
        assertThat(ArtilleryShell.CLUSTER_ENRICHED_URANIUM.requiredTier()).isEqualTo(ChargeTier.TIER_III);
        assertThat(ArtilleryShell.CONDENSED_ANTIMATTER.minimumLevel()).isEqualTo(ArtilleryLevel.VII);
        assertThat(ArtilleryShell.CONDENSED_ANTIMATTER.requiredTier()).isEqualTo(ChargeTier.TIER_III);
    }

    @Test
    void theExplosiveAndNapalmShellsDetonateOnContactAndTheRestBurstInTheAir() {
        assertThat(ArtilleryShell.EXPLOSIVE.detonation()).isEqualTo(ArtilleryShell.Detonation.CONTACT);
        assertThat(ArtilleryShell.HIGH_EXPLOSIVE.detonation()).isEqualTo(ArtilleryShell.Detonation.CONTACT);
        assertThat(ArtilleryShell.NAPALM.detonation()).isEqualTo(ArtilleryShell.Detonation.CONTACT);
        assertThat(ArtilleryShell.WHITE_PHOSPHORUS.detonation()).isEqualTo(ArtilleryShell.Detonation.AIR_BURST);
        assertThat(ArtilleryShell.CLUSTER.detonation()).isEqualTo(ArtilleryShell.Detonation.AIR_BURST);
        assertThat(ArtilleryShell.CONDENSED_ANTIMATTER.detonation()).isEqualTo(ArtilleryShell.Detonation.AIR_BURST);
    }

    @Test
    void theEffectOfEachShellIsItsOwn() {
        assertThat(ArtilleryShell.WHITE_PHOSPHORUS.effect()).isEqualTo(ArtilleryShell.Effect.SMOKE);
        assertThat(ArtilleryShell.NAPALM.effect()).isEqualTo(ArtilleryShell.Effect.FIRE);
        assertThat(ArtilleryShell.CLUSTER.effect()).isEqualTo(ArtilleryShell.Effect.SUBMUNITIONS);
        assertThat(ArtilleryShell.CLUSTER_ENRICHED_URANIUM.effect()).isEqualTo(ArtilleryShell.Effect.SUBMUNITIONS);
        assertThat(ArtilleryShell.EXPLOSIVE.effect()).isEqualTo(ArtilleryShell.Effect.BLAST);
    }

    @Test
    void aShellsKeyIsItsNameInLowerCaseWithDashes() {
        assertThat(ArtilleryShell.CLUSTER_ENRICHED_URANIUM.key()).isEqualTo("cluster-enriched-uranium");
        assertThat(ArtilleryShell.HIGH_EXPLOSIVE.key()).isEqualTo("high-explosive");
    }
}
