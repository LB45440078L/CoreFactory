package io.corefactory.core.water;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class PumpLevelsTest {

    @Test
    void aKitNamesItsLevelFromTwoToFive() {
        assertThat(PumpLevels.kitLevel("water_pump_upgrade_2")).isEqualTo(OptionalInt.of(2));
        assertThat(PumpLevels.kitLevel("water_pump_upgrade_5")).isEqualTo(OptionalInt.of(5));
        assertThat(PumpLevels.kitLevel("water_pump_upgrade_6")).isEmpty();
        assertThat(PumpLevels.kitLevel("water_pump_upgrade_1")).isEmpty();
        assertThat(PumpLevels.kitLevel("water_pump_upgrade_x")).isEmpty();
        assertThat(PumpLevels.kitLevel("water_pump")).isEmpty();
        assertThat(PumpLevels.kitLevel(null)).isEmpty();
    }

    @Test
    void aKitTakesThePumpOneStepUp() {
        assertThat(PumpLevels.upgrade(1, 2)).isEqualTo(OptionalInt.of(2));
        assertThat(PumpLevels.upgrade(4, 5)).isEqualTo(OptionalInt.of(5));
    }

    @Test
    void aKitThatSkipsALevelOrRepeatsOneIsRefused() {
        assertThat(PumpLevels.upgrade(1, 3)).isEmpty();
        assertThat(PumpLevels.upgrade(2, 2)).isEmpty();
        assertThat(PumpLevels.upgrade(3, 2)).isEmpty();
        assertThat(PumpLevels.upgrade(5, 5)).isEmpty();
    }

    @Test
    void aRefusalNamesItsReason() {
        assertThat(PumpLevels.refusal(1, 3)).contains("Fit the level 2 kit first");
        assertThat(PumpLevels.refusal(2, 2)).isEqualTo("This pump is already level 2 or higher.");
        assertThat(PumpLevels.refusal(5, 5)).isEqualTo("This pump is already at its top level.");
    }

    @Test
    void theModelOfAPumpIsNamedByItsLevel() {
        assertThat(PumpLevels.modelKey(1)).isEqualTo("water_pump");
        assertThat(PumpLevels.modelKey(2)).isEqualTo("water_pump_2");
        assertThat(PumpLevels.modelKey(5)).isEqualTo("water_pump_5");
    }

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
}
