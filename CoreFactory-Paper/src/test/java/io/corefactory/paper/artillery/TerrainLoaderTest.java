package io.corefactory.paper.artillery;

import io.corefactory.paper.artillery.TerrainCache.ChunkId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the terrain loader against a source that holds each request until the test answers it: an explored chunk is read without generation,
 * an unexplored one is generated only within the budget, a finished chunk replaces its loading tile, and the requests that go stale or whose
 * world unloads give their places back.
 */
class TerrainLoaderTest {

    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final int SEEN = 37;

    /** A request the source holds until the test answers it. */
    private record Request(ChunkId id, boolean generate, Consumer<byte[]> done) {
    }

    /** A source that records every request, and keeps the open ones until they are answered. */
    private static final class Source implements TerrainLoader.ChunkSource {
        final List<Request> open = new ArrayList<>();
        final List<Request> all = new ArrayList<>();

        @Override
        public void fetch(ChunkId id, boolean generate, Consumer<byte[]> done) {
            Request request = new Request(id, generate, done);
            open.add(request);
            all.add(request);
        }

        long openReads() {
            return open.stream().filter(r -> !r.generate()).count();
        }

        long openGenerations() {
            return open.stream().filter(Request::generate).count();
        }

        /** Answers the first open request with the given terrain, or null for a chunk that is not generated. */
        void answer(byte[] terrain) {
            Request request = open.remove(0);
            request.done().accept(terrain);
        }
    }

    private static ChunkId chunk(int x, int z) {
        return new ChunkId(WORLD, x, z);
    }

    /** Terrain whose column at index i has colour i, so a column can be told apart. */
    private static byte[] terrain() {
        byte[] colours = new byte[TerrainCache.COLUMNS];
        for (int i = 0; i < colours.length; i++) {
            colours[i] = (byte) i;
        }
        return colours;
    }

    @Test
    void anExploredChunkIsReadWithoutGeneration() {
        Source source = new Source();
        TerrainLoader loader = new TerrainLoader(source, new TerrainCache(16), new TerrainBudget(8, 4));
        ChunkId id = chunk(0, 0);

        assertThat(loader.want(id, 0, 0)).isTrue();
        loader.tick(0);
        assertThat(source.all).hasSize(1);
        assertThat(source.all.get(0).generate()).isFalse();

        source.answer(terrain());
        assertThat(loader.want(id, 0, 1)).isFalse();
        loader.tick(1);
        assertThat(source.all).hasSize(1);
        assertThat(loader.pending()).isZero();
    }

    @Test
    void anUnexploredChunkIsGeneratedOnlyAfterItsReadAndWithinTheBudget() {
        Source source = new Source();
        TerrainLoader loader = new TerrainLoader(source, new TerrainCache(64), new TerrainBudget(8, 4));
        for (int i = 0; i < 10; i++) {
            loader.want(chunk(i, 0), i, 0);
        }

        loader.tick(0);
        assertThat(source.openReads()).isEqualTo(8);
        assertThat(source.openGenerations()).isZero();

        for (int i = 0; i < 8; i++) {
            source.answer(null);
        }
        loader.tick(0);
        assertThat(source.openGenerations()).isEqualTo(4);
        assertThat(source.openReads()).isEqualTo(2);
    }

