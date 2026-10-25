package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.energy.Pos;
import io.corefactory.paper.chunks.ChunkKey;
import io.corefactory.paper.chunks.ChunkTickets;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArtilleryChunksTest {

    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000003");

    /** A port that keeps the tickets in a set. */
    private static final class FakePort implements ChunkTickets.Port {
        private final Set<ChunkKey> placed = new HashSet<>();

        @Override
        public boolean place(ChunkKey key) {
            placed.add(key);
            return true;
        }

        @Override
        public void remove(ChunkKey key) {
            placed.remove(key);
        }
    }

    private final FakePort port = new FakePort();
    private final ChunkTickets tickets = new ChunkTickets(port);

    private static CommandCenter center(String id, double aimX, double aimZ) {
        CommandCenter center = new CommandCenter(id, OWNER, WORLD, new Pos(0, 64, 0));
        center.setAim(aimX, aimZ);
        return center;
    }

    @Test
    void theTargetAreaIsTheAimsChunkAndItsEightNeighbours() {
        Set<ChunkKey> area = ArtilleryChunks.targetArea(WORLD, 8.5, 8.5);

        assertThat(area).hasSize(9);
        assertThat(area).containsExactlyInAnyOrder(
                new ChunkKey(WORLD, -1, -1), new ChunkKey(WORLD, 0, -1), new ChunkKey(WORLD, 1, -1),
                new ChunkKey(WORLD, -1, 0), new ChunkKey(WORLD, 0, 0), new ChunkKey(WORLD, 1, 0),
                new ChunkKey(WORLD, -1, 1), new ChunkKey(WORLD, 0, 1), new ChunkKey(WORLD, 1, 1));
    }

    @Test
    void theTargetAreaFloorsNegativeAimsAsTheFiringRulesDo() {
        // Block -1 is in chunk -1, so an aim at x -0.5 is in chunk -1 and its area starts at chunk -2.
        Set<ChunkKey> area = ArtilleryChunks.targetArea(WORLD, -0.5, 17.9);

        assertThat(area).hasSize(9);
        assertThat(area).contains(new ChunkKey(WORLD, -1, 1), new ChunkKey(WORLD, -2, 0), new ChunkKey(WORLD, 0, 2));
        assertThat(area).doesNotContain(new ChunkKey(WORLD, -3, 1), new ChunkKey(WORLD, -1, 3));
    }

    @Test
    void aTargetHoldsItsAreaWhileItIsSetAndReleasesItWhenCleared() {
        ArtilleryChunks chunks = new ArtilleryChunks(tickets, true);
        CommandCenter center = center("a", 8.5, 8.5);

        chunks.holdTarget(center);
        assertThat(port.placed).hasSize(9);

        center.setAim(Double.NaN, Double.NaN);
        chunks.holdTarget(center);
        assertThat(port.placed).isEmpty();
        assertThat(tickets.heldChunks()).isZero();
    }

    @Test
    void aNewAimReplacesTheAreaOfTheOldOne() {
        ArtilleryChunks chunks = new ArtilleryChunks(tickets, true);
        CommandCenter center = center("a", 8.5, 8.5);
        chunks.holdTarget(center);

        center.setAim(8.5 + 16 * 5, 8.5);
        chunks.holdTarget(center);

        assertThat(port.placed).hasSize(9);
        assertThat(port.placed).containsExactlyInAnyOrderElementsOf(ArtilleryChunks.targetArea(WORLD, 8.5 + 16 * 5, 8.5));
        assertThat(chunks.isHeld(new ChunkKey(WORLD, 0, 0))).isFalse();
    }

    @Test
    void releasingAClosedTargetDropsItsAreaOnlyOnce() {
        ArtilleryChunks chunks = new ArtilleryChunks(tickets, true);
        CommandCenter center = center("a", 8.5, 8.5);
        chunks.holdTarget(center);

        chunks.releaseTarget("a");
        chunks.releaseTarget("a");

        assertThat(port.placed).isEmpty();
    }

    @Test
    void twoCentersHoldTheirOwnAreasAndAnOverlapKeepsOneTicket() {
        ArtilleryChunks chunks = new ArtilleryChunks(tickets, true);
        chunks.holdTarget(center("a", 8.5, 8.5));
        chunks.holdTarget(center("b", 8.5 + 16, 8.5));

        // The two areas share the chunks of x 0 and 1 in the middle column, so 9 + 9 - 6 chunks are held.
        assertThat(port.placed).hasSize(12);

        chunks.releaseTarget("a");
        assertThat(port.placed).hasSize(9);
        assertThat(port.placed).containsExactlyInAnyOrderElementsOf(ArtilleryChunks.targetArea(WORLD, 8.5 + 16, 8.5));
    }

    @Test
    void withKeepLoadedOffNoTargetIsHeld() {
        ArtilleryChunks chunks = new ArtilleryChunks(tickets, false);
        chunks.holdTarget(center("a", 8.5, 8.5));

        assertThat(port.placed).isEmpty();
    }

    @Test
    void thePiecesHoldEveryChunkTheyAreGivenWithNoCap() {
        ArtilleryChunks chunks = new ArtilleryChunks(tickets, true);
        Set<ChunkKey> many = new HashSet<>();
        for (int x = 0; x < 300; x++) {
            many.add(new ChunkKey(WORLD, x, 0));
        }

        chunks.holdPieces(many);

        assertThat(port.placed).hasSize(300);
        assertThat(tickets.chunksOf(ArtilleryChunks.PIECES)).hasSize(300);
    }

    @Test
    void thePiecesHoldOnlyTheChunksNowGiven() {
        ArtilleryChunks chunks = new ArtilleryChunks(tickets, true);
        chunks.holdPieces(Set.of(new ChunkKey(WORLD, 0, 0), new ChunkKey(WORLD, 1, 0)));
        chunks.holdPieces(Set.of(new ChunkKey(WORLD, 1, 0)));

        assertThat(port.placed).containsExactly(new ChunkKey(WORLD, 1, 0));
    }

    @Test
    void withKeepLoadedOffThePiecesHoldNothing() {
        ArtilleryChunks chunks = new ArtilleryChunks(tickets, false);
        chunks.holdPieces(Set.of(new ChunkKey(WORLD, 0, 0)));

        assertThat(port.placed).isEmpty();
    }

    @Test
    void aPieceChunkIsHeldWhileThePiecesOwnThatChunk() {
        ArtilleryChunks chunks = new ArtilleryChunks(tickets, true);
        ArtilleryDevice piece = new ArtilleryDevice("p", OWNER, WORLD, new Pos(40, 70, -3), ArtilleryLevel.I);

        chunks.holdPieces(Set.of(ArtilleryChunks.pieceChunk(piece)));

        assertThat(chunks.isHeld(new ChunkKey(WORLD, 2, -1))).isTrue();
        assertThat(chunks.isHeld(new ChunkKey(WORLD, 0, 0))).isFalse();
    }
}
