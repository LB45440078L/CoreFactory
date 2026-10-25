package io.corefactory.paper.turret;

import io.corefactory.core.railgun.RailgunCycle;
import io.corefactory.paper.artillery.CommandCenterScreen;

import java.util.List;
import java.util.Locale;

/**
 * The lines the rail gun's screen shows, top first: its power, the watts it uses now and the watts it asks for, the grid's satisfaction, the
 * charge as a bar, and whether it is switched on and what it is doing. Each line is MiniMessage, read by {@code Text.parse}. The lines are built
 * from a {@link Readout}, so they are tested without a server.
 */
final class RailgunScreen {

    /** The cells of the charge bar. */
    static final int BAR_CELLS = 10;

    private static final String HEADER = "<gradient:#FF2A2A:#FF8A8A><b>RAIL GUN</b></gradient>";

    private RailgunScreen() {
    }

    /**
     * The values a screen shows.
     *
     * @param watts        the power the rail gun uses now, in W: the power it asks for, times the share it gets
     * @param demandW      the power it asks for now, in W
     * @param satisfaction the grid's satisfaction from 0 to 1, NaN when the rail gun is on no grid
     * @param charge       the share of the charge done, from 0 to 1
     * @param phase        what the cycle is doing
     * @param on           whether the rail gun is switched on
     */
    record Readout(double watts, double demandW, double satisfaction, double charge, RailgunCycle.Phase phase, boolean on) {
    }

    /**
     * The screen's lines, top first.
     *
     * @param r the values
     * @return the five lines
     */
    static List<String> lines(Readout r) {
        return List.of(
                HEADER,
                power(r.watts(), r.demandW()),
                satisfied(r.satisfaction()),
                charge(r.charge(), r.on()),
                state(r.phase(), r.on()));
    }

    /** The power line: the watts used now and the watts asked for, in the artillery's units. */
    static String power(double watts, double demand) {
        return "Power <value>" + CommandCenterScreen.power(watts) + "</value> <muted>of " + CommandCenterScreen.power(demand) + "</muted>";
    }

    /** The satisfaction line: the grid's share of what the rail gun asks for, or that it is on no grid. */
    static String satisfied(double satisfaction) {
        if (Double.isNaN(satisfaction)) {
            return "Satisfied <bad>no grid</bad>";
        }
        long percent = Math.round(Math.max(0.0, Math.min(1.0, satisfaction)) * 100);
        String colour = satisfaction >= 0.9995 ? "good" : "warn";
        return "Satisfied <" + colour + ">" + percent + "%</" + colour + ">";
    }

    /**
     * The charge line: a bar of the charge share, and its percentage. A switched-off rail gun has no charge to show.
     */
    static String charge(double share, boolean on) {
        if (!on) {
            return "Charge <muted>off</muted>";
        }
        double clamped = Math.max(0.0, Math.min(1.0, Double.isNaN(share) ? 0.0 : share));
        int filled = (int) Math.round(clamped * BAR_CELLS);
        return String.format(Locale.ROOT, "Charge <good>%s</good><muted>%s</muted> <value>%d%%</value>",
                "|".repeat(filled), ".".repeat(BAR_CELLS - filled), Math.round(clamped * 100));
    }

    /** The state line: ON with what the cycle is doing, or OFF. */
    static String state(RailgunCycle.Phase phase, boolean on) {
        if (!on) {
            return "Switch <bad>OFF</bad>";
        }
        return "Switch <good>ON</good> <muted>" + switch (phase) {
            case IDLE -> "idle";
            case CHARGING -> "charging";
            case RELOADING -> "reloading";
        } + "</muted>";
    }
}
