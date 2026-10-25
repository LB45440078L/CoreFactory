package io.corefactory.core.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigValuesTest {

    private static final Map<String, Object> CONFIG = TestConfig.yaml("""
            oil:
              tube-capacity-litres: 250
              pipe-flow-litres-per-second: 2.5
            names: [stone, "deepslate", 7]
            flag: true
            label: text
            wrong:
              count: many
            """);

    @Test
    void aNestedPathWalksTheSections() {
        assertThat(ConfigValues.number(CONFIG, "oil.pipe-flow-litres-per-second", 0.0)).isEqualTo(2.5);
        assertThat(ConfigValues.integer(CONFIG, "oil.tube-capacity-litres", 0)).isEqualTo(250);
    }

    @Test
    void aMissingKeyGivesTheDefault() {
        assertThat(ConfigValues.integer(CONFIG, "oil.absent", 42)).isEqualTo(42);
        assertThat(ConfigValues.integer(CONFIG, "absent.deeper", 42)).isEqualTo(42);
        assertThat(ConfigValues.bool(CONFIG, "absent", true)).isTrue();
        assertThat(ConfigValues.string(CONFIG, "absent", "fallback")).isEqualTo("fallback");
        assertThat(ConfigValues.stringList(CONFIG, "absent", List.of("x"))).containsExactly("x");
    }

    @Test
    void aMissingConfigGivesEveryDefault() {
        assertThat(ConfigValues.integer(null, "oil.tube-capacity-litres", 7)).isEqualTo(7);
        assertThat(ConfigValues.section(null, "oil")).isEmpty();
    }

    @Test
    void aWrongTypedValueGivesTheDefault() {
        assertThat(ConfigValues.integer(CONFIG, "wrong.count", 3)).isEqualTo(3);
        assertThat(ConfigValues.number(CONFIG, "label", 1.5)).isEqualTo(1.5);
        assertThat(ConfigValues.bool(CONFIG, "label", false)).isFalse();
        assertThat(ConfigValues.stringList(CONFIG, "label", List.of("d"))).containsExactly("d");
        assertThat(ConfigValues.optionalSection(CONFIG, "label")).isNull();
    }

    @Test
    void anIntegerIsReadAsANumberAndAsAnInt() {
        assertThat(ConfigValues.number(CONFIG, "oil.tube-capacity-litres", 0.0)).isEqualTo(250.0);
        assertThat(ConfigValues.integer(CONFIG, "oil.tube-capacity-litres", 0)).isEqualTo(250);
        assertThat(ConfigValues.longValue(CONFIG, "oil.tube-capacity-litres", 0L)).isEqualTo(250L);
        assertThat(ConfigValues.isInteger(CONFIG, "oil.tube-capacity-litres")).isTrue();
        assertThat(ConfigValues.isInteger(CONFIG, "oil.pipe-flow-litres-per-second")).isFalse();
    }

    @Test
    void aListIsReadAsAStringList() {
        assertThat(ConfigValues.stringList(CONFIG, "names", List.of())).containsExactly("stone", "deepslate", "7");
    }

    @Test
    void aSectionOrAnEmptyMapIsReturned() {
        assertThat(ConfigValues.section(CONFIG, "oil")).containsEntry("tube-capacity-litres", 250);
        assertThat(ConfigValues.section(CONFIG, "absent")).isEmpty();
        assertThat(ConfigValues.optionalSection(CONFIG, "absent")).isNull();
    }

    @Test
    void aKeyThatHoldsADotIsFoundWhole() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "a.b", 1);
        config.put("c.d", 2);

        assertThat(ConfigValues.integer(config, "c.d", 0)).isEqualTo(2);
        assertThat(ConfigValues.integer(config, "a.b", 0)).isEqualTo(1);
    }

    @Test
    void aMapListKeepsTheSectionsOnly() {
        Map<String, Object> config = TestConfig.yaml("""
                outputs:
                  - item: a
                    amount: 2
                  - just-text
                """);

        assertThat(ConfigValues.mapList(config, "outputs")).hasSize(1);
        assertThat(ConfigValues.mapList(config, "outputs").get(0)).containsEntry("item", "a");
    }
}
