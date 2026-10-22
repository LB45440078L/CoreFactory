package io.corefactory.core.deposit;

import java.util.ArrayList;
import java.util.List;

/**
 * The ore filter of the scanner. It is either {@link #ALL}, which takes every ore, or one ore id. Each use of the scanner's
 * filter key moves to the next state: all ores, then each ore in the order of ores.yml, then all ores again.
 */
public final class DepositFilter {

    /** The filter that takes every ore. */
    public static final String ALL = "";

    private DepositFilter() {
    }

    /**
     * The filter after {@code current} in the cycle of all ores and then each ore. A current filter that is not in the list, such as
     * an ore removed from ores.yml, is treated as the start of the cycle, so the next filter is the first ore.
     *
     * @param current the current filter, {@link #ALL} or an ore id; null reads as {@link #ALL}
     * @param ores    the ore ids in order
     * @return the next filter; {@link #ALL} when there are no ores
     */
    public static String next(String current, List<String> ores) {
        List<String> states = new ArrayList<>(ores.size() + 1);
        states.add(ALL);
        states.addAll(ores);
        int index = Math.max(0, states.indexOf(current == null ? ALL : current));
        return states.get((index + 1) % states.size());
    }

    /**
     * True when a deposit of this ore passes the filter.
     *
     * @param filter the filter, {@link #ALL} or an ore id
     * @param ore    the deposit's ore id
     */
    public static boolean matches(String filter, String ore) {
        return filter == null || filter.isEmpty() || filter.equals(ore);
    }
}
