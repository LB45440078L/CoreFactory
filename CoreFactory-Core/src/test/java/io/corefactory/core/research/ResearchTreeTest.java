package io.corefactory.core.research;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResearchTreeTest {

    private static ResearchTree.Node node(String id, List<String> requires, List<String> unlocks) {
        return new ResearchTree.Node(id, id, "", requires, Map.of("pack", 1), unlocks);
    }

    @Test
    void aLineOfNodesIsAccepted() {
        ResearchTree tree = ResearchTree.of(List.of(
                node("a", List.of(), List.of("x")),
                node("b", List.of("a"), List.of("y"))));

        assertThat(tree.nodes()).hasSize(2);
        assertThat(tree.isGated("x")).isTrue();
        assertThat(tree.isGated("free")).isFalse();
        assertThat(tree.gatesOf("y")).containsExactly("b");
    }

    @Test
    void aNodeMayNotRequireOneListedLater() {
        assertThatThrownBy(() -> ResearchTree.of(List.of(
                node("a", List.of("b"), List.of()),
                node("b", List.of(), List.of()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires 'b'");
    }

    @Test
    void aNodeMayNotBeListedTwice() {
        assertThatThrownBy(() -> ResearchTree.of(List.of(
                node("a", List.of(), List.of()),
                node("a", List.of(), List.of()))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anIdGatedByTwoNodesIsUnlockedByEither() {
        ResearchTree tree = ResearchTree.of(List.of(
                node("a", List.of(), List.of("shared")),
                node("b", List.of("a"), List.of("shared"))));

        assertThat(tree.gatesOf("shared")).containsExactly("a", "b");
    }

    @Test
    void theProgressionTakesOnlyWhatIsStillMissing() {
        var result = Progression.apply(Map.of("basic", 5, "nuclear", 2), Map.of("basic", 3),
                Map.of("basic", 10, "nuclear", 1));

        assertThat(result.consumed()).containsEntry("basic", 2).containsEntry("nuclear", 1);
        assertThat(result.finished()).isFalse();
        assertThat(result.done()).containsEntry("basic", 5).containsEntry("nuclear", 1);
    }

    @Test
    void theProgressionFinishesWhenEveryPartIsCovered() {
        var result = Progression.apply(Map.of("basic", 2), Map.of(), Map.of("basic", 2));

        assertThat(result.finished()).isTrue();
        assertThat(result.consumed()).containsEntry("basic", 2);
    }
}
