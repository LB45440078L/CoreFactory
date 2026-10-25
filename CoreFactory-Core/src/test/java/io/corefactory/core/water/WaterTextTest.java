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
    void theTitleNamesThePumpAndItsRomanLevel() {
        assertThat(WaterText.title(1)).isEqualTo("&fWater pump &7Level I");
        assertThat(WaterText.title(5)).isEqualTo("&fWater pump &7Level V");
    }

    @Test
    void theFlowLineGivesWhatItDeliversAgainstWhatItCanGive() {
        assertThat(WaterText.flow(4.0, 8.0)).isEqualTo("&bFlow  &f4 &7of 8 /s");
        assertThat(WaterText.flow(0.0, 2.0)).isEqualTo("&bFlow  &f0 &7of 2 /s");
    }

    @Test
    void theSourceIsInfiniteAtTheMinimumAndLowBelowIt() {
        assertThat(WaterText.source(100, 100)).isEqualTo("&7Source  &aInfinite");
        assertThat(WaterText.source(101, 100)).isEqualTo("&7Source  &aInfinite");
        assertThat(WaterText.source(37, 100)).isEqualTo("&7Source  &c37 &7of 100 blocks");
    }

    @Test
    void theFuelLineNamesTheFuelAndItsState() {
        assertThat(WaterText.fuel(1, 0, false)).isEqualTo("&7Fuel  &cEmpty");
        assertThat(WaterText.fuel(1, 12, true)).isEqualTo("&7Fuel  &f12 coal or charcoal  &a· burning");
        assertThat(WaterText.fuel(2, 0, false)).isEqualTo("&7Fuel  &fdiesel from the oil network  &e· waiting");
    }

    @Test
    void thePowerLineGivesThePercentOfTheDemandInKilowatts() {
        assertThat(WaterText.power(0.5, 60000)).isEqualTo("&7Power  &e50% &7of 60 kW");
        assertThat(WaterText.power(1.0, 120000)).isEqualTo("&7Power  &a100% &7of 120 kW");
        assertThat(WaterText.power(0.0, 60000)).isEqualTo("&7Power  &c0% &7of 60 kW");
        assertThat(WaterText.power(1.0, 0)).isEqualTo("&7Power  &7none needed");
    }

    @Test
    void aPumpWithNoGridOrNoSourceSaysSo() {
        assertThat(WaterText.noGrid()).isEqualTo("&7Power  &cNo grid");
        assertThat(WaterText.noSource()).isEqualTo("&7Power  &7waiting for a source");
    }

    @Test
    void theConsumerLineGivesWhatItGetsAgainstWhatItAsks() {
        assertThat(WaterText.consumerLine(0.5, 1.0)).isEqualTo("&7Water  &b0.5 &7of &f1 /s");
    }

    @Test
    void theStatusLineNamesTheStateOfThePump() {
        assertThat(WaterText.status(PumpStatus.PUMPING)).isEqualTo("&7Status  &aPumping");
        assertThat(WaterText.status(PumpStatus.NO_FUEL)).isEqualTo("&7Status  &cNo fuel");
    }

    @Test
    void theRomanNumeralsRunFromOneToFive() {
        assertThat(WaterText.roman(3)).isEqualTo("III");
        assertThat(WaterText.roman(5)).isEqualTo("V");
    }
}
