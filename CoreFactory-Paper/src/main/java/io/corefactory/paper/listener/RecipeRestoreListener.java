package io.corefactory.paper.listener;

import io.corefactory.core.CoreFactoryCore;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Puts the plugin's recipes back when the server reloads its resources. A reload of datapacks (the {@code /reload} command, or a datapack
 * being enabled or disabled) drops every recipe added through the API, and Paper fires {@link ServerResourcesReloadedEvent} after the reload
 * so that plugins can register them again.
 */
public final class RecipeRestoreListener implements Listener {

    private final CoreFactoryCore core;

    /** @param core the core, whose recipes are registered again when any are missing */
    public RecipeRestoreListener(CoreFactoryCore core) {
        this.core = core;
    }

    @EventHandler
    public void onResourcesReloaded(ServerResourcesReloadedEvent event) {
        core.recipes().restoreIfMissing();
    }
}
