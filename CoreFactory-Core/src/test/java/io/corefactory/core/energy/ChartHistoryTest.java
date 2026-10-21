package io.corefactory.core.energy;

import io.corefactory.core.energy.ChartHistory.Sample;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChartHistoryTest {

    private static Sample sample(double supplyW) {
        return new Sample(supplyW, 0.0, 1.0, 0.0, 0.0);
    }

    @Test
    void theCapacityIsFortyEightSamples() {
        assertThat(ChartHistory.CAPACITY).isEqualTo(48);
    }

    @Test
    void anEmptyHistoryHasNoSamples() {
        ChartHistory history = new ChartHistory();
        assertThat(history.size()).isZero();
        assertThat(history.samples()).isEmpty();
    }

    @Test
    void samplesAreKeptOldestFirst() {
        ChartHistory history = new ChartHistory();
        history.add(sample(1));
        history.add(sample(2));
        history.add(sample(3));
        assertThat(history.size()).isEqualTo(3);
        assertThat(history.samples()).extracting(Sample::supplyW).containsExactly(1.0, 2.0, 3.0);
    }

    @Test
    void theOldestSamplesAreDroppedWhenFull() {
        ChartHistory history = new ChartHistory();
        for (int i = 0; i < 50; i++) {
            history.add(sample(i));
        }
        assertThat(history.size()).isEqualTo(48);
        List<Sample> samples = history.samples();
        assertThat(samples).hasSize(48);
        assertThat(samples.get(0).supplyW()).isEqualTo(2.0);
        assertThat(samples.get(47).supplyW()).isEqualTo(49.0);
    }

    @Test
    void theSamplesAreReturnedInOrderAtEveryFill() {
        ChartHistory history = new ChartHistory();
        for (int i = 0; i < 100; i++) {
            history.add(sample(i));
            List<Sample> samples = history.samples();
            assertThat(samples).hasSize(Math.min(i + 1, 48));
            for (int k = 1; k < samples.size(); k++) {
                assertThat(samples.get(k).supplyW()).isEqualTo(samples.get(k - 1).supplyW() + 1);
            }
            assertThat(samples.get(samples.size() - 1).supplyW()).isEqualTo(i);
        }
    }

    @Test
    void aSampleWithASatisfactionOutsideZeroToOneIsRefused() {
        assertThatThrownBy(() -> new Sample(1, 1, 1.5, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Sample(-1, 1, 0.5, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
