package io.corefactory.paper.energy;

import io.corefactory.core.energy.GeneratorKind;
import org.junit.jupiter.api.Test;

import static io.corefactory.paper.energy.GeneratorStatus.CLOSED;
import static io.corefactory.paper.energy.GeneratorStatus.NIGHT;
import static io.corefactory.paper.energy.GeneratorStatus.NOT_UNDERWATER;
import static io.corefactory.paper.energy.GeneratorStatus.NO_FUEL;
import static io.corefactory.paper.energy.GeneratorStatus.RUNNING;
import static org.assertj.core.api.Assertions.assertThat;

class GeneratorStatusTest {

    @Test
    void theWordsAreTheOnesTheMenuShows() {
        assertThat(RUNNING.word()).isEqualTo("running");
        assertThat(NO_FUEL.word()).isEqualTo("no fuel");
        assertThat(CLOSED.word()).isEqualTo("closed (wind)");
        assertThat(NIGHT.word()).isEqualTo("night (solar)");
        assertThat(NOT_UNDERWATER.word()).isEqualTo("needs water");
    }

    @Test
    void aWindGeneratorIsClosedUnlessItsSiteIsOpen() {
        assertThat(GeneratorStatus.of(GeneratorKind.WIND, false, false, false, false, 0)).isEqualTo(CLOSED);
        assertThat(GeneratorStatus.of(GeneratorKind.WIND, true, false, true, false, 0)).isEqualTo(RUNNING);
    }

    @Test
    void aSolarPanelIsAtNightWhileItGivesNothing() {
        assertThat(GeneratorStatus.of(GeneratorKind.SOLAR, false, false, false, false, 0)).isEqualTo(NIGHT);
        assertThat(GeneratorStatus.of(GeneratorKind.SOLAR, false, false, true, false, 0)).isEqualTo(RUNNING);
    }

    @Test
    void anItemFuelKindIsRunningWhileItBurnsOrHoldsFuelAndNoFuelOtherwise() {
        assertThat(GeneratorStatus.of(GeneratorKind.CHARCOAL, false, false, true, true, 0)).isEqualTo(RUNNING);
        assertThat(GeneratorStatus.of(GeneratorKind.CHARCOAL, false, false, false, false, 4)).isEqualTo(RUNNING);
        assertThat(GeneratorStatus.of(GeneratorKind.CHARCOAL, false, false, false, false, 0)).isEqualTo(NO_FUEL);
    }

    @Test
    void aFluidKindIsRunningWhileItDrawsLitresAndHasNoFuelWhenItDrawsNone() {
        for (GeneratorKind kind : new GeneratorKind[] {GeneratorKind.PETROLEUM, GeneratorKind.DIESEL}) {
            assertThat(GeneratorStatus.of(kind, false, false, true, false, 0)).as(kind.name()).isEqualTo(RUNNING);
            assertThat(GeneratorStatus.of(kind, false, false, false, false, 0)).as(kind.name()).isEqualTo(NO_FUEL);
        }
    }

    @Test
    void aNuclearGeneratorOutOfWaterSaysSoWhateverItHolds() {
        assertThat(GeneratorStatus.of(GeneratorKind.NUCLEAR, false, false, false, false, 3)).isEqualTo(NOT_UNDERWATER);
        assertThat(GeneratorStatus.of(GeneratorKind.NUCLEAR, false, true, true, true, 0)).isEqualTo(RUNNING);
        assertThat(GeneratorStatus.of(GeneratorKind.NUCLEAR, false, true, false, false, 0)).isEqualTo(NO_FUEL);
        assertThat(GeneratorStatus.of(GeneratorKind.NUCLEAR, false, true, false, false, 1)).isEqualTo(RUNNING);
    }
}
