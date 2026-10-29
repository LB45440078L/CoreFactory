package io.corefactory.core.geology;

import io.corefactory.core.config.ConfigValues;
import io.corefactory.core.config.TestConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OreDefinitionTest {

    private static final String GOOD = """
            ores:
              good_ore:
                name: "&aGood"
                host: SMOOTH_BASALT
                drop: raw_good
                drop-amount: [2, 4]
                replace: [STONE, DEEPSLATE]
                min-y: -48
                max-y: 24
                volume: 400
              bad_range:
                host: SMOOTH_BASALT
                drop: raw_bad
                drop-amount: [2, 4]
                replace: [STONE]
                min-y: 24
                max-y: -48
              unknown_block:
                host: NOT_A_BLOCK
                drop: raw_x
                drop-amount: [1, 1]
                replace: [STONE]
                min-y: 0
                max-y: 10
              other_good:
                host: MUD_BRICKS
                drop: raw_other
                drop-amount: [1, 3]
                replace: [GRANITE]
                min-y: 0
                max-y: 80
                veins-per-chunk: 0.5
            """;

    private static final Logger LOG = Logger.getLogger("ore-test");

    @Test
    void parseKeepsFileOrderAndSkipsBadEntries() {
        Map<String, Object> yaml = TestConfig.yaml(GOOD);

        List<OreDefinition> ores = OreDefinition.parse(ConfigValues.optionalSection(yaml, "ores"), LOG);

        assertThat(ores).extracting(OreDefinition::id).containsExactly("good_ore", "other_good");
        OreDefinition good = ores.get(0);
        assertThat(good.name()).isEqualTo("&aGood");
        assertThat(good.host()).isEqualTo("SMOOTH_BASALT");
        assertThat(good.replace()).containsExactlyInAnyOrder("STONE", "DEEPSLATE");
        assertThat(good.dropMin()).isEqualTo(2);
        assertThat(good.dropMax()).isEqualTo(4);
        assertThat(good.minY()).isEqualTo(-48);
        assertThat(good.maxY()).isEqualTo(24);
        assertThat(good.volume()).isEqualTo(400);
        assertThat(ores.get(1).volume()).isZero();
    }

    @Test
    void aWeightIsReadAndAnOrdinaryOreHasTheOrdinaryWeight() {
        String weighted = """
            ores:
              rare_ore:
                host: SMOOTH_BASALT
                drop: raw_rare
                drop-amount: [1, 1]
                replace: [STONE]
                min-y: 0
                max-y: 10
                weight: 1
              plain_ore:
                host: SMOOTH_BASALT
                drop: raw_plain
                drop-amount: [1, 1]
                replace: [STONE]
                min-y: 0
                max-y: 10
              zero_ore:
                host: SMOOTH_BASALT
                drop: raw_zero
                drop-amount: [1, 1]
                replace: [STONE]
                min-y: 0
                max-y: 10
                weight: 0
            """;
        List<OreDefinition> ores = OreDefinition.parse(ConfigValues.optionalSection(TestConfig.yaml(weighted), "ores"), LOG);
        assertThat(ores).extracting(OreDefinition::id).containsExactly("rare_ore", "plain_ore");
        assertThat(ores.get(0).weight()).isEqualTo(1);
        assertThat(ores.get(1).weight()).isEqualTo(OreDefinition.ORDINARY_WEIGHT);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, Set.of("STONE"), 0, 10, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseOfAMissingSectionLoadsNothing() {
        assertThat(OreDefinition.parse(null, LOG)).isEmpty();
    }

    @Test
    void aFluidOreWithAVolumeNeedsNoDropAndYieldsNoItem() {
        String fluid = """
                ores:
                  crude:
                    host: WHITE_GLAZED_TERRACOTTA
                    replace: [STONE]
                    min-y: -64
                    max-y: 0
                    volume: 400
                """;
        Map<String, Object> yaml = TestConfig.yaml(fluid);

        List<OreDefinition> ores = OreDefinition.parse(ConfigValues.optionalSection(yaml, "ores"), LOG);

        assertThat(ores).hasSize(1);
        assertThat(ores.get(0).fluid()).isTrue();
        assertThat(ores.get(0).drop()).isNull();
        assertThat(ores.get(0).volume()).isEqualTo(400);
    }

    @Test
    void aCountOreWithNoDropIsRefused() {
        assertThatThrownBy(() -> new OreDefinition("ore_y", "", "STONE", null, 1, 1, Set.of("STONE"), 0, 10))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("has no drop");
    }

    @Test
    void nameDefaultsToTheId() {
        OreDefinition ore = new OreDefinition("ore_x", "", "STONE", "raw", 1, 1, Set.of("STONE"), 0, 10);

        assertThat(ore.name()).isEqualTo("ore_x");
    }

    @Test
    void constructorChecksTheRanges() {
        Set<String> stone = Set.of("STONE");

        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, stone, 10, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 3, 2, stone, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, stone, -65, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, stone, 0, 960))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, stone, 0, 10, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, Set.of(), 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
