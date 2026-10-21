package io.corefactory.core.energy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The persistent grid ids: the id of each pylon's grid, and the next id a fresh grid gets. Given the previous assignment and
 * the connected components of the new tick, {@link #assign(List)} returns the new assignment. It never changes its own
 * state; the caller keeps the returned registry.
 *
 * <p>The rules, applied per component of pylons:
 * <ul>
 *   <li>Each previous grid id is claimed by the component that holds the smallest of its pylons that still exist.</li>
 *   <li>A component takes the smallest id it claims. When two grids join, the surviving id is the smaller one.</li>
 *   <li>A component that claims nothing gets a fresh id, one above every id issued before. Components are numbered in the
 *       order of their smallest pylon id, so the same input always gives the same ids.</li>
 * </ul>
 * Ids are never reused once they have been issued, so a dissolved grid's id is never given to a later grid.
 *
 * @param idByPylon   the grid id of each pylon, by pylon id
 * @param nextFreshId the smallest id that has never been issued
 */
public record GridRegistry(Map<String, Integer> idByPylon, int nextFreshId) {

    /** No grids yet: no pylon has an id, and the first fresh id is 0. */
    public static final GridRegistry EMPTY = new GridRegistry(Map.of(), 0);

    public GridRegistry {
        idByPylon = Map.copyOf(idByPylon);
        Checks.nonNegative(nextFreshId, "next fresh grid id");
    }

    /**
     * The grid id of a pylon.
     *
     * @param pylonId the pylon's id
     * @return the id of its grid
     * @throws IllegalArgumentException when the pylon has no grid in this registry
     */
    public int idOf(String pylonId) {
        Integer id = idByPylon.get(pylonId);
        if (id == null) {
            throw new IllegalArgumentException("no grid for pylon " + pylonId);
        }
        return id;
    }

    /**
     * The registry of a tick: the grid ids of the given components, which must be disjoint sets of pylon ids.
     *
     * @param components the connected components of pylons of the tick
     * @return the new registry, with the same rules for every order of the components
     * @throws IllegalArgumentException when a pylon is in two components
     */
    public GridRegistry assign(List<Set<String>> components) {
        List<List<String>> sorted = new ArrayList<>();
        Map<String, Integer> componentOf = new HashMap<>();
        for (Set<String> component : components) {
            List<String> members = component.stream().sorted().toList();
            if (members.isEmpty()) {
                throw new IllegalArgumentException("a component has no pylon");
            }
            sorted.add(members);
        }
        sorted.sort((a, b) -> a.get(0).compareTo(b.get(0)));
        for (int c = 0; c < sorted.size(); c++) {
            for (String pylon : sorted.get(c)) {
                if (componentOf.put(pylon, c) != null) {
                    throw new IllegalArgumentException("pylon in two components: " + pylon);
                }
            }
        }

        Map<Integer, String> smallestOfGrid = new HashMap<>();
        for (Map.Entry<String, Integer> entry : idByPylon.entrySet()) {
            if (!componentOf.containsKey(entry.getKey())) {
                continue;
            }
            smallestOfGrid.merge(entry.getValue(), entry.getKey(), (x, y) -> x.compareTo(y) <= 0 ? x : y);
        }

        int[] assigned = new int[sorted.size()];
        Arrays.fill(assigned, -1);
        for (Map.Entry<Integer, String> entry : smallestOfGrid.entrySet()) {
            int component = componentOf.get(entry.getValue());
            if (assigned[component] < 0 || entry.getKey() < assigned[component]) {
                assigned[component] = entry.getKey();
            }
        }
        int next = nextFreshId;
        for (int c = 0; c < assigned.length; c++) {
            if (assigned[c] < 0) {
                assigned[c] = next;
                next = Math.addExact(next, 1);
            }
        }

        Map<String, Integer> byPylon = new HashMap<>();
        for (Map.Entry<String, Integer> entry : componentOf.entrySet()) {
            byPylon.put(entry.getKey(), assigned[entry.getValue()]);
        }
        return new GridRegistry(byPylon, next);
    }
}
