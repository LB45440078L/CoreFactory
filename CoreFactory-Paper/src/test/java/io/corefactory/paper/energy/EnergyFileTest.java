package io.corefactory.paper.energy;

import io.corefactory.core.energy.Pos;
import io.corefactory.paper.energy.StoredEnergy.StoredAccumulator;
import io.corefactory.paper.energy.StoredEnergy.StoredPylon;
import io.corefactory.paper.energy.StoredEnergy.StoredWire;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyFileTest {

    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static StoredEnergy sample() {
        return new StoredEnergy(
                List.of(new StoredPylon("p1", WORLD, new Pos(10, 64, -3), 1),
                        new StoredPylon("p2", WORLD, new Pos(-40, 70, 22), 3)),
                List.of(new StoredAccumulator("a1", WORLD, new Pos(11, 65, -3), 2, 1234.5, true)),
                List.of(new StoredWire("w1", "p1", "p2")));
    }

    @Test
    void theStateSurvivesAWriteAndARead() {
        StoredEnergy original = sample();
        StoredEnergy read = EnergyFile.read(EnergyFile.write(original), message -> {
            throw new AssertionError(message);
        });
        assertThat(read).isEqualTo(original);
    }

    @Test
    void theHighestPylonAndAccumulatorLevelsAreKept() {
        StoredEnergy original = new StoredEnergy(
                List.of(new StoredPylon("p4", WORLD, new Pos(10, 64, -3), 4),
                        new StoredPylon("p5", WORLD, new Pos(-40, 70, 22), 5)),
                List.of(new StoredAccumulator("a5", WORLD, new Pos(11, 65, -3), 5, 1000.0, false)),
                List.of());
        StoredEnergy read = EnergyFile.read(EnergyFile.write(original), message -> {
            throw new AssertionError(message);
        });
        assertThat(read).isEqualTo(original);
    }

    @Test
    void anEmptyFileReadsAsNothing() {
        StoredEnergy read = EnergyFile.read(EnergyFile.write(StoredEnergy.empty()), message -> {
        });
        assertThat(read).isEqualTo(StoredEnergy.empty());
    }

    @Test
    void aMissingTextReadsAsNothing() {
        assertThat(EnergyFile.read("", message -> { })).isEqualTo(StoredEnergy.empty());
    }

    @Test
    void anEntryWithAnInvalidLevelOrWorldIsSkippedAndReported() {
        String text = """
                version: 2
                pylons:
                  0:
                    id: good
                    world: %s
                    x: 1
                    y: 64
                    z: 1
                    level: 2
                  1:
                    id: bad_level
                    world: %s
                    x: 1
                    y: 64
                    z: 1
                    level: 9
                  2:
                    id: bad_world
                    world: not-a-uuid
                    x: 1
                    y: 64
                    z: 1
                    level: 1
                """.formatted(WORLD, WORLD);
        List<String> warnings = new ArrayList<>();
        StoredEnergy read = EnergyFile.read(text, warnings::add);
        assertThat(read.pylons()).extracting(StoredPylon::id).containsExactly("good");
        assertThat(warnings).hasSize(2);
    }

    @Test
    void aNegativeStoredEnergyIsReadAsZero() {
        String text = """
                version: 2
                accumulators:
                  0:
                    id: a
                    world: %s
                    x: 0
                    y: 66
                    z: 0
                    level: 1
                    stored-wh: -4.0
                    discharging: false
                """.formatted(WORLD);
        assertThat(EnergyFile.read(text, message -> { }).accumulators().get(0).stored()).isZero();
    }
}
