package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure placement checks for belt units. */
public final class Placement {

    private Placement() {
    }

    /**
     * Whether a belt flowing {@code flow} can be placed at {@code pos} among the existing units and endpoints.
     *
     * <p>Refused as {@link PlacementReason#OCCUPIED} when the cell holds a belt, picker, splitter or port. Refused as
     * {@link PlacementReason#MERGE} when the new unit would take a second input, or would give a second input to an
     * existing unit. Refused as {@link PlacementReason#BLOCKED} when it would sit head-on with a belt, or when it would
     * feed a picker or splitter from a cell that does not accept it.
     *
     * @param pos       the cell to place the belt on
     * @param dir       the flow of the new belt
     * @param units     the belts already placed
     * @param endpoints the pickers, splitters and port cells already placed
     * @return the reason, {@link PlacementReason#OK} when the placement is allowed
     */
    public static PlacementReason canPlace(Pos pos, Dir dir, List<BeltUnit> units, Endpoints endpoints) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(dir, "dir");
        Objects.requireNonNull(units, "units");
        Objects.requireNonNull(endpoints, "endpoints");
        Topology existing = new Topology(units, endpoints.pickers(), endpoints.splitters(), endpoints.duplexes());
        if (existing.occupied(pos) || endpoints.ports().contains(pos)) {
            return PlacementReason.OCCUPIED;
        }
        List<BeltUnit> next = new ArrayList<>(units);
        next.add(new BeltUnit(pos, dir, LogisticsLevel.I));
        return new Topology(next, endpoints.pickers(), endpoints.splitters(), endpoints.duplexes()).validate();
    }

    /**
     * Whether a picker can be placed among the existing units and endpoints. The reasons are those of
     * {@link #canPlace}: a cell that is taken is OCCUPIED, and a picker that would be fed from the wrong side, or would
     * feed a unit that already has an input, is BLOCKED or MERGE.
     *
     * @param candidate the picker to place, with its id not used by another picker
     * @param units     the belts already placed
     * @param endpoints the pickers, splitters and port cells already placed
     * @return the reason, {@link PlacementReason#OK} when the placement is allowed
     */
    public static PlacementReason canPlacePicker(Picker candidate, List<BeltUnit> units, Endpoints endpoints) {
        Objects.requireNonNull(candidate, "candidate");
        List<Picker> pickers = new ArrayList<>(endpoints.pickers());
        pickers.add(candidate);
        return canPlaceEndpoint(candidate.pos(), units, endpoints, pickers, endpoints.splitters(), endpoints.duplexes());
    }

    /**
     * Whether a splitter can be placed among the existing units and endpoints. The reasons are those of
     * {@link #canPlace}: a splitter takes its input from a belt at its back only, and feeds three cells.
     *
     * @param candidate the splitter to place, with its id not used by another splitter
     * @param units     the belts already placed
     * @param endpoints the pickers, splitters and port cells already placed
     * @return the reason, {@link PlacementReason#OK} when the placement is allowed
     */
    public static PlacementReason canPlaceSplitter(Splitter candidate, List<BeltUnit> units, Endpoints endpoints) {
        Objects.requireNonNull(candidate, "candidate");
        List<Splitter> splitters = new ArrayList<>(endpoints.splitters());
        splitters.add(candidate);
        return canPlaceEndpoint(candidate.pos(), units, endpoints, endpoints.pickers(), splitters, endpoints.duplexes());
    }

    /**
     * Whether a belt duplex can be placed among the existing units and endpoints. Both of its cells must be free of belts, pickers,
     * splitters, duplexes and ports. The reasons are those of {@link #canPlace} otherwise: a duplex with two input belts is MERGE, and
     * a duplex fed from a side of either cell, or fed by a picker or splitter, is BLOCKED. A duplex with no input belt is allowed here;
     * the caller checks that a belt stands behind one of its cells.
     *
     * @param candidate the duplex to place, with its id not used by another duplex
     * @param units     the belts already placed
     * @param endpoints the pickers, splitters, duplexes and port cells already placed
     * @return the reason, {@link PlacementReason#OK} when the placement is allowed
     */
    public static PlacementReason canPlaceDuplex(Duplex candidate, List<BeltUnit> units, Endpoints endpoints) {
        Objects.requireNonNull(candidate, "candidate");
        List<Duplex> duplexes = new ArrayList<>(endpoints.duplexes());
        duplexes.add(candidate);
        return canPlaceEndpoint(candidate.anchor(), units, endpoints, endpoints.pickers(), endpoints.splitters(), duplexes,
                candidate.cells());
    }

    private static PlacementReason canPlaceEndpoint(Pos pos, List<BeltUnit> units, Endpoints before, List<Picker> pickers,
                                                    List<Splitter> splitters, List<Duplex> duplexes) {
        return canPlaceEndpoint(pos, units, before, pickers, splitters, duplexes, List.of(pos));
    }

    private static PlacementReason canPlaceEndpoint(Pos pos, List<BeltUnit> units, Endpoints before, List<Picker> pickers,
                                                    List<Splitter> splitters, List<Duplex> duplexes, List<Pos> cells) {
        Topology existing = new Topology(units, before.pickers(), before.splitters(), before.duplexes());
        for (Pos cell : cells) {
            if (existing.occupied(cell) || before.ports().contains(cell)) {
                return PlacementReason.OCCUPIED;
            }
        }
        return new Topology(units, pickers, splitters, duplexes).validate();
    }
}
