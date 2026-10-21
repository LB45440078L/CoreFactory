package io.corefactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class UnitsTest {

    @Test
    void theContractConstantsAreThePlannedValues() {
        assertThat(Units.NOMINAL_VOLTAGE_V).isEqualTo(230.0);
        assertThat(Units.PYLON_CONTACT_OHM).isEqualTo(0.01);
        assertThat(Units.WIRE_OHM_PER_BLOCK).isEqualTo(0.001);
        assertThat(Units.SECONDS_PER_HOUR).isEqualTo(3600.0);
    }

    @Test
    void powerTextIsWattsBelowAKilowattKilowattsBelowAMegawattAndMegawattsAbove() {
        assertThat(Units.powerText(850)).isEqualTo("850 W");
        assertThat(Units.powerText(2_800)).isEqualTo("2.8 kW");
        assertThat(Units.powerText(999_000)).isEqualTo("999.0 kW");
        assertThat(Units.powerText(1_200_000)).isEqualTo("1.2 MW");
    }

    @Test
    void energyTextIsWhBelowAKilowattHourKilowattHoursBelowAMegawattHourAndMegawattHoursAbove() {
        assertThat(Units.energyText(850)).isEqualTo("850 Wh");
        assertThat(Units.energyText(4_500)).isEqualTo("4.5 kWh");
        assertThat(Units.energyText(80_000)).isEqualTo("80.0 kWh");
        assertThat(Units.energyText(1_200_000)).isEqualTo("1.2 MWh");
    }

    @Test
    void voltageAndCurrentAreShownWithOneDecimal() {
        assertThat(Units.voltageText(230)).isEqualTo("230.0 V");
        assertThat(Units.voltageText(229.77)).isEqualTo("229.8 V");
        assertThat(Units.currentText(12.4)).isEqualTo("12.4 A");
        assertThat(Units.currentText(0)).isEqualTo("0.0 A");
    }

    @Test
    void percentIsAFractionShownWithNoDecimals() {
        assertThat(Units.percentText(0.8)).isEqualTo("80 %");
        assertThat(Units.percentText(0)).isEqualTo("0 %");
        assertThat(Units.percentText(1)).isEqualTo("100 %");
    }

    @Test
    void theNumericConversionsAreKilowattsAndKilowattHours() {
        assertThat(Units.kilowatts(2_800)).isCloseTo(2.8, within(1e-12));
        assertThat(Units.kilowattHours(4_500)).isCloseTo(4.5, within(1e-12));
    }

    @Test
    void aPowerOverATimeGivesEnergyAndBack() {
        assertThat(Units.wattHours(1_000, 3_600)).isCloseTo(1_000, within(1e-12));
        assertThat(Units.wattHours(100, 0.05)).isCloseTo(100 * 0.05 / 3600.0, within(1e-15));
        assertThat(Units.watts(1_000, 3_600)).isCloseTo(1_000, within(1e-12));
        assertThat(Units.watts(Units.wattHours(250, 2.5), 2.5)).isCloseTo(250, within(1e-12));
    }

    @Test
    void joulesAndWattHoursConvertAtThreeThousandSixHundredPerHour() {
        assertThat(Units.wattHoursFromJoules(3_600)).isCloseTo(1.0, within(1e-12));
        assertThat(Units.joulesFromWattHours(1.0)).isCloseTo(3_600, within(1e-12));
    }

    @Test
    void theResistanceOfAPathIsTheContactPlusTheWire() {
        assertThat(Units.resistanceOhm(0)).isCloseTo(0.01, within(1e-12));
        assertThat(Units.resistanceOhm(24)).isCloseTo(0.034, within(1e-12));
    }

    @Test
    void anUnloadedGridHasTheNominalVoltage() {
        assertThat(Units.voltageV(0, 0.034)).isEqualTo(230.0);
    }

    @Test
    void theVoltageDropsByTheCurrentTimesTheResistance() {
        assertThat(Units.voltageV(23_000, 0.01)).isCloseTo(229.0, within(1e-9));
        assertThat(Units.voltageV(4_000, 0.013)).isCloseTo(230.0 - (4_000 / 230.0) * 0.013, within(1e-12));
    }

    @Test
    void theVoltageNeverGoesBelowZero() {
        assertThat(Units.voltageV(1e9, 1.0)).isZero();
    }

    @Test
    void theCurrentIsPowerOverVoltageAndZeroWithoutVoltage() {
        assertThat(Units.currentA(23_000, 230)).isCloseTo(100.0, within(1e-12));
        assertThat(Units.currentA(5_000, 0)).isZero();
    }
}
