package io.corefactory.paper.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.Fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The decision on the oil networks whose tubes hold two fluids, as a saved file may hold them after a crash, an edit of the file, or a network
 * that merged through a chunk loaded late. Each network is given as the positions of its tubes. A network whose tubes hold two kinds keeps the
 * kind that more of its tubes hold, and the tubes of the other kind are emptied. On a tie petroleum is kept and diesel is emptied; the tie is
 * settled here, not left to the order of the file. A tube with no fluid counts as no kind, and is never emptied. Pure, so the rule is tested
 * without a server.
 */
final class MixedNetworks {

    private static final Comparator<Pos> ORDER =
            Comparator.comparingInt(Pos::x).thenComparingInt(Pos::y).thenComparingInt(Pos::z);

    private MixedNetworks() {
    }

    /**
     * A tube as it is saved.
     *
     * @param pos   its block
     * @param fluid the id of the fluid its tank holds, or null when the tank is empty
     */
    record Tube(Pos pos, String fluid) {
    }

    /**
     * A network that holds two fluids, and the tubes to empty.
     *
     * @param first   the lowest position of its tubes
     * @param kept    the id of the fluid that stays in the network
     * @param emptied the id of the fluid of the tubes that are emptied
     * @param tubes   the positions of the tubes to empty, in position order
     */
    record Mixed(Pos first, String kept, String emptied, List<Pos> tubes) {
    }

    /**
     * The networks that hold two fluids, each with its tubes to empty. A network with one fluid kind, or with none, is not listed.
     *
     * @param tubes    every tube of the file
     * @param networks the positions of each network; a position with no tube in {@code tubes} counts for nothing
     * @return the mixed networks, in the order of {@code networks}
     * @throws IllegalArgumentException when a network holds more than two fluid kinds
     */
    static List<Mixed> decide(List<Tube> tubes, List<List<Pos>> networks) {
        Map<Pos, String> fluidAt = new HashMap<>();
        for (Tube tube : tubes) {
            fluidAt.put(tube.pos(), tube.fluid());
        }
        List<Mixed> mixed = new ArrayList<>();
        for (List<Pos> network : networks) {
            Map<String, List<Pos>> held = new TreeMap<>();
            for (Pos pos : network) {
                String fluid = fluidAt.get(pos);
                if (fluid != null) {
                    held.computeIfAbsent(fluid, key -> new ArrayList<>()).add(pos);
                }
            }
            if (held.size() < 2) {
                continue;
            }
            if (held.size() > 2) {
                throw new IllegalArgumentException("a network holds two fluid kinds at most, got " + held.keySet());
            }
            String kept = Collections.max(held.keySet(), Comparator
                    .comparingInt((String fluid) -> held.get(fluid).size())
                    .thenComparing(fluid -> fluid.equals(Fluid.PETROLEUM.id())));
            String emptied = held.keySet().stream().filter(fluid -> !fluid.equals(kept)).findFirst().orElseThrow();
            List<Pos> drop = new ArrayList<>(held.get(emptied));
            drop.sort(ORDER);
            Pos first = held.values().stream().flatMap(List::stream).min(ORDER).orElseThrow();
            mixed.add(new Mixed(first, kept, emptied, List.copyOf(drop)));
        }
        return List.copyOf(mixed);
    }
}
