package io.corefactory.core.artillery;

import java.util.ArrayList;
import java.util.List;

/**
 * The flight of a shell in the vertical plane of its shot. The shell is integrated one tick at a time with a fourth-order
 * Runge-Kutta step, under constant gravity and a small linear air drag, so the arc is close to a parabola but not exact.
 * The fourth-order step is exact for a drag-free shot, and the landing and the apex are found by bisecting inside the last tick, so
 * the range and the apex carry no step error of their own.
 *
 * <p>Units: a speed is in blocks per tick, gravity is in blocks per tick squared, and drag is a fraction of the speed lost per tick.
 * Gravity is 9.8 m/s-equivalent: one block is one metre, and a tick is 0.05 seconds, so gravity is 9.8 x 0.05 x 0.05 blocks per tick
 * squared.
 */
public final class ArtilleryBallistics {

    /** The length of one server tick, in seconds. */
    public static final double SECONDS_PER_TICK = 0.05;

    /** Standard gravity, in m/s2, which is one block per metre. */
    public static final double GRAVITY_M_PER_S2 = 9.8;

    /** Gravity in blocks per tick squared. */
    public static final double GRAVITY_PER_TICK2 = GRAVITY_M_PER_S2 * SECONDS_PER_TICK * SECONDS_PER_TICK;

    /** The longest flight the simulation follows, in ticks: an hour. A shot still in the air after that is not landed. */
    private static final int MAX_TICKS = 72_000;

    /** Bisection steps that place a landing or an apex inside its tick. Each halves the interval, so 60 is far below a float's step. */
    private static final int REFINE_STEPS = 60;

    /** Bisection steps that find the speed for a range. Each halves the interval, so 100 is far below a float's step. */
    private static final int SEARCH_STEPS = 100;

    /** Doublings that bracket the speed for a range. 100 doublings reach any speed a shell could have. */
    private static final int MAX_DOUBLINGS = 100;

    private ArtilleryBallistics() {
    }

    /**
     * One sampled point of an arc, in the vertical plane of the shot.
     *
     * @param distance the horizontal distance from the launch point along the shot, in blocks
     * @param height   the height above the launch point, in blocks
     */
    public record ArcSample(double distance, double height) {
    }

    /**
     * A simulated flight.
     *
     * @param range      the horizontal distance from the launch point to where the shell comes down through the target height, in
     *                   blocks; 0 when the shell never comes down through it
     * @param apex       the highest point of the arc above the launch point, in blocks
     * @param flightTicks the ticks from the launch to the landing, fractional; the ticks flown when the shell did not land
     * @param landed     whether the shell came down through the target height
     * @param arc        the sampled points of the arc, one per tick, from the launch point to the landing point
     */
    public record Trajectory(double range, double apex, double flightTicks, boolean landed, List<ArcSample> arc) {
    }

    /** The state of a shell: its position and velocity in the vertical plane. A derivative reuses it: x and y hold the velocity. */
    private record State(double x, double y, double vx, double vy) {
    }

    /**
     * The linear drag per tick for a drag given per second.
     *
     * @param dragPerSecond the fraction of the speed lost per second
     * @return the fraction lost per tick
     */
    public static double dragPerTick(double dragPerSecond) {
        return dragPerSecond * SECONDS_PER_TICK;
    }

    /**
     * Simulates one shot: its arc, its apex, and where it comes down through the target height.
     *
     * @param elevationDegrees the elevation of the barrel, above the horizontal, from above 0 to below 90 degrees
     * @param speed            the muzzle speed, in blocks per tick, above 0
     * @param targetHeight     the height of the target above the launch point, in blocks; the landing is where the shell comes down
     *                         through this height
     * @param gravity          the gravity, in blocks per tick squared
     * @param drag             the linear drag, the fraction of the speed lost per tick; 0 for no drag
     * @return the flight
     * @throws IllegalArgumentException when the elevation or the speed is out of range
     */
    public static Trajectory fly(double elevationDegrees, double speed, double targetHeight, double gravity, double drag) {
        requireElevation(elevationDegrees);
        if (!(speed > 0) || !Double.isFinite(speed)) {
            throw new IllegalArgumentException("muzzle speed must be finite and above 0: " + speed);
        }
        double theta = Math.toRadians(elevationDegrees);
        State state = new State(0.0, 0.0, speed * Math.cos(theta), speed * Math.sin(theta));
        List<ArcSample> arc = new ArrayList<>();
        arc.add(new ArcSample(0.0, 0.0));
        double apex = 0.0;
        boolean apexFound = false;
        int tick = 0;
        while (tick < MAX_TICKS) {
            State next = step(state, 1.0, gravity, drag);
            if (!apexFound && state.vy() > 0 && next.vy() <= 0) {
                apex = apexHeight(state, gravity, drag);
                apexFound = true;
            } else if (!apexFound) {
                apex = Math.max(apex, next.y());
            }
            if (state.y() > targetHeight && next.y() <= targetHeight) {
                double fraction = crossingFraction(state, targetHeight, gravity, drag);
                State at = step(state, fraction, gravity, drag);
                arc.add(new ArcSample(at.x(), at.y()));
                return new Trajectory(at.x(), apex, tick + fraction, true, List.copyOf(arc));
            }
            arc.add(new ArcSample(next.x(), next.y()));
            if (next.vy() < 0 && next.y() < targetHeight) {
                break;
            }
            state = next;
            tick++;
        }
        return new Trajectory(0.0, apex, tick, false, List.copyOf(arc));
    }

