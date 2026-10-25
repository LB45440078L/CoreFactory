package io.corefactory.paper.energy;

import io.corefactory.core.energy.AccumulatorLevel;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.EnergySnapshot;
import io.corefactory.core.energy.GridRegistry;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.energy.PylonType;
import io.corefactory.paper.energy.StoredEnergy.StoredAccumulator;
import io.corefactory.paper.energy.StoredEnergy.StoredPylon;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnergySnapshotsTest {

    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void aPylonLevelMapsToItsSize() {
        assertThat(EnergySnapshots.pylon(new StoredPylon("p", WORLD, new Pos(0, 64, 0), 1)).type()).isEqualTo(PylonType.SMALL);
        assertThat(EnergySnapshots.pylon(new StoredPylon("p", WORLD, new Pos(0, 64, 0), 2)).type()).isEqualTo(PylonType.MEDIUM);
        assertThat(EnergySnapshots.pylon(new StoredPylon("p", WORLD, new Pos(0, 64, 0), 3)).type()).isEqualTo(PylonType.LARGE);
        assertThat(EnergySnapshots.pylon(new StoredPylon("p", WORLD, new Pos(0, 64, 0), 4)).type()).isEqualTo(PylonType.HUGE);
        assertThat(EnergySnapshots.pylon(new StoredPylon("p", WORLD, new Pos(0, 64, 0), 5)).type()).isEqualTo(PylonType.COLOSSAL);
    }

    @Test
    void aPylonLevelOutsideOneToFiveIsRefused() {
        assertThatThrownBy(() -> EnergySnapshots.pylon(new StoredPylon("p", WORLD, new Pos(0, 64, 0), 6)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anAccumulatorLevelMapsToItsLevelUpToFive() {
        assertThat(EnergySnapshots.accumulator(new StoredAccumulator("a", WORLD, new Pos(0, 66, 0), 4, 0, false)).level())
                .isEqualTo(AccumulatorLevel.IV);
        assertThat(EnergySnapshots.accumulator(new StoredAccumulator("a", WORLD, new Pos(0, 66, 0), 5, 0, false)).level())
                .isEqualTo(AccumulatorLevel.V);
        assertThatThrownBy(() -> EnergySnapshots.accumulator(new StoredAccumulator("a", WORLD, new Pos(0, 66, 0), 6, 0, false)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anAccumulatorKeepsItsPositionAndLevel() {
        var accumulator = EnergySnapshots.accumulator(new StoredAccumulator("a", WORLD, new Pos(2, 66, 3), 2, 0, false));
        assertThat(accumulator.id()).isEqualTo("a");
        assertThat(accumulator.pos()).isEqualTo(new Pos(2, 66, 3));
        assertThat(accumulator.level()).isEqualTo(AccumulatorLevel.II);
    }

    @Test
    void theStateCarriesTheStoredEnergyAndTheDischargingFlag() {
        var state = EnergySnapshots.state(new StoredAccumulator("a", WORLD, new Pos(0, 66, 0), 1, 120.5, true));
        assertThat(state.storedWh()).isEqualTo(120.5);
        assertThat(state.discharging()).isTrue();
    }

    @Test
    void aNegativeStoredEnergyStartsAtZero() {
        var state = EnergySnapshots.state(new StoredAccumulator("a", WORLD, new Pos(0, 66, 0), 1, -5.0, false));
        assertThat(state.storedWh()).isZero();
    }

    @Test
    void theSnapshotHoldsTheDevicesAndGivesEveryAccumulatorAState() {
        EnergySnapshot snapshot = EnergySnapshots.build(
                List.of(new StoredPylon("p2", WORLD, new Pos(0, 64, 0), 2), new StoredPylon("p1", WORLD, new Pos(9, 64, 9), 1)),
                List.of(new StoredAccumulator("a", WORLD, new Pos(1, 66, 1), 1, 40.0, false)),
                List.of(),
                List.of(EnergySnapshots.consumer(WORLD, new Pos(3, 64, 3), 15.0)),
                List.of(),
                GridRegistry.EMPTY);
        assertThat(snapshot.pylons()).extracting(pylon -> pylon.id()).containsExactly("p1", "p2");
        assertThat(snapshot.accumulatorStates()).hasSize(1);
        assertThat(snapshot.accumulatorStates().get(0).storedWh()).isEqualTo(40.0);
        assertThat(snapshot.consumers()).hasSize(1);
        assertThat(snapshot.generators()).isEmpty();
    }

    @Test
    void aDuplicateDeviceIdIsRefusedByTheSnapshot() {
        assertThatThrownBy(() -> EnergySnapshots.build(
                List.of(new StoredPylon("p", WORLD, new Pos(0, 64, 0), 1), new StoredPylon("p", WORLD, new Pos(5, 64, 5), 1)),
                List.of(), List.of(), List.of(), List.of(), GridRegistry.EMPTY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aConsumerIdNamesItsWorldAndBlock() {
        Consumer consumer = EnergySnapshots.consumer(WORLD, new Pos(3, 64, -7), 30.0);
        assertThat(consumer.id()).isEqualTo(EnergySnapshots.consumerId(WORLD, 3, 64, -7));
        assertThat(consumer.id()).isNotEqualTo(EnergySnapshots.consumerId(WORLD, 3, 65, -7));
        assertThat(consumer.demandW()).isEqualTo(30.0);
    }

    @Test
    void theLevelNamesReadBackOnlyForTheirOwnPrefixAndRange() {
        assertThat(EnergyLevels.levelOf("pylon_2", EnergyLevels.PYLON)).contains(2);
        assertThat(EnergyLevels.levelOf("accumulator_3", EnergyLevels.ACCUMULATOR)).contains(3);
        assertThat(EnergyLevels.levelOf("accumulator_1", EnergyLevels.PYLON)).isEmpty();
        assertThat(EnergyLevels.levelOf("pylon_4", EnergyLevels.PYLON)).contains(4);
        assertThat(EnergyLevels.levelOf("pylon_5", EnergyLevels.PYLON)).contains(5);
        assertThat(EnergyLevels.levelOf("accumulator_5", EnergyLevels.ACCUMULATOR)).contains(5);
        assertThat(EnergyLevels.levelOf("pylon_6", EnergyLevels.PYLON)).isEmpty();
        assertThat(EnergyLevels.levelOf("accumulator_0", EnergyLevels.ACCUMULATOR)).isEmpty();
        assertThat(EnergyLevels.levelOf("pylon_", EnergyLevels.PYLON)).isEmpty();
        assertThat(EnergyLevels.levelOf(null, EnergyLevels.PYLON)).isEmpty();
        assertThat(EnergyLevels.componentId(EnergyLevels.PYLON, 1)).isEqualTo("pylon_1");
        assertThat(EnergyLevels.componentId(EnergyLevels.ACCUMULATOR, 5)).isEqualTo("accumulator_5");
        assertThat(EnergyLevels.roman(3)).isEqualTo("III");
        assertThat(EnergyLevels.roman(4)).isEqualTo("IV");
        assertThat(EnergyLevels.roman(5)).isEqualTo("V");
    }
}
