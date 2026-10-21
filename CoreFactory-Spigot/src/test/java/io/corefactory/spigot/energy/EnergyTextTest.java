package io.corefactory.spigot.energy;

import io.corefactory.core.energy.Grid;
import io.corefactory.core.energy.GridResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyTextTest {

    private static GridResult reading() {
        Grid grid = new Grid(3, 0xFF0000, List.of("p1"), List.of(), 2800.0, 23000.0, 0.011);
        return new GridResult(grid, 2800.0, 0.0, 2800.0, 1.0, 230.0, 12.4);
    }

    @Test
    void anAccumulatorShowsItsLevelWhatItHoldsAndItsStateOnTheFirstLine() {
        List<String> lines = EnergyText.accumulator(1, 120.4, 500, true, false, false);
        assertThat(lines).hasSize(EnergyText.ACCUMULATOR_LINES);
        assertThat(lines.get(0)).isEqualTo("&7Accumulator I  &f120 Wh &7/ 500 Wh  24 %  &acharging");
    }

    @Test
    void theStateWordSaysChargingDischargingOrIdle() {
        assertThat(EnergyText.state(true, false)).isEqualTo("&acharging");
        assertThat(EnergyText.state(false, true)).isEqualTo("&cdischarging");
        assertThat(EnergyText.state(false, false)).isEqualTo("&7idle");
        assertThat(EnergyText.accumulator(2, 1000, 20_000, false, true, false).get(0))
                .isEqualTo("&7Accumulator II  &f1.0 kWh &7/ 20.0 kWh  5 %  &cdischarging");
    }

    @Test
    void theFlashLineReadsOnlyWhileFlashing() {
        assertThat(EnergyText.accumulator(3, 0, 80_000, false, true, true).get(1)).isEqualTo("&6&lDischarge started");
        assertThat(EnergyText.accumulator(3, 0, 80_000, false, true, false).get(1)).isEmpty();
    }

    @Test
    void aFullStoreShowsItsKilowattHoursAndAHundredPercent() {
        assertThat(EnergyText.accumulator(2, 1999.6, 2000, false, false, false).get(0))
                .isEqualTo("&7Accumulator II  &f2.0 kWh &7/ 2.0 kWh  100 %  &7idle");
    }

    @Test
    void theGridLineShowsVoltageDemandAndSatisfactionWithAColour() {
        assertThat(EnergyText.grid(230.0, 4_000.0, 1.0)).isEqualTo("&aGrid 230.0 V  4.0 kW  100 %");
        assertThat(EnergyText.grid(230.0, 4_000.0, 0.4)).isEqualTo("&eGrid 230.0 V  4.0 kW  40 %");
        assertThat(EnergyText.grid(230.0, 4_000.0, 0.0)).isEqualTo("&cGrid 230.0 V  4.0 kW  0 %");
    }

    @Test
    void theNetworkLineIsAddedAtTheBottomSoTheCountDoesNotChange() {
        assertThat(EnergyText.withGrid(List.of("a", "b", "c"), 230.0, 4_000.0, 1.0))
                .containsExactly("a", "b", "c", "&aGrid 230.0 V  4.0 kW  100 %");
        assertThat(EnergyText.withGrid(List.of("a", "b", "c"), 230.0, 4_000.0, 0.5))
                .containsExactly("a", "b", "c", "&eGrid 230.0 V  4.0 kW  50 %");
    }

    @Test
    void aPylonShowsItsGridIdVoltageAndCurrentThenThePowerOfItsCapacity() {
        assertThat(EnergyText.pylon(3, reading()))
                .containsExactly("&7Grid #3  &f230.0 V  12.4 A", "&f2.8 kW &7of 23.0 kW");
        assertThat(EnergyText.pylon(3, reading())).hasSize(EnergyText.PYLON_LINES);
    }

    @Test
    void aPylonWithoutAReadingWaitsForTheFirstTick() {
        assertThat(EnergyText.pylon(3, null)).isEqualTo(EnergyText.pylonWaiting());
        assertThat(EnergyText.pylonWaiting()).hasSize(EnergyText.PYLON_LINES);
        assertThat(EnergyText.pylonWaiting().get(0)).contains("waiting");
    }
}
