package io.corefactory.spigot.machine;

import io.corefactory.core.geology.OreDefinition;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Opens the resources menu of a drill and keeps the open ones current. The menus are read again on a timer, so the deposits
 * and their amounts follow the drill as it mines.
 */
public final class DrillResources {

    private final Map<String, OreDefinition> ores;
    private final Runnable save;
    private final List<DrillResourcesMenu> open = new ArrayList<>();

    /**
     * @param ores the ore definitions by id
     * @param save writes the machines file, after a player toggles an ore
     */
    public DrillResources(Map<String, OreDefinition> ores, Runnable save) {
        this.ores = Map.copyOf(ores);
        this.save = save;
    }

    /** Opens the resources menu of a drill for a player. */
    public void open(PlacedMachine placed, Player player) {
        DrillResourcesMenu menu = new DrillResourcesMenu(placed, ores, save);
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
