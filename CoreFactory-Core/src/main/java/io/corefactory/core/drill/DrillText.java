package io.corefactory.core.drill;

import io.corefactory.core.hologram.HologramText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The words a drill shows players: its rate, its power, and the lines of its info item. Pure string work with ampersand
 * colour codes, so it is tested without a server.
 */
public final class DrillText {

    private DrillText() {
    }

    /** The rate of a drill as {@code 0.17/s  (10.0/min)}, with no colour codes. */
    public static String rate(double efficiency, int cycleTicks) {
        return String.format(Locale.ROOT, "%.2f/s  (%.1f/min)", DrillRules.itemsPerSecond(efficiency, cycleTicks),
                DrillRules.itemsPerMinute(efficiency, cycleTicks));
    }

    /** The power of a drill as {@code redstone 100%}: its source and its percentage, with no colour codes. */
    public static String power(DrillPower power) {
        return power.source().label() + " " + HologramText.percent(power.efficiency());
    }

    /**
     * The lines of a drill's info item, with colour codes. The rate reads {@code stopped} unless the drill is running.
     *
     * @param kind   the drill's kind
     * @param spec   the drill's level
     * @param power  the drill's power this cycle
     * @param status the drill's status
     */
    public static List<String> infoLines(DrillKind kind, DrillSpec spec, DrillPower power, DrillStatus status) {
        boolean running = status == DrillStatus.RUNNING;
        List<String> lines = new ArrayList<>(List.of(
                "&7Level &f" + spec.level(),
                "&7Area &f" + spec.area() + "x" + spec.area(),
                "&7Depth &f" + spec.depth() + " blocks",
                "&7Status " + (running ? "&a" : "&c") + status.text(kind, spec),
                "&7Rate " + (running ? "&f" + rate(power.efficiency(), spec.cycleTicks()) : "&cstopped"),
                "&7Power: &f" + power(power),
                "&7Powered by:"));
        for (String source : poweredBy(kind)) {
            lines.add("&8- " + source);
        }
        lines.add("&8The strongest source runs it.");
        return List.copyOf(lines);
    }

    /** What powers a drill of this kind, one short phrase per source, with no colour codes. */
    static List<String> poweredBy(DrillKind kind) {
        if (kind == DrillKind.ORE) {
            return List.of("petroleum in the fuel slot");
        }
        return List.of("a redstone block or wire next to it", "a solar panel next to it", "charcoal in the fuel slot");
    }
}
