package io.corefactory.core.oil;

import io.corefactory.core.config.TestConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OilSettingsTest {

    @Test
    void theDefaultsAreAHundredLitresPerTubeAndFourLitresPerSecond() {
        assertThat(OilSettings.defaults().tubeCapacityLitres()).isEqualTo(100.0);
        assertThat(OilSettings.defaults().pipeFlowLitresPerSecond()).isEqualTo(4.0);
    }

    @Test
    void noSectionReadsAsTheDefaults() {
        assertThat(OilSettings.from(null)).isEqualTo(OilSettings.defaults());
        assertThat(OilSettings.from(Map.of())).isEqualTo(OilSettings.defaults());
    }

    @Test
    void theCapacityAndTheFlowAreReadFromTheSection() {
        Map<String, Object> config = TestConfig.yaml(
                "oil:\n  tube-capacity-litres: 250\n  pipe-flow-litres-per-second: 2.5\n");

        assertThat(OilSettings.from(config)).isEqualTo(new OilSettings(250.0, 2.5));
    }

    @Test
    void aNegativeFlowTakesTheDefault() {
        Map<String, Object> config = TestConfig.yaml("oil:\n  pipe-flow-litres-per-second: -3\n");

        assertThat(OilSettings.from(config)).isEqualTo(OilSettings.defaults());
    }

    @Test
    void aZeroOrNegativeCapacityTakesTheDefault() {
        Map<String, Object> zero = TestConfig.yaml("oil:\n  tube-capacity-litres: 0\n");
        Map<String, Object> negative = TestConfig.yaml("oil:\n  tube-capacity-litres: -5\n");

        assertThat(OilSettings.from(zero).tubeCapacityLitres()).isEqualTo(OilSettings.DEFAULT_TUBE_CAPACITY_LITRES);
        assertThat(OilSettings.from(negative).tubeCapacityLitres()).isEqualTo(OilSettings.DEFAULT_TUBE_CAPACITY_LITRES);
    }

    @Test
    void theRecordRefusesANonPositiveCapacityAndANegativeOrNaNFlow() {
        assertThatThrownBy(() -> new OilSettings(0.0, 4.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OilSettings(Double.NaN, 4.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OilSettings(100.0, -1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OilSettings(100.0, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }
}
