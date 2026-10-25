package io.corefactory.paper.research;

import io.corefactory.core.research.ResearchTree.Node;
import io.corefactory.core.text.Palette;
import io.corefactory.core.text.Text;
import net.kyori.adventure.text.Component;

/**
 * The messages that say a research node is needed. The node's name is shown in the value colour, and hovering over it shows what the node
 * unlocks, so a player learns what a refusal is about without opening a menu.
 */
public final class ResearchHints {

    private ResearchHints() {
    }

    /**
     * {@code Research 'Name' is needed.}, with the node's description on hover.
     *
     * @param node the node the player has not finished
     */
    public static Component needed(Node node) {
        return Text.parse("&cResearch '").append(named(node)).append(Text.parse("&c' is needed."));
    }

    /**
     * {@code Requires research: Name. Finish the earlier nodes at a Research Console.}, with the node's description on hover.
     *
     * @param node the node the player has not finished
     */
    public static Component requires(Node node) {
        return Text.parse("&cRequires research: ").append(named(node))
                .append(Text.parse("&c. Finish the earlier nodes at a Research Console."));
    }

    /**
     * The node's name, coloured by the colour code the name carries in research.yml (such as {@code &b}), and by the value colour when it
     * carries none. A name is parsed, not written as it is, so its codes are never shown as text.
     */
    private static Component named(Node node) {
        Component name = Component.empty().color(Palette.VALUE).append(Text.parse(node.name()));
        Component tip = Component.text(Text.plain(Text.parse(node.name())), Palette.RESEARCH);
        String description = node.description() == null ? "" : node.description();
        if (!description.isEmpty()) {
            tip = tip.append(Component.newline()).append(Component.text(description, Palette.MUTED));
        }
        return Text.hover(name, tip);
    }
}
