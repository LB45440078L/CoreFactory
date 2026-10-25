package io.corefactory.paper.energy;

import io.corefactory.core.energy.OutlineTracer;

import java.util.ArrayList;
import java.util.List;

/**
 * The points the energy visuals draw: the loops of a grid's outline, the line of a wire, and a ring. Each point is an array
 * of its coordinates. Pure, so the spacing and the count are tested without a server.
 */
public final class EnergyGeometry {

    private EnergyGeometry() {
    }

    /**
     * The points along the loops of a grid's outline, at a height. Each loop is a list of corners in order, and its edges run from one
     * corner to the next and back to the first. Along each edge a point is put every {@code spacing} blocks from the edge's start, so
     * no corner is listed twice. Each point is {x, y, z}.
     *
     * @param loops   the loops of the outline, from {@link io.corefactory.core.energy.OutlineTracer#trace}
     * @param y       the height of the points
     * @param spacing the distance between points along an edge, in blocks, positive
     */
    public static List<double[]> loops(List<List<OutlineTracer.Corner>> loops, double y, double spacing) {
        List<double[]> points = new ArrayList<>();
        for (List<OutlineTracer.Corner> loop : loops) {
            int n = loop.size();
            for (int i = 0; i < n; i++) {
                OutlineTracer.Corner from = loop.get(i);
                OutlineTracer.Corner to = loop.get((i + 1) % n);
                List<double[]> edge = line(new double[] {from.x(), y, from.z()}, new double[] {to.x(), y, to.z()}, spacing);
                points.addAll(edge.subList(0, edge.size() - 1));
            }
        }
        return points;
    }

    /**
     * The points along a straight line from a to b, one every spacing blocks, starting at a. The last point is b when the length is a
     * whole number of spacings; otherwise the last point is the last full step before b. Each point is {x, y, z}.
     *
     * @param a       the start, {x, y, z}
     * @param b       the end, {x, y, z}
     * @param spacing the distance between points, in blocks, positive
     */
    public static List<double[]> line(double[] a, double[] b, double spacing) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double dz = b[2] - a[2];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int count = (int) Math.floor(length / spacing + 1e-9);
        List<double[]> points = new ArrayList<>();
        for (int i = 0; i <= count; i++) {
            double f = length == 0 ? 0 : (i * spacing) / length;
            points.add(new double[] {a[0] + dx * f, a[1] + dy * f, a[2] + dz * f});
        }
        return points;
    }

    /**
     * The points of a horizontal ring around a centre, evenly spaced, starting at the positive x direction. Each point is {x, y, z}.
     *
     * @param cx     the centre x
     * @param cy     the centre y
     * @param cz     the centre z
     * @param radius the radius, in blocks
     * @param count  the number of points, positive
     */
    public static List<double[]> ring(double cx, double cy, double cz, double radius, int count) {
        List<double[]> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            points.add(new double[] {cx + radius * Math.cos(angle), cy, cz + radius * Math.sin(angle)});
        }
        return points;
    }
}
