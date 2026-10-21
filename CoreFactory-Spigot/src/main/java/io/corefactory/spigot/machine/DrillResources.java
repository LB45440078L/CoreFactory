package io.corefactory.spigot.machine;

import io.corefactory.core.ledger.MiningLedger;
import io.corefactory.spigot.geology.GeologyWorker;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Opens the resources menu of a drill and keeps the open ones current. The menus are read again on a timer, so the counts
 * in range follow the drill as it mines.
 */
public final class DrillResources {

    private final GeologyWorker geology;
    private final MiningLedger ledger;
    private final Runnable save;
    private final Logger log;
    private final List<DrillResourcesMenu> open = new ArrayList<>();

    /**
     * @param save writes the machines file, after a player toggles an ore
     */
    public DrillResources(GeologyWorker geology, MiningLedger ledger, Runnable save, Logger log) {
        this.geology = geology;
        this.ledger = ledger;
        this.save = save;
        this.log = log;
    }

    /** Opens the resources menu of a drill for a player. */
    public void open(PlacedMachine placed, Player player) {
        DrillResourcesMenu menu = new DrillResourcesMenu(placed, geology, ledger, save, log);
        open.add(menu);
        player.openInventory(menu.getInventory());
    }

    /** Reads the open menus again, and forgets the ones nobody has open. */
    public void refreshOpen() {
        Iterator<DrillResourcesMenu> menus = open.iterator();
        while (menus.hasNext()) {
            DrillResourcesMenu menu = menus.next();
            if (menu.getInventory().getViewers().isEmpty()) {
                menus.remove();
            } else {
                menu.render();
            }
        }
    }
}
