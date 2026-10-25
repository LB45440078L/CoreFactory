package io.corefactory.core.artillery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Checks the strength, the damage and the lifetime of a phosphorus cloud. */
class ArtilleryCloudTest {

    @Test
    void theStrengthIsFullAtTheBurstAndHalvesEachHalfLife() {
        assertThat(ArtilleryCloud.strength(0.0, 20.0)).isEqualTo(1.0);
        assertThat(ArtilleryCloud.strength(20.0, 20.0)).isCloseTo(0.5, within(1e-12));
        assertThat(ArtilleryCloud.strength(40.0, 20.0)).isCloseTo(0.25, within(1e-12));
    }

    @Test
    void theStrengthIsFullBeforeTheBurst() {
        assertThat(ArtilleryCloud.strength(-5.0, 20.0)).isEqualTo(1.0);
    }

    @Test
    void theStrengthNeedsAHalfLifeAboveZero() {
        assertThatThrownBy(() -> ArtilleryCloud.strength(1.0, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArtilleryCloud.strength(1.0, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theDamageIsScaledByTheStrengthInsideTheRadiusOnly() {
        assertThat(ArtilleryCloud.damagePerTick(0.05, 1.0, 3.0, 8.0)).isEqualTo(0.05);
        assertThat(ArtilleryCloud.damagePerTick(0.05, 0.5, 3.0, 8.0)).isEqualTo(0.025);
        assertThat(ArtilleryCloud.damagePerTick(0.05, 1.0, 8.0, 8.0)).isEqualTo(0.05);
        assertThat(ArtilleryCloud.damagePerTick(0.05, 1.0, 8.5, 8.0)).isEqualTo(0.0);
    }

    @Test
    void theCloudStandsUntilItsDuration() {
        assertThat(ArtilleryCloud.standing(59.9, 60.0)).isTrue();
        assertThat(ArtilleryCloud.standing(60.0, 60.0)).isFalse();
    }
}
