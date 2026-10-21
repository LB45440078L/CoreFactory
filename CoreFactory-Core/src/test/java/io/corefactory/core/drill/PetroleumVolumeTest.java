package io.corefactory.core.drill;

import io.corefactory.core.geology.DepositVolumes;
import io.corefactory.core.geology.OreDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetroleumVolumeTest {

    @Test
    void aConfiguredVolumeIsUsedAndAMissingOneFallsBackToTheDefault() {
        assertThat(PetroleumVolume.volumeOf(400)).isEqualTo(400);
        assertThat(PetroleumVolume.volumeOf(250)).isEqualTo(250);
        assertThat(PetroleumVolume.volumeOf(0)).isEqualTo(PetroleumVolume.DEFAULT_VOLUME).isEqualTo(400);
        assertThat(PetroleumVolume.volumeOf(-5)).isEqualTo(400);
    }

    @Test
    void eachPumpTakesOneUnitAndZeroDepletesTheDeposit() {
        int volume = 3;
        volume = PetroleumVolume.afterPump(volume);
        assertThat(volume).isEqualTo(2);
        assertThat(PetroleumVolume.depleted(volume)).isFalse();

        volume = PetroleumVolume.afterPump(volume);
        volume = PetroleumVolume.afterPump(volume);
        assertThat(volume).isZero();
        assertThat(PetroleumVolume.depleted(volume)).isTrue();
        assertThat(PetroleumVolume.afterPump(volume)).isZero();
    }

    @Test
    void aDepositHoldsItsFullVolumeUntilItIsPumped() {
        int full = PetroleumVolume.volumeOf(0);
        int[] volumes = null;
        assertThat(DepositVolumes.get(volumes, 4, -20, 4, full)).isEqualTo(400);

        volumes = DepositVolumes.put(volumes, 4, -20, 4, PetroleumVolume.afterPump(full));
        assertThat(DepositVolumes.get(volumes, 4, -20, 4, full)).isEqualTo(399);
        assertThat(DepositVolumes.get(volumes, 5, -20, 4, full)).isEqualTo(400);
    }

    @Test
    void theVolumeOfAPumpedDepositSurvivesAnEncodeAndDecodeOfTheChunkData() {
        int[] volumes = DepositVolumes.put(new int[0], 1, -10, 1, 17);
        int[] copy = volumes.clone();

        assertThat(DepositVolumes.get(copy, 1, -10, 1, 400)).isEqualTo(17);
    }

    @Test
    void theDefaultVolumeIsReadFromTheOreDefinitionWhenGiven() {
        OreDefinition deposit = new OreDefinition("crude_petroleum_deposit", "", "WHITE_GLAZED_TERRACOTTA", "petroleum",
                1, 2, java.util.Set.of("STONE"), -64, 0, 0.3, 3, 6, 400);

        assertThat(PetroleumVolume.volumeOf(deposit.volume())).isEqualTo(400);
        assertThat(PetroleumVolume.volumeOf(new OreDefinition("crude_petroleum_deposit", "", "STONE", "petroleum", 1, 2,
                java.util.Set.of("STONE"), -64, 0, 0.3, 3, 6).volume())).isEqualTo(400);
    }

    @Test
    void aNegativeVolumeIsRefused() {
        assertThatThrownBy(() -> DepositVolumes.put(null, 0, 0, 0, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
