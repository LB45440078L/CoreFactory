package io.corefactory.core.turret;

/**
 * The aiming maths of a turret, in degrees with Minecraft's convention: yaw 0 faces south (+Z), yaw 90 faces west (-X), and a positive
 * pitch looks down. A turret turns toward its target by at most a fixed angle each tick, stays within an arc centred on the way it
 * faced when it was placed, and tilts within a limit. Pure: no world, no entity.
 */
public final class TurretAim {

    /** How close, in degrees on both axes, the barrel must be to the target before a shot is fired. */
    public static final double AIM_TOLERANCE_DEGREES = 3.0;

    private static final double EPS = 1e-9;

    /** The degrees of a full turn: an arc of this size or more holds every yaw. */
    private static final double FULL_TURN_DEGREES = 360.0;

    private TurretAim() {
    }

    /** An angle wrapped into [-180, 180). */
    public static double wrap(double degrees) {
        double wrapped = (degrees + 180.0) % 360.0;
        if (wrapped < 0) {
            wrapped += 360.0;
        }
        return wrapped - 180.0;
    }

    /** The yaw that looks along a horizontal offset (dx, dz). */
    public static double yawOf(double dx, double dz) {
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** The pitch that looks along an offset (dx, dy, dz): positive when the offset is below. */
    public static double pitchOf(double dx, double dy, double dz) {
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        return Math.toDegrees(-Math.atan2(dy, horizontal));
    }

    /** The unit vector {x, y, z} that a yaw and a pitch look along. */
    public static double[] direction(double yaw, double pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        return new double[] {
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
        };
    }

    /**
     * True when a yaw lies within half the arc on either side of the home yaw. An arc of a full turn or more holds every yaw, so the
     * default arc refuses no target by its facing.
     */
    public static boolean withinArc(double yaw, double homeYaw, double arcDegrees) {
        if (arcDegrees >= FULL_TURN_DEGREES) {
            return true;
        }
        return Math.abs(wrap(yaw - homeYaw)) <= arcDegrees / 2.0 + EPS;
    }

    /**
     * True when the rotation sound plays this tick. It plays only while the turret turns toward a target it has detected; an idle sweep
     * is silent.
     *
     * @param aimingAtTarget true when a cannon holds a detected target
     * @param turning        true when the barrel moved this tick
     */
    public static boolean playsRotationSound(boolean aimingAtTarget, boolean turning) {
        return aimingAtTarget && turning;
    }

    /** True when a target can be reached: within the arc on the yaw axis, and within the tilt limit on the pitch axis. */
    public static boolean inReach(double targetYaw, double targetPitch, double homeYaw, TurretSettings settings) {
        return withinArc(targetYaw, homeYaw, settings.arcDegrees())
                && Math.abs(targetPitch) <= settings.pitchLimitDegrees() + EPS;
    }

    /**
     * One step of turning from {@code current} toward {@code target} along the shorter way, by at most {@code maxStep} degrees.
     *
     * @return the new angle, wrapped; equal to the target once it is reached
     */
    public static double step(double current, double target, double maxStep) {
        double diff = wrap(target - current);
        if (Math.abs(diff) <= maxStep) {
            return wrap(current + diff);
        }
        return wrap(current + Math.copySign(maxStep, diff));
    }

    /** True when the barrel is aimed at the target within the aim tolerance on both axes. */
    public static boolean aligned(double yaw, double pitch, double targetYaw, double targetPitch) {
        return Math.abs(wrap(yaw - targetYaw)) <= AIM_TOLERANCE_DEGREES && Math.abs(pitch - targetPitch) <= AIM_TOLERANCE_DEGREES;
    }

    /**
     * The turret's yaw and sweep direction after one tick of idle sweeping. A partial arc sweeps from one end to the other and turns back
     * at each end. A full turn has no ends, since both ends are the same angle, so it turns on in its heading and never reverses.
     *
     * @param yaw     the yaw now
     * @param homeYaw the centre of the arc
     * @param arc     the arc's total sweep, in degrees
     * @param stepDeg the most it turns in one tick
     * @param forward true when it is heading for the arc's end on the positive side, or turning positively for a full turn
     */
    public static Idle idle(double yaw, double homeYaw, double arc, double stepDeg, boolean forward) {
        if (arc >= FULL_TURN_DEGREES) {
            return new Idle(wrap(yaw + (forward ? stepDeg : -stepDeg)), forward);
        }
        double end = homeYaw + (forward ? arc / 2.0 : -arc / 2.0);
        double next = step(yaw, end, stepDeg);
        boolean reached = Math.abs(wrap(next - end)) < EPS;
        return new Idle(next, reached ? !forward : forward);
    }

    /**
     * A spread offset for one shot.
     *
     * @param unit        a random number from -1 to 1
     * @param spreadDeg   the half angle of the spread, in degrees
     * @return the offset in degrees, from -spreadDeg to spreadDeg
     */
    public static double spreadOffset(double unit, double spreadDeg) {
        return Math.max(-1.0, Math.min(1.0, unit)) * spreadDeg;
    }

    /**
     * The state of an idle sweep after one tick.
     *
     * @param yaw     the yaw
     * @param forward the heading: true toward the positive end of the arc
     */
    public record Idle(double yaw, boolean forward) {
    }
}
