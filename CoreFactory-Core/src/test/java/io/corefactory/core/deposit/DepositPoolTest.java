package io.corefactory.core.deposit;

import io.corefactory.core.geology.OreDefinition;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

class DepositPoolTest {

    private static final String OIL = "crude_petroleum_deposit";

    private static OreDefinition ore(String id, int weight) {
        return new OreDefinition(id, id, "BLUE_GLAZED_TERRACOTTA", "raw_" + id, 1, 2, Set.of("STONE"), 0, 64, 0, weight);
    }

    private static final List<OreDefinition> ORES = List.of(
            ore("iron_ore", 6),
            new OreDefinition(OIL, OIL, "WHITE_GLAZED_TERRACOTTA", null, 0, 0, Set.of("STONE"), -64, 0, 400, 9),
            ore("diamond_ore", 1),
            ore("galena_ore", 2));

    @Test
    void eachSolidOreAppearsOncePerPointOfItsWeightAndTheOilDepositIsLeftOut() {
        assertThat(DepositPool.solid(ORES, OIL)).containsExactly(
                "iron_ore", "iron_ore", "iron_ore", "iron_ore", "iron_ore", "iron_ore",
                "diamond_ore",
                "galena_ore", "galena_ore");
    }

    @Test
    void anOreWithTheOrdinaryWeightAppearsTwiceAndAnOreWithNoWeightHasIt() {
        OreDefinition plain = new OreDefinition("zinc_deposit", "z", "CYAN_GLAZED_TERRACOTTA", "raw_z", 1, 2,
                Set.of("STONE"), 0, 64, 0);
        assertThat(plain.weight()).isEqualTo(OreDefinition.ORDINARY_WEIGHT);
        assertThat(DepositPool.solid(List.of(plain), OIL)).containsExactly("zinc_deposit", "zinc_deposit");
    }

    @Test
    void ironAndCopperAreCommonGoldIsUncommonAndDiamondIsTheRarest() {
        List<OreDefinition> shipped = List.of(
                ore("iron_ore", 6), ore("copper_ore", 6), ore("gold_ore", 3), ore("diamond_ore", 1),
                ore("galena_ore", OreDefinition.ORDINARY_WEIGHT), ore("zinc_deposit", OreDefinition.ORDINARY_WEIGHT));
        Map<String, Long> counts = DepositPool.solid(shipped, OIL).stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertThat(counts.get("iron_ore")).isEqualTo(counts.get("copper_ore"));
        assertThat(counts.get("iron_ore")).isGreaterThan(counts.get("gold_ore"));
        assertThat(counts.get("gold_ore")).isGreaterThan(counts.get("diamond_ore"));
        assertThat(counts.get("diamond_ore")).isLessThan(counts.get("galena_ore"));
    }

    @Test
    void thePlannedDepositsFollowTheWeightsOverManyCells() {
        // Deterministic: the same seed and cells always give the same plan, so the counts are fixed.
        List<String> pool = DepositPool.solid(List.of(
                ore("iron_ore", 6), ore("copper_ore", 6), ore("gold_ore", 3), ore("diamond_ore", 1),
                ore("galena_ore", OreDefinition.ORDINARY_WEIGHT)), OIL);
        DepositSettings settings = DepositSettings.defaults();
        Map<String, Integer> counts = new HashMap<>();
        for (int cellX = 0; cellX < 100; cellX++) {
            for (int cellZ = 0; cellZ < 100; cellZ++) {
                DepositPlanner.cell(7L, "world", cellX, cellZ, pool, OIL, settings).ifPresent(deposit -> {
                    if (!OIL.equals(deposit.ore())) {
                        counts.merge(deposit.ore(), 1, Integer::sum);
                    }
                });
            }
        }
        int solidPicks = counts.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(solidPicks).isPositive();
        // Each ore's share of the solid picks is its weight over the pool's total weight, here 6 + 6 + 3 + 1 + 2 = 18.
        int total = pool.size();
        assertThat(total).isEqualTo(18);
        assertThat((double) counts.get("iron_ore")).isCloseTo(solidPicks * 6.0 / total, withinPercentage(20));
        assertThat((double) counts.get("diamond_ore")).isCloseTo(solidPicks * 1.0 / total, withinPercentage(35));
        assertThat(counts.get("iron_ore")).isGreaterThan(counts.get("galena_ore"));
        assertThat(counts.get("galena_ore")).isGreaterThan(counts.get("diamond_ore"));
    }
}
