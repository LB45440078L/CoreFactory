package io.corefactory.core.railgun;

import java.util.ArrayList;
import java.util.List;

/**
 * The state of one rail gun shot in flight: its flame frontier, the path the frontier has taken, and the helix drawn around that path.
 *
 * <p>A shot is created at its source, the barrel, and {@link #advance advanced} once per server tick with the target's current position, so
 * the frontier curves toward a moving target. Each advance moves the frontier toward the target by the level's helix speed over 20 blocks
 * per tick, and reports arrival once the frontier is within the hit distance of the target. After arrival the shot draws nothing and
 * advances no more. A target the frontier never reaches keeps the shot in flight for as long as it is advanced; the caller must give up on
 * it, such as when the target leaves the turret's reach.
 *
 * <p>The points to draw each tick come from {@link #points()}, as {@link Sample}s of three kinds: the red strands, which wind around the path
 * from the barrel to the frontier; the flame frontier, the point at the head; and, from the spiral delay on, the blue spiral, a single helix
 * of larger radius that wraps the strands from the point the frontier had at the delay to the frontier. The strands and the spiral stop
 * being drawn at arrival.
 *
 * <p><b>Coordinates.</b> World space, in blocks: x east, y up, z south. Times are server ticks, 20 to the second.
 *
 * <p><b>Bounded work.</b> The path is kept as at most {@link #CAPACITY} vertices. A new vertex is committed each time the frontier has
 * travelled the spacing since the last one. When the list is full, every second vertex is dropped and the spacing doubles, so the path keeps
 * its whole length and its shape at a coarser resolution. Each sample is found by one binary search over the vertices, and a tick draws at
 * most the particle budget of samples. The strands are sampled at fixed distances from the barrel, so a short path is a continuous helix,
 * and a long one is drawn in full with wider spacing rather than cut short.
 *
 * <p>The positions of samples are computed into scratch arrays held by the shot, so a tick allocates only its list and the samples in it.
 */
public final class RailgunBeam {

    /** The kind of a point to draw. */
    public enum Kind {
        /** One of the red strands, which wind around the path from the barrel to the frontier. */
        RED_STRAND,
        /** The flame frontier, the point at the head of the path. */
        FLAME_FRONTIER,
        /** The blue spiral, which wraps the strands from the point the frontier had at the spiral delay to the frontier. */
        BLUE_SPIRAL
    }

    /**
     * A point to draw this tick.
     *
     * @param kind the kind of the point
     * @param x    the east-west coordinate, in blocks
     * @param y    the height, in blocks
     * @param z    the north-south coordinate, in blocks
     */
    public record Sample(Kind kind, double x, double y, double z) {
    }

    /** The most vertices kept of the path. */
    static final int CAPACITY = 256;

    /** The distance in blocks the frontier travels between two committed vertices, at the start. */
    static final double VERTEX_SPACING = 0.5;

    /** Once the spiral has started it takes one part in this many of the budget left after the frontier, never more than the strands leave. */
    static final int SPIRAL_SHARE = 4;

    private final double stepBlocks;
    private final double hitDistance;
    private final int strands;
    private final double strandRadius;
    private final double strandTurns;
    private final double spiralRadius;
    private final double spiralTurns;
    private final double sampleSpacing;
    private final double spinTurnsPerSecond;
    private final int budget;
    private final int spiralDelayTicks;

    /** The committed vertices of the path: their positions and the distance along the path to each. */
    private final double[] vertexX = new double[CAPACITY];
    private final double[] vertexY = new double[CAPACITY];
    private final double[] vertexZ = new double[CAPACITY];
    private final double[] vertexArc = new double[CAPACITY];
    private int count;
    private double spacing = VERTEX_SPACING;

    private double headX;
    private double headY;
    private double headZ;
    private double headArc;

    /** The unit direction the frontier last moved in: the tangent where the path has no segment of its own. */
    private double dirX;
    private double dirY;
    private double dirZ = 1.0;

    private int ticks;
    private boolean arrived;
    private boolean spiralOn;
    private double spiralStartArc;

    /** Scratch: the position and the unit tangent of the path at a distance along it. */
    private final double[] pathPoint = new double[6];
    /** Scratch: the offset of a helix point from the path. */
    private final double[] offset = new double[3];

    /**
     * A shot fired from a source point. The frontier starts at the source and has not moved.
     *
     * @param settings the rail gun's settings: the helix, the strands, the spiral, the hit distance and the particle budget
     * @param level    the level of the rail gun that fired, which sets the frontier's speed
     * @param x        the source's east-west coordinate, in blocks
     * @param y        the source's height, in blocks
     * @param z        the source's north-south coordinate, in blocks
     */
    public RailgunBeam(RailgunSettings settings, RailgunLevel level, double x, double y, double z) {
        this.stepBlocks = settings.helixSpeed(level) / RailgunSettings.TICKS_PER_SECOND;
        this.hitDistance = settings.hitDistanceBlocks();
        this.strands = settings.strands();
        this.strandRadius = settings.strandRadiusBlocks();
        this.strandTurns = settings.strandTwistTurnsPerBlock();
        this.spiralRadius = settings.spiralRadiusBlocks();
        this.spiralTurns = settings.spiralTurnsPerBlock();
        this.sampleSpacing = settings.sampleSpacingBlocks();
        this.spinTurnsPerSecond = settings.spinTurnsPerSecond();
        this.budget = settings.particleBudget();
        this.spiralDelayTicks = settings.spiralDelayTicks();
        this.headX = x;
        this.headY = y;
        this.headZ = z;
        this.vertexX[0] = x;
        this.vertexY[0] = y;
        this.vertexZ[0] = z;
        this.vertexArc[0] = 0.0;
        this.count = 1;
    }

