package io.corefactory.core.water;

import io.corefactory.core.config.TestConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WaterSettingsTest {

    @Test
    void theDefaultsAreTheDocumentedValues() {
        WaterSettings defaults = WaterSettings.defaults();

        assertThat(defaults.sourceMinBlocks()).isEqualTo(100);
        assertThat(defaults.sourceCheckTicks()).isEqualTo(200);
        assertThat(defaults.solveTicks()).isEqualTo(5);
        assertThat(defaults.unitPerProcessRun()).isEqualTo(1);
        assertThat(defaults.pumpFlow()).containsExactly(2.0, 4.0, 8.0, 12.0, 16.0);
        assertThat(defaults.pumpPowerW()).containsExactly(0, 0, 60000, 120000, 240000);
    }

    @Test
    void noSectionReadsAsTheDefaults() {
        assertThat(WaterSettings.from(null)).isEqualTo(WaterSettings.defaults());
        assertThat(WaterSettings.from(Map.of())).isEqualTo(WaterSettings.defaults());
    }

    @Test
    void aGivenKeyOverridesItsDefaultAndTheRestStay() {
        Map<String, Object> config = TestConfig.yaml("""
                water:
                  source-min-blocks: 50
                  solve-ticks: 2
                """);

        WaterSettings settings = WaterSettings.from(config);

        assertThat(settings.sourceMinBlocks()).isEqualTo(50);
        assertThat(settings.solveTicks()).isEqualTo(2);
        assertThat(settings.sourceCheckTicks()).isEqualTo(200);
        assertThat(settings.pumpFlow()).isEqualTo(WaterSettings.defaults().pumpFlow());
    }

    @Test
    void aListOfTheWrongLengthTakesTheDefaults() {
        Map<String, Object> config = TestConfig.yaml("""
                water:
                  pump-flow: [1, 2, 3]
                  pump-power-w: [0, -5, 0, 0, 0]
                """);

        WaterSettings settings = WaterSettings.from(config);

        assertThat(settings.pumpFlow()).isEqualTo(WaterSettings.defaults().pumpFlow());
        assertThat(settings.pumpPowerW()).isEqualTo(WaterSettings.defaults().pumpPowerW());
    }

    @Test
    void theFlowTableIsIndexedFromLevelOne() {
        assertThat(WaterSettings.defaults().flowTable()).containsExactly(2.0, 4.0, 8.0, 12.0, 16.0);
    }

    @Test
    void aRecordRefusesAValueOutOfRange() {
        assertThatCtor(() -> new WaterSettings(0, 200, 5, List.of(1.0, 1.0, 1.0, 1.0, 1.0), List.of(0, 0, 0, 0, 0), 1));
        assertThatCtor(() -> new WaterSettings(100, 200, 5, List.of(1.0, 1.0, 1.0, 1.0, -1.0), List.of(0, 0, 0, 0, 0), 1));
    }

    private static void assertThatCtor(Runnable construction) {
        org.assertj.core.api.Assertions.assertThatThrownBy(construction::run).isInstanceOf(IllegalArgumentException.class);
    }
}
