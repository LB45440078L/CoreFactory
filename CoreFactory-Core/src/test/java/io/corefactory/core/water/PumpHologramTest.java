package io.corefactory.core.water;

import io.corefactory.core.hologram.Tone;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PumpHologramTest {

    /** A pump with its source met and nothing else set: level 1, 8 units a second at most, a 100-block minimum. */
    private static PumpHologram.Reading reading(int level, boolean running, int source, boolean grid, double satisfaction, int fuel) {
        return new PumpHologram.Reading(level, running ? 4.0 : 0.0, 8.0, source, 100, running, grid, satisfaction, 60000, fuel);
    }

    @Test
    void aPumpingLevelOneHasItsFiveLinesWithTheStatusSecond() {
        PumpHologram.Reading pumping = reading(1, true, 500, false, 0.0, 12);

        assertThat(PumpHologram.lines(pumping)).containsExactly(
                "&fWater pump &7Level I",
                "&7Status  &aPumping",
                "&bFlow  &f4 &7of 8 /s",
                "&7Source  &aInfinite",
                "&7Fuel  &f12 coal or charcoal  &a· burning");
        assertThat(PumpHologram.lines(pumping)).hasSize(PumpHologram.LINES);
        assertThat(PumpHologram.status(pumping)).isEqualTo(PumpStatus.PUMPING);
        assertThat(PumpHologram.tone(pumping)).isEqualTo(Tone.WORKING);
    }

    @Test
    void aLevelOneWithoutFuelSaysNoFuelAndIsRed() {
        PumpHologram.Reading empty = reading(1, false, 500, false, 0.0, 0);

        assertThat(PumpHologram.status(empty)).isEqualTo(PumpStatus.NO_FUEL);
        assertThat(PumpHologram.lines(empty)).element(1).isEqualTo("&7Status  &cNo fuel");
        assertThat(PumpHologram.lines(empty)).element(4).isEqualTo("&7Fuel  &cEmpty");
        assertThat(PumpHologram.tone(empty)).isEqualTo(Tone.STUCK);
    }

    @Test
    void aSourceBelowTheMinimumStopsAnyPumpFirst() {
        PumpHologram.Reading low = reading(1, false, 37, false, 0.0, 12);

        assertThat(PumpHologram.status(low)).isEqualTo(PumpStatus.NO_SOURCE);
        assertThat(PumpHologram.lines(low)).element(1).isEqualTo("&7Status  &cNo water source");
        assertThat(PumpHologram.lines(low)).element(3).isEqualTo("&7Source  &c37 &7of 100 blocks");
    }

    @Test
    void aLevelTwoWithoutItsDieselSaysNoDiesel() {
        PumpHologram.Reading noDiesel = reading(2, false, 500, false, 0.0, 0);

        assertThat(PumpHologram.status(noDiesel)).isEqualTo(PumpStatus.NO_DIESEL);
        assertThat(PumpHologram.lines(noDiesel)).element(4)
                .isEqualTo("&7Fuel  &fdiesel from the oil network  &e· waiting");
    }

    @Test
    void aPoweredPumpWithNoGridSaysNoGrid() {
        PumpHologram.Reading noGrid = reading(3, false, 500, false, 0.0, 0);

        assertThat(PumpHologram.status(noGrid)).isEqualTo(PumpStatus.NO_GRID);
        assertThat(PumpHologram.lines(noGrid)).element(1).isEqualTo("&7Status  &cNo power grid");
        assertThat(PumpHologram.lines(noGrid)).element(4).isEqualTo("&7Power  &cNo grid");
    }

    @Test
    void aPoweredPumpOnAGridThatGivesNothingSaysNoPower() {
        PumpHologram.Reading unpowered = reading(4, false, 500, true, 0.0, 0);

        assertThat(PumpHologram.status(unpowered)).isEqualTo(PumpStatus.NO_POWER);
        assertThat(PumpHologram.lines(unpowered)).element(4).isEqualTo("&7Power  &c0% &7of 60 kW");
        assertThat(PumpHologram.tone(unpowered)).isEqualTo(Tone.STUCK);
    }

    @Test
    void aPoweredPumpWithoutSourceWaitsForIt() {
        PumpHologram.Reading noSource = reading(3, false, 12, true, 0.0, 0);

        assertThat(PumpHologram.status(noSource)).isEqualTo(PumpStatus.NO_SOURCE);
        assertThat(PumpHologram.lines(noSource)).element(4).isEqualTo("&7Power  &7waiting for a source");
    }

    @Test
    void aPoweredPumpThatRunsOnPartialPowerIsPumpingAndShowsTheShare() {
        PumpHologram.Reading partial = reading(5, true, 500, true, 0.5, 0);

        assertThat(PumpHologram.status(partial)).isEqualTo(PumpStatus.PUMPING);
        assertThat(PumpHologram.lines(partial)).element(4).isEqualTo("&7Power  &e50% &7of 60 kW");
        assertThat(PumpHologram.tone(partial)).isEqualTo(Tone.WORKING);
    }

    @Test
    void everyStoppedStateIsRedAndNoneIsNeutral() {
        assertThat(Arrays.stream(PumpStatus.values()).filter(status -> status != PumpStatus.PUMPING))
                .allSatisfy(status -> assertThat(status.tone()).isEqualTo(Tone.STUCK));
        assertThat(PumpStatus.PUMPING.tone()).isEqualTo(Tone.WORKING);
    }
}
