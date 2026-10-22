package io.corefactory.core.drill;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.hologram.HologramText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The words a drill shows players: its rate, its power, its deposit, and the lines of its info item. Pure string work with
 * ampersand colour codes, so it is tested without a server.
 */
public final class DrillText {

    private static final long THOUSAND = 1_000L;
    private static final long MILLION = 1_000_000L;
    private static final long TEN_THOUSAND = 10_000L;
    private static final long TEN_MILLION = 10_000_000L;

    private DrillText() {
    }

    /** The rate of a drill as {@code 1.50/s  (90.0/min)}, from its items per second, with no colour codes. */
    public static String rate(double itemsPerSecond) {
        double perSecond = Math.max(0, itemsPerSecond);
        return String.format(Locale.ROOT, "%.2f/s  (%.1f/min)", perSecond, perSecond * 60);
    }

    /** The power of a drill as {@code redstone 100%}: its source and its percentage, with no colour codes. */
    public static String power(DrillPower power) {
        return power.source().label() + " " + HologramText.percent(power.efficiency());
    }

    /**
     * An amount with a unit for large numbers, truncated rather than rounded so a figure never reads past its unit: {@code 850},
     * {@code 4.2k}, {@code 850k}, {@code 1.2M}, {@code 25M}. Negative amounts read as 0.
     */
    public static String amount(long amount) {
        long value = Math.max(0, amount);
        if (value < THOUSAND) {
            return Long.toString(value);
        }
        if (value < TEN_THOUSAND) {
            return tenths(value, THOUSAND) + "k";
        }
        if (value < MILLION) {
            return (value / THOUSAND) + "k";
        }
        if (value < TEN_MILLION) {
            return tenths(value, MILLION) + "M";
        }
        return (value / MILLION) + "M";
    }

    /**
     * A distance in blocks, in metres under a kilometre and in kilometres above it, truncated to one decimal: {@code 999 m},
     * {@code 1.0 km}, {@code 12.5 km}. Negative distances read as 0 m.
     */
    public static String distance(long blocks) {
        long value = Math.max(0, blocks);
        if (value < THOUSAND) {
            return value + " m";
        }
        return tenths(value, THOUSAND) + " km";
    }

    /** The share of a deposit's initial amount that is left, as a percentage such as {@code 48%}. A deposit with none planned reads 0%. */
    public static String share(Deposit deposit) {
        if (deposit.initialAmount() == 0) {
            return HologramText.percent(0.0);
        }
        return HologramText.percent((double) deposit.remaining() / deposit.initialAmount());
    }

    /** A deposit's amounts as {@code 1.2M / 2.5M (48%)}: what is left, what it started with, and the share left. */
    public static String remaining(Deposit deposit) {
        return amount(deposit.remaining()) + " / " + amount(deposit.initialAmount()) + " (" + share(deposit) + ")";
    }

    /**
     * The deposit line of a drill's text, with colour codes: the ore and what is left of its deposit, or none.
     *
     * @param oreName the ore's display name, with colour codes
     * @param deposit the deposit the drill works on, or null when it works on none
     */
    public static String deposit(String oreName, Deposit deposit) {
        if (deposit == null) {
            return "&7Deposit &cnone in range";
        }
        return "&7Deposit &f" + oreName + " &7" + remaining(deposit);
    }

    /**
     * The lines of a drill's info item, with colour codes. The rate reads {@code stopped} unless the drill is running.
     *
     * @param kind           the drill's kind
     * @param spec           the drill's level
     * @param power          the drill's power this moment
     * @param status         the drill's status
     * @param itemsPerSecond the items the drill yields per second now, its level's rate times its power
     * @param depositLine    the deposit line from {@link #deposit}
     */
    public static List<String> infoLines(DrillKind kind, DrillSpec spec, DrillPower power, DrillStatus status,
                                         double itemsPerSecond, String depositLine) {
        boolean running = status == DrillStatus.RUNNING;
        List<String> lines = new ArrayList<>(List.of(
                "&7Level &f" + spec.level(),
                "&7Area &f" + spec.area() + "x" + spec.area(),
                "&7Depth &f" + spec.depth() + " blocks",
                "&7Status " + (running ? "&a" : "&c") + status.text(kind),
                "&7Rate " + (running ? "&f" + rate(itemsPerSecond) : "&cstopped"),
                depositLine,
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

    /** One decimal place of {@code value / unit}, truncated: 4250 with unit 1000 reads {@code 4.2}. */
    private static String tenths(long value, long unit) {
        long tenths = value * 10 / unit;
        return (tenths / 10) + "." + (tenths % 10);
    }
}
