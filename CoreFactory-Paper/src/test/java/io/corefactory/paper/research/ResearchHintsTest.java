package io.corefactory.paper.research;

import io.corefactory.core.research.ResearchTree;
import io.corefactory.core.text.Text;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The research hints show a node's name as its text, never with the colour code it carries in research.yml. */
class ResearchHintsTest {

    private static final ResearchTree.Node COLOURED = new ResearchTree.Node("belts", "&bBelt Duplexes I and II", "", List.of(),
            Map.of(), List.of());

    @Test
    void theRequiresMessageShowsTheNameWithoutItsColourCode() {
        String text = Text.plain(ResearchHints.requires(COLOURED));

        assertThat(text).contains("Belt Duplexes I and II");
        assertThat(text).doesNotContain("&");
    }

    @Test
    void theNeededMessageShowsTheNameWithoutItsColourCode() {
        String text = Text.plain(ResearchHints.needed(COLOURED));

        assertThat(text).contains("Belt Duplexes I and II");
        assertThat(text).doesNotContain("&");
    }

    @Test
    void aNameWithoutACodeIsStillShown() {
        ResearchTree.Node plain = new ResearchTree.Node("artillery", "Artillery", "", List.of(), Map.of(), List.of());
        Component message = ResearchHints.requires(plain);

        assertThat(Text.plain(message)).contains("Artillery");
    }
}
