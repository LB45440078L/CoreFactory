package io.corefactory.spigot.radiation;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Shows the reading of a Geiger counter on the action bar of every player who holds one. Runs on a timer, a few times
 * a second, so the rate follows the player as they walk.
 */
public final class GeigerDisplay implements Runnable {

    private final CoreFactoryCore core;

    public GeigerDisplay(CoreFactoryCore core) {
        this.core = core;
    }

    @Override
    public void run() {
        long tick = core.currentTick();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!core.radiation().geiger().isHeld(player)) {
                continue;
            }
            String line = core.radiation().geiger().readout(player, core.radiation().fallout(), tick);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(TextComponent.fromLegacyText(Messages.color(line))));
        }
    }
}
