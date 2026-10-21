package io.corefactory.core.energy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Traces the outline of a set of covered block cells: the boundary of their union, as closed loops of integer corner points.
 * A cell (x, z) is the unit square from corner (x, z) to corner (x + 1, z + 1). Every edge between a covered and an
 * uncovered cell is on the boundary; the loops are the outer boundary and the holes, each in order. Only the corners where
 * the outline turns are listed.
 *
 * <p>Each loop starts at its smallest corner, and the loops are listed by their start. Where two covered cells touch only
 * at a corner, the outline turns to keep each cell's loop separate, so such cells give two loops.
 */
public final class OutlineTracer {

    private OutlineTracer() {
    }

    /** A covered block cell, by its x and z. */
    public record Cell(int x, int z) {
    }

    /** An integer corner point of the outline. */
    public record Corner(int x, int z) {
    }

    private record Edge(Corner from, Corner to) {
    }

    /**
     * The cells of a square around a block: half-side blocks on each side of its x and z, the centre included.
     *
     * @param x        the centre's x
     * @param z        the centre's z
     * @param halfSide the half-side, not negative
     */
    public static Set<Cell> square(int x, int z, int halfSide) {
        if (halfSide < 0) {
            throw new IllegalArgumentException("half-side must not be negative: " + halfSide);
        }
        Set<Cell> cells = new HashSet<>();
        for (int dx = -halfSide; dx <= halfSide; dx++) {
            for (int dz = -halfSide; dz <= halfSide; dz++) {
                cells.add(new Cell(x + dx, z + dz));
            }
        }
        return cells;
    }

    /**
     * The union of the range squares of some pylons.
     *
     * @param pylons the pylons whose squares are covered
     */
    public static Set<Cell> coverOf(Collection<Pylon> pylons) {
        Set<Cell> cells = new HashSet<>();
        for (Pylon pylon : pylons) {
            cells.addAll(square(pylon.pos().x(), pylon.pos().z(), pylon.type().halfSide()));
        }
        return cells;
    }

    /**
     * The loops of the outline of a set of covered cells.
     *
     * @param covered the covered cells
     * @return the closed loops, each a list of the corners where the outline turns, in order around the loop; the first
     *         corner is not repeated at the end
     */
    public static List<List<Corner>> trace(Set<Cell> covered) {
        List<Edge> edges = new ArrayList<>();
        for (Cell cell : covered) {
            int x = cell.x();
            int z = cell.z();
            if (!covered.contains(new Cell(x, z - 1))) {
                edges.add(new Edge(new Corner(x, z), new Corner(x + 1, z)));
            }
            if (!covered.contains(new Cell(x + 1, z))) {
                edges.add(new Edge(new Corner(x + 1, z), new Corner(x + 1, z + 1)));
            }
            if (!covered.contains(new Cell(x, z + 1))) {
                edges.add(new Edge(new Corner(x + 1, z + 1), new Corner(x, z + 1)));
            }
            if (!covered.contains(new Cell(x - 1, z))) {
                edges.add(new Edge(new Corner(x, z + 1), new Corner(x, z)));
            }
        }
        edges.sort(Comparator.comparing((Edge edge) -> edge.from().x())
                .thenComparing(edge -> edge.from().z())
                .thenComparing(edge -> edge.to().x())
                .thenComparing(edge -> edge.to().z()));

        Set<Edge> unused = new HashSet<>(edges);
        List<List<Corner>> loops = new ArrayList<>();
        for (Edge start : edges) {
            if (!unused.remove(start)) {
                continue;
            }
            List<Corner> walk = new ArrayList<>();
            walk.add(start.from());
            Edge current = start;
            while (!current.to().equals(start.from())) {
                walk.add(current.to());
                current = next(current, unused);
            }
            loops.add(turningCorners(walk));
        }
        return List.copyOf(loops);
    }

    private static List<Corner> turningCorners(List<Corner> walk) {
        int n = walk.size();
        List<Corner> corners = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Corner before = walk.get((i + n - 1) % n);
            Corner at = walk.get(i);
            Corner after = walk.get((i + 1) % n);
            int inX = Integer.signum(at.x() - before.x());
            int inZ = Integer.signum(at.z() - before.z());
            int outX = Integer.signum(after.x() - at.x());
            int outZ = Integer.signum(after.z() - at.z());
            if (inX != outX || inZ != outZ) {
                corners.add(at);
            }
        }
        return List.copyOf(corners);
    }

    private static Edge next(Edge incoming, Set<Edge> unused) {
        int dx = incoming.to().x() - incoming.from().x();
        int dz = incoming.to().z() - incoming.from().z();
        int[][] turns = {
            {-dz, dx},
            {dx, dz},
            {dz, -dx}
        };
        Corner at = incoming.to();
        for (int[] turn : turns) {
            Edge candidate = new Edge(at, new Corner(at.x() + turn[0], at.z() + turn[1]));
            if (unused.remove(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("outline does not close at " + at);
    }
}