    @Test
    void theBudgetHoldsAcrossAWholeRunOfUnexploredChunks() {
        Source source = new Source();
        TerrainLoader loader = new TerrainLoader(source, new TerrainCache(64), new TerrainBudget(8, 4));
        for (int i = 0; i < 10; i++) {
            loader.want(chunk(i, 0), i, 0);
        }
        int tick = 0;
        while (loader.pending() > 0 && tick < 100) {
            for (int i = 0; i < 10; i++) {
                loader.want(chunk(i, 0), i, tick);
            }
            loader.tick(tick);
            assertThat(source.openReads()).isLessThanOrEqualTo(8);
            assertThat(source.openGenerations()).isLessThanOrEqualTo(4);
            if (!source.open.isEmpty()) {
                Request next = source.open.get(0);
                source.answer(next.generate() ? terrain() : null);
            }
            tick++;
        }

        assertThat(loader.pending()).isZero();
        assertThat(source.all.stream().filter(Request::generate)).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(loader.colourAt(chunk(i, 0), 1, 1, 0)).isEqualTo(TerrainCache.index(1, 1));
        }
    }

    @Test
    void aFinishedChunkReplacesItsLoadingTile() {
        Source source = new Source();
        TerrainLoader loader = new TerrainLoader(source, new TerrainCache(16), new TerrainBudget(8, 4));
        ChunkId id = chunk(2, 3);
        loader.want(id, 0, 0);
        loader.tick(0);

        assertThat(loader.colourAt(id, 5, 3, 0)).isEqualTo(LoadingAnimation.colour(5, 3, 0) & 0xFF);

        source.answer(terrain());
        assertThat(loader.colourAt(id, 5, 3, 0)).isEqualTo(TerrainCache.index(5, 3));
    }

    @Test
    void aChunkThatCannotBeGeneratedIsNotAskedForAgain() {
        Source source = new Source();
        TerrainLoader loader = new TerrainLoader(source, new TerrainCache(16), new TerrainBudget(8, 4));
        ChunkId id = chunk(9, 9);
        loader.want(id, 0, 0);
        loader.tick(0);
        source.answer(null);
        loader.tick(0);
        source.answer(null);

        assertThat(loader.colourAt(id, 0, 0, 0)).isEqualTo(-1);
        assertThat(loader.want(id, 0, 1)).isFalse();
        loader.tick(1);
        assertThat(source.all).hasSize(2);
    }

    @Test
    void aWantedChunkThatNoRedrawAsksForLatelyIsDroppedBeforeItStarts() {
        Source source = new Source();
        TerrainLoader loader = new TerrainLoader(source, new TerrainCache(16), new TerrainBudget(1, 1));
        loader.want(chunk(0, 0), 0, 0);
        loader.want(chunk(1, 0), 1, 0);
        loader.tick(0);
        assertThat(source.all).hasSize(1);

        loader.tick(TerrainLoader.WANT_TICKS + 1);

        assertThat(loader.pending()).isEqualTo(1);
        assertThat(source.all).hasSize(1);
    }

    @Test
    void aWorldThatUnloadsCancelsItsRequestsAndGivesTheirPlacesBack() {
        Source source = new Source();
        TerrainLoader loader = new TerrainLoader(source, new TerrainCache(16), new TerrainBudget(1, 1));
        ChunkId first = chunk(0, 0);
        loader.want(first, 0, 0);
        loader.tick(0);
        Request cancelled = source.open.get(0);

        loader.forget(WORLD);
        assertThat(loader.pending()).isZero();
        loader.want(chunk(5, 5), 0, 0);
        loader.tick(0);
        assertThat(source.all).hasSize(2);

        cancelled.done().accept(terrain());
        assertThat(loader.colourAt(first, 5, 3, 0)).isEqualTo(LoadingAnimation.colour(5, 3, 0) & 0xFF);
    }

    @Test
    void aKeptChunkIsReplacedOnRefreshAndAnUnkeptOneIsNot() {
        TerrainCache cache = new TerrainCache(16);
        TerrainLoader loader = new TerrainLoader(new Source(), cache, new TerrainBudget(8, 4));
        ChunkId kept = chunk(1, 1);
        ChunkId unkept = chunk(2, 2);
        cache.put(kept, new byte[TerrainCache.COLUMNS]);

        loader.refresh(kept, () -> {
            byte[] fresh = new byte[TerrainCache.COLUMNS];
            fresh[TerrainCache.index(4, 4)] = (byte) SEEN;
            return fresh;
        });
        loader.refresh(unkept, () -> new byte[TerrainCache.COLUMNS]);

        assertThat(loader.colourAt(kept, 4, 4, 0)).isEqualTo(SEEN);
        assertThat(cache.contains(unkept)).isFalse();
    }
}
