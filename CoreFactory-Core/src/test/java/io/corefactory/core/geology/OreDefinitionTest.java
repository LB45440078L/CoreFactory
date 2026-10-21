package io.corefactory.core.geology;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
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
                veins-per-chunk: 1
                vein-size: [5, 8]
              bad_range:
                host: SMOOTH_BASALT
                drop: raw_bad
                drop-amount: [2, 4]
                replace: [STONE]
                min-y: 24
                max-y: -48
                veins-per-chunk: 1.0
                vein-size: [5, 8]
              unknown_block:
                host: NOT_A_BLOCK
                drop: raw_x
                drop-amount: [1, 1]
                replace: [STONE]
                min-y: 0
                max-y: 10
                veins-per-chunk: 1.0
                vein-size: [1, 2]
              other_good:
                host: MUD_BRICKS
                drop: raw_other
                drop-amount: [1, 3]
                replace: [GRANITE]
                min-y: 0
                max-y: 80
                veins-per-chunk: 0.5
                vein-size: [3, 6]
            """;

    private static final Logger LOG = Logger.getLogger("ore-test");

    @Test
    void parseKeepsFileOrderAndSkipsBadEntries() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(GOOD));

        List<OreDefinition> ores = OreDefinition.parse(yaml.getConfigurationSection("ores"), LOG);

        assertThat(ores).extracting(OreDefinition::id).containsExactly("good_ore", "other_good");
        OreDefinition good = ores.get(0);
        assertThat(good.name()).isEqualTo("&aGood");
        assertThat(good.host()).isEqualTo("SMOOTH_BASALT");
        assertThat(good.replace()).containsExactlyInAnyOrder("STONE", "DEEPSLATE");
        assertThat(good.dropMin()).isEqualTo(2);
        assertThat(good.dropMax()).isEqualTo(4);
        assertThat(good.veinsPerChunk()).isEqualTo(1.0);
        assertThat(good.veinSizeMin()).isEqualTo(5);
        assertThat(good.veinSizeMax()).isEqualTo(8);
    }

    @Test
    void parseOfAMissingSectionLoadsNothing() {
        assertThat(OreDefinition.parse(null, LOG)).isEmpty();
    }

    @Test
    void nameDefaultsToTheId() {
        OreDefinition ore = new OreDefinition("ore_x", "", "STONE", "raw", 1, 1, Set.of("STONE"), 0, 10, 1.0, 1, 2);

        assertThat(ore.name()).isEqualTo("ore_x");
    }

    @Test
    void constructorChecksTheRanges() {
        Set<String> stone = Set.of("STONE");

        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, stone, 10, 10, 1.0, 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 3, 2, stone, 0, 10, 1.0, 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, stone, 0, 10, 1.0, 0, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, stone, 0, 10, 1.0, 5, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, stone, -65, 10, 1.0, 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, stone, 0, 960, 1.0, 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, stone, 0, 10, -0.5, 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreDefinition("o", "o", "STONE", "r", 1, 1, Set.of(), 0, 10, 1.0, 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
