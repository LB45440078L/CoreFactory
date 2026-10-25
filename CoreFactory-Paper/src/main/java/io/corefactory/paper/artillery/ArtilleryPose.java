package io.corefactory.paper.artillery;

import io.corefactory.core.turret.TurretAim;

/**
 * The pose of an artillery piece: the yaw of its body and the pitch of its barrel, in degrees, and the rotations its displays show them with.
 * The body turns about the vertical axis through the middle of its block, and the barrel tilts about its trunnion. Both use the game's convention,
 * which {@link TurretAim} also uses: yaw 0 faces south, toward +z, yaw 90 faces west, toward -x, and yaw 180 faces north. A barrel's elevation is
 * positive when its muzzle rises. Pure: no world and no entity, so the maths is tested without a server.
 *
 * <p>A model's front faces north, toward -z, so a body of yaw {@link #NORTH_YAW} is shown with no rotation. A display turns its model by
 * {@code 180 - yaw} about the vertical axis, which is the rotation about +y that turns the model's front to the body's yaw.
 */
public final class ArtilleryPose {

    /** The yaw of a body that faces north: the front of its models, with no rotation. The yaw of a new piece. */
    public static final double NORTH_YAW = 180.0;

    /** The pitch of a barrel that lies horizontal: the idle elevation. */
    public static final double IDLE_PITCH = 0.0;

    private static final double EPS = 1e-9;

    private ArtilleryPose() {
    }

    /**
     * The yaw that turns a body's front to an aim point: the direction from the body's centre to the aim, in the game's convention. An aim
     * straight over the body has no direction, so the yaw stays as it is.
     *
     * @param fromX   the x of the body's centre
     * @param fromZ   the z of the body's centre
     * @param aimX    the x of the aim point
     * @param aimZ    the z of the aim point
     * @param current the body's yaw now, kept when the aim is over the body
     * @return the yaw, from 0 up to 360
     */
    public static double bodyYaw(double fromX, double fromZ, double aimX, double aimZ, double current) {
        double dx = aimX - fromX;
        double dz = aimZ - fromZ;
        if (Math.abs(dx) < EPS && Math.abs(dz) < EPS) {
            return current;
        }
        return normalize(TurretAim.yawOf(dx, dz));
    }

    /**
     * The rotation about the vertical axis that a display gives its model, in degrees. The model's front faces -z, so the rotation is 180 degrees
     * less the body's yaw.
     *
     * @param bodyYaw the body's yaw
     * @return the rotation, from 0 up to 360
     */
    public static double displayYaw(double bodyYaw) {
        return normalize(NORTH_YAW - bodyYaw);
    }

    /**
     * The translation of a barrel's display, in blocks from the centre of its block. A display rotates about its own origin, the centre of the
     * block, and the barrel must turn about its trunnion. So the translation moves the trunnion where it rests: the rotation then leaves the
     * trunnion in place, and the barrel swings about it. The rotation is the body's yaw, and then the elevation about the barrel's own X axis.
     *
     * @param restTranslation the translation of the display at rest, in blocks: the lift and the offset of the barrel's model
     * @param trunnion        the height of the trunnion above the centre of the block, in blocks, at rest
     * @param scale           the scale of the display
     * @param bodyYaw         the body's yaw, which the barrel turns with
     * @param pitch           the barrel's elevation, in degrees
     * @return the translation, x, y and z, in blocks
     */
    public static double[] barrelTranslation(double restTranslation, double trunnion, double scale, double bodyYaw, double pitch) {
        double theta = Math.toRadians(displayYaw(bodyYaw));
        double phi = Math.toRadians(pitch);
        // The trunnion's offset from the display's origin, in model units before the scale: at rest it is at the trunnion height.
        double offset = (trunnion - restTranslation) / scale;
        return new double[] {
            -scale * offset * Math.sin(phi) * Math.sin(theta),
            restTranslation + scale * offset * (1 - Math.cos(phi)),
            -scale * offset * Math.sin(phi) * Math.cos(theta)
        };
    }

    /**
     * The direction a barrel's muzzle points in, as a unit vector {x, y, z}, for a body yaw and a barrel elevation, as the displays show them.
     * With an elevation of 0 it is the body's direction, which {@link TurretAim#direction} gives for the same yaw and a pitch of 0.
     *
     * @param bodyYaw the body's yaw
     * @param pitch   the barrel's elevation, in degrees, positive when the muzzle rises
     * @return the unit vector, x, y and z
     */
    public static double[] muzzle(double bodyYaw, double pitch) {
        double theta = Math.toRadians(displayYaw(bodyYaw));
        double phi = Math.toRadians(pitch);
        return new double[] {-Math.cos(phi) * Math.sin(theta), Math.sin(phi), -Math.cos(phi) * Math.cos(theta)};
    }

    /**
     * The translation a barrel's display takes when it recoils: back along the barrel's axis, against its muzzle direction, by a distance. It is
     * a translation only, so the rotation, and the aim it gives the barrel, are the same as at rest.
     *
     * @param bodyYaw the body's yaw
     * @param pitch   the barrel's elevation, in degrees
     * @param blocks  the distance back along the axis, in blocks; 0 at rest
     * @return the translation, x, y and z, in blocks
     */
    public static double[] recoil(double bodyYaw, double pitch, double blocks) {
        double[] muzzle = muzzle(bodyYaw, pitch);
        return new double[] {-muzzle[0] * blocks, -muzzle[1] * blocks, -muzzle[2] * blocks};
    }

    /** An angle in degrees, from 0 up to 360. Adding zero turns a negative zero into zero. */
    static double normalize(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped < 0) {
            wrapped += 360.0;
        }
        return wrapped + 0.0;
    }
}
