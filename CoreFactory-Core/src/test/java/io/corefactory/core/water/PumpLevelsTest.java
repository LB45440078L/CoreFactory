package io.corefactory.core.water;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class PumpLevelsTest {


    @Test
    void onlyTheHighLevelsDrawPower() {
        assertThat(PumpLevels.powered(2)).isFalse();
        assertThat(PumpLevels.powered(3)).isTrue();
        assertThat(PumpLevels.powered(5)).isTrue();
    }

    @Test
    void theWaterPartsNameTheirComponents() {
        assertThat(WaterPart.ofComponent("water_pump")).contains(WaterPart.PUMP);
        assertThat(WaterPart.ofComponent("water_splitter_x")).contains(WaterPart.CROSS);
        assertThat(WaterPart.ofComponent("water_pump_upgrade_2")).isEmpty();
        assertThat(WaterPart.TEE.splitter()).isTrue();
        assertThat(WaterPart.PIPE.splitter()).isFalse();
        assertThat(WaterPart.PUMP.kind()).isEqualTo(WaterKind.PUMP);
    }

    @Test
    void levelsFourAndFiveStandTwoBlocksTall() {
        assertThat(PumpLevels.height(1)).isEqualTo(1);
        assertThat(PumpLevels.height(3)).isEqualTo(1);
        assertThat(PumpLevels.height(4)).isEqualTo(2);
        assertThat(PumpLevels.height(5)).isEqualTo(2);
    }
}
