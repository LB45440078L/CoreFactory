package io.corefactory.paper.artillery;

import io.corefactory.core.turret.TurretAim;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Checks the pose maths of an artillery piece: the body's yaw toward an aim, the barrel's tilt, and the convention the displays use. */
class ArtilleryPoseTest {

    private static final double EPS = 1e-9;

    /** The centre of the piece's block at block (0, 0). */
    private static final double FROM = 0.5;

    @Test
    void aimDueNorthGivesYaw180() {
        assertThat(ArtilleryPose.bodyYaw(FROM, FROM, FROM, FROM - 20, 0.0)).isCloseTo(180.0, within(EPS));
    }

    @Test
    void aimDueSouthGivesYaw0() {
        assertThat(ArtilleryPose.bodyYaw(FROM, FROM, FROM, FROM + 20, 180.0)).isCloseTo(0.0, within(EPS));
    }

    @Test
    void aimDueWestGivesYaw90AndDueEastGivesYaw270() {
        assertThat(ArtilleryPose.bodyYaw(FROM, FROM, FROM - 20, FROM, 0.0)).isCloseTo(90.0, within(EPS));
        assertThat(ArtilleryPose.bodyYaw(FROM, FROM, FROM + 20, FROM, 0.0)).isCloseTo(270.0, within(EPS));
    }

    @Test
    void aimStraightOverThePieceKeepsTheYaw() {
        assertThat(ArtilleryPose.bodyYaw(FROM, FROM, FROM, FROM, 37.0)).isEqualTo(37.0);
    }

    @Test
    void theYawIsMeasuredFromTheCentreOfTheBlock() {
        // A piece at block (10, 10) with its aim due north of its centre, on the same column.
        assertThat(ArtilleryPose.bodyYaw(10.5, 10.5, 10.5, 0.5, 0.0)).isCloseTo(180.0, within(EPS));
    }

    @Test
    void theYawIsTheDirectionThatTheTurretConventionLooksAlong() {
        for (int degrees = 0; degrees < 360; degrees += 15) {
            double[] toward = TurretAim.direction(degrees, 0.0);
            double yaw = ArtilleryPose.bodyYaw(FROM, FROM, FROM + 30 * toward[0], FROM + 30 * toward[2], 0.0);
            assertThat(TurretAim.wrap(yaw - degrees)).as("yaw toward %d degrees", degrees).isCloseTo(0.0, within(1e-6));
        }
    }

    @Test
    void theBodyFacesTheAimOnceTurned() {
        double[][] aims = {{30, 0}, {-20, 10}, {0, -40}, {25, 25}, {-30, -30}};
        for (double[] offset : aims) {
            double yaw = ArtilleryPose.bodyYaw(FROM, FROM, FROM + offset[0], FROM + offset[1], 0.0);
            double[] front = ArtilleryPose.muzzle(yaw, 0.0);
            double length = Math.hypot(offset[0], offset[1]);
            assertThat(front[0]).as("x toward %s", Arrays.toString(offset)).isCloseTo(offset[0] / length, within(EPS));
            assertThat(front[1]).as("y toward %s", Arrays.toString(offset)).isCloseTo(0.0, within(EPS));
            assertThat(front[2]).as("z toward %s", Arrays.toString(offset)).isCloseTo(offset[1] / length, within(EPS));
        }
    }

    @Test
    void aNorthFacingBodyIsShownWithNoRotation() {
        assertThat(ArtilleryPose.displayYaw(ArtilleryPose.NORTH_YAW)).isCloseTo(0.0, within(EPS));
        assertThat(ArtilleryPose.displayYaw(90.0)).isCloseTo(90.0, within(EPS));
        assertThat(ArtilleryPose.displayYaw(0.0)).isCloseTo(180.0, within(EPS));
    }

    @Test
    void aBarrelAt45DegreesRisesToTheNorth() {
        double[] muzzle = ArtilleryPose.muzzle(180.0, 45.0);
        assertThat(muzzle[0]).isCloseTo(0.0, within(EPS));
        assertThat(muzzle[1]).isCloseTo(Math.sqrt(0.5), within(EPS));
        assertThat(muzzle[2]).isCloseTo(-Math.sqrt(0.5), within(EPS));
    }

