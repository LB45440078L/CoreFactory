package io.corefactory.core.artillery;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The ground geometry of the shell effects: where the submunitions of a cluster shell land, the ellipse of a napalm burst, the disc of
 * a phosphorus cloud, and the outline of an explosion for drawing. Pure geometry, so the platform decides what to set on fire, damage or
 * draw; a block is named by its column, the integer x and z of its corner, and its centre is half a block further on each axis.
 */
public final class ArtilleryEffects {

    /**
     * The tries one submunition gets to find a free spot. A spot is free with a good chance once the field is half full, so a thousand
     * tries fail only when the layout is really impossible.
     */
    static final int ATTEMPTS_PER_POINT = 1000;

    private ArtilleryEffects() {
    }

    /**
     * A block column on the ground, named by the integer x and z of its corner.
     *
     * @param x the x of the column's corner, in blocks
     * @param z the z of the column's corner, in blocks
     */
    public record Column(int x, int z) {
    }

    /**
     * The ground of a phosphorus cloud and the light particles one burst spawns.
     *
     * @param ground         the columns the cloud covers on the ground
     * @param lightParticles the light particles to spawn for the visual
     */
    public record PhosphorusCloud(List<Column> ground, int lightParticles) {
    }

    /**
     * The points a cluster shell's submunitions land on: uniform in a disc, with a minimum spacing between any two of them. The spacing is
     * enforced by rejection: each point is tried up to 1000 times and the layout is refused when a point finds no
     * free spot, so a layout that cannot fit is reported rather than looped on.
     *
     * @param cx         the x of the disc's centre, in blocks
     * @param cz         the z of the disc's centre, in blocks
     * @param radius     the disc's radius, in blocks, above 0
     * @param count      how many points, 0 or more
     * @param seed       the seed of the random source; the same seed gives the same points
     * @param minSpacing the least distance between two points, in blocks, 0 or more
     * @return the points, in the order they were placed
     * @throws IllegalArgumentException when an argument is out of range
     * @throws IllegalStateException    when a point finds no free spot in 1000 tries
     */
    public static List<GroundPoint> clusterImpacts(double cx, double cz, double radius, int count, long seed, double minSpacing) {
        if (!(radius > 0) || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("radius must be finite and above 0: " + radius);
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be 0 or more: " + count);
        }
        if (!(minSpacing >= 0) || !Double.isFinite(minSpacing)) {
            throw new IllegalArgumentException("min spacing must be finite and 0 or more: " + minSpacing);
        }
        Random random = new Random(seed);
        List<GroundPoint> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            GroundPoint placed = null;
            for (int attempt = 0; attempt < ATTEMPTS_PER_POINT && placed == null; attempt++) {
                // The square root makes the points uniform by area, rather than crowding the centre.
                double r = radius * Math.sqrt(random.nextDouble());
                double angle = random.nextDouble() * 2.0 * Math.PI;
                GroundPoint candidate = new GroundPoint(cx + r * Math.cos(angle), cz + r * Math.sin(angle));
                if (clearOf(points, candidate, minSpacing)) {
                    placed = candidate;
                }
            }
            if (placed == null) {
                throw new IllegalStateException("no free spot for submunition " + (i + 1) + " of " + count + " in a disc of radius "
                        + radius + " with spacing " + minSpacing);
            }
            points.add(placed);
        }
        return List.copyOf(points);
    }

    /**
     * The columns of a napalm burst: the ellipse around the impact, elongated along the shot. Its long axis runs along the direction
     * {@code (dx, dz)} with a length of {@code length}, and its short axis is {@code width}. A column is in the burst when its centre
     * is inside the ellipse. With no direction, the burst is a circle whose diameter is the length, so it covers the ellipse whatever
     * way the shot went.
     *
     * @param ix     the x of the impact, in blocks
     * @param iz     the z of the impact, in blocks
     * @param dx     the x component of the shot's horizontal direction; it need not be a unit vector
     * @param dz     the z component of the shot's horizontal direction
     * @param length the length of the burst along the shot, in blocks, above 0
     * @param width  the width of the burst across the shot, in blocks, above 0
     * @return the columns in the burst, in order of x and then z
     * @throws IllegalArgumentException when the length or the width is not above 0
     */
    public static List<Column> napalmBurst(double ix, double iz, double dx, double dz, double length, double width) {
        if (!(length > 0) || !Double.isFinite(length) || !(width > 0) || !Double.isFinite(width)) {
            throw new IllegalArgumentException("burst length and width must be finite and above 0");
        }
        double direction = Math.hypot(dx, dz);
        double ux = direction > 0 ? dx / direction : 1.0;
        double uz = direction > 0 ? dz / direction : 0.0;
        double along = length / 2.0;
        double across = direction > 0 ? width / 2.0 : length / 2.0;
        double reach = along;
        List<Column> burst = new ArrayList<>();
        for (int x = (int) Math.floor(ix - reach) - 1; x <= (int) Math.ceil(ix + reach) + 1; x++) {
            for (int z = (int) Math.floor(iz - reach) - 1; z <= (int) Math.ceil(iz + reach) + 1; z++) {
                double px = x + 0.5 - ix;
                double pz = z + 0.5 - iz;
                double p = px * ux + pz * uz;
                double q = -px * uz + pz * ux;
                if (p * p / (along * along) + q * q / (across * across) <= 1.0) {
                    burst.add(new Column(x, z));
                }
            }
        }
        return List.copyOf(burst);
    }

    /**
     * The ground of a phosphorus cloud: the columns whose centres are inside the disc, and the light particles the burst spawns.
     *
     * @param cx             the x of the cloud's centre, in blocks
     * @param cz             the z of the cloud's centre, in blocks
     * @param radius         the cloud's radius on the ground, in blocks, above 0
     * @param lightParticles the light particles one burst spawns, 0 or more
     * @return the cloud's ground and its particle count
     */
    public static PhosphorusCloud phosphorusCloud(double cx, double cz, double radius, int lightParticles) {
        if (!(radius > 0) || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("radius must be finite and above 0: " + radius);
        }
        if (lightParticles < 0) {
            throw new IllegalArgumentException("light particles must be 0 or more: " + lightParticles);
        }
        List<Column> ground = new ArrayList<>();
        for (int x = (int) Math.floor(cx - radius) - 1; x <= (int) Math.ceil(cx + radius) + 1; x++) {
            for (int z = (int) Math.floor(cz - radius) - 1; z <= (int) Math.ceil(cz + radius) + 1; z++) {
                double dx = x + 0.5 - cx;
                double dz = z + 0.5 - cz;
                if (dx * dx + dz * dz <= radius * radius) {
                    ground.add(new Column(x, z));
                }
            }
        }
        return new PhosphorusCloud(List.copyOf(ground), lightParticles);
    }

    /**
     * The outline of an explosion: points spaced evenly around its circle, for drawing the ring that runs out ahead of the blast.
     *
     * @param cx     the x of the centre, in blocks
     * @param cz     the z of the centre, in blocks
     * @param radius the radius of the ring, in blocks, above 0
     * @param points how many points the ring is drawn with, at least 3
     * @return the points, the first one on the positive x axis, in order around the ring
     * @throws IllegalArgumentException when the radius is not above 0 or there are fewer than 3 points
     */
    public static List<GroundPoint> explosionRing(double cx, double cz, double radius, int points) {
        if (!(radius > 0) || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("radius must be finite and above 0: " + radius);
        }
        if (points < 3) {
            throw new IllegalArgumentException("a ring needs at least 3 points: " + points);
        }
        List<GroundPoint> ring = new ArrayList<>(points);
        for (int i = 0; i < points; i++) {
            double angle = 2.0 * Math.PI * i / points;
            ring.add(new GroundPoint(cx + radius * Math.cos(angle), cz + radius * Math.sin(angle)));
        }
        return List.copyOf(ring);
    }

    private static boolean clearOf(List<GroundPoint> points, GroundPoint candidate, double minSpacing) {
        for (GroundPoint placed : points) {
            double dx = placed.x() - candidate.x();
            double dz = placed.z() - candidate.z();
            if (dx * dx + dz * dz < minSpacing * minSpacing) {
                return false;
            }
        }
        return true;
    }
}
