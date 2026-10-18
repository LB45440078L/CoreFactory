package io.moretntplus.spigot.listener;

import io.moretntplus.core.MoreTntPlusCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

/**
 * Pistons cannot move a placed TNT away from its registry entry and display, so moves that would carry one are
 * cancelled when {@code protection.protect-from-pistons} is on.
 */
public final class ProtectionListener implements Listener {

    private final MoreTntPlusCore core;

    public ProtectionListener(MoreTntPlusCore core) {
        this.core = core;
    }

    @EventHandler(ignoreCancelled = true)
    public void onExtend(BlockPistonExtendEvent event) {
        if (core.settings().protectFromPistons() && moves(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRetract(BlockPistonRetractEvent event) {
        if (core.settings().protectFromPistons() && moves(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    private boolean moves(java.util.List<org.bukkit.block.Block> blocks) {
        return blocks.stream().anyMatch(block -> core.placedAt(block.getLocation()).isPresent());
    }
}
