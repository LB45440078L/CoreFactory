package io.corefactory.core.vertical;

import io.corefactory.core.energy.Pos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The rules that group vertical belts into towers and set a tower's direction. Pure: no server types. */
public final class VerticalTowers {

    private static final Comparator<Pos> COLUMN_ORDER = Comparator.comparingInt(Pos::x)
            .thenComparingInt(Pos::z)
            .thenComparingInt(Pos::y);

    private VerticalTowers() {
    }

    /**
     * Groups the belts into towers: each maximal run of belts that are contiguous in y at the same x and z is one tower.
     * Towers come back ordered by x, then z, then bottom y.
     *
     * @param belts the belts by position
     * @return the towers, each with its belts bottom to top
     * @throws IllegalArgumentException when a run mixes directions; a tower has one direction, kept by {@link #setDirection}
     */
    public static List<VerticalTower> towersOf(Map<Pos, Belt> belts) {
        Objects.requireNonNull(belts, "belts");
        List<Pos> keys = new ArrayList<>(belts.keySet());
        keys.sort(COLUMN_ORDER);
        List<VerticalTower> towers = new ArrayList<>();
        for (Pos bottom : keys) {
            if (belts.containsKey(new Pos(bottom.x(), bottom.y() - 1, bottom.z()))) {
                continue;
            }
            List<Pos> positions = new ArrayList<>();
            List<Belt> run = new ArrayList<>();
            Pos position = bottom;
            while (belts.containsKey(position)) {
                positions.add(position);
                run.add(belts.get(position));
                position = new Pos(position.x(), position.y() + 1, position.z());
            }
            towers.add(new VerticalTower(positions, run));
        }
        return towers;
    }

    /**
     * Sets the direction of the whole tower that holds a position: every belt above and below it, the cascade. The belts of
     * the other towers are unchanged.
     *
     * @param towers the towers, as {@link #towersOf} returned them
     * @param anyPosition a belt of the tower to change
     * @param direction the new direction
     * @return the belts of all the given towers, with the direction changed on the tower of {@code anyPosition}
     * @throws IllegalArgumentException when no tower holds {@code anyPosition}
     */
    public static Map<Pos, Belt> setDirection(List<VerticalTower> towers, Pos anyPosition, VerticalDir direction) {
        Objects.requireNonNull(towers, "towers");
        Objects.requireNonNull(anyPosition, "anyPosition");
        Objects.requireNonNull(direction, "direction");
        VerticalTower target = null;
        for (VerticalTower tower : towers) {
            if (tower.contains(anyPosition)) {
                target = tower;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("no tower holds " + anyPosition);
        }
        Map<Pos, Belt> result = new HashMap<>();
        for (VerticalTower tower : towers) {
            for (int i = 0; i < tower.size(); i++) {
                Belt belt = tower.belts().get(i);
                result.put(tower.positions().get(i), tower == target ? belt.withDirection(direction) : belt);
            }
        }
        return Map.copyOf(result);
    }
}
