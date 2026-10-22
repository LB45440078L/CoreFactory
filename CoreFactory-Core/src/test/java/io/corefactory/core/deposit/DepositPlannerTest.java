package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class DepositPlannerTest {

    private static final long SEED = 4242L;
    private static final String WORLD = "world";
    private static final List<String> SOLID = List.of("galena_ore", "thorite_ore", "fluorite_ore", "uraninite_ore");
    private static final String OIL = "crude_petroleum_deposit";
    private static final DepositSettings SETTINGS = DepositSettings.defaults();

    private static List<Deposit> plan(int chunkX, int chunkZ) {
        return DepositPlanner.planChunk(SEED, WORLD, chunkX, chunkZ, SOLID, OIL, SETTINGS);
    }

    private static Optional<Deposit> cell(int cellX, int cellZ) {
        return DepositPlanner.cell(SEED, WORLD, cellX, cellZ, SOLID, OIL, SETTINGS);
    }

    @Test
    void theSameInputsGiveTheSameDepositsInAnyChunkOrder() {
        Map<String, List<Deposit>> forward = new LinkedHashMap<>();
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                forward.put(x + "," + z, plan(x, z));
            }
        }
        for (int x = 5; x >= -5; x--) {
            for (int z = 5; z >= -5; z--) {
                assertThat(plan(x, z)).isEqualTo(forward.get(x + "," + z));
            }
        }
    }

    @Test
    void aDifferentSeedGivesDifferentDeposits() {
        int differing = 0;
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                if (!DepositPlanner.planChunk(SEED + 1, WORLD, x, z, SOLID, OIL, SETTINGS).equals(plan(x, z))) {
                    differing++;
                }
            }
        }
        assertThat(differing).isPositive();
    }

    @Test
    void aDepositCrossingAChunkBorderIsReturnedForEveryChunkItTouches() {
        Map<Long, Deposit> deposits = new HashMap<>();
        for (int x = -40; x <= 40; x++) {
            for (int z = -40; z <= 40; z++) {
                for (Deposit deposit : plan(x, z)) {
                    deposits.put(deposit.id(), deposit);
                }
            }
        }

        int crossing = 0;
        for (Deposit deposit : deposits.values()) {
            int touched = 0;
            for (int x = Math.floorDiv(deposit.centreX() - deposit.radius(), 16);
                 x <= Math.floorDiv(deposit.centreX() + deposit.radius(), 16); x++) {
                for (int z = Math.floorDiv(deposit.centreZ() - deposit.radius(), 16);
                     z <= Math.floorDiv(deposit.centreZ() + deposit.radius(), 16); z++) {
                    if (coversAnyBlock(deposit, x, z)) {
                        touched++;
                        assertThat(plan(x, z)).contains(deposit);
                    }
                }
            }
            if (touched > 1) {
                crossing++;
            }
        }
        assertThat(crossing).isPositive();
    }

    @Test
    void aPlannedDepositStartsWithItsWholeAmount() {
        int planned = 0;
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                for (Deposit deposit : plan(x, z)) {
                    planned++;
                    assertThat(deposit.remaining()).isEqualTo(deposit.initialAmount());
                }
            }
        }
        assertThat(planned).isPositive();
    }

    @Test
    void aCellHoldsADepositAtTheDensityRateAndTheOilShare() {
        int present = 0;
        int oil = 0;
        Map<String, Integer> solidCounts = new HashMap<>();
        for (int x = 0; x < 100; x++) {
            for (int z = 0; z < 100; z++) {
                Optional<Deposit> deposit = cell(x, z);
                if (deposit.isEmpty()) {
                    continue;
                }
                present++;
                if (deposit.get().ore().equals(OIL)) {
                    oil++;
                } else {
                    solidCounts.merge(deposit.get().ore(), 1, Integer::sum);
                }
            }
        }

        assertThat(present / 10_000.0).isCloseTo(SETTINGS.density(), within(0.03));
        assertThat(oil / (double) present).isCloseTo(SETTINGS.oilShare(), within(0.03));
        int solid = present - oil;
        for (String ore : SOLID) {
            assertThat(solidCounts.getOrDefault(ore, 0) / (double) solid).isCloseTo(0.25, within(0.03));
        }
    }

    @Test
    void aDepositLiesInsideItsCellAtLeastItsRadiusFromEveryEdge() {
        int cellSize = SETTINGS.cellSize();
        Set<Long> ids = new HashSet<>();
        for (int x = 0; x < 100; x++) {
            for (int z = 0; z < 100; z++) {
                Optional<Deposit> maybe = cell(x, z);
                if (maybe.isEmpty()) {
                    continue;
                }
                Deposit deposit = maybe.get();
                int originX = x * cellSize;
                int originZ = z * cellSize;
                assertThat(deposit.centreX() - deposit.radius()).isGreaterThanOrEqualTo(originX);
                assertThat(deposit.centreX() + deposit.radius()).isLessThanOrEqualTo(originX + cellSize);
                assertThat(deposit.centreZ() - deposit.radius()).isGreaterThanOrEqualTo(originZ);
                assertThat(deposit.centreZ() + deposit.radius()).isLessThanOrEqualTo(originZ + cellSize);
                assertThat(deposit.radius()).isBetween(SETTINGS.minRadius(), SETTINGS.maxRadius());
                assertThat(SOLID.contains(deposit.ore()) || deposit.ore().equals(OIL)).isTrue();
                assertThat(ids.add(deposit.id())).isTrue();
            }
        }
    }

    @Test
    void amountsAreLogUniformWithinTheBounds() {
        List<Long> amounts = new ArrayList<>();
        for (int x = 0; x < 100; x++) {
            for (int z = 0; z < 100; z++) {
                cell(x, z).ifPresent(deposit -> amounts.add(deposit.initialAmount()));
            }
        }

        long min = SETTINGS.minAmount();
        long max = SETTINGS.maxAmount();
        assertThat(amounts).allSatisfy(amount -> assertThat(amount).isBetween(min, max));

        long geometricMean = Math.round(Math.sqrt((double) min * max));
        long below = amounts.stream().filter(amount -> amount < geometricMean).count();
        assertThat(below / (double) amounts.size()).isBetween(0.45, 0.55);

        List<Long> sorted = amounts.stream().sorted().toList();
        long median = sorted.get(sorted.size() / 2);
        assertThat(Math.abs(Math.log(median) - Math.log(geometricMean))).isLessThan(0.1);
    }

    @Test
    void anEmptySolidOreListIsRefused() {
        assertThatThrownBy(() -> DepositPlanner.planChunk(SEED, WORLD, 0, 0, List.of(), OIL, SETTINGS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static boolean coversAnyBlock(Deposit deposit, int chunkX, int chunkZ) {
        for (int x = chunkX * DepositPlanner.CHUNK_SIZE; x < chunkX * DepositPlanner.CHUNK_SIZE + DepositPlanner.CHUNK_SIZE; x++) {
            for (int z = chunkZ * DepositPlanner.CHUNK_SIZE; z < chunkZ * DepositPlanner.CHUNK_SIZE + DepositPlanner.CHUNK_SIZE; z++) {
                if (deposit.contains(x, z)) {
                    return true;
                }
            }
        }
        return false;
    }
}
