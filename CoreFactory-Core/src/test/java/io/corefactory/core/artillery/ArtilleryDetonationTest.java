package io.corefactory.core.artillery;

import io.corefactory.core.artillery.ArtillerySettings.ShellSpec;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Checks the burst altitude, the submunition radius, the centre damage and the megaton scale of a detonation. */
class ArtilleryDetonationTest {

    @Test
    void theBurstAltitudeStaysInItsRange() {
        ShellSpec phosphorus = new ShellSpec(0.0, 25.0, 35.0);
        for (long seed = 0; seed < 200; seed++) {
            double altitude = ArtilleryDetonation.burstAltitude(phosphorus, new Random(seed));
            assertThat(altitude).isBetween(25.0, 35.0);
        }
    }

    @Test
    void aShellWithOneAltitudeBurstsThere() {
        ShellSpec nuclear = new ShellSpec(15_000.0, 8.0, 8.0);
        assertThat(ArtilleryDetonation.burstAltitude(nuclear, new Random(7))).isEqualTo(8.0);
    }

    @Test
    void theBurstAltitudeReplaysFromItsSeed() {
        ShellSpec cluster = new ShellSpec(3.0, 30.0, 50.0);
        assertThat(ArtilleryDetonation.burstAltitude(cluster, new Random(42)))
                .isEqualTo(ArtilleryDetonation.burstAltitude(cluster, new Random(42)));
    }

    @Test
    void aSubmunitionBlastHasAFloorAndGrowsWithItsYield() {
        assertThat(ArtilleryDetonation.submunitionRadiusBlocks(3.0, 1.15)).isEqualTo(3.0);
        assertThat(ArtilleryDetonation.submunitionRadiusBlocks(1000.0, 1.15)).isCloseTo(11.5, within(1e-9));
    }

    @Test
    void theCentreDamageGrowsWithTheCubeRootAndIsCapped() {
        assertThat(ArtilleryDetonation.damage(1.0)).isCloseTo(12.0, within(1e-9));
        assertThat(ArtilleryDetonation.damage(1_000_000_000.0)).isEqualTo(ArtilleryDetonation.MAX_DAMAGE);
        assertThat(ArtilleryDetonation.damage(0.0)).isEqualTo(0.0);
    }

    @Test
    void aYieldInTonsIsConvertedToMegatons() {
        assertThat(ArtilleryDetonation.megatons(15_000.0)).isCloseTo(0.015, within(1e-12));
        assertThat(ArtilleryDetonation.megatons(15.0)).isCloseTo(0.000015, within(1e-12));
    }
}
