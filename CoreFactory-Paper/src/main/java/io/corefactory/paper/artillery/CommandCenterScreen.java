package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.FiringVerdict;

import java.util.List;
import java.util.Locale;

/**
 * The lines the Command Center's screen shows, and the words of its safety check. Each line is MiniMessage, read by {@code Text.parse}, and
 * the screen shows them top first. The lines are built from a {@link Readout} of the values, so they are tested without a server. A value
 * that is not known reads as a plain word: NaN for an aim, a missing shell, or a grid that is not there.
 */
public final class CommandCenterScreen {

    /**
     * The values a screen shows.
     *
     * @param aimX        the x of the aim point, NaN when no target is set
     * @param aimZ        the z of the aim point, NaN when no target is set
     * @param shell       the name of the shell chosen, or null when none is chosen
     * @param tier        the name of the charge tier, such as {@code II}
     * @param elevation   the barrel's elevation, in degrees
     * @param connected   the number of connected artillery
     * @param wattsNeeded the power the connected artillery need, in W
     * @param satisfaction the grid's satisfaction from 0 to 1, NaN when the command center is on no grid
     * @param safety      the safety check's words, short: {@code OK} or the reason
     * @param safe        true when the safety check passes
     */
    public record Readout(double aimX, double aimZ, String shell, String tier, double elevation, int connected, double wattsNeeded,
                          double satisfaction, String safety, boolean safe) {
    }

    private static final String HEADER = "<gradient:#FFB02E:#FFE070><b>ARTILLERY COMMAND</b></gradient>";

    private CommandCenterScreen() {
    }

    /**
     * The screen's lines, top first.
     *
     * @param r the values
     * @return the eight lines
     */
    public static List<String> lines(Readout r) {
        return List.of(
                HEADER,
                impact(r.aimX(), r.aimZ()),
                shell(r.shell(), r.tier()),
                String.format(Locale.ROOT, "Elevation <value>%.0f</value> deg", r.elevation()),
                r.connected() == 0 ? "Artillery <bad>none connected</bad>"
                        : String.format(Locale.ROOT, "Artillery <value>%d</value> connected", r.connected()),
                "Needs <value>" + power(r.wattsNeeded()) + "</value>",
                satisfied(r.satisfaction()),
                r.safe() ? "Safety <good>OK</good>" : "Safety <bad>" + r.safety() + "</bad>");
    }

    /**
     * The short words of a firing verdict, for the screen: the reason a shot is refused, or {@code OK}.
     *
     * @param verdict the verdict of the firing rules
     * @return the words
     */
    public static String reason(FiringVerdict verdict) {
        return switch (verdict) {
            case OK -> "OK";
            case CHARGE_TOO_LOW -> "charge too weak";
            case LEVEL_TOO_LOW -> "piece too small";
            case ELEVATION_TOO_LOW -> "elevation too low";
            case ELEVATION_TOO_HIGH -> "elevation too high";
            case CLUSTER_ELEVATION_TOO_LOW -> "needs a steeper elevation";
            case OUT_OF_RANGE -> "out of range";
            case TOO_CLOSE -> "too close";
            case BLAST_REACHES_ARTILLERY -> "blast reaches the piece";
            case BLAST_REACHES_PLAYER -> "blast reaches you";
            case NUCLEAR_NEAR_PLAYER -> "players too near";
            case COOLDOWN_ACTIVE -> "antimatter cooldown";
            case POWER_SHORT -> "piece lacks power";
        };
    }

    /**
     * A power in watts, in the largest unit that keeps the number under a thousand: W, kW, MW or GW.
     *
     * @param watts the power, 0 or more
     * @return the text, such as {@code 1.27 MW}
     */
    public static String power(double watts) {
        if (watts >= 1e9) {
            return String.format(Locale.ROOT, "%.2f GW", watts / 1e9);
        }
        if (watts >= 1e6) {
            return String.format(Locale.ROOT, "%.2f MW", watts / 1e6);
        }
        if (watts >= 1e3) {
            return String.format(Locale.ROOT, "%.1f kW", watts / 1e3);
        }
        return String.format(Locale.ROOT, "%.0f W", watts);
    }

    private static String impact(double aimX, double aimZ) {
        if (Double.isNaN(aimX) || Double.isNaN(aimZ)) {
            return "Impact <hint>not set</hint>";
        }
        return String.format(Locale.ROOT, "Impact <value>X %d   Z %d</value>", Math.round(aimX), Math.round(aimZ));
    }

    private static String shell(String shell, String tier) {
        if (shell == null) {
            return "Shell <hint>none chosen</hint>";
        }
        return "Shell <value>" + shell + "</value> <muted>tier " + tier + "</muted>";
    }

    private static String satisfied(double satisfaction) {
        if (Double.isNaN(satisfaction)) {
            return "Satisfied <bad>no grid</bad>";
        }
        long percent = Math.round(satisfaction * 100);
        String colour = satisfaction >= 0.9995 ? "good" : "warn";
        return "Satisfied <" + colour + ">" + percent + "%</" + colour + ">";
    }
}