    @Test
    void aBarrelAtTheClusterEnrichedMinimumOf25DegreesRises() {
        double[] muzzle = ArtilleryPose.muzzle(180.0, 25.0);
        assertThat(muzzle[1]).isCloseTo(Math.sin(Math.toRadians(25)), within(EPS));
        assertThat(muzzle[1]).isPositive();
    }

    @Test
    void anIdleBarrelLiesHorizontal() {
        assertThat(ArtilleryPose.muzzle(123.0, ArtilleryPose.IDLE_PITCH)[1]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void theMuzzleMatchesTheTurretConventionWithTheElevationAsItsNegativePitch() {
        // The turret's pitch is positive when it looks down, so an elevation of e is the turret's pitch of -e.
        for (double yaw : new double[] {0, 45, 90, 180, 270}) {
            for (double elevation : new double[] {0, 25, 45, 80}) {
                double[] muzzle = ArtilleryPose.muzzle(yaw, elevation);
                double[] turret = TurretAim.direction(yaw, -elevation);
                for (int axis = 0; axis < 3; axis++) {
                    assertThat(muzzle[axis]).as("axis %d at yaw %s elevation %s", axis, yaw, elevation)
                            .isCloseTo(turret[axis], within(EPS));
                }
            }
        }
    }

    @Test
    void theBarrelTranslationLeavesTheTrunnionInPlace() {
        double scale = 1.02;
        double rest = 1.0;
        double trunnion = 1.125;
        for (double yaw : new double[] {0, 60, 180, 300}) {
            for (double elevation : new double[] {0, 25, 45, 85}) {
                double[] moved = ArtilleryPose.barrelTranslation(rest, trunnion, scale, yaw, elevation);
                // The trunnion is at (0, pv, 0) in the display's model units, with pv = (trunnion - rest) / scale, turned by the barrel's pose.
                double pv = (trunnion - rest) / scale;
                double theta = Math.toRadians(ArtilleryPose.displayYaw(yaw));
                double phi = Math.toRadians(elevation);
                double x = moved[0] + scale * pv * Math.sin(phi) * Math.sin(theta);
                double y = moved[1] + scale * pv * Math.cos(phi);
                double z = moved[2] + scale * pv * Math.sin(phi) * Math.cos(theta);
                assertThat(x).as("x at yaw %s elevation %s", yaw, elevation).isCloseTo(0.0, within(EPS));
                assertThat(y).as("y at yaw %s elevation %s", yaw, elevation).isCloseTo(trunnion, within(EPS));
                assertThat(z).as("z at yaw %s elevation %s", yaw, elevation).isCloseTo(0.0, within(EPS));
            }
        }
    }

    @Test
    void aBarrelAtRestHasItsLayoutTranslation() {
        double[] moved = ArtilleryPose.barrelTranslation(4.0, 3.25, 1.0, 180.0, ArtilleryPose.IDLE_PITCH);
        assertThat(moved[0]).isCloseTo(0.0, within(EPS));
        assertThat(moved[1]).isCloseTo(4.0, within(EPS));
        assertThat(moved[2]).isCloseTo(0.0, within(EPS));
    }

    @Test
    void aRecoilMovesTheBarrelBackAlongItsAxisAndKeepsItsAim() {
        double yaw = 30.0;
        double pitch = 45.0;
        double[] muzzle = ArtilleryPose.muzzle(yaw, pitch);
        double[] back = ArtilleryPose.recoil(yaw, pitch, 0.5);

        // The kick is against the muzzle direction, so the barrel moves towards its breech.
        assertThat(back[0]).isCloseTo(-0.5 * muzzle[0], within(EPS));
        assertThat(back[1]).isCloseTo(-0.5 * muzzle[1], within(EPS));
        assertThat(back[2]).isCloseTo(-0.5 * muzzle[2], within(EPS));
        // The muzzle is a unit vector, so the kick is exactly 0.5 blocks long.
        assertThat(Math.sqrt(back[0] * back[0] + back[1] * back[1] + back[2] * back[2])).isCloseTo(0.5, within(EPS));
        // The rotation is not part of the recoil: at rest the recoil is nothing.
        double[] rest = ArtilleryPose.recoil(yaw, pitch, 0.0);
        assertThat(rest[0]).isCloseTo(0.0, within(EPS));
        assertThat(rest[1]).isCloseTo(0.0, within(EPS));
        assertThat(rest[2]).isCloseTo(0.0, within(EPS));
    }
}
