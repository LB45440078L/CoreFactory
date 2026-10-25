package io.corefactory.core.artillery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArtilleryTableTest {

    private final ArtilleryTable table = new ArtilleryTable(ArtillerySettings.defaults());

    @Test
    void theEndLevelsHaveTheirConfiguredRanges() {
        // Level I: 1500 x 6^0 = 1500. Level VII: 1500 x 6^1 = 9000.
        assertThat(table.rangeBlocks(ArtilleryLevel.I, ChargeTier.TIER_I)).isCloseTo(1500.0, within(1e-9));
        assertThat(table.rangeBlocks(ArtilleryLevel.VII, ChargeTier.TIER_I)).isCloseTo(9000.0, within(1e-9));
    }

    @Test
    void aMiddleLevelRangeIsTheGeometricStep() {
        // Level III: 1500 x 6^(2/6) = 1500 x 6^(1/3) = 1500 x 1.817121 = 2725.68.
        assertThat(table.rangeBlocks(ArtilleryLevel.III, ChargeTier.TIER_I)).isCloseTo(2725.68, within(0.01));
        // Level II: 1500 x 6^(1/6) = 2022.01, about 1.35 times level I.
        assertThat(table.rangeBlocks(ArtilleryLevel.II, ChargeTier.TIER_I)).isCloseTo(2022.01, within(0.01));
    }

    @Test
    void theTierTwoChargeMultipliesTheRangeByOneAndAQuarter() {
        // Level VII with tier II: 9000 x 1.25 = 11250.
        assertThat(table.rangeBlocks(ArtilleryLevel.VII, ChargeTier.TIER_II)).isCloseTo(11250.0, within(1e-9));
        // Level I with tier III: 1500 x 1.6 = 2400.
        assertThat(table.rangeBlocks(ArtilleryLevel.I, ChargeTier.TIER_III)).isCloseTo(2400.0, within(1e-9));
    }

    @Test
    void theReloadIsInStepsFromTwentyToSixtySeconds() {
        assertThat(table.reloadTicks(ArtilleryLevel.I, ChargeTier.TIER_I)).isEqualTo(400);
        assertThat(table.reloadTicks(ArtilleryLevel.VII, ChargeTier.TIER_I)).isEqualTo(1200);
        // Level III: 400 + 800 x (2/6) = 666.67, rounded to 667.
        assertThat(table.reloadTicks(ArtilleryLevel.III, ChargeTier.TIER_I)).isEqualTo(667);
    }

    @Test
    void theChargeMultipliesTheReload() {
        // Level I with tier II: 400 x 1.25 = 500. With tier III: 400 x 1.5 = 600.
        assertThat(table.reloadTicks(ArtilleryLevel.I, ChargeTier.TIER_II)).isEqualTo(500);
        assertThat(table.reloadTicks(ArtilleryLevel.I, ChargeTier.TIER_III)).isEqualTo(600);
        // Level VII with tier III: 1200 x 1.5 = 1800.
        assertThat(table.reloadTicks(ArtilleryLevel.VII, ChargeTier.TIER_III)).isEqualTo(1800);
    }

    @Test
    void theScatterAt1000BlocksFallsFromSixtyToEightBlocks() {
        assertThat(table.scatterSigmaAt1000Blocks(ArtilleryLevel.I, ChargeTier.TIER_I)).isCloseTo(60.0, within(1e-9));
        assertThat(table.scatterSigmaAt1000Blocks(ArtilleryLevel.VII, ChargeTier.TIER_I)).isCloseTo(8.0, within(1e-9));
        // Level II: 60 x (8/60)^(1/6) = 42.885.
        assertThat(table.scatterSigmaAt1000Blocks(ArtilleryLevel.II, ChargeTier.TIER_I)).isCloseTo(42.885, within(0.001));
        // Level III: 60 x (8/60)^(2/6) = 30.652.
        assertThat(table.scatterSigmaAt1000Blocks(ArtilleryLevel.III, ChargeTier.TIER_I)).isCloseTo(30.652, within(0.001));
    }

    @Test
    void theTierMultipliesTheScatter() {
        // Level VII with tier II: 8 x 1.2 = 9.6. Level I with tier III: 60 x 1.5 = 90.
        assertThat(table.scatterSigmaAt1000Blocks(ArtilleryLevel.VII, ChargeTier.TIER_II)).isCloseTo(9.6, within(1e-9));
        assertThat(table.scatterSigmaAt1000Blocks(ArtilleryLevel.I, ChargeTier.TIER_III)).isCloseTo(90.0, within(1e-9));
    }

    @Test
    void theScatterOfAShotIsLinearInItsRange() {
        // Level I, tier I, at 1000 blocks: 60. At 500 blocks: 60 x 500 / 1000 = 30.
        assertThat(table.scatterSigmaBlocks(ArtilleryLevel.I, ChargeTier.TIER_I, 1000.0)).isCloseTo(60.0, within(1e-9));
        assertThat(table.scatterSigmaBlocks(ArtilleryLevel.I, ChargeTier.TIER_I, 500.0)).isCloseTo(30.0, within(1e-9));
        // Level VII, tier I, at 9000 blocks: 8 x 9 = 72.
        assertThat(table.scatterSigmaBlocks(ArtilleryLevel.VII, ChargeTier.TIER_I, 9000.0)).isCloseTo(72.0, within(1e-9));
    }
}
