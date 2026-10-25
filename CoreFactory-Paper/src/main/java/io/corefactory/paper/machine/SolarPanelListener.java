package io.corefactory.paper.machine;

import io.corefactory.core.CoreFactoryCore;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Gives a solar panel back as its component when it is broken. A solar panel is a daylight detector: it is placed as one,
 * and it is not a machine, so it has no menu and no record. Breaking any daylight detector returns the solar panel item.
 */
public final class SolarPanelListener implements Listener {

    private static final String SOLAR_PANEL = "solar_panel";

    private final CoreFactoryCore core;

    public SolarPanelListener(CoreFactoryCore core) {
        this.core = core;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DAYLIGHT_DETECTOR) {
            return;
        }
        event.setDropItems(false);
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        core.createComponentItem(SOLAR_PANEL, 1).ifPresent(item ->
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item));
    }
}
