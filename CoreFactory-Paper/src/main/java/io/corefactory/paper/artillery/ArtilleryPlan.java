package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryBallistics;
import io.corefactory.core.artillery.ArtilleryBallistics.ArcSample;
import io.corefactory.core.artillery.ArtilleryBallistics.Trajectory;
import io.corefactory.core.artillery.ArtillerySettings;
import io.corefactory.core.artillery.Position;

import java.util.List;

/**
 * The flight of one shot, planned before it is fired: the muzzle speed that lands the shell on its aim at the barrel's elevation, the
 * simulated arc, and the direction on the ground. The arc is sampled once per tick, and {@link #positionAt} places the shell in the world at
 * a tick of its flight. Pure, so the flight can be tested without a server.
 *
 * @param launch         the point the shell leaves the barrel, in blocks
 * @param distance       the horizontal distance from the launch to the aim point, in blocks
 * @param elevationDegrees the elevation of the barrel, in degrees
 * @param speed          the muzzle speed, in blocks per tick
 * @param trajectory     the simulated arc, which lands on the aim point
 * @param dirX           the x component of the unit direction on the ground, from the launch towards the aim
 * @param dirZ           the z component of the unit direction on the ground
 */
public record ArtilleryPlan(Position launch, double distance, double elevationDegrees, double speed, Trajectory trajectory,
                            double dirX, double dirZ) {

    /**
     * Plans the flight of a shot.
     *
     * @param settings         the settings whose drag the flight uses
     * @param launch           the point the shell leaves the barrel
     * @param aimX             the x of the aim point, in blocks
     * @param aimZ             the z of the aim point, in blocks
     * @param groundY          the height of the ground at the aim point, in blocks
     * @param elevationDegrees the elevation of the barrel, in degrees, above 0 and below 90
     * @return the plan
     * @throws IllegalArgumentException when the aim is on the launch point or the elevation is out of range
     * @throws IllegalStateException    when the shell does not come down on the aim point
     */
    public static ArtilleryPlan of(ArtillerySettings settings, Position launch, double aimX, double aimZ, double groundY,
                                   double elevationDegrees) {
        double dx = aimX - launch.x();
        double dz = aimZ - launch.z();
        double distance = Math.hypot(dx, dz);
        double targetHeight = groundY - launch.y();
        double drag = ArtilleryBallistics.dragPerTick(settings.dragPerSecond());
        double speed = ArtilleryBallistics.speedFor(elevationDegrees, distance, targetHeight,
                ArtilleryBallistics.GRAVITY_PER_TICK2, drag);
        Trajectory trajectory = ArtilleryBallistics.fly(elevationDegrees, speed, targetHeight,
                ArtilleryBallistics.GRAVITY_PER_TICK2, drag);
        if (!trajectory.landed()) {
            throw new IllegalStateException("the shell does not come down on the aim point");
        }
        return new ArtilleryPlan(launch, distance, elevationDegrees, speed, trajectory, dx / distance, dz / distance);
    }

    /** The last tick of the flight: the tick the shell lands on. */
    public int lastTick() {
        return trajectory.arc().size() - 1;
    }

    /**
     * Where the shell is at a tick of its flight: the sample of the arc at that tick, placed along the direction on the ground. A tick past
     * the landing is the landing point, and a negative tick is the launch.
     *
     * @param tick the tick of the flight, from 0 at the launch
     * @return the position in blocks
     */
    public Position positionAt(int tick) {
        List<ArcSample> arc = trajectory.arc();
        ArcSample sample = arc.get(Math.max(0, Math.min(tick, lastTick())));
        return new Position(launch.x() + dirX * sample.distance(), launch.y() + sample.height(),
                launch.z() + dirZ * sample.distance());
    }
}
