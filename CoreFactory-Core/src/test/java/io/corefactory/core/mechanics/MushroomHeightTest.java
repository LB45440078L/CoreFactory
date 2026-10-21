package io.corefactory.core.mechanics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MushroomHeightTest {

    @Test
    void biggerYieldsReachHigher() {
        assertThat(NuclearMechanic.mushroomHeight(0.5)).isLessThan(NuclearMechanic.mushroomHeight(3.0));
        assertThat(NuclearMechanic.mushroomHeight(3.0)).isLessThan(NuclearMechanic.mushroomHeight(15.0));
        assertThat(NuclearMechanic.mushroomHeight(15.0)).isLessThan(NuclearMechanic.mushroomHeight(50.0));
    }

    @Test
    void aFiftyMegatonCloudTowersAndTheHeightIsClampedToItsRange() {
        assertThat(NuclearMechanic.mushroomHeight(50.0)).isGreaterThan(70.0);
        assertThat(NuclearMechanic.mushroomHeight(200.0)).isEqualTo(NuclearMechanic.mushroomHeight(50.0));
        assertThat(NuclearMechanic.mushroomHeight(0.0)).isEqualTo(NuclearMechanic.mushroomHeight(0.5));
    }
}
