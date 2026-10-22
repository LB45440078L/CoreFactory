package io.corefactory.spigot.scanner;

import io.corefactory.core.config.Messages;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/** Sends a line of ampersand-coloured text to the action bar of one player. */
final class ActionBars {

    private ActionBars() {
    }

    /**
     * @param player the player who sees the line
     * @param text   the line, with ampersand colour codes
     */
    static void send(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(TextComponent.fromLegacyText(Messages.color(text))));
    }
}
