package io.corefactory.paper.energy;

import io.corefactory.core.energy.GeneratorEnvironment;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.OpenAir;
import io.corefactory.core.energy.Units;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** A generator device ticked without a server: its block has no world, which the tick does not read. */
class GeneratorDeviceTest {

    private static final Location SPOT = new Location(null, 10, 70, 10);
    private static final OpenAir OPEN = new OpenAir(6, 96);
    private static final OpenAir CLOSED = new OpenAir(5, 96);
    private static final double TICK_SECONDS = 0.05;

    private static GeneratorDevice device(GeneratorKind kind, int fuel, double remainingSeconds) {
        return new GeneratorDevice("g", kind, SPOT, fuel, remainingSeconds, 0.0, null);
    }

    private static GeneratorEnvironment noon(OpenAir air, boolean underwater) {
        return GeneratorEnvironment.at(6_000, 7L, air, underwater);
    }

    @Test
    void aCharcoalDeviceBurnsOneItemAndKeepsTheRest() {
        GeneratorDevice charcoal = device(GeneratorKind.CHARCOAL, 3, 0);
        charcoal.tick(noon(OPEN, false), TICK_SECONDS);
        assertThat(charcoal.outputW()).isEqualTo(25_000.0);
        assertThat(charcoal.fuel()).isEqualTo(2);
        assertThat(charcoal.remainingSeconds()).isCloseTo(30.0 - TICK_SECONDS, within(1e-9));
        assertThat(charcoal.status()).isEqualTo(GeneratorStatus.RUNNING);
    }

    @Test
    void theTotalGrowsByTheEnergyOfEachTick() {
        GeneratorDevice petroleum = device(GeneratorKind.PETROLEUM, 0, 0);
        for (int i = 0; i < 20; i++) {
            petroleum.tickFluid(noon(OPEN, false), TICK_SECONDS, petroleum.litresWanted(TICK_SECONDS));
        }
        assertThat(petroleum.totalWh()).isCloseTo(Units.wattHours(80_000.0, 1.0), within(1e-9));
    }

    @Test
    void aFluidDeviceDrawsTheLitresItWantsAndGivesItsRatedOutput() {
        GeneratorDevice diesel = device(GeneratorKind.DIESEL, 0, 0);
        diesel.tickFluid(noon(OPEN, false), TICK_SECONDS, diesel.litresWanted(TICK_SECONDS));
        assertThat(diesel.outputW()).isCloseTo(150_000.0, within(1e-6));
        assertThat(diesel.status()).isEqualTo(GeneratorStatus.RUNNING);
        diesel.tickFluid(noon(OPEN, false), TICK_SECONDS, diesel.litresWanted(TICK_SECONDS) / 2);
        assertThat(diesel.outputW()).isCloseTo(75_000.0, within(1e-6));
    }

    @Test
    void aDeviceWithNoFuelGivesNothingAndSaysNoFuel() {
        GeneratorDevice diesel = device(GeneratorKind.DIESEL, 0, 0);
        diesel.tickFluid(noon(OPEN, false), TICK_SECONDS, 0.0);
        assertThat(diesel.outputW()).isZero();
        assertThat(diesel.status()).isEqualTo(GeneratorStatus.NO_FUEL);
        assertThat(diesel.hologramLines()).containsExactly("&fDiesel generator  &cno fuel  &7network 0 L  &70.0104 L/s");
    }

    @Test
    void aFluidDeviceHoldsNoItemsAndShowsTheLitresItsNetworkHolds() {
        GeneratorDevice petroleum = device(GeneratorKind.PETROLEUM, 9, 9);
        assertThat(petroleum.fuel()).isZero();
        assertThat(petroleum.remainingSeconds()).isZero();
        assertThat(petroleum.addFuel()).isFalse();
        petroleum.setNetworkLitres(850.0);
        petroleum.tickFluid(noon(OPEN, false), TICK_SECONDS, petroleum.litresWanted(TICK_SECONDS));
        assertThat(petroleum.hologramLines().get(0)).startsWith("&fPetroleum generator  &a80.0 kW  &7network 850 L  &7");
    }

    @Test
    void aNuclearDeviceOutOfWaterKeepsItsFuel() {
        GeneratorDevice nuclear = device(GeneratorKind.NUCLEAR, 2, 0);
        nuclear.tick(noon(OPEN, false), TICK_SECONDS);
        assertThat(nuclear.outputW()).isZero();
        assertThat(nuclear.fuel()).isEqualTo(2);
        assertThat(nuclear.status()).isEqualTo(GeneratorStatus.NOT_UNDERWATER);
    }

    @Test
    void aNuclearDeviceUnderWaterBurnsAFuelRod() {
        GeneratorDevice nuclear = device(GeneratorKind.NUCLEAR, 2, 0);
        nuclear.tick(noon(OPEN, true), TICK_SECONDS);
        assertThat(nuclear.outputW()).isEqualTo(2_000_000.0);
        assertThat(nuclear.fuel()).isEqualTo(1);
        assertThat(nuclear.remainingSeconds()).isCloseTo(640_800.0 - TICK_SECONDS, within(1e-6));
    }

    @Test
    void aWindDeviceAtAClosedSiteIsClosedAndAtAnOpenSiteGivesItsWind() {
        GeneratorDevice wind = device(GeneratorKind.WIND, 0, 0);
        wind.tick(noon(CLOSED, false), TICK_SECONDS);
        assertThat(wind.outputW()).isZero();
        assertThat(wind.status()).isEqualTo(GeneratorStatus.CLOSED);
        wind.tick(noon(OPEN, false), TICK_SECONDS);
        assertThat(wind.outputW()).isBetween(0.0, 3_000.0);
        assertThat(wind.status()).isEqualTo(GeneratorStatus.RUNNING);
    }

    @Test
    void aSolarPanelAtNightIsAtNight() {
        GeneratorDevice solar = device(GeneratorKind.SOLAR, 0, 0);
        solar.tick(GeneratorEnvironment.at(18_000, 7L, OPEN, false), TICK_SECONDS);
        assertThat(solar.outputW()).isZero();
        assertThat(solar.status()).isEqualTo(GeneratorStatus.NIGHT);
    }

    @Test
    void aSavedCountAboveTheCapIsKeptAtSixtyFourAndAKindWithNoItemKeepsNone() {
        assertThat(device(GeneratorKind.CHARCOAL, 100, 0).fuel()).isEqualTo(64);
        GeneratorDevice wind = device(GeneratorKind.WIND, 9, 9);
        assertThat(wind.fuel()).isZero();
        assertThat(wind.remainingSeconds()).isZero();
    }

    @Test
    void theHologramShowsTheOutputAndTheFuelWhileItProduces() {
        GeneratorDevice charcoal = device(GeneratorKind.CHARCOAL, 14, 0);
        charcoal.tick(noon(OPEN, false), TICK_SECONDS);
        assertThat(charcoal.hologramLines()).containsExactly("&fCharcoal generator  &a25.0 kW  &7fuel 13");
    }
}
