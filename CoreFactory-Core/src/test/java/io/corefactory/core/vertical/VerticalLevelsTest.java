package io.corefactory.core.vertical;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerticalLevelsTest {

    @Test
    void theConfigValuesAreReadPerLevel() {
        VerticalLevels levels = VerticalLevels.of(new double[] {1.875, 3.75, 7.5}, new int[] {4, 8, 12});

        assertThat(levels.speed(1)).isEqualTo(1.875);
        assertThat(levels.speed(2)).isEqualTo(3.75);
        assertThat(levels.speed(3)).isEqualTo(7.5);
        assertThat(levels.capacity(1)).isEqualTo(4);
        assertThat(levels.capacity(2)).isEqualTo(8);
        assertThat(levels.capacity(3)).isEqualTo(12);
    }

    @Test
    void theTableDoesNotChangeWhenTheInputArraysChange() {
        double[] speeds = {1.875, 3.75, 7.5};
        int[] capacities = {4, 8, 12};
        VerticalLevels levels = VerticalLevels.of(speeds, capacities);

        speeds[0] = 99.0;
        capacities[0] = 99;

        assertThat(levels.speed(1)).isEqualTo(1.875);
        assertThat(levels.capacity(1)).isEqualTo(4);
    }

    @Test
    void levelsOutsideOneToThreeAreRejected() {
        VerticalLevels levels = VerticalLevels.of(new double[] {1.0, 2.0, 3.0}, new int[] {1, 2, 3});

        assertThatThrownBy(() -> levels.speed(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> levels.speed(4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> levels.capacity(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> levels.capacity(4)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aMissingOrShortSpeedListIsRejected() {
        assertThatThrownBy(() -> VerticalLevels.of(null, new int[] {4, 8, 12})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VerticalLevels.of(new double[] {1.875, 3.75}, new int[] {4, 8, 12}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VerticalLevels.of(new double[] {1.875, 3.75, 7.5, 9.0}, new int[] {4, 8, 12}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aMissingOrShortCapacityListIsRejected() {
        assertThatThrownBy(() -> VerticalLevels.of(new double[] {1.875, 3.75, 7.5}, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VerticalLevels.of(new double[] {1.875, 3.75, 7.5}, new int[] {4, 8}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.875, Double.NaN, Double.POSITIVE_INFINITY})
    void aSpeedThatIsNotPositiveAndFiniteIsRejected(double bad) {
        assertThatThrownBy(() -> VerticalLevels.of(new double[] {bad, 3.75, 7.5}, new int[] {4, 8, 12}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -4})
    void aCapacityBelowOneIsRejected(int bad) {
        assertThatThrownBy(() -> VerticalLevels.of(new double[] {1.875, 3.75, 7.5}, new int[] {bad, 8, 12}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