    /**
     * The muzzle speed that lands a shot at a horizontal distance, found by bisection on the simulated range. The simulated range
     * is used rather than the closed form, so drag and the target height are included.
     *
     * @param elevationDegrees the elevation of the barrel, from above 0 to below 90 degrees
     * @param distance         the horizontal distance to the target, in blocks, above 0
     * @param targetHeight     the height of the target above the launch point, in blocks
     * @param gravity          the gravity, in blocks per tick squared
     * @param drag             the linear drag per tick
     * @return the muzzle speed in blocks per tick, which lands within a tiny fraction of a block of the distance
     * @throws IllegalArgumentException when no speed reaches the distance at this elevation
     */
    public static double speedFor(double elevationDegrees, double distance, double targetHeight, double gravity, double drag) {
        if (!(distance > 0) || !Double.isFinite(distance)) {
            throw new IllegalArgumentException("distance must be finite and above 0: " + distance);
        }
        double low = 0.0;
        double high = 1.0;
        int doublings = 0;
        while (fly(elevationDegrees, high, targetHeight, gravity, drag).range() < distance) {
            if (doublings++ >= MAX_DOUBLINGS) {
                throw new IllegalArgumentException("no speed reaches " + distance + " blocks at " + elevationDegrees + " degrees");
            }
            low = high;
            high *= 2.0;
        }
        for (int i = 0; i < SEARCH_STEPS; i++) {
            double mid = (low + high) / 2.0;
            if (fly(elevationDegrees, mid, targetHeight, gravity, drag).range() < distance) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    /**
     * The ground point a shot lands on, from the launch point, a direction on the ground and the range.
     *
     * @param launch the launch point
     * @param dirX   the x component of the direction on the ground; it need not be a unit vector
     * @param dirZ   the z component of the direction on the ground
     * @param range  the horizontal distance along the direction, in blocks
     * @return the landing point, on the ground plane
     * @throws IllegalArgumentException when the direction is zero
     */
    public static GroundPoint landing(Position launch, double dirX, double dirZ, double range) {
        double length = Math.hypot(dirX, dirZ);
        if (!(length > 0) || !Double.isFinite(length)) {
            throw new IllegalArgumentException("the direction on the ground must not be zero");
        }
        return new GroundPoint(launch.x() + dirX / length * range, launch.z() + dirZ / length * range);
    }

    private static void requireElevation(double elevationDegrees) {
        if (!(elevationDegrees > 0 && elevationDegrees < 90)) {
            throw new IllegalArgumentException("elevation must be above 0 and below 90 degrees: " + elevationDegrees);
        }
    }

    /** The derivative of a state: the velocity, and the acceleration from drag and gravity. */
    private static State derivative(State s, double gravity, double drag) {
        return new State(s.vx(), s.vy(), -drag * s.vx(), -drag * s.vy() - gravity);
    }

    private static State advance(State s, State d, double h) {
        return new State(s.x() + h * d.x(), s.y() + h * d.y(), s.vx() + h * d.vx(), s.vy() + h * d.vy());
    }

    /** One classic Runge-Kutta step of length {@code h} ticks. */
    private static State step(State s, double h, double gravity, double drag) {
        State k1 = derivative(s, gravity, drag);
        State k2 = derivative(advance(s, k1, h / 2.0), gravity, drag);
        State k3 = derivative(advance(s, k2, h / 2.0), gravity, drag);
        State k4 = derivative(advance(s, k3, h), gravity, drag);
        double w = h / 6.0;
        return new State(
                s.x() + w * (k1.x() + 2 * k2.x() + 2 * k3.x() + k4.x()),
                s.y() + w * (k1.y() + 2 * k2.y() + 2 * k3.y() + k4.y()),
                s.vx() + w * (k1.vx() + 2 * k2.vx() + 2 * k3.vx() + k4.vx()),
                s.vy() + w * (k1.vy() + 2 * k2.vy() + 2 * k3.vy() + k4.vy()));
    }

    /**
     * The fraction of the tick, from 0 to 1, at which the shell comes down through the target height. The shell is above the height
     * at the start of the tick and at or below it at the end, so the fraction is bracketed.
     */
    private static double crossingFraction(State s, double targetHeight, double gravity, double drag) {
        double low = 0.0;
        double high = 1.0;
        for (int i = 0; i < REFINE_STEPS; i++) {
            double mid = (low + high) / 2.0;
            if (step(s, mid, gravity, drag).y() > targetHeight) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    /** The height at the top of the arc, found inside the tick where the vertical speed changes sign. */
    private static double apexHeight(State s, double gravity, double drag) {
        double low = 0.0;
        double high = 1.0;
        for (int i = 0; i < REFINE_STEPS; i++) {
            double mid = (low + high) / 2.0;
            if (step(s, mid, gravity, drag).vy() > 0) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return step(s, high, gravity, drag).y();
    }
}
