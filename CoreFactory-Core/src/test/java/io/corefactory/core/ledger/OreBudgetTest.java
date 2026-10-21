package io.corefactory.core.ledger;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OreBudgetTest {

    @Test
    void remainingIsPlannedLessMined() {
        assertThat(new OreBudget(10, 3).remaining()).isEqualTo(7);
        assertThat(new OreBudget(10, 0).remaining()).isEqualTo(10);
        assertThat(new OreBudget(10, 10).remaining()).isZero();
    }

    @Test
    void remainingNeverGoesNegative() {
        assertThat(new OreBudget(4, 9).remaining()).isZero();
    }

    @Test
    void negativeCountsAreRefused() {
        assertThatThrownBy(() -> new OreBudget(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OreBudget(1, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withMinedAddsToMinedAndKeepsPlanned() {
        OreBudget after = new OreBudget(5, 1).withMined(2);
        assertThat(after).isEqualTo(new OreBudget(5, 3));
        assertThat(after.remaining()).isEqualTo(2);
    }

    @Test
    void memoryBudgetsPlantOnceAndCountMinedPerChunk() {
        ChunkBudgets budgets = new MemoryChunkBudgets();
        assertThat(budgets.isBudgeted("world", 1, 2)).isFalse();

        budgets.plant("world", 1, 2, Map.of("galena_ore", 6L, "thorite_ore", 0L));
        budgets.plant("world", 1, 2, Map.of("galena_ore", 99L));
        assertThat(budgets.isBudgeted("world", 1, 2)).isTrue();
        assertThat(budgets.find("world", 1, 2, "galena_ore")).contains(new OreBudget(6, 0));

        assertThat(budgets.recordMined("world", 1, 2, "galena_ore", 2)).isTrue();
        assertThat(budgets.recordMined("world", 1, 2, "fluorite_ore", 1)).isFalse();
        assertThat(budgets.find("world", 1, 2, "galena_ore").orElseThrow().remaining()).isEqualTo(4);
        assertThat(budgets.find("world", 1, 3, "galena_ore")).isEmpty();
    }

    @Test
    void memoryLedgerAddsPerDrillAndPerOre() {
        MiningLedger ledger = new MemoryMiningLedger();
        ledger.add("world:1:2:3", "galena_ore", 1);
        ledger.add("world:1:2:3", "galena_ore", 2);
        ledger.add("world:1:2:3", "thorite_ore", 4);
        ledger.add("world:9:9:9", "galena_ore", 7);

        assertThat(ledger.mined("world:1:2:3", "galena_ore")).isEqualTo(3);
        assertThat(ledger.minedBy("world:1:2:3")).containsOnly(Map.entry("galena_ore", 3L), Map.entry("thorite_ore", 4L));
        assertThat(ledger.mined("world:0:0:0", "galena_ore")).isZero();
        assertThat(ledger.minedBy("world:0:0:0")).isEmpty();
    }
}
