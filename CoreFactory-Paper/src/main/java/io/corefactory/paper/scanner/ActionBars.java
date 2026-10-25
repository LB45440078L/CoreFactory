package io.corefactory.paper.scanner;

import io.corefactory.core.text.Text;
import org.bukkit.entity.Player;

/** Sends a line of coloured text to the action bar of one player. */
final class ActionBars {

    private ActionBars() {
    }

    /**
     * @param player the player who sees the line
     * @param text   the line, with ampersand colour codes or MiniMessage tags
     */
    static void send(Player player, String text) {
        player.sendActionBar(Text.parse(text));
    }
}
