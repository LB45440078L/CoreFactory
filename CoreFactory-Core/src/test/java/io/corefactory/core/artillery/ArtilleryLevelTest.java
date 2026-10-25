package io.corefactory.core.artillery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtilleryLevelTest {

    @Test
    void theLevelsAreNumberedFromOneToSeven() {
        assertThat(ArtilleryLevel.I.number()).isEqualTo(1);
        assertThat(ArtilleryLevel.IV.number()).isEqualTo(4);
        assertThat(ArtilleryLevel.VII.number()).isEqualTo(7);
    }

    @Test
    void aLevelIsFoundByItsNumber() {
        assertThat(ArtilleryLevel.ofNumber(3)).isEqualTo(ArtilleryLevel.III);
        assertThatThrownBy(() -> ArtilleryLevel.ofNumber(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ArtilleryLevel.ofNumber(8)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theChargeTiersHaveTheirConfigKeys() {
        assertThat(ChargeTier.TIER_I.key()).isEqualTo("tier-1");
        assertThat(ChargeTier.TIER_III.key()).isEqualTo("tier-3");
    }
}
