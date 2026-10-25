package io.corefactory.paper.chunks;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkTicketsTest {

    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final ChunkKey A = new ChunkKey(WORLD, 0, 0);
    private static final ChunkKey B = new ChunkKey(WORLD, 1, 0);
    private static final ChunkKey C = new ChunkKey(WORLD, 2, 0);
    private static final String ARTILLERY = "shot 1";
    private static final String OTHER_SHOT = "shot 2";

    /** A port that keeps the tickets in a set, and counts every call to place. */
    private static final class FakePort implements ChunkTickets.Port {
        private final Set<ChunkKey> placed = new HashSet<>();
        private final List<ChunkKey> removals = new ArrayList<>();
        private int placeCalls;
        private boolean worldLoaded = true;

        @Override
        public boolean place(ChunkKey key) {
            placeCalls++;
            if (!worldLoaded) {
                return false;
            }
            placed.add(key);
            return true;
        }

        @Override
        public void remove(ChunkKey key) {
            removals.add(key);
            placed.remove(key);
        }
    }

    private static final Logger LOG = Logger.getLogger("ChunkTicketsTest");

    private final FakePort port = new FakePort();
    private final ChunkTickets tickets = new ChunkTickets(port);

    private ChunkKeeper keeper(int maxForced) {
        return new ChunkKeeper(tickets, world -> port.worldLoaded, LOG, true, maxForced);
    }

    @Test
    void addingPlacesATicketOnEachChunkAndReleasingRemovesIt() {
        tickets.hold(ARTILLERY, List.of(A, B));

        assertThat(port.placed).containsExactlyInAnyOrder(A, B);
        assertThat(tickets.chunksOf(ARTILLERY)).containsExactlyInAnyOrder(A, B);

        tickets.release(ARTILLERY);

        assertThat(port.removals).containsExactlyInAnyOrder(A, B);
        assertThat(tickets.heldChunks()).isZero();
        assertThat(tickets.chunksOf(ARTILLERY)).isEmpty();
    }

    @Test
    void releasingTwiceDoesNothingTheSecondTime() {
        tickets.hold(ARTILLERY, List.of(A));
        tickets.release(ARTILLERY);
        tickets.release(ARTILLERY);
        tickets.release(null);

        assertThat(port.removals).containsExactly(A);
    }

    @Test
    void twoOwnersHoldOneChunkWithOneTicket() {
        tickets.hold(ARTILLERY, List.of(A));
        tickets.hold(ChunkKeeper.OWNER, List.of(A));

        assertThat(port.placeCalls).isEqualTo(1);
        assertThat(port.placed).containsExactly(A);
    }

    @Test
    void releasingOneOwnerKeepsTheTicketUntilTheLastOneLetsGo() {
        tickets.hold(ARTILLERY, List.of(A));
        tickets.hold(ChunkKeeper.OWNER, List.of(A));

        tickets.release(ARTILLERY);
        assertThat(port.removals).isEmpty();
        assertThat(port.placed).containsExactly(A);

        tickets.release(ChunkKeeper.OWNER);
        assertThat(port.removals).containsExactly(A);
        assertThat(port.placed).isEmpty();
    }

    @Test
    void aChunkHeldByTwoOwnersIsRemovedOnlyWhenTheLastOneLetsGo() {
        tickets.hold(ARTILLERY, List.of(A, B));
        tickets.hold(OTHER_SHOT, List.of(B, C));
        assertThat(port.placeCalls).isEqualTo(3);

        tickets.release(ARTILLERY);
        assertThat(port.removals).containsExactly(A);
        assertThat(port.placed).containsExactlyInAnyOrder(B, C);

        tickets.release(OTHER_SHOT);
        assertThat(port.removals).containsExactlyInAnyOrder(A, B, C);
        assertThat(port.placed).isEmpty();
    }

    @Test
    void theSameChunkHeldTwiceByOneOwnerIsPlacedOnce() {
        tickets.hold(ARTILLERY, List.of(A));
        tickets.hold(ARTILLERY, List.of(A, A));

        assertThat(port.placeCalls).isEqualTo(1);
        tickets.release(ARTILLERY);
        assertThat(port.removals).containsExactly(A);
    }

    @Test
    void aChunkWhoseWorldIsNotLoadedIsReportedAndPlacedOnALaterCall() {
        port.worldLoaded = false;
        Set<ChunkKey> unticketed = tickets.hold(ARTILLERY, List.of(A));
        assertThat(unticketed).containsExactly(A);
        assertThat(port.placed).isEmpty();

        port.worldLoaded = true;
        assertThat(tickets.hold(ARTILLERY, List.of(A))).isEmpty();
        assertThat(port.placed).containsExactly(A);

        tickets.release(ARTILLERY);
        assertThat(port.removals).containsExactly(A);
    }

    @Test
    void ensurePlacesAgainATicketThatWentMissing() {
        tickets.hold(ARTILLERY, List.of(A, B));
        port.placed.remove(A);

        tickets.ensureAll();

        assertThat(port.placed).containsExactlyInAnyOrder(A, B);
        tickets.release(ARTILLERY);
        assertThat(port.removals).containsExactlyInAnyOrder(A, B);
    }

    @Test
    void ensureOfOneOwnerLeavesOtherOwnersAlone() {
        tickets.hold(ARTILLERY, List.of(A));
        tickets.hold(OTHER_SHOT, List.of(B));
        port.placed.remove(A);
        port.placed.remove(B);

        tickets.ensure(ARTILLERY);

        assertThat(port.placed).containsExactly(A);
    }

    @Test
    void setOwnedDropsTheChunksNoLongerGivenAndKeepsTheRest() {
        tickets.hold(ARTILLERY, List.of(A, B));

        Set<ChunkKey> unticketed = tickets.setOwned(ARTILLERY, Set.of(B, C));

        assertThat(unticketed).isEmpty();
        assertThat(port.removals).containsExactly(A);
        assertThat(port.placed).containsExactlyInAnyOrder(B, C);
        assertThat(tickets.chunksOf(ARTILLERY)).containsExactlyInAnyOrder(B, C);
    }

    @Test
    void keeperDoesNotRemoveAChunkTheArtilleryStillHolds() {
        tickets.hold(ARTILLERY, List.of(A));
        keeper(10).reconcile(Set.of(A));

        keeper(10).reconcile(Set.of());

        assertThat(port.removals).isEmpty();
        assertThat(port.placed).containsExactly(A);
    }

    @Test
    void keeperDoesNotPlaceAgainAChunkTheArtilleryHolds() {
        tickets.hold(ARTILLERY, List.of(A));

        keeper(10).reconcile(Set.of(A));
        keeper(10).reconcile(Set.of(A));

        assertThat(port.placeCalls).isEqualTo(1);
        assertThat(port.placed).containsExactly(A);
    }

    @Test
    void keeperCapCountsOnlyTheKeepersOwnChunks() {
        tickets.hold(ARTILLERY, List.of(A));

        keeper(1).reconcile(Set.of(B, C));

        assertThat(port.placed).containsExactlyInAnyOrder(A, B);
        assertThat(tickets.chunksOf(ChunkKeeper.OWNER)).containsExactly(B);
    }

    @Test
    void anOwnerHeldOutsideTheKeeperIsNotCutByItsCap() {
        tickets.setOwned(ARTILLERY, Set.of(A, B, C));

        keeper(1).reconcile(Set.of(new ChunkKey(WORLD, -5, 0), new ChunkKey(WORLD, -4, 0)));

        assertThat(tickets.chunksOf(ARTILLERY)).containsExactlyInAnyOrder(A, B, C);
        assertThat(port.placed).contains(A, B, C);
        assertThat(port.removals).doesNotContain(A, B, C);
    }

    @Test
    void isHeldReportsAChunkAnyOwnerHoldsWithOrWithoutItsTicket() {
        assertThat(tickets.isHeld(A)).isFalse();

        tickets.hold(ARTILLERY, List.of(A));
        assertThat(tickets.isHeld(A)).isTrue();

        port.worldLoaded = false;
        tickets.hold(OTHER_SHOT, List.of(B));
        assertThat(tickets.isHeld(B)).isTrue();

        tickets.release(ARTILLERY);
        assertThat(tickets.isHeld(A)).isFalse();
    }

    @Test
    void keeperReleaseLeavesTheArtilleryTicket() {
        tickets.hold(ARTILLERY, List.of(A));
        keeper(10).reconcile(Set.of(A, B));

        keeper(10).release();

        assertThat(port.removals).containsExactly(B);
        assertThat(port.placed).containsExactly(A);
    }

    @Test
    void keeperSkipsAChunkOfAnUnloadedWorldAndKeepsItOnceTheWorldIsBack() {
        port.worldLoaded = false;
        keeper(10).reconcile(Set.of(A));
        assertThat(port.placed).isEmpty();

        port.worldLoaded = true;
        keeper(10).reconcile(Set.of(A));
        assertThat(port.placed).containsExactly(A);
    }

    @Test
    void keeperRetriesAChunkTheBookReportedAsUnticketed() {
        ChunkKeeper keeper = new ChunkKeeper(tickets, world -> true, LOG, true, 10);
        port.worldLoaded = false;
        keeper.reconcile(Set.of(A));
        assertThat(port.placed).isEmpty();
        assertThat(tickets.chunksOf(ChunkKeeper.OWNER)).containsExactly(A);

        port.worldLoaded = true;
        keeper.reconcile(Set.of(A));
        assertThat(port.placed).containsExactly(A);
    }

    @Test
    void shutdownReleasesEveryOwnersTickets() {
        tickets.hold(ARTILLERY, List.of(A, B));
        tickets.hold(OTHER_SHOT, List.of(C));
        keeper(10).reconcile(Set.of(A));

        tickets.releaseAll();

        assertThat(port.removals).containsExactlyInAnyOrder(A, B, C);
        assertThat(port.placed).isEmpty();
        assertThat(tickets.heldChunks()).isZero();

        tickets.releaseAll();
        assertThat(port.removals).hasSize(3);
    }
}
