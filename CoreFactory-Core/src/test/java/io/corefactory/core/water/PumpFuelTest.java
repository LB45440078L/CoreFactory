package io.corefactory.core.water;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.water.PumpFuel.Burn;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PumpFuelTest {

    @Test
    void anOfferTakesOnlyWhatFitsTheCapacity() {
        assertThat(PumpFuel.accept(0, 20)).isEqualTo(PumpFuel.CAPACITY);
        assertThat(PumpFuel.accept(10, 3)).isEqualTo(3);
        assertThat(PumpFuel.accept(PumpFuel.CAPACITY, 5)).isZero();
        assertThat(PumpFuel.accept(0, 0)).isZero();
    }

    @Test
    void aPumpThatIsNotWantedBurnsNothing() {
        Burn held = new Burn(3, 0);

        PumpFuel.Step step = PumpFuel.step(held, false);

        assertThat(step.running()).isFalse();
        assertThat(step.burn()).isEqualTo(held);
    }

    @Test
    void anEmptyPumpDoesNotRun() {
        PumpFuel.Step step = PumpFuel.step(Burn.empty(), true);

        assertThat(step.running()).isFalse();
        assertThat(step.burn()).isEqualTo(Burn.empty());
    }

    @Test
    void eachItemBurnsForSixtySecondsAndTheNextTakesItsPlace() {
        Burn burn = new Burn(2, 0);
        int running = 0;
        for (int tick = 0; tick < 5000; tick++) {
            PumpFuel.Step step = PumpFuel.step(burn, true);
            if (step.running()) {
                running++;
            }
            burn = step.burn();
        }

        assertThat(running).isEqualTo(2 * PumpFuel.ITEM_TICKS);
        assertThat(burn).isEqualTo(Burn.empty());
    }

    @Test
    void aTickThatTakesAnItemIsTheFirstTickOfItsBurn() {
        PumpFuel.Step step = PumpFuel.step(new Burn(1, 0), true);

        assertThat(step.running()).isTrue();
        assertThat(step.burn()).isEqualTo(new Burn(0, PumpFuel.ITEM_TICKS - 1));
    }

    @Test
    void theItemBurningNowCountsAsFuelHeld() {
        assertThat(Burn.empty().held()).isZero();
        assertThat(new Burn(0, PumpFuel.ITEM_TICKS - 1).held()).isEqualTo(1);
        assertThat(new Burn(2, 5).held()).isEqualTo(3);
    }

    @Test
    void levelOneTakesCoalOrCharcoalAndNoItemIsTakenAtLevelTwo() {
        assertThat(PumpFuel.accepts(1, ItemRef.material("COAL"))).isTrue();
        assertThat(PumpFuel.accepts(1, ItemRef.material("CHARCOAL"))).isTrue();
        assertThat(PumpFuel.accepts(1, ItemRef.item("charcoal"))).isTrue();
        assertThat(PumpFuel.accepts(1, ItemRef.item("diesel"))).isFalse();
        assertThat(PumpFuel.accepts(2, ItemRef.item("diesel"))).isFalse();
        assertThat(PumpFuel.accepts(2, ItemRef.material("COAL"))).isFalse();
    }

    @Test
    void levelTwoDrawsDieselFromTheNetworkAndTheOthersDoNot() {
        assertThat(PumpFuel.drawsFluid(1)).isFalse();
        assertThat(PumpFuel.drawsFluid(2)).isTrue();
        assertThat(PumpFuel.drawsFluid(3)).isFalse();
        assertThat(PumpFuel.litresWanted(2)).isEqualTo(PumpFuel.DIESEL_LITRES_PER_TICK);
        assertThat(PumpFuel.litresWanted(1)).isZero();
        assertThat(PumpFuel.FLUID).isEqualTo(io.corefactory.core.oil.Fluid.DIESEL);
        assertThat(PumpFuel.DIESEL_LITRES_PER_TICK * PumpFuel.ITEM_TICKS).isCloseTo(1.0, org.assertj.core.api.Assertions.within(1e-12));
    }

    @Test
    void poweredLevelsTakeNoFuelAndNothingIsAcceptedInAnEmptyHand() {
        assertThat(PumpFuel.accepts(3, ItemRef.item("diesel"))).isFalse();
        assertThat(PumpFuel.accepts(4, ItemRef.material("COAL"))).isFalse();
        assertThat(PumpFuel.accepts(1, null)).isFalse();
    }
}
