package io.corefactory.spigot.machine;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.drill.DrillSelection;
import io.corefactory.core.drill.DrillText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The lines of a drill's resources menu: one per deposit in its square that the drill may mine, with the ore's icon, the amounts
 * left in the deposit in the lore, and the toggle of its ore. The menu turns a line into items. Pure, so the lines are tested
 * without a server.
 */
public final class DrillResourceRows {

    /** The icon of an exhausted deposit: grey glass, not a barrier. */
    public static final String EXHAUSTED_ICON = "GRAY_STAINED_GLASS_PANE";

    /** The icon of an ore whose host block is unknown. */
    public static final String DEFAULT_ICON = "STONE";

    /**
     * What the menu shows for an ore.
     *
     * @param name the display name, with colour codes from ores.yml
     * @param host the material name of the ore's host block, or null when unknown
     */
    public record OreLabel(String name, String host) {
    }

    /** The state of an ore's toggle: mined, or ignored. */
    public enum Toggle {
        MINING,
        IGNORING
    }

    /**
     * One line of the menu.
     *
     * @param depositId the deposit's id
     * @param oreId     the ore id, which the toggle flips for every deposit of that ore
     * @param name      the display name, with colour codes
     * @param icon      the material name of the icon
     * @param lore      the icon's lore lines, with colour codes
     * @param toggle    the state of the toggle beside the icon
     * @param exhausted true when nothing is left in the deposit
     */
    public record Line(long depositId, String oreId, String name, String icon, List<String> lore, Toggle toggle,
                       boolean exhausted) {
    }

    private DrillResourceRows() {
    }

    /**
     * The lines, one per deposit and in the order given.
     *
     * @param deposits  the deposits in the drill's square that its kind may mine
     * @param labels    the name and host of every ore; an ore without a label is shown by its id
     * @param selection the drill's selection, which sets each toggle
     */
    public static List<Line> of(List<Deposit> deposits, Map<String, OreLabel> labels, DrillSelection selection) {
        List<Line> lines = new ArrayList<>();
        for (Deposit deposit : deposits) {
            OreLabel label = labels.getOrDefault(deposit.ore(), new OreLabel(deposit.ore(), null));
            boolean exhausted = deposit.remaining() <= 0;
            String name = label.name();
            String icon = label.host() == null ? DEFAULT_ICON : label.host();
            List<String> lore = new ArrayList<>(List.of(
                    "&7Left &f" + DrillText.remaining(deposit),
                    "&7At &f" + deposit.centreX() + " " + deposit.centreZ()));
            if (exhausted) {
                name = "&7" + plain(name);
                icon = EXHAUSTED_ICON;
                lore.add("&8empty");
            }
            Toggle toggle = selection.isMining(deposit.ore()) ? Toggle.MINING : Toggle.IGNORING;
            lines.add(new Line(deposit.id(), deposit.ore(), name, icon, List.copyOf(lore), toggle, exhausted));
        }
        return List.copyOf(lines);
    }

    /** The text with its colour codes removed. */
    static String plain(String text) {
        return text.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }
}
