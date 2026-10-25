package io.corefactory.paper.machine;

import io.corefactory.core.process.Machine;
import io.corefactory.paper.ShippedContent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The one-time move of stored stacks into an empty barrel. Pure, so no server is needed. */
class BarrelMigrationTest {

    @Test
    void aStoredStackMovesIntoAnEmptyBarrelSlot() {
        BarrelMigration.Plan<Integer, String> plan = BarrelMigration.plan(Map.of(4, "ore"), slot -> slot - 4, index -> true);
        assertThat(plan.moved()).containsExactly(Map.entry(0, "ore"));
        assertThat(plan.kept()).isEmpty();
    }

    @Test
    void aStoredStackNeverOverwritesAnOccupiedSlot() {
        BarrelMigration.Plan<Integer, String> plan = BarrelMigration.plan(Map.of(4, "ore"), slot -> slot - 4,
                index -> false);
        assertThat(plan.moved()).isEmpty();
        assertThat(plan.kept()).containsExactly(Map.entry(4, "ore"));
    }

    @Test
    void onlyTheEmptySlotsTakeTheirStacks() {
        Map<Integer, String> stored = Map.of(4, "ore", 5, "calcine");
        BarrelMigration.Plan<Integer, String> plan = BarrelMigration.plan(stored, slot -> slot - 4,
                index -> index == 1);
        assertThat(plan.moved()).containsExactly(Map.entry(1, "calcine"));
        assertThat(plan.kept()).containsExactly(Map.entry(4, "ore"));
    }

    @Test
    void aStackWithoutABarrelSlotIsKept() {
        BarrelMigration.Plan<Integer, String> plan = BarrelMigration.plan(Map.of(19, "slag"), slot -> -1, index -> true);
        assertThat(plan.moved()).isEmpty();
        assertThat(plan.kept()).containsExactly(Map.entry(19, "slag"));
    }

    @Test
    void ofTwoStacksForOneSlotTheFirstMovesAndTheSecondStays() {
        Map<Integer, String> stored = new LinkedHashMap<>();
        stored.put(4, "first");
        stored.put(5, "second");
        BarrelMigration.Plan<Integer, String> plan = BarrelMigration.plan(stored, slot -> 0, index -> true);
        assertThat(plan.moved()).containsExactly(Map.entry(0, "first"));
        assertThat(plan.kept()).containsExactly(Map.entry(5, "second"));
    }

    @Test
    void noStoredStacksMeansNothingToMove() {
        BarrelMigration.Plan<Integer, String> plan = BarrelMigration.plan(Map.of(), slot -> slot, index -> true);
        assertThat(plan.moved()).isEmpty();
        assertThat(plan.kept()).isEmpty();
    }

    @Test
    void theOlderVersionsHostsAreKnownForEveryShippedMachine() {
        for (Machine machine : ShippedContent.load().processes().machines()) {
            boolean known = BarrelMigration.legacyHost(machine.id()).isPresent() || BarrelMigration.addedLater(machine.id());
            assertThat(known).as(machine.id()).isTrue();
        }
        assertThat(BarrelMigration.legacyHost("roaster")).contains("FURNACE");
        assertThat(BarrelMigration.legacyHost("research_console")).contains("BOOKSHELF");
    }

    @Test
    void aBlockThatWasNeverALabToolHasNoLegacyHost() {
        assertThat(BarrelMigration.legacyHost("petroleum_drill_1")).isEmpty();
    }

    @Test
    void anOreDrillStoodOnADropperBeforeItsBarrel() {
        for (int level = 1; level <= 5; level++) {
            assertThat(BarrelMigration.legacyHost("ore_drill_" + level)).contains("DROPPER");
        }
    }
}
