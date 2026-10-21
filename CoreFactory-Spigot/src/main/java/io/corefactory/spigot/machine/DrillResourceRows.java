package io.corefactory.spigot.machine;

import io.corefactory.core.drill.DrillReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The lines of a drill's resources menu, built from its {@link DrillReport}. Each line names its ore, the material of its icon,
 * the lore, and the state of its toggle; the menu turns a line into items. Pure, so the rows are tested without a server.
 */
public final class DrillResourceRows {

    /** The icon of an exhausted ore: grey glass, not a barrier. */
    public static final String EXHAUSTED_ICON = "GRAY_STAINED_GLASS_PANE";

    /** The icon of an ore whose host block is unknown. */
    public static final String DEFAULT_ICON = "STONE";

    /**
     * What the menu shows for an ore.
     *
     * @param name   the display name, with colour codes from ores.yml
     * @param host   the material name of the ore's host block, or null when unknown
     * @param volume true for a deposit, whose counts are volumes rather than blocks
     */
    public record OreLabel(String name, String host, boolean volume) {
    }

    /** The state of an ore's toggle: mined, or ignored. */
    public enum Toggle {
        MINING,
        IGNORING
    }

    /**
     * One row of the menu.
     *
     * @param oreId     the ore id
     * @param name      the display name, with colour codes
     * @param icon      the material name of the icon
     * @param lore      the icon's lore lines, with colour codes
     * @param toggle    the state of the toggle beside the icon
     * @param exhausted true when the ore is mined but nothing of it is left in range
     */
    public record Line(String oreId, String name, String icon, List<String> lore, Toggle toggle, boolean exhausted) {
    }

    private DrillResourceRows() {
    }

    /**
     * The lines, one per report row and in the same order.
     *
     * @param rows   the report's rows
     * @param labels the name, host and kind of every ore; an ore without a label is shown by its id
     */
    public static List<Line> of(List<DrillReport.Row> rows, Map<String, OreLabel> labels) {
        List<Line> lines = new ArrayList<>();
        for (DrillReport.Row row : rows) {
            OreLabel label = labels.getOrDefault(row.oreId(), new OreLabel(row.oreId(), null, false));
            String unit = label.volume() ? " units" : "";
            List<String> lore = new ArrayList<>(List.of(
                    "&7In range now: &f" + row.inRange() + unit,
                    "&7Mined by this drill: &f" + row.minedByDrill() + unit,
                    "&7Total in range: &f" + row.totalInRange() + unit));
            String name = label.name();
            String icon = label.host() == null ? DEFAULT_ICON : label.host();
            if (row.exhausted()) {
                name = "&7" + plain(name);
                icon = EXHAUSTED_ICON;
                lore.add("&8exhausted in range");
            }
            lines.add(new Line(row.oreId(), name, icon, List.copyOf(lore),
                    row.selected() ? Toggle.MINING : Toggle.IGNORING, row.exhausted()));
        }
        return List.copyOf(lines);
    }

    /** The text with its colour codes removed. */
    static String plain(String text) {
        return text.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }
}
