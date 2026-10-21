package io.corefactory.core.device;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FootprintTest {

    @Test
    void theGeneratorsExceptSolarAndTheDrillLevelsFourAndFiveAreTall() {
        assertThat(Footprint.TALL_IDS).containsExactlyInAnyOrder(
                "wind_generator", "charcoal_generator", "petroleum_generator", "diesel_generator", "nuclear_generator",
                "ore_drill_4", "ore_drill_5", "petroleum_drill_4", "petroleum_drill_5");
    }

    @Test
    void solarAndTheLowerDrillLevelsStandOneBlock() {
        assertThat(Footprint.heightOf("solar_panel")).isEqualTo(Footprint.SHORT);
        assertThat(Footprint.heightOf("ore_drill_1")).isEqualTo(Footprint.SHORT);
        assertThat(Footprint.heightOf("petroleum_drill_3")).isEqualTo(Footprint.SHORT);
        assertThat(Footprint.heightOf("pylon_2")).isEqualTo(Footprint.SHORT);
    }

    @Test
    void aTallDeviceStandsTwoBlocksAndNoIdIsTaller() {
        assertThat(Footprint.heightOf("nuclear_generator")).isEqualTo(Footprint.TALL);
        assertThat(Footprint.heightOf("ore_drill_5")).isEqualTo(Footprint.TALL);
        assertThat(Footprint.MAX_HEIGHT).isEqualTo(Footprint.TALL);
    }

    @Test
    void aMissingIdIsOneBlock() {
        assertThat(Footprint.heightOf(null)).isEqualTo(Footprint.SHORT);
    }

    @Test
    void theAnchorAndTheBlockAboveItAreCoveredByATallDevice() {
        assertThat(Footprint.covers(Footprint.TALL, 0)).isTrue();
        assertThat(Footprint.covers(Footprint.TALL, 1)).isTrue();
    }

    @Test
    void theBlockTwoAboveATallDeviceIsNotCovered() {
        assertThat(Footprint.covers(Footprint.TALL, 2)).isFalse();
        assertThat(Footprint.covers(Footprint.TALL, -1)).isFalse();
    }

    @Test
    void aShortDeviceCoversOnlyItsOwnBlock() {
        assertThat(Footprint.covers(Footprint.SHORT, 0)).isTrue();
        assertThat(Footprint.covers(Footprint.SHORT, 1)).isFalse();
    }

    @Test
    void theTallIdsAreImmutable() {
        assertThatThrownBy(() -> Footprint.TALL_IDS.add("solar_panel"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
