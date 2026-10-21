package io.corefactory.spigot.energy;

import io.corefactory.core.energy.ChartMode;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.GridRegistry;
import io.corefactory.core.energy.Pos;
import io.corefactory.spigot.energy.StoredEnergy.StoredAccumulator;
import io.corefactory.spigot.energy.StoredEnergy.StoredChart;
import io.corefactory.spigot.energy.StoredEnergy.StoredGenerator;
import io.corefactory.spigot.energy.StoredEnergy.StoredPylon;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyFileGridTest {

    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static StoredEnergy grids() {
        return new StoredEnergy(
                List.of(new StoredPylon("p3", WORLD, new Pos(0, 64, 0), 1, 3),
                        new StoredPylon("p7", WORLD, new Pos(40, 64, 40), 2, 7)),
                List.of(new StoredAccumulator("a1", WORLD, new Pos(2, 66, 1), 2, 1_234.5, true)),
                List.of(new StoredGenerator("g1", WORLD, new Pos(3, 70, 0), GeneratorKind.CHARCOAL, 14, 29.5, 42.25)),
                List.of(),
                9,
                List.of(new StoredChart("Main", WORLD, new Pos(0, 70, 0), 3, ChartMode.STORAGE)));
    }

    private static String legacyText() {
        return """
                pylons:
                  0:
                    id: p3
                    world: %1$s
                    x: 0
                    y: 64
                    z: 0
                    level: 1
                    grid: 3
                accumulators:
                  0:
                    id: a1
                    world: %1$s
                    x: 2
                    y: 66
                    z: 1
                    level: 2
                    stored-wh: 1500.0
                    discharging: true
                generators:
                  0:
                    id: g1
                    world: %1$s
                    x: 3
                    y: 70
                    z: 0
                    kind: CHARCOAL
                    fuel: 14
                    remaining-seconds: 600
                    total-wh: 1500.5
                """.formatted(WORLD);
    }

    @Test
    void theGridIdsNextIdChartsWattHoursAndSecondsSurviveAWriteAndARead() {
        StoredEnergy original = grids();
        assertThat(EnergyFile.write(original)).contains("version: 2");
        List<String> warnings = new ArrayList<>();
        StoredEnergy read = EnergyFile.read(EnergyFile.write(original), warnings::add);
        assertThat(read).isEqualTo(original);
        assertThat(read.nextGridId()).isEqualTo(9);
        assertThat(read.charts())
                .containsExactly(new StoredChart("Main", WORLD, new Pos(0, 70, 0), 3, ChartMode.STORAGE));
        assertThat(read.accumulators().get(0).stored()).isEqualTo(1_234.5);
        assertThat(read.generators().get(0).remainingSeconds()).isEqualTo(29.5);
        assertThat(read.generators().get(0).totalWh()).isEqualTo(42.25);
        assertThat(warnings).isEmpty();
    }

    @Test
    void aLegacyTextWithoutAVersionResetsEnergyAndKeepsEverythingElse() {
        List<String> warnings = new ArrayList<>();
        StoredEnergy read = EnergyFile.read(legacyText(), warnings::add);
        assertThat(warnings).containsExactly(EnergyFile.LEGACY_RESET);
        assertThat(read.accumulators()).hasSize(1);
        assertThat(read.accumulators().get(0).stored()).isZero();
        assertThat(read.accumulators().get(0).discharging()).isFalse();
        assertThat(read.accumulators().get(0).pos()).isEqualTo(new Pos(2, 66, 1));
        assertThat(read.generators()).hasSize(1);
        assertThat(read.generators().get(0).remainingSeconds()).isZero();
        assertThat(read.generators().get(0).totalWh()).isZero();
        assertThat(read.generators().get(0).fuel()).isEqualTo(14);
        assertThat(read.pylons().get(0).grid()).isEqualTo(3);
        assertThat(read.pylons().get(0).pos()).isEqualTo(new Pos(0, 64, 0));
    }

    @Test
    void theGridIdsOfThePylonsAreKeptAcrossASave() {
        StoredEnergy read = EnergyFile.read(EnergyFile.write(grids()), message -> {
            throw new AssertionError(message);
        });
        GridRegistry registry = EnergySnapshots.grids(read.pylons(), read.nextGridId());
        assertThat(registry.idOf("p3")).isEqualTo(3);
        assertThat(registry.idOf("p7")).isEqualTo(7);
        assertThat(registry.nextFreshId()).isEqualTo(9);
    }

    @Test
    void aFreshGridAfterARestartNeverReusesAnIssuedId() {
        StoredEnergy read = EnergyFile.read(EnergyFile.write(grids()), message -> {
            throw new AssertionError(message);
        });
        GridRegistry registry = EnergySnapshots.grids(read.pylons(), read.nextGridId())
                .assign(List.of(Set.of("n1"), Set.of("n2")));
        assertThat(registry.idOf("n1")).isEqualTo(9);
        assertThat(registry.idOf("n2")).isEqualTo(10);
        assertThat(registry.nextFreshId()).isEqualTo(11);
    }

    @Test
    void aGridThatDissolvedDoesNotHandItsIdToANewGrid() {
        StoredEnergy saved = grids();
        GridRegistry registry = EnergySnapshots.grids(saved.pylons(), saved.nextGridId())
                .assign(List.of(Set.of("n1")));
        assertThat(registry.idOf("n1")).isEqualTo(9);
    }

    @Test
    void aJoinOfTwoGridsKeepsTheSmallerId() {
        StoredEnergy saved = grids();
        GridRegistry registry = EnergySnapshots.grids(saved.pylons(), saved.nextGridId())
                .assign(List.of(Set.of("p3", "p7")));
        assertThat(registry.idOf("p3")).isEqualTo(3);
        assertThat(registry.idOf("p7")).isEqualTo(3);
    }
}
