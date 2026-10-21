package io.corefactory.core.radiation;

import io.corefactory.core.item.ItemFactory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * The Geiger counter: a held tool that reads the radiation where its holder stands. The readout is a line for the
 * action bar, with the dose rate in the unit a counter uses, its colour set by severity, the kind of radiation, and
 * the dose the holder has taken.
 */
public final class GeigerCounter {

    /** The component id of the counter in items.yml. */
    public static final String ID = "geiger_counter";

    private final ItemFactory items;

    public GeigerCounter(ItemFactory items) {
        this.items = items;
    }

    /** True when the player holds a Geiger counter in either hand. */
    public boolean isHeld(Player player) {
        return isCounter(player.getInventory().getItemInMainHand()) || isCounter(player.getInventory().getItemInOffHand());
    }

    /**
     * The action bar line for a player, in {@code &} colour codes.
     *
     * @param tick the current server tick, for the zones' decay
     */
    public String readout(Player player, Fallout fallout, long tick) {
        double rate = fallout.rateAt(player.getLocation(), tick);
        double shown = Math.max(rate, RadiationModel.BACKGROUND_SV_PER_SECOND);
        String colour = RadiationModel.severityColour(RadiationModel.mSvPerHour(shown));
        String type = rate > 0 ? "Gamma and beta (fallout)" : "Natural background";
        double dose = fallout.doseOf(player.getUniqueId());
        return colour + "Rate " + RadiationModel.formatRate(shown)
                + " &8| &7" + type
                + " &8| &7Dose &f" + String.format(Locale.ROOT, "%.3f Sv", dose);
    }

    private boolean isCounter(ItemStack item) {
        return items.componentId(item).filter(ID::equals).isPresent();
    }
}
