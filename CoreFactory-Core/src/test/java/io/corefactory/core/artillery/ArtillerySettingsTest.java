package io.corefactory.core.artillery;

import io.corefactory.core.config.TestConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArtillerySettingsTest {

    @Test
    void theDefaultsAreTheValuesOfConfigYml() {
        ArtillerySettings s = ArtillerySettings.defaults();
        assertThat(s.heightOf(ArtilleryLevel.III)).isCloseTo(2.5, within(1e-9));
        assertThat(s.heightOf(ArtilleryLevel.VII)).isCloseTo(3.75, within(1e-9));
        assertThat(s.powerOf(ArtilleryLevel.I)).isCloseTo(100_000.0, within(1e-9));
        assertThat(s.powerOf(ArtilleryLevel.VII)).isCloseTo(1_270_000.0, within(1e-9));
        assertThat(s.charge(ChargeTier.TIER_II).range()).isCloseTo(1.25, within(1e-9));
        assertThat(s.shell(ArtilleryShell.HIGH_EXPLOSIVE).yieldTons()).isCloseTo(10.0, within(1e-9));
        assertThat(s.shell(ArtilleryShell.ENRICHED_URANIUM).yieldTons()).isCloseTo(15_000.0, within(1e-9));
        assertThat(s.shell(ArtilleryShell.CLUSTER_ENRICHED_URANIUM).yieldTons()).isCloseTo(15.0, within(1e-9));
        assertThat(s.shell(ArtilleryShell.CONDENSED_ANTIMATTER).yieldTons()).isCloseTo(5e8, within(1e-3));
        assertThat(s.shell(ArtilleryShell.HYDROGEN_THERMONUCLEAR).burstMinBlocks()).isCloseTo(20.0, within(1e-9));
        assertThat(s.shell(ArtilleryShell.HYDROGEN_THERMONUCLEAR).burstMaxBlocks()).isCloseTo(30.0, within(1e-9));
        assertThat(s.cluster().submunitions()).isEqualTo(40);
        assertThat(s.phosphorus().lightParticles()).isEqualTo(600);
        assertThat(s.safety().minDistanceAntimatterBlocks()).isCloseTo(2000.0, within(1e-9));
        assertThat(s.antimatterCooldownMillis()).isEqualTo(12L * 3_600_000L);
        assertThat(s.magazineCapacity()).isEqualTo(16);
        assertThat(s.shell(ArtilleryShell.NAPALM).yieldTons()).isZero();
        assertThat(s.shell(ArtilleryShell.WHITE_PHOSPHORUS).yieldTons()).isZero();
    }

    @Test
    void theMagazineCapacityIsReadAndMustBeAboveZero() {
        Map<String, Object> config = Map.of("artillery", Map.of("magazine-capacity", 24));
        assertThat(ArtillerySettings.from(config).magazineCapacity()).isEqualTo(24);

        Map<String, Object> bad = Map.of("artillery", Map.of("magazine-capacity", 0));
        assertThat(ArtillerySettings.from(bad).magazineCapacity()).isEqualTo(16);
    }

    @Test
    void aMapOverridesOnlyTheValuesItNames() {
        Map<String, Object> safety = Map.of("blast-margin-blocks", 35);
        Map<String, Object> highExplosive = Map.of("tnt-blocks", 20);
        Map<String, Object> shells = Map.of("tnt-block-tons", 2.0, "high-explosive", highExplosive);
        Map<String, Object> artillery = Map.of("safety", safety, "shells", shells);
        ArtillerySettings s = ArtillerySettings.from(Map.of("artillery", artillery));

        assertThat(s.safety().blastMarginBlocks()).isCloseTo(35.0, within(1e-9));
        // Twenty blocks of TNT at 2 tons each: 40 tons.
        assertThat(s.shell(ArtilleryShell.HIGH_EXPLOSIVE).yieldTons()).isCloseTo(40.0, within(1e-9));
        assertThat(s.safety().minDistanceConventionalBlocks()).isCloseTo(50.0, within(1e-9));
    }

    @Test
    void aBadValueGivesItsDefault() {
        Map<String, Object> levels = Map.of("power-w", List.of(1, 2));
        Map<String, Object> safety = Map.of("scatter-sigmas", -1, "blast-margin-blocks", "lots");
        ArtillerySettings s = ArtillerySettings.from(Map.of("artillery", Map.of("levels", levels, "safety", safety)));

        assertThat(s.powerOf(ArtilleryLevel.I)).isCloseTo(100_000.0, within(1e-9));
        assertThat(s.safety().scatterSigmas()).isCloseTo(3.0, within(1e-9));
        assertThat(s.safety().blastMarginBlocks()).isCloseTo(20.0, within(1e-9));
    }

    @Test
    void noConfigAndAnEmptySectionGiveTheSameSettings() {
        assertThat(ArtillerySettings.from(null)).isEqualTo(ArtillerySettings.defaults());
        assertThat(ArtillerySettings.from(Map.of())).isEqualTo(ArtillerySettings.defaults());
    }

    @Test
    void theNuclearYieldsComeFromTntsYmlNotFromConfigYml() {
        Map<String, Object> tnts = Map.of("tnts", Map.of(
                "nuke_little_boy", Map.of("settings", Map.of("yield-megatons", 0.5)),
                "nuke_davy_crockett", Map.of("settings", Map.of("yield-megatons", 0.0001))));
        Map<String, Object> enriched = Map.of("yield-tons", 1);
        Map<String, Object> config = Map.of("artillery", Map.of("shells", Map.of("enriched-uranium", enriched)));
        ArtillerySettings s = ArtillerySettings.from(config, tnts);

        // The Little Boy's half a megaton, in tons; the yield in config.yml is not read for this shell.
        assertThat(s.shell(ArtilleryShell.ENRICHED_URANIUM).yieldTons()).isCloseTo(500_000.0, within(1e-6));
        // Three quarters of the 100 tons of the Davy Crockett.
        assertThat(s.shell(ArtilleryShell.CLUSTER_ENRICHED_URANIUM).yieldTons()).isCloseTo(75.0, within(1e-9));
    }

    @Test
    void theDefaultsReadTheShippedTntsYmlAndConfigYml() throws IOException {
        Map<String, Object> config = shipped("config.yml");
        Map<String, Object> tnts = shipped("tnts.yml");

        assertThat(ArtillerySettings.from(config, tnts)).isEqualTo(ArtillerySettings.defaults());
        assertThat(ArtillerySettings.from(config, tnts).shell(ArtilleryShell.ENRICHED_URANIUM).yieldTons())
                .isCloseTo(15_000.0, within(1e-6));
    }

    private static Map<String, Object> shipped(String name) throws IOException {
        return TestConfig.yaml(Files.readString(Path.of("..", "CoreFactory-Paper", "src", "main", "resources", name)));
    }

    @Test
    void thePresentationIsReadAndABadValueGivesItsDefault() {
        Map<String, Object> presentation = Map.of("launch-volume", 2.5, "trail-puffs-per-tick", 5,
                "whistle-interval-ticks", 0, "recoil-blocks", -1);
        ArtillerySettings s = ArtillerySettings.from(Map.of("artillery", Map.of("presentation", presentation)));

        assertThat(s.presentation().launchVolume()).isCloseTo(2.5, within(1e-9));
        assertThat(s.presentation().trailPuffsPerTick()).isEqualTo(5);
        assertThat(s.presentation().whistleIntervalTicks()).isEqualTo(ArtillerySettings.DEFAULT_WHISTLE_INTERVAL_TICKS);
        assertThat(s.presentation().recoilBlocks()).isCloseTo(ArtillerySettings.DEFAULT_RECOIL_BLOCKS, within(1e-9));
        assertThat(s.presentation().launchPitch()).isCloseTo(ArtillerySettings.DEFAULT_LAUNCH_PITCH, within(1e-9));
    }
}
