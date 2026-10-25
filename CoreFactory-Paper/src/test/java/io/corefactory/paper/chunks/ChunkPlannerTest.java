package io.corefactory.paper.chunks;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkPlannerTest {

    private static final UUID WORLD = new UUID(0L, 1L);
    private static final UUID OTHER = new UUID(0L, 2L);

    private static ChunkKey chunk(UUID world, int x, int z) {
        return new ChunkKey(world, x, z);
    }

    @Test
    void addsOnlyTheChunksThatHaveNoTicketYet() {
        ChunkKey a = chunk(WORLD, 0, 0);
        ChunkKey b = chunk(WORLD, 1, 0);
        ChunkPlanner.Plan plan = ChunkPlanner.plan(Set.of(a), Set.of(a, b), 10);
        assertThat(plan.toTicket()).containsExactly(b);
        assertThat(plan.toRelease()).isEmpty();
        assertThat(plan.skipped()).isZero();
    }

    @Test
    void releasesOnlyTheHeldChunksThatAreNoLongerNeeded() {
        ChunkKey a = chunk(WORLD, 0, 0);
        ChunkKey b = chunk(WORLD, 1, 0);
        ChunkPlanner.Plan plan = ChunkPlanner.plan(Set.of(a, b), Set.of(a), 10);
        assertThat(plan.toTicket()).isEmpty();
        assertThat(plan.toRelease()).containsExactly(b);
        assertThat(plan.skipped()).isZero();
    }

    @Test
    void nothingNeededGivesReleasesOnlyInStableOrder() {
        ChunkKey a = chunk(WORLD, 0, 0);
        ChunkKey b = chunk(WORLD, 1, 0);
        ChunkPlanner.Plan plan = ChunkPlanner.plan(Set.of(b, a), Set.of(), 10);
        assertThat(plan.toTicket()).isEmpty();
        assertThat(plan.toRelease()).containsExactly(a, b);
        assertThat(plan.skipped()).isZero();
    }

    @Test
    void theCapIsRespectedAndTheFirstChunksInOrderAreKept() {
        ChunkKey a = chunk(WORLD, 0, 0);
        ChunkKey b = chunk(WORLD, 0, 1);
        ChunkKey c = chunk(WORLD, 1, 0);
        ChunkKey d = chunk(WORLD, 1, 1);
        ChunkPlanner.Plan plan = ChunkPlanner.plan(Set.of(), Set.of(a, b, c, d), 2);
        assertThat(plan.toTicket()).containsExactly(a, b);
        assertThat(plan.toRelease()).isEmpty();
        assertThat(plan.skipped()).isEqualTo(2);
    }

    @Test
    void aHeldChunkKeepsItsTicketBeforeTheCapFillsWithOthers() {
        ChunkKey a = chunk(WORLD, 0, 0);
        ChunkKey b = chunk(WORLD, 0, 1);
        ChunkKey c = chunk(WORLD, 1, 0);
        ChunkPlanner.Plan plan = ChunkPlanner.plan(Set.of(c), Set.of(a, b, c), 2);
        assertThat(plan.toTicket()).containsExactly(a);
        assertThat(plan.toRelease()).isEmpty();
        assertThat(plan.skipped()).isEqualTo(1);
    }

    @Test
    void theOrderIsStableWhateverTheInsertionOrder() {
        ChunkKey otherWorld = chunk(OTHER, 0, 0);
        ChunkKey east = chunk(WORLD, 1, 0);
        ChunkKey origin = chunk(WORLD, 0, 0);
        ChunkKey south = chunk(WORLD, 0, 1);

        Set<ChunkKey> forward = new LinkedHashSet<>(List.of(otherWorld, east, origin, south));
        Set<ChunkKey> backward = new LinkedHashSet<>(List.of(south, origin, east, otherWorld));

        ChunkPlanner.Plan first = ChunkPlanner.plan(Set.of(), forward, 3);
        ChunkPlanner.Plan second = ChunkPlanner.plan(Set.of(), backward, 3);

        assertThat(first).isEqualTo(second);
        assertThat(first.toTicket()).containsExactly(origin, south, east);
        assertThat(first.skipped()).isEqualTo(1);
    }

    @Test
    void zeroCapKeepsNoChunk() {
        ChunkKey a = chunk(WORLD, 0, 0);
        ChunkKey b = chunk(WORLD, 1, 0);
        ChunkPlanner.Plan plan = ChunkPlanner.plan(Set.of(a), Set.of(a, b), 0);
        assertThat(plan.toTicket()).isEmpty();
        assertThat(plan.toRelease()).containsExactly(a);
        assertThat(plan.skipped()).isEqualTo(2);
    }

    @Test
    void aNegativeCapCountsAsZero() {
        ChunkKey a = chunk(WORLD, 0, 0);
        ChunkPlanner.Plan plan = ChunkPlanner.plan(Set.of(), Set.of(a), -5);
        assertThat(plan.toTicket()).isEmpty();
        assertThat(plan.skipped()).isEqualTo(1);
    }

    @Test
    void aBlockBelongsToTheChunkThatRoundsItsCoordinatesDown() {
        assertThat(ChunkKey.of(WORLD, -1, -16)).isEqualTo(chunk(WORLD, -1, -1));
        assertThat(ChunkKey.of(WORLD, -17, 15)).isEqualTo(chunk(WORLD, -2, 0));
        assertThat(ChunkKey.of(WORLD, 16, 0)).isEqualTo(chunk(WORLD, 1, 0));
    }
}
