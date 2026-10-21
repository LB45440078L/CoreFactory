package io.corefactory.spigot.energy;

import io.corefactory.core.energy.GeneratorKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorTextTest {

    @Test
    void theNamesAreTheKindNames() {
        assertThat(GeneratorText.name(GeneratorKind.WIND)).isEqualTo("Wind generator");
        assertThat(GeneratorText.name(GeneratorKind.SOLAR)).isEqualTo("Solar panel");
        assertThat(GeneratorText.name(GeneratorKind.CHARCOAL)).isEqualTo("Charcoal generator");
        assertThat(GeneratorText.name(GeneratorKind.PETROLEUM)).isEqualTo("Petroleum generator");
        assertThat(GeneratorText.name(GeneratorKind.DIESEL)).isEqualTo("Diesel generator");
        assertThat(GeneratorText.name(GeneratorKind.NUCLEAR)).isEqualTo("Nuclear generator");
    }

    @Test
    void aProducingFuelGeneratorShowsItsOutputAndTheFuelKeptOnOneLine() {
        assertThat(GeneratorText.hologram(GeneratorKind.CHARCOAL, 12_500.0, 14, GeneratorStatus.RUNNING))
                .containsExactly("&fCharcoal generator  &a12.5 kW  &7fuel 14");
    }

    @Test
    void aProducingWindGeneratorShowsOnlyItsOutput() {
        assertThat(GeneratorText.hologram(GeneratorKind.WIND, 3_000.0, 0, GeneratorStatus.RUNNING))
                .containsExactly("&fWind generator  &a3.0 kW");
    }

    @Test
    void aGeneratorNotProducingShowsItsStatusWordInPlaceOfTheOutput() {
        assertThat(GeneratorText.hologram(GeneratorKind.WIND, 0.0, 0, GeneratorStatus.CLOSED))
                .containsExactly("&fWind generator  &7closed (wind)");
        assertThat(GeneratorText.hologram(GeneratorKind.CHARCOAL, 0.0, 0, GeneratorStatus.NO_FUEL))
                .containsExactly("&fCharcoal generator  &cno fuel  &7fuel 0");
        assertThat(GeneratorText.hologram(GeneratorKind.NUCLEAR, 0.0, 2, GeneratorStatus.NOT_UNDERWATER))
                .containsExactly("&fNuclear generator  &enot underwater (nuclear)  &7fuel 2");
    }

    @Test
    void thePowerIsShownInWattsKilowattsOrMegawatts() {
        assertThat(GeneratorText.power(25.0)).isEqualTo("25 W");
        assertThat(GeneratorText.power(2_040.0)).isEqualTo("2.0 kW");
        assertThat(GeneratorText.power(25_000.0)).isEqualTo("25.0 kW");
        assertThat(GeneratorText.power(1_200_000.0)).isEqualTo("1.2 MW");
    }

    @Test
    void aDurationIsWholeSecondsUntilAnHourAndThenHoursMinutesAndSeconds() {
        assertThat(GeneratorText.duration(0)).isEqualTo("0 s");
        assertThat(GeneratorText.duration(96)).isEqualTo("96 s");
        assertThat(GeneratorText.duration(3_599)).isEqualTo("3599 s");
        assertThat(GeneratorText.duration(3_600)).isEqualTo("1:00:00");
        assertThat(GeneratorText.duration(3_725)).isEqualTo("1:02:05");
        assertThat(GeneratorText.duration(-5)).isEqualTo("0 s");
    }

    @Test
    void aPartOfASecondCountsAsTheNextWholeSecond() {
        assertThat(GeneratorText.duration(29.95)).isEqualTo("30 s");
        assertThat(GeneratorText.duration(0.01)).isEqualTo("1 s");
    }

    @Test
    void theFuelLineSaysItemsAndTakesTheSingularForOne() {
        assertThat(GeneratorText.fuelTitle(GeneratorKind.DIESEL, 1)).isEqualTo("&fFuel &e1 item");
        assertThat(GeneratorText.fuelTitle(GeneratorKind.DIESEL, 14)).isEqualTo("&fFuel &e14 items");
        assertThat(GeneratorText.fuelTitle(GeneratorKind.SOLAR, 0)).isEqualTo("&fNo fuel needed");
    }

    @Test
    void theFuelLoreTellsTheTimeLeftOnTheItemBurning() {
        assertThat(GeneratorText.fuelLore(GeneratorKind.CHARCOAL, 600).get(0))
                .isEqualTo("&7Burning: 600 s left on this item.");
        assertThat(GeneratorText.fuelLore(GeneratorKind.CHARCOAL, 0).get(0)).isEqualTo("&7No item is burning.");
        assertThat(GeneratorText.fuelLore(GeneratorKind.WIND, 0))
                .containsExactly("&7Wind and sun cost nothing to run.");
    }

    @Test
    void theStatusLineCarriesTheWordInTheColourOfTheStatus() {
        assertThat(GeneratorText.statusTitle(GeneratorStatus.RUNNING)).isEqualTo("&fStatus: &arunning");
        assertThat(GeneratorText.statusTitle(GeneratorStatus.NIGHT)).isEqualTo("&fStatus: &7night (solar)");
    }

    @Test
    void theOutputAndTotalLinesShowTheirNumbers() {
        assertThat(GeneratorText.outputTitle(12_500.0)).isEqualTo("&fOutput &a12.5 kW");
        assertThat(GeneratorText.outputTitle(0.0)).isEqualTo("&fOutput &70 W");
        assertThat(GeneratorText.producedTitle(1_499.6)).isEqualTo("&fProduced &e1.5 kWh");
        assertThat(GeneratorText.producedTitle(850.0)).isEqualTo("&fProduced &e850 Wh");
    }
}
