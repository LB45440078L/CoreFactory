package io.corefactory.core.drill;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The rows of a drill's resources menu: one per ore that is in range now or was mined by this drill. For a row, the total
 * in range is what is in range now plus what this drill has mined, so it is the amount the drill had in its box at the
 * start. Pure: the menu's items are built from these rows.
 */
public final class DrillReport {

    /**
     * One ore of the report.
     *
     * @param oreId         the ore id
     * @param inRange       what is still in the drill's box (blocks, or volume for a deposit)
     * @param minedByDrill  what this drill has mined of it
     * @param totalInRange  inRange plus minedByDrill
     * @param selected      true when the drill mines this ore
     * @param exhausted     true when the drill mines this ore but nothing of it is left in its box
     */
    public record Row(String oreId, long inRange, long minedByDrill, long totalInRange, boolean selected,
                      boolean exhausted) {
    }

    private DrillReport() {
    }

    /**
     * Builds the rows. The rows follow {@code oreOrder}, then any other ore id in alphabetical order. Ores with nothing in
     * range and nothing mined are left out. Negative counts are read as zero.
     *
     * @param oreOrder   the ore ids in the order the menu lists them, such as the file order
     * @param inRange    what is in range per ore id now
     * @param minedByDrill what this drill has mined per ore id
     * @param selection  the drill's selection
     */
    public static List<Row> build(List<String> oreOrder, Map<String, Long> inRange, Map<String, Long> minedByDrill,
                                  DrillSelection selection) {
        Set<String> ores = new LinkedHashSet<>(oreOrder);
        Set<String> extra = new TreeSet<>();
        extra.addAll(inRange.keySet());
        extra.addAll(minedByDrill.keySet());
        extra.removeAll(ores);
        ores.addAll(extra);

        List<Row> rows = new ArrayList<>();
        for (String ore : ores) {
            long here = Math.max(0, inRange.getOrDefault(ore, 0L));
            long mined = Math.max(0, minedByDrill.getOrDefault(ore, 0L));
            if (here == 0 && mined == 0) {
                continue;
            }
            boolean selected = selection.isMining(ore);
            rows.add(new Row(ore, here, mined, here + mined, selected, selected && here == 0));
        }
        return List.copyOf(rows);
    }
}
