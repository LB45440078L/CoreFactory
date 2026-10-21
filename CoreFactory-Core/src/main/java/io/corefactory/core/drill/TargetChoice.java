package io.corefactory.core.drill;

import io.corefactory.core.geology.Placement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * Chooses the registered block a drill mines or pumps next. Only ores the drill's selection mines, and whose remaining
 * budget is above zero, are candidates; among those, the block nearest the drill wins, with the tie rules of
 * {@link DrillRules#nearest}. Pure, so the choice is tested with plain positions.
 */
public final class TargetChoice {

    private TargetChoice() {
    }

    /**
     * @param byOre     the registered blocks inside the box, grouped by ore id; the caller leaves out ores the drill's kind
     *                  may not take
     * @param box       the drill's region; a block outside it is never chosen
     * @param x         the drill's x
     * @param y         the drill's y
     * @param z         the drill's z
     * @param selection the ores the drill mines
     * @param remaining the budget left for an ore id; an ore with nothing left is never chosen
     * @return the chosen block, or empty when no selected ore has a block with budget left in the box
     */
    public static Optional<Placement> choose(Map<String, List<Placement>> byOre, DrillBox box, int x, int y, int z,
                                             DrillSelection selection, ToIntFunction<String> remaining) {
        List<Placement> candidates = new ArrayList<>();
        for (Map.Entry<String, List<Placement>> entry : byOre.entrySet()) {
            String ore = entry.getKey();
            if (selection.isMining(ore) && remaining.applyAsInt(ore) > 0) {
                candidates.addAll(entry.getValue());
            }
        }
        return DrillRules.nearest(candidates, box, x, y, z, index -> true);
    }
}
