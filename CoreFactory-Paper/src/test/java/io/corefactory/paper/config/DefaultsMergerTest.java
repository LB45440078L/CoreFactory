package io.corefactory.paper.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultsMergerTest {

    private static YamlConfiguration yaml(String text) {
        return YamlConfiguration.loadConfiguration(new StringReader(text));
    }

    private static final String DEFAULTS = """
            tnts:
              nuke_davy_crockett:
                radius: 14
              exotic_antimatter_bomb:
                radius: 260
              four_dimensional_tnt:
                radius: 340
            """;

    @Test
    void missingEntriesAreAddedAndExistingOnesAreKept() {
        YamlConfiguration current = yaml("""
                tnts:
                  nuke_davy_crockett:
                    radius: 14
                  four_dimensional_tnt:
                    radius: 10
                """);

        DefaultsMerger.Result result = DefaultsMerger.mergeInto(yaml(DEFAULTS), current, List.of("tnts"));

        assertThat(result.added()).containsExactly("exotic_antimatter_bomb");
        assertThat(current.getInt("tnts.exotic_antimatter_bomb.radius")).isEqualTo(260);
        assertThat(current.getInt("tnts.four_dimensional_tnt.radius")).isEqualTo(10);
        assertThat(result.differing()).containsExactly("four_dimensional_tnt");
    }

    @Test
    void aCompleteFileGainsNothing() {
        YamlConfiguration current = yaml(DEFAULTS);

        DefaultsMerger.Result result = DefaultsMerger.mergeInto(yaml(DEFAULTS), current, List.of("tnts"));

        assertThat(result.added()).isEmpty();
        assertThat(result.differing()).isEmpty();
    }

    @Test
    void nestedEntriesThatMatchTheDefaultsAreNotReportedAsDifferent() {
        String nested = """
                components:
                  hazmat_fabric:
                    name: "&eHazmat Fabric"
                    recipe:
                      type: shapeless
                      ingredients: [material:LEATHER, material:LEAD]
                """;

        DefaultsMerger.Result result = DefaultsMerger.mergeInto(yaml(nested), yaml(nested), List.of("components"));

        assertThat(result.added()).isEmpty();
        assertThat(result.differing()).isEmpty();
    }

    @Test
    void aNestedEntryThatChangedIsReported() {
        YamlConfiguration current = yaml("""
                components:
                  hazmat_fabric:
                    name: "&eHazmat Fabric"
                    recipe:
                      type: shapeless
                      ingredients: [material:LEATHER]
                """);
        String shipped = """
                components:
                  hazmat_fabric:
                    name: "&eHazmat Fabric"
                    recipe:
                      type: shapeless
                      ingredients: [material:LEATHER, material:LEAD]
                """;

        DefaultsMerger.Result result = DefaultsMerger.mergeInto(yaml(shipped), current, List.of("components"));

        assertThat(result.differing()).containsExactly("hazmat_fabric");
    }

    @Test
    void onlyTheRequestedSectionsAreMerged() {
        YamlConfiguration defaults = yaml("""
                tnts:
                  nuke_davy_crockett:
                    radius: 14
                ores:
                  uraninite_ore:
                    host: BLACK_GLAZED_TERRACOTTA
                """);
        YamlConfiguration current = yaml("tnts: {}\n");

        DefaultsMerger.Result result = DefaultsMerger.mergeInto(defaults, current, List.of("tnts"));

        assertThat(result.added()).containsExactly("nuke_davy_crockett");
        assertThat(current.contains("ores.uraninite_ore")).isFalse();
    }

    @Test
    void aMissingSectionInTheFileIsCreated() {
        YamlConfiguration current = yaml("settings: {}\n");

        DefaultsMerger.Result result = DefaultsMerger.mergeInto(yaml(DEFAULTS), current, List.of("tnts"));

        assertThat(result.added()).hasSize(3);
        assertThat(current.contains("tnts.nuke_davy_crockett")).isTrue();
    }
}
