package io.corefactory.core.artillery;

import io.corefactory.core.config.TestConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CommandCenterSettingsTest {

    @Test
    void theDefaultsAreTheValuesOfConfigYml() {
        CommandCenterSettings s = CommandCenterSettings.defaults();

        assertThat(s.listenTimeoutSeconds()).isEqualTo(20);
        assertThat(s.mapRadiusBlocks()).isCloseTo(64.0, within(1e-9));
        assertThat(s.zoomLevelsBlocks()).containsExactly(1.0, 2.0, 4.0);
        assertThat(s.screenRefreshTicks()).isEqualTo(10);
        assertThat(s.mapWallSize()).isEqualTo(5);
        assertThat(s.terrainReadsInFlight()).isEqualTo(8);
        assertThat(s.terrainGenerationsInFlight()).isEqualTo(4);
        assertThat(s.terrainCacheChunks()).isEqualTo(4096);
    }

    @Test
    void theTerrainBudgetOfTheMapIsRead() {
        Map<String, Object> section = Map.of("terrain-reads-in-flight", 3, "terrain-generations-in-flight", 2,
                "terrain-cache-chunks", 100);
        CommandCenterSettings s = CommandCenterSettings.from(Map.of("command-center", section));

        assertThat(s.terrainReadsInFlight()).isEqualTo(3);
        assertThat(s.terrainGenerationsInFlight()).isEqualTo(2);
        assertThat(s.terrainCacheChunks()).isEqualTo(100);
    }

    @Test
    void aTerrainBudgetBelowOneGivesItsDefault() {
        Map<String, Object> section = Map.of("terrain-reads-in-flight", 0, "terrain-cache-chunks", -5);
        CommandCenterSettings s = CommandCenterSettings.from(Map.of("command-center", section));

        assertThat(s.terrainReadsInFlight()).isEqualTo(8);
        assertThat(s.terrainCacheChunks()).isEqualTo(4096);
    }

    @Test
    void theShippedConfigYmlReadsAsTheDefaults() throws IOException {
        Map<String, Object> config = TestConfig.yaml(
                Files.readString(Path.of("..", "CoreFactory-Paper", "src", "main", "resources", "config.yml")));

        assertThat(CommandCenterSettings.from(config)).isEqualTo(CommandCenterSettings.defaults());
    }

    @Test
    void aMapOverridesOnlyTheValuesItNames() {
        Map<String, Object> section = Map.of("listen-timeout-seconds", 45, "zoom-levels", List.of(2, 8));
        CommandCenterSettings s = CommandCenterSettings.from(Map.of("command-center", section));

        assertThat(s.listenTimeoutSeconds()).isEqualTo(45);
        assertThat(s.zoomLevelsBlocks()).containsExactly(2.0, 8.0);
        assertThat(s.mapRadiusBlocks()).isCloseTo(64.0, within(1e-9));
        assertThat(s.screenRefreshTicks()).isEqualTo(10);
        assertThat(s.mapWallSize()).isEqualTo(5);
    }

    @Test
    void theMapWallIsThreeOrFiveFramesAndAnyOtherSizeGivesTheDefault() {
        CommandCenterSettings five = CommandCenterSettings.from(Map.of("command-center", Map.of("map-wall-size", 5)));
        CommandCenterSettings four = CommandCenterSettings.from(Map.of("command-center", Map.of("map-wall-size", 4)));

        assertThat(five.mapWallSize()).isEqualTo(5);
        assertThat(four.mapWallSize()).isEqualTo(5);
    }

    @Test
    void aBadValueGivesItsDefault() {
        Map<String, Object> section = Map.of(
                "listen-timeout-seconds", 0,
                "map-radius-blocks", -5.0,
                "screen-refresh-ticks", "often",
                "map-wall-size", "seven");
        CommandCenterSettings s = CommandCenterSettings.from(Map.of("command-center", section));

        assertThat(s).isEqualTo(CommandCenterSettings.defaults());
    }

    @Test
    void zoomLevelsMustBeIncreasingAndAboveZero() {
        Map<String, Object> falling = Map.of("zoom-levels", List.of(4, 2, 1));
        Map<String, Object> zero = Map.of("zoom-levels", List.of(0, 2));
        Map<String, Object> empty = Map.of("zoom-levels", List.of());

        for (Map<String, Object> section : List.of(falling, zero, empty)) {
            CommandCenterSettings s = CommandCenterSettings.from(Map.of("command-center", section));
            assertThat(s.zoomLevelsBlocks()).containsExactly(1.0, 2.0, 4.0);
        }
    }

    @Test
    void theZoomLevelsAreKeptInTheirOrder() {
        CommandCenterSettings s = CommandCenterSettings.from(Map.of("command-center", Map.of("zoom-levels", List.of(1, 3, 9))));

        assertThat(s.zoomLevelsBlocks()).containsExactly(1.0, 3.0, 9.0);
    }

    @Test
    void aRecordWithNoZoomLevelIsRefused() {
        assertThatThrownBy(() -> new CommandCenterSettings(20, 64.0, List.of(), 10, 3, 8, 4, 4096))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRecordWithAMapWallOtherThanThreeOrFiveIsRefused() {
        assertThatThrownBy(() -> new CommandCenterSettings(20, 64.0, List.of(1.0), 10, 4, 8, 4, 4096))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
