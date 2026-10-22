package io.corefactory.core.water;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WaterTextTest {

    @Test
    void aWholeNumberHasNoDecimalsAndAnotherOneDecimal() {
        assertThat(WaterText.number(2.0)).isEqualTo("2");
        assertThat(WaterText.number(1.5)).isEqualTo("1.5");
        assertThat(WaterText.number(0.0)).isEqualTo("0");
    }

    @Test
    void thePumpLineNamesTheLevelAndTheDeliveredFlow() {
        assertThat(WaterText.pumpFlow(1, 2.0)).isEqualTo("Pump L1  2 /s delivered");
        assertThat(WaterText.pumpFlow(4, 11.5)).isEqualTo("Pump L4  11.5 /s delivered");
    }

    @Test
    void theSourceIsInfiniteAtTheMinimumAndLowBelowIt() {
        assertThat(WaterText.source(100, 100)).isEqualTo("Water source: infinite");
        assertThat(WaterText.source(101, 100)).isEqualTo("Water source: infinite");
        assertThat(WaterText.source(37, 100)).isEqualTo("Source low: 37 blocks, needs 100");
    }

    @Test
    void theFuelLineNamesTheFuelAndItsState() {
        assertThat(WaterText.fuel(1, 0, false)).isEqualTo("Fuel: empty, stopped");
        assertThat(WaterText.fuel(1, 12, true)).isEqualTo("Fuel: 12 coal or charcoal, burning");
        assertThat(WaterText.fuel(2, 3, false)).isEqualTo("Fuel: 3 diesel, waiting");
    }

    @Test
    void thePowerLineGivesThePercentOfTheDemandInKilowatts() {
        assertThat(WaterText.power(0.5, 60000)).isEqualTo("Power: 50% of 60 kW");
        assertThat(WaterText.power(0.333, 120000)).isEqualTo("Power: 33% of 120 kW");
        assertThat(WaterText.power(1.0, 0)).isEqualTo("Power: none needed");
    }

    @Test
    void theRefusalGivesTheCount() {
        assertThat(WaterText.sourceTooSmall(42, 100))
                .isEqualTo("A pump needs at least 100 water blocks connected to it. This one has 42.");
    }

    @Test
    void theConsumerLineGivesWhatItGetsAgainstWhatItAsks() {
        assertThat(WaterText.consumerLine(0.5, 1.0)).isEqualTo("Water: 0.5 of 1 /s");
    }
}
