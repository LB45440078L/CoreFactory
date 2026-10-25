package io.corefactory.core.artillery;

import io.corefactory.core.config.TestConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class NuclearYieldsTest {

    @Test
    void theDefaultsAreTheLittleBoyAndTheDavyCrockettInTons() {
        NuclearYields y = NuclearYields.defaults();

        assertThat(y.littleBoyTons()).isCloseTo(15_000.0, within(1e-6));
        assertThat(y.davyCrockettTons()).isCloseTo(20.0, within(1e-9));
    }

    @Test
    void theYieldsAreReadInMegatonsAndConvertedToTons() {
        Map<String, Object> tnts = Map.of("tnts", Map.of(
                "nuke_little_boy", Map.of("settings", Map.of("yield-megatons", 0.25)),
                "nuke_davy_crockett", Map.of("settings", Map.of("yield-megatons", 0.001))));
        NuclearYields y = NuclearYields.from(tnts);

        assertThat(y.littleBoyTons()).isCloseTo(250_000.0, within(1e-6));
        assertThat(y.davyCrockettTons()).isCloseTo(1_000.0, within(1e-6));
    }

    @Test
    void aMissingOrBadEntryTakesTheShippedValue() {
        Map<String, Object> tnts = Map.of("tnts", Map.of(
                "nuke_little_boy", Map.of("settings", Map.of("yield-megatons", -2)),
                "nuke_davy_crockett", Map.of("settings", Map.of("yield-megatons", "twenty"))));
        NuclearYields y = NuclearYields.from(tnts);

        assertThat(y).isEqualTo(NuclearYields.defaults());
        assertThat(NuclearYields.from(Map.of())).isEqualTo(NuclearYields.defaults());
        assertThat(NuclearYields.from(null)).isEqualTo(NuclearYields.defaults());
    }

    @Test
    void theShippedTntsYmlGivesTheDefaults() throws IOException {
        Map<String, Object> tnts = TestConfig.yaml(
                Files.readString(Path.of("..", "CoreFactory-Paper", "src", "main", "resources", "tnts.yml")));

        assertThat(NuclearYields.from(tnts)).isEqualTo(NuclearYields.defaults());
    }
}
