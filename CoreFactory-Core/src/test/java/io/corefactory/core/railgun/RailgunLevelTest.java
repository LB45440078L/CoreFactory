package io.corefactory.core.railgun;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RailgunLevelTest {

    @Test
    void theLevelsAreNumberedFromOneToThree() {
        assertThat(RailgunLevel.I.number()).isEqualTo(1);
        assertThat(RailgunLevel.II.number()).isEqualTo(2);
        assertThat(RailgunLevel.III.number()).isEqualTo(3);
        assertThat(RailgunLevel.values()).hasSize(3);
    }

    @Test
    void aNumberGivesItsLevel() {
        assertThat(RailgunLevel.ofNumber(1)).isEqualTo(RailgunLevel.I);
        assertThat(RailgunLevel.ofNumber(2)).isEqualTo(RailgunLevel.II);
        assertThat(RailgunLevel.ofNumber(3)).isEqualTo(RailgunLevel.III);
    }

    @Test
    void aNumberOutsideOneToThreeIsRefused() {
        assertThatThrownBy(() -> RailgunLevel.ofNumber(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RailgunLevel.ofNumber(4)).isInstanceOf(IllegalArgumentException.class);
    }
}
