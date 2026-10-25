package io.corefactory.paper.energy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WorkPaceTest {

    @Test
    void aFullNetworkRunsEveryTickAndLeavesTheCreditAlone() {
        WorkPace pace = WorkPace.step(0.0, 1.0);
        assertThat(pace.runTick()).isTrue();
        assertThat(pace.credit()).isZero();
    }

    @Test
    void aDeadNetworkNeverRuns() {
        WorkPace pace = WorkPace.step(0.0, 0.0);
        assertThat(pace.runTick()).isFalse();
        assertThat(pace.credit()).isZero();
    }

    @Test
    void halfPowerRunsEveryOtherTick() {
        WorkPace first = WorkPace.step(0.0, 0.5);
        assertThat(first.runTick()).isFalse();
        WorkPace second = WorkPace.step(first.credit(), 0.5);
        assertThat(second.runTick()).isTrue();
        assertThat(second.credit()).isZero();
    }

    @Test
    void progressAveragesTheSatisfactionOverTime() {
        double credit = 0;
        int runs = 0;
        for (int tick = 0; tick < 100; tick++) {
            WorkPace pace = WorkPace.step(credit, 0.3);
            credit = pace.credit();
            if (pace.runTick()) {
                runs++;
            }
        }
        assertThat(runs).isCloseTo(30, within(1));
        assertThat(credit).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
    }

    @Test
    void aSatisfactionOutsideZeroToOneIsClamped() {
        assertThat(WorkPace.step(0.0, 7.0).runTick()).isTrue();
        assertThat(WorkPace.step(0.0, -2.0).runTick()).isFalse();
        assertThat(WorkPace.step(0.0, Double.NaN).runTick()).isFalse();
    }
}
