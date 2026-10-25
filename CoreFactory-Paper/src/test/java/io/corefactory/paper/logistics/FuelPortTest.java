package io.corefactory.paper.logistics;

import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.logistics.Item;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FuelPortTest {

    /** An in-memory tank: a count the port adds to. */
    private static final class MemoryTank implements FuelPort.Tank {

        int count;

        @Override
        public int count() {
            return count;
        }

        @Override
        public void addOne() {
            count++;
        }
    }

    @Test
    void aCharcoalGeneratorTakesPlainCharcoalOnly() {
        FuelPort port = new FuelPort(GeneratorKind.CHARCOAL, new MemoryTank());
        assertThat(port.accepts(Item.material("CHARCOAL"))).isTrue();
        assertThat(port.accepts(Item.material("COAL"))).isFalse();
        assertThat(port.accepts(Item.component("CHARCOAL"))).isFalse();
    }

    @Test
    void aPetroleumGeneratorTakesNoItemBecauseItDrawsItsFuelFromTheNetwork() {
        FuelPort port = new FuelPort(GeneratorKind.PETROLEUM, new MemoryTank());
        assertThat(port.accepts(Item.component("petroleum"))).isFalse();
        assertThat(port.accepts(Item.component("diesel"))).isFalse();
        assertThat(port.accepts(Item.material("petroleum"))).isFalse();
    }

    @Test
    void aWindGeneratorTakesNoFuel() {
        FuelPort port = new FuelPort(GeneratorKind.WIND, new MemoryTank());
        assertThat(port.accepts(Item.material("CHARCOAL"))).isFalse();
        assertThat(port.accepts(Item.component("petroleum"))).isFalse();
    }

    @Test
    void anAcceptedItemIsCountedInTheTankAndNeverTaken() {
        MemoryTank tank = new MemoryTank();
        FuelPort port = new FuelPort(GeneratorKind.CHARCOAL, tank);
        port.accept(Item.material("CHARCOAL"));
        port.accept(Item.material("CHARCOAL"));
        assertThat(tank.count).isEqualTo(2);
        assertThat(port.storedCount()).isEqualTo(2);
        assertThat(port.peekOutput()).isNull();
        assertThat(port.takeOutput()).isNull();
    }

    @Test
    void aTankAtTheCapRefusesMoreFuel() {
        MemoryTank tank = new MemoryTank();
        tank.count = GeneratorFuel.CAP - 1;
        FuelPort port = new FuelPort(GeneratorKind.CHARCOAL, tank);
        assertThat(port.accepts(Item.material("CHARCOAL"))).isTrue();
        port.accept(Item.material("CHARCOAL"));
        assertThat(tank.count).isEqualTo(GeneratorFuel.CAP);
        assertThat(port.accepts(Item.material("CHARCOAL"))).isFalse();
    }

    @Test
    void aRefusedItemIsNotAccepted() {
        FuelPort port = new FuelPort(GeneratorKind.CHARCOAL, new MemoryTank());
        assertThatThrownBy(() -> port.accept(Item.material("COAL"))).isInstanceOf(IllegalStateException.class);
    }
}
