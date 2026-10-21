package io.corefactory.spigot.energy;

import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.Pos;
import io.corefactory.spigot.energy.StoredEnergy.StoredGenerator;
import io.corefactory.spigot.energy.StoredEnergy.StoredPylon;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The generator records of energy.yml: the round trip, the files of earlier phases, and the entries that are skipped. */
class GeneratorFileTest {

    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static StoredEnergy sample() {
        return new StoredEnergy(
                List.of(new StoredPylon("p1", WORLD, new Pos(10, 64, -3), 1)),
                List.of(),
                List.of(new StoredGenerator("c1", WORLD, new Pos(2, 70, 2), GeneratorKind.CHARCOAL, 14, 600, 1_500.5),
                        new StoredGenerator("w1", WORLD, new Pos(-8, 100, 4), GeneratorKind.WIND, 0, 0, 42.0),
                        new StoredGenerator("n1", WORLD, new Pos(0, 60, 0), GeneratorKind.NUCLEAR, 64, 0, 0.0)),
                List.of());
    }

    @Test
    void theGeneratorRecordsSurviveAWriteAndARead() {
        StoredEnergy original = sample();
        StoredEnergy read = EnergyFile.read(EnergyFile.write(original), message -> {
            throw new AssertionError(message);
        });
        assertThat(read).isEqualTo(original);
    }

    @Test
    void aFileWrittenBeforeGeneratorsExistedReadsWithNone() {
        String oldFile = """
                pylons:
                  0:
                    id: p1
                    world: %s
                    x: 10
                    y: 64
                    z: -3
                    level: 1
                """.formatted(WORLD);
        List<String> warnings = new ArrayList<>();
        StoredEnergy read = EnergyFile.read(oldFile, warnings::add);
        assertThat(read.pylons()).hasSize(1);
        assertThat(read.generators()).isEmpty();
        assertThat(warnings).containsExactly(EnergyFile.LEGACY_RESET);
    }

    @Test
    void anEntryWithAnUnknownKindIsSkippedAndReported() {
        String text = """
                version: 2
                generators:
                  0:
                    id: good
                    world: %s
                    x: 1
                    y: 70
                    z: 1
                    kind: CHARCOAL
                    fuel: 3
                  1:
                    id: bad_kind
                    world: %s
                    x: 1
                    y: 70
                    z: 1
                    kind: FUSION
                """.formatted(WORLD, WORLD);
        List<String> warnings = new ArrayList<>();
        StoredEnergy read = EnergyFile.read(text, warnings::add);
        assertThat(read.generators()).extracting(StoredGenerator::id).containsExactly("good");
        assertThat(warnings).hasSize(1);
    }

    @Test
    void aSavedFuelAboveTheCapIsReadAsSixtyFourAndANegativeBurnAsZero() {
        String text = """
                version: 2
                generators:
                  0:
                    id: big
                    world: %s
                    x: 1
                    y: 70
                    z: 1
                    kind: DIESEL
                    fuel: 500
                    remaining-seconds: -7
                    total-wh: -1.0
                """.formatted(WORLD);
        StoredGenerator read = EnergyFile.read(text, message -> { }).generators().get(0);
        assertThat(read.fuel()).isEqualTo(64);
        assertThat(read.remainingSeconds()).isZero();
        assertThat(read.totalWh()).isZero();
    }
}
