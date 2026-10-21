package io.corefactory.core.energy;

import io.corefactory.core.energy.GeneratorFuel.Source;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorFuelTest {

    @Test
    void charcoalTakesOnlyPlainCharcoal() {
        assertThat(GeneratorFuel.accepts(GeneratorKind.CHARCOAL, Source.MATERIAL, "CHARCOAL")).isTrue();
        assertThat(GeneratorFuel.accepts(GeneratorKind.CHARCOAL, Source.MATERIAL, "COAL")).isFalse();
        assertThat(GeneratorFuel.accepts(GeneratorKind.CHARCOAL, Source.COMPONENT, "charcoal")).isFalse();
        assertThat(GeneratorFuel.accepts(GeneratorKind.CHARCOAL, Source.COMPONENT, "petroleum")).isFalse();
    }

    @Test
    void petroleumDieselAndNuclearTakeOnlyTheirOwnComponent() {
        assertThat(GeneratorFuel.accepts(GeneratorKind.PETROLEUM, Source.COMPONENT, "petroleum")).isTrue();
        assertThat(GeneratorFuel.accepts(GeneratorKind.PETROLEUM, Source.COMPONENT, "diesel")).isFalse();
        assertThat(GeneratorFuel.accepts(GeneratorKind.PETROLEUM, Source.MATERIAL, "petroleum")).isFalse();
        assertThat(GeneratorFuel.accepts(GeneratorKind.DIESEL, Source.COMPONENT, "diesel")).isTrue();
        assertThat(GeneratorFuel.accepts(GeneratorKind.DIESEL, Source.COMPONENT, "petroleum")).isFalse();
        assertThat(GeneratorFuel.accepts(GeneratorKind.NUCLEAR, Source.COMPONENT, "nuclear_fuel_rod")).isTrue();
        assertThat(GeneratorFuel.accepts(GeneratorKind.NUCLEAR, Source.COMPONENT, "diesel")).isFalse();
        assertThat(GeneratorFuel.accepts(GeneratorKind.NUCLEAR, Source.MATERIAL, "BLAZE_ROD")).isFalse();
    }

    @Test
    void windAndSolarTakeNoFuel() {
        for (Source source : Source.values()) {
            for (String id : List.of("CHARCOAL", "petroleum", "diesel", "nuclear_fuel_rod", "solar_panel")) {
                assertThat(GeneratorFuel.accepts(GeneratorKind.WIND, source, id)).isFalse();
                assertThat(GeneratorFuel.accepts(GeneratorKind.SOLAR, source, id)).isFalse();
            }
        }
    }

    @Test
    void everyFuelKindHasAFuelItemExactlyWhenItBurns() {
        for (GeneratorKind kind : GeneratorKind.values()) {
            assertThat(GeneratorFuel.itemIdOf(kind).isPresent()).as(kind.name()).isEqualTo(kind.burnsFuel());
            assertThat(GeneratorFuel.sourceOf(kind).isPresent()).as(kind.name()).isEqualTo(kind.burnsFuel());
        }
    }

    @Test
    void theCountIsCappedAtSixtyFourAndNeverNegative() {
        assertThat(GeneratorFuel.countOf(100)).isEqualTo(64);
        assertThat(GeneratorFuel.countOf(64)).isEqualTo(64);
        assertThat(GeneratorFuel.countOf(-3)).isZero();
        assertThat(GeneratorFuel.room(60)).isEqualTo(4);
        assertThat(GeneratorFuel.room(64)).isZero();
    }

    @Test
    void aCountIsGivenBackInOneStackOfAtMostSixtyFour() {
        assertThat(GeneratorFuel.stacks(64)).containsExactly(64);
        assertThat(GeneratorFuel.stacks(20)).containsExactly(20);
        assertThat(GeneratorFuel.stacks(130)).containsExactly(64);
        assertThat(GeneratorFuel.stacks(0)).isEmpty();
    }

    @Test
    void aBurnTakesOneItemFromTheCappedCountAndTheStacksStillAddUp() {
        int stored = GeneratorFuel.countOf(130);
        Generator charcoal = new Generator("g", new Pos(0, 70, 0), GeneratorKind.CHARCOAL, new GeneratorState(0, stored));
        GeneratorStep step = charcoal.tick(GeneratorEnvironment.at(6_000, 1L, new OpenAir(6, 96), true), EnergyTable.SECONDS_PER_TICK);
        int kept = step.state().fuelItems();
        assertThat(kept).isEqualTo(63);
        assertThat(GeneratorFuel.stacks(kept).stream().mapToInt(Integer::intValue).sum()).isEqualTo(kept);
    }
}
