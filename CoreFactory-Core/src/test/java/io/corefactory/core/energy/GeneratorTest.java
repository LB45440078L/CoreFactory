package io.corefactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GeneratorTest {

    private static final Pos POS = new Pos(0, 64, 0);
    private static final double TICK = EnergyTable.SECONDS_PER_TICK;

    private static GeneratorEnvironment env(double irradiance, boolean underwater) {
        return new GeneratorEnvironment(0, 0, irradiance, new OpenAir(0, 64), underwater);
    }

    private static Generator fuel(GeneratorKind kind, double remainingSeconds, int items) {
        return new Generator("g", POS, kind, new GeneratorState(remainingSeconds, items));
    }

    /** The ticks a generator gives output with the given items and nothing burning, from the start of the burn. */
    private static int ticksOfOutput(GeneratorKind kind, int items, double dt, boolean underwater) {
        GeneratorState state = new GeneratorState(0.0, items);
        int ticks = 0;
        while (true) {
            GeneratorStep step = new Generator("g", POS, kind, state).tick(env(0, underwater), dt);
            if (step.outputW() == 0.0) {
                return ticks;
            }
            ticks++;
            state = step.state();
            assertThat(ticks).isLessThan(10_000_000);
        }
    }

    @Test
    void charcoalGivesItsRatedOutputWhileBurningAndUsesOneItem() {
        GeneratorStep step = fuel(GeneratorKind.CHARCOAL, 0.0, 1).tick(env(0, false), TICK);
        assertThat(step.outputW()).isEqualTo(25_000.0);
        assertThat(step.state().fuelItems()).isZero();
        assertThat(step.state().remainingSeconds()).isCloseTo(30.0 - TICK, within(1e-9));
    }

    @Test
    void charcoalBurnsOneItemForThirtySeconds() {
        assertThat(ticksOfOutput(GeneratorKind.CHARCOAL, 1, TICK, false) * TICK).isCloseTo(30.0, within(TICK));
    }

    @Test
    void petroleumBurnsOneItemForAboutAHundredSixtySeconds() {
        assertThat(ticksOfOutput(GeneratorKind.PETROLEUM, 1, TICK, false) * TICK).isCloseTo(160.125, within(TICK));
    }

    @Test
    void dieselBurnsOneItemForAboutNinetySixSeconds() {
        assertThat(ticksOfOutput(GeneratorKind.DIESEL, 1, TICK, false) * TICK).isCloseTo(96.27, within(TICK));
    }

    @Test
    void twoCharcoalItemsBurnForSixtySeconds() {
        assertThat(ticksOfOutput(GeneratorKind.CHARCOAL, 2, TICK, false) * TICK).isCloseTo(60.0, within(TICK));
    }

    @Test
    void aNuclearRodBurnsForOneHundredSeventyEightHoursAtTwoMegawatts() {
        assertThat(ticksOfOutput(GeneratorKind.NUCLEAR, 1, 3_600.0, true)).isEqualTo(178);
        GeneratorStep step = fuel(GeneratorKind.NUCLEAR, 0.0, 1).tick(env(0, true), 3_600.0);
        assertThat(step.outputW()).isEqualTo(2_000_000.0);
    }

    @Test
    void aNuclearGeneratorOutOfWaterGivesNothingAndKeepsItsFuel() {
        Generator generator = fuel(GeneratorKind.NUCLEAR, 0.0, 3);
        GeneratorStep step = generator.tick(env(0, false), TICK);
        assertThat(step.outputW()).isZero();
        assertThat(step.state()).isEqualTo(new GeneratorState(0.0, 3));
    }

    @Test
    void aFuelGeneratorWithNoFuelGivesZeroAndKeepsItsState() {
        Generator generator = fuel(GeneratorKind.DIESEL, 0.0, 0);
        GeneratorStep step = generator.tick(env(0, false), TICK);
        assertThat(step.outputW()).isZero();
        assertThat(step.state()).isEqualTo(GeneratorState.EMPTY);
    }

    @Test
    void fuelInsertedIntoAnIdleGeneratorStartsBurningInTheSameTick() {
        GeneratorStep step = fuel(GeneratorKind.PETROLEUM, 0.0, 1).tick(env(0, false), TICK);
        assertThat(step.outputW()).isEqualTo(80_000.0);
        assertThat(step.state().remainingSeconds()).isCloseTo(160.125 - TICK, within(1e-9));
    }

    @Test
    void aBurningItemCarriesItsRemainingSecondsAcrossTicks() {
        GeneratorStep step = fuel(GeneratorKind.DIESEL, 10.0, 0).tick(env(0, false), 0.5);
        assertThat(step.outputW()).isEqualTo(150_000.0);
        assertThat(step.state().remainingSeconds()).isCloseTo(9.5, within(1e-12));
    }

    @Test
    void runningOutOfFuelStopsTheOutput() {
        Generator generator = fuel(GeneratorKind.CHARCOAL, 0.0, 0);
        assertThat(generator.tick(env(0, false), TICK).outputW()).isZero();
    }

    @Test
    void windGivesItsOpenAirOutputWithNoFuel() {
        GeneratorEnvironment open = new GeneratorEnvironment(6_000, 99, 0, new OpenAir(6, 100), false);
        Generator wind = new Generator("w", POS, GeneratorKind.WIND, GeneratorState.EMPTY);
        GeneratorStep step = wind.tick(open, TICK);
        assertThat(step.outputW()).isEqualTo(Sky.windOutputW(new OpenAir(6, 100), 6_000, 99));
        assertThat(step.state()).isEqualTo(GeneratorState.EMPTY);
    }

    @Test
    void windGivesNothingAtAClosedSite() {
        GeneratorEnvironment closed = new GeneratorEnvironment(6_000, 99, 0, new OpenAir(5, 100), false);
        Generator wind = new Generator("w", POS, GeneratorKind.WIND, GeneratorState.EMPTY);
        assertThat(wind.tick(closed, TICK).outputW()).isZero();
    }

    @Test
    void solarGivesThreeHundredTwentyWattsAtNoonAndNothingAtNight() {
        Generator solar = new Generator("s", POS, GeneratorKind.SOLAR, GeneratorState.EMPTY);
        assertThat(solar.tick(GeneratorEnvironment.at(6_000, 0, new OpenAir(0, 64), false), TICK).outputW())
                .isCloseTo(320.0, within(1e-9));
        assertThat(solar.tick(GeneratorEnvironment.at(15_000, 0, new OpenAir(0, 64), false), TICK).outputW())
                .isZero();
    }

    @Test
    void aNuclearGeneratorIsUnderwaterOnlyWithWaterUnderItAndNothingElseAround() {
        assertThat(Generator.isUnderwater(true, 0)).isTrue();
        assertThat(Generator.isUnderwater(true, 1)).isFalse();
        assertThat(Generator.isUnderwater(false, 0)).isFalse();
        assertThatThrownBy(() -> Generator.isUnderwater(true, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTickMustHaveAPositiveLength() {
        Generator generator = fuel(GeneratorKind.CHARCOAL, 0.0, 1);
        assertThatThrownBy(() -> generator.tick(env(0, false), 0.0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withFuelAddsItemsAndRefusesNegativeCounts() {
        assertThat(GeneratorState.EMPTY.withFuel(3)).isEqualTo(new GeneratorState(0.0, 3));
        assertThatThrownBy(() -> GeneratorState.EMPTY.withFuel(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aStateRefusesNegativeOrNonFiniteValues() {
        assertThatThrownBy(() -> new GeneratorState(-1.0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeneratorState(Double.NaN, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeneratorState(0.0, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
