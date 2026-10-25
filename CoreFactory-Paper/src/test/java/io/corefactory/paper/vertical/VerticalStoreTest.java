package io.corefactory.paper.vertical;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.vertical.VerticalDir;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VerticalStoreTest {

    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void aSavedStateReadsBackAsItWasWritten() {
        VerticalStore.State state = new VerticalStore.State(
                List.of(new VerticalStore.Saved("a", OWNER, WORLD, new Pos(4, 70, -3), 2, VerticalDir.DOWN),
                        new VerticalStore.Saved("b", null, WORLD, new Pos(4, 71, -3), 1, VerticalDir.DOWN)),
                List.of(new VerticalStore.SavedItem(WORLD, 4, 70.25, -3, Item.material("IRON_INGOT")),
                        new VerticalStore.SavedItem(WORLD, 4, 71.5, -3, Item.component("copper_wire"))));

        VerticalStore.State read = VerticalStore.decode(VerticalStore.encode(state), warning -> {
        });

        assertThat(read).isEqualTo(state);
    }

    @Test
    void anEntryThatIsNotValidIsSkippedAndReported() {
        List<String> warnings = new ArrayList<>();
        String text = "version: 1\n"
                + "belts:\n"
                + "  0:\n"
                + "    id: good\n"
                + "    world: " + WORLD + "\n"
                + "    x: 1\n    y: 64\n    z: 2\n    level: 3\n    direction: UP\n"
                + "  1:\n"
                + "    id: bad-level\n"
                + "    world: " + WORLD + "\n"
                + "    x: 1\n    y: 64\n    z: 2\n    level: 9\n    direction: UP\n"
                + "  2:\n"
                + "    id: bad-direction\n"
                + "    world: " + WORLD + "\n"
                + "    level: 1\n    direction: SIDEWAYS\n";

        VerticalStore.State read = VerticalStore.decode(text, warnings::add);

        assertThat(read.belts()).hasSize(1);
        assertThat(read.belts().get(0).id()).isEqualTo("good");
        assertThat(warnings).hasSize(2);
    }

    @Test
    void aMissingFileIsAnEmptyState() {
        VerticalStore store = new VerticalStore(new java.io.File("does-not-exist-vertical.yml"),
                java.util.logging.Logger.getLogger("vertical-store-test"));

        assertThat(store.read()).isEqualTo(VerticalStore.State.empty());
    }
}