    /**
     * Moves the frontier one tick toward the target's current position. It moves by the helix speed over 20 blocks, or to the target when the
     * target is nearer than that. The shot arrives when the frontier is within the hit distance of the target. Once arrived, the shot does
     * nothing more.
     *
     * @param targetX the target's east-west coordinate now, in blocks
     * @param targetY the target's height now, in blocks
     * @param targetZ the target's north-south coordinate now, in blocks
     * @return true only on the tick the frontier arrives, so the caller applies the damage once
     */
    public boolean advance(double targetX, double targetY, double targetZ) {
        if (arrived) {
            return false;
        }
        double dx = targetX - headX;
        double dy = targetY - headY;
        double dz = targetZ - headZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > 0.0) {
            double move = Math.min(stepBlocks, distance);
            dirX = dx / distance;
            dirY = dy / distance;
            dirZ = dz / distance;
            headX += dirX * move;
            headY += dirY * move;
            headZ += dirZ * move;
            headArc += move;
            commitIfFar();
        }
        ticks++;
        if (!spiralOn && ticks >= spiralDelayTicks) {
            spiralOn = true;
            spiralStartArc = headArc;
        }
        double rx = targetX - headX;
        double ry = targetY - headY;
        double rz = targetZ - headZ;
        if (Math.sqrt(rx * rx + ry * ry + rz * rz) <= hitDistance) {
            arrived = true;
            return true;
        }
        return false;
    }

    /**
     * The points to draw this tick, at most the particle budget. See {@link #points(int)}.
     *
     * @return the points, the flame frontier first
     */
    public List<Sample> points() {
        return points(budget);
    }

    /**
     * The points to draw this tick, at most the given allowance and never more than the beam's own budget. The flame frontier comes first,
     * then the red strands, each with at least one point when the budget allows, then the blue spiral when it has started: it takes one part
     * in {@link #SPIRAL_SHARE} of what is left, and never more than what the strands leave.
     *
     * <p>The samples of a strand are anchored at the barrel and spaced by the sample spacing along the path, so the helix is drawn from the
     * source to the frontier as one continuous curve, and its samples do not slide as the frontier moves. A path too long for the budget at
     * that spacing is drawn with the spacing doubled until it fits, so it is always complete. The helix turns about the path as time passes,
     * toward the target, so the beam is animated for as long as it flies. The shots of several rail guns share one per-tick cap, so each asks
     * for its share.
     *
     * @param allowance the most points this shot may draw this tick
     * @return the points, the flame frontier first
     */
    public List<Sample> points(int allowance) {
        List<Sample> out = new ArrayList<>();
        int limit = Math.min(budget, allowance);
        if (arrived || limit <= 0) {
            return out;
        }
        out.add(new Sample(Kind.FLAME_FRONTIER, headX, headY, headZ));
        int remaining = limit - 1;
        int perStrand = (spiralOn ? remaining - remaining / SPIRAL_SHARE : remaining) / strands;
        if (perStrand == 0 && remaining >= strands) {
            perStrand = 1;
        }
        int spiralCount = spiralOn ? Math.max(0, Math.min(remaining / SPIRAL_SHARE, remaining - perStrand * strands)) : 0;
        double spin = -RailgunHelix.TWO_PI * spinTurnsPerSecond * ticks / RailgunSettings.TICKS_PER_SECOND;
        if (perStrand > 0) {
            double spacing = spacingFor(headArc, perStrand);
            int last = (int) Math.floor(headArc / spacing);
            for (int strand = 0; strand < strands; strand++) {
                double shift = RailgunHelix.TWO_PI * strand / strands + spin;
                for (int k = 0; k <= last; k++) {
                    pointOnStrand(k * spacing, shift, out);
                }
            }
        }
        if (spiralCount > 0) {
            double length = headArc - spiralStartArc;
            double spacing = spacingFor(length, spiralCount);
            int last = (int) Math.floor(length / spacing);
            for (int k = 0; k <= last; k++) {
                pointOnSpiral(spiralStartArc + k * spacing, spin, out);
            }
        }
        return out;
    }

    /**
     * The spacing of samples along a length, so that they number at most {@code maxSamples}: the sample spacing when the budget allows it,
     * otherwise the spacing doubled until they fit. Doubling keeps the work of a tick bounded however long the path is.
     */
    private double spacingFor(double length, int maxSamples) {
        double spacing = sampleSpacing;
        while (Math.floor(length / spacing) + 1 > maxSamples) {
            spacing *= 2.0;
        }
        return spacing;
    }

    /** True once the frontier has reached the target. */
    public boolean arrived() {
        return arrived;
    }

    /** The advances made so far, one per tick since the shot was fired. */
    public int ticks() {
        return ticks;
    }

    /** The frontier's east-west coordinate, in blocks. */
    public double frontierX() {
        return headX;
    }

    /** The frontier's height, in blocks. */
    public double frontierY() {
        return headY;
    }

    /** The frontier's north-south coordinate, in blocks. */
    public double frontierZ() {
        return headZ;
    }

    /** The distance the frontier has travelled along its path from the barrel, in blocks. */
    public double pathLength() {
        return headArc;
    }

    /** True once the spiral delay has passed, and the blue spiral is drawn. */
    public boolean spiralStarted() {
        return spiralOn;
    }

    /** The vertices kept of the path, which never exceeds {@link #CAPACITY}. For tests and diagnostics. */
    public int historySize() {
        return count;
    }

    private void commitIfFar() {
        if (headArc - vertexArc[count - 1] < spacing) {
            return;
        }
        if (count == CAPACITY) {
            decimate();
        }
        vertexX[count] = headX;
        vertexY[count] = headY;
        vertexZ[count] = headZ;
        vertexArc[count] = headArc;
        count++;
    }

    /** Drops every second vertex, keeping the first, and doubles the spacing. The distance along the path of each kept vertex is unchanged. */
    private void decimate() {
        int kept = 0;
        for (int i = 0; i < count; i += 2) {
            vertexX[kept] = vertexX[i];
            vertexY[kept] = vertexY[i];
            vertexZ[kept] = vertexZ[i];
            vertexArc[kept] = vertexArc[i];
            kept++;
        }
        count = kept;
        spacing *= 2.0;
    }

    /** A point of a red strand: its angle about the path is the helix phase at the distance, plus the strand's shift and the spin. */
    private void pointOnStrand(double arc, double shift, List<Sample> out) {
        pathAt(arc);
        RailgunHelix.offset(pathPoint[3], pathPoint[4], pathPoint[5],
                RailgunHelix.phase(arc, strandTurns) + shift, strandRadius, offset);
        out.add(new Sample(Kind.RED_STRAND, pathPoint[0] + offset[0], pathPoint[1] + offset[1], pathPoint[2] + offset[2]));
    }

    /** A point of the blue spiral: its angle about the path is its own helix phase plus the spin, so it turns with the strands. */
    private void pointOnSpiral(double arc, double spin, List<Sample> out) {
        pathAt(arc);
        RailgunHelix.offset(pathPoint[3], pathPoint[4], pathPoint[5],
                RailgunHelix.phase(arc, spiralTurns) + spin, spiralRadius, offset);
        out.add(new Sample(Kind.BLUE_SPIRAL, pathPoint[0] + offset[0], pathPoint[1] + offset[1], pathPoint[2] + offset[2]));
    }

    /**
     * Writes into {@link #pathPoint} the position of the path at a distance along it, and its unit tangent. A distance past the committed
     * vertices is on the segment to the frontier. Where the segment has no length, the tangent is the last direction of travel.
     */
    private void pathAt(double arc) {
        int last = count - 1;
        double sx;
        double sy;
        double sz;
        double ex;
        double ey;
        double ez;
        double s0;
        double s1;
        if (arc >= vertexArc[last]) {
            sx = vertexX[last];
            sy = vertexY[last];
            sz = vertexZ[last];
            ex = headX;
            ey = headY;
            ez = headZ;
            s0 = vertexArc[last];
            s1 = headArc;
        } else {
            int i = segmentBefore(arc);
            sx = vertexX[i];
            sy = vertexY[i];
            sz = vertexZ[i];
            ex = vertexX[i + 1];
            ey = vertexY[i + 1];
            ez = vertexZ[i + 1];
            s0 = vertexArc[i];
            s1 = vertexArc[i + 1];
        }
        double length = s1 - s0;
        double t = length > 0.0 ? Math.max(0.0, Math.min(1.0, (arc - s0) / length)) : 0.0;
        pathPoint[0] = sx + (ex - sx) * t;
        pathPoint[1] = sy + (ey - sy) * t;
        pathPoint[2] = sz + (ez - sz) * t;
        double cx = ex - sx;
        double cy = ey - sy;
        double cz = ez - sz;
        double chord = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (chord > 1e-9) {
            pathPoint[3] = cx / chord;
            pathPoint[4] = cy / chord;
            pathPoint[5] = cz / chord;
        } else {
            pathPoint[3] = dirX;
            pathPoint[4] = dirY;
            pathPoint[5] = dirZ;
        }
    }

    /** The index of the committed vertex that starts the segment holding a distance: the last one at or before it. */
    private int segmentBefore(double arc) {
        int lo = 0;
        int hi = count - 2;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (vertexArc[mid] <= arc) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }
}
