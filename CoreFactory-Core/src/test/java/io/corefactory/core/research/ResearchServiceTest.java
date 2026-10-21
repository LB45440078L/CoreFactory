package io.corefactory.core.research;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class ResearchServiceTest {

    private static final Logger LOG = Logger.getLogger("test");

    /** A store that keeps everything in memory. */
    static final class MemoryStore implements ResearchStore {
        final Map<UUID, Set<String>> unlocked = new HashMap<>();
        final Map<UUID, Map<String, Integer>> progress = new HashMap<>();

        @Override
        public Set<String> unlocked(UUID player) {
            return new HashSet<>(unlocked.getOrDefault(player, Set.of()));
        }

        @Override
        public boolean unlock(UUID player, String node, long epochMillis) {
            return unlocked.computeIfAbsent(player, key -> new HashSet<>()).add(node);
        }

        @Override
        public int progress(UUID player, String node) {
            return progress.getOrDefault(player, Map.of()).getOrDefault(node, 0);
        }

        @Override
        public void setProgress(UUID player, String node, int units) {
            progress.computeIfAbsent(player, key -> new HashMap<>()).put(node, units);
        }
    }

    private static ResearchTree tree() {
        ResearchTree.Node basics = new ResearchTree.Node("basics", "Basics", "", List.of(), Map.of(),
                List.of("console"));
        ResearchTree.Node safety = new ResearchTree.Node("safety", "Safety", "", List.of("basics"),
                Map.of("basic", 2), List.of("suit"));
        ResearchTree.Node fission = new ResearchTree.Node("fission", "Fission", "", List.of("safety"),
                Map.of("basic", 1, "nuclear", 1), List.of("bomb"));
        return ResearchTree.of(List.of(basics, safety, fission));
    }

    private final UUID alice = UUID.randomUUID();
    private final MemoryStore store = new MemoryStore();
    private final ResearchService service = new ResearchService(tree(), store, Runnable::run, LOG);

    @Test
    void theFirstNodeIsFinishedAtOnceAndGatesNothingBeyondItsIds() {
        service.load(alice).join();

        assertThat(service.isFinished(alice, "basics")).isTrue();
        assertThat(service.isUnlocked(alice, "console")).isTrue();
        assertThat(service.isUnlocked(alice, "suit")).isFalse();
        assertThat(service.isUnlocked(alice, "anything_free")).isTrue();
    }

    @Test
    void aNodeTakesPacksOnlyUpToItsCostAndSurplusStaysWithThePlayer() {
        service.load(alice).join();

        var first = service.feed(alice, "safety", Map.of("basic", 5));

        assertThat(first.consumed()).containsEntry("basic", 2);
        assertThat(first.finished()).isTrue();
        assertThat(service.isUnlocked(alice, "suit")).isTrue();
    }

    @Test
    void nothingCanBeFedBeforeTheNodeBeforeItIsFinished() {
        service.load(alice).join();

        var result = service.feed(alice, "fission", Map.of("basic", 1, "nuclear", 1));

        assertThat(result.consumed()).isEmpty();
        assertThat(result.finished()).isFalse();
        assertThat(service.isUnlocked(alice, "bomb")).isFalse();
    }

    @Test
    void partialFeedingIsKeptAndPersisted() {
        service.load(alice).join();
        service.feed(alice, "safety", Map.of("basic", 1));

        assertThat(service.isFinished(alice, "safety")).isFalse();
        assertThat(service.progressOf(alice, "safety")).containsEntry("basic", 1);
        assertThat(store.progress(alice, "safety/basic")).isEqualTo(1);
    }

    @Test
    void finishedNodesArePersistedAndComeBackOnLoad() {
        service.load(alice).join();
        service.feed(alice, "safety", Map.of("basic", 2));

        ResearchService again = new ResearchService(tree(), store, Runnable::run, LOG);
        again.load(alice).join();

        assertThat(again.isFinished(alice, "safety")).isTrue();
        assertThat(again.isUnlocked(alice, "suit")).isTrue();
        assertThat(store.unlocked.get(alice)).contains("safety");
    }

    @Test
    void partialProgressSurvivesAReload() {
        service.load(alice).join();
        service.feed(alice, "safety", Map.of("basic", 1));

        ResearchService again = new ResearchService(tree(), store, Runnable::run, LOG);
        again.load(alice).join();
        var finish = again.feed(alice, "safety", Map.of("basic", 1));

        assertThat(finish.finished()).isTrue();
    }

    @Test
    void researchIsPerPlayer() {
        UUID bob = UUID.randomUUID();
        service.load(alice).join();
        service.load(bob).join();
        service.feed(alice, "safety", Map.of("basic", 2));

        assertThat(service.isUnlocked(alice, "suit")).isTrue();
        assertThat(service.isUnlocked(bob, "suit")).isFalse();
    }

    @Test
    void forgetDropsMemoryOnly() {
        service.load(alice).join();
        service.feed(alice, "safety", Map.of("basic", 2));
        service.forget(alice);

        assertThat(service.isUnlocked(alice, "suit")).isFalse();
        assertThat(store.unlocked.get(alice)).contains("safety");
    }

    @Test
    void aFinishListenerIsToldOnceWhenANodeIsFinishedByFeeding() {
        List<String> finished = new ArrayList<>();
        service.onFinished((player, node) -> finished.add(player + "/" + node));
        service.load(alice).join();

        service.feed(alice, "safety", Map.of("basic", 1));
        service.feed(alice, "safety", Map.of("basic", 1));
        service.feed(alice, "safety", Map.of("basic", 1));

        assertThat(finished).containsExactly(alice + "/safety");
    }

    @Test
    void aGrantedNodeDoesNotCallTheFinishListener() {
        List<String> finished = new ArrayList<>();
        service.onFinished((player, node) -> finished.add(node));
        service.load(alice).join();

        service.grant(alice, "safety");

        assertThat(finished).isEmpty();
    }

    @Test
    void nodeForNamesTheNodeThatStillGatesAnId() {
        service.load(alice).join();

        assertThat(service.nodeFor(alice, "bomb")).map(ResearchTree.Node::id).contains("fission");
        assertThat(service.nodeFor(alice, "console")).isEmpty();
    }
}
