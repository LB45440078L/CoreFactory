package io.corefactory.paper.logistics;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;

import java.util.Optional;

/**
 * Where a logistics device goes and how it faces, from the block and the face a player aimed at. A belt takes its flow from the
 * player's yaw; a picker and a splitter take their input side from the face, and so need a side face (north, south, east or
 * west). A face is an offset {@code (dx, dy, dz)} with one component of magnitude one. Pure.
 */
public final class PlacementAim {

    /**
     * Where a device goes.
     *
     * @param target the cell the device fills, the aimed block moved one step out through the face
     * @param flow   the flow of a belt or a belt duplex; null for a picker and a splitter
     * @param inDir  for a picker, the direction from the picker back to the aimed block; for a splitter, the direction its items
     *               travel; null for a belt
     */
    public record Plan(Pos target, Dir flow, Dir inDir) {
    }

    private PlacementAim() {
    }

    /**
     * The plan of a device placed against a face of a block.
     *
     * @param kind    the kind of device
     * @param clicked the block the player aimed at
     * @param face    the face offset, such as {@code (1, 0, 0)} for the east face
     * @param yaw     the player's yaw, used for a belt's flow
     * @return the plan, or empty when the kind needs a side face and the face is up or down
     */
    public static Optional<Plan> plan(LogisticsIds.Kind kind, Pos clicked, Pos face, double yaw) {
        Pos target = new Pos(clicked.x() + face.x(), clicked.y() + face.y(), clicked.z() + face.z());
        return switch (kind) {
            case BELT, DUPLEX -> Optional.of(new Plan(target, Dir.fromPlacement(yaw), null));
            case PICKER -> side(face).map(side -> new Plan(target, null, side.opposite()));
            case SPLITTER -> side(face).map(side -> new Plan(target, null, side));
        };
    }

    /** The side a horizontal face offset points to, or empty for an up or down face. */
    public static Optional<Dir> side(Pos face) {
        if (face.y() != 0) {
            return Optional.empty();
        }
        for (Dir dir : Dir.values()) {
            if (dir.step(new Pos(0, 0, 0)).equals(new Pos(face.x(), 0, face.z()))) {
                return Optional.of(dir);
            }
        }
        return Optional.empty();
    }

    /**
     * The face of a unit block through which an aim ray enters it. Used when a player aims at a device's interaction box, which
     * covers the block: the face the ray comes in through is the face the new device goes on. When the ray misses the block (it
     * can, at the edge of the box), the face is the dominant axis from the block's centre toward the eye.
     *
     * @param eye  the eye position {x, y, z}
     * @param look the unit look vector {x, y, z}
     * @param cell the block's position
     * @return the face offset, one component of magnitude one
     */
    public static Pos entryFace(double[] eye, double[] look, Pos cell) {
        double[] low = {cell.x(), cell.y(), cell.z()};
        double entry = Double.NEGATIVE_INFINITY;
        int axis = -1;
        int sign = 0;
        for (int i = 0; i < 3; i++) {
            if (look[i] == 0) {
                if (eye[i] < low[i] || eye[i] > low[i] + 1) {
                    return dominantFace(eye, low);
                }
                continue;
            }
            double a = (low[i] - eye[i]) / look[i];
            double b = (low[i] + 1 - eye[i]) / look[i];
            double near = Math.min(a, b);
            if (near > entry) {
                entry = near;
                axis = i;
                sign = look[i] > 0 ? -1 : 1;
            }
        }
        if (axis < 0) {
            return dominantFace(eye, low);
        }
        return offsetOf(axis, sign);
    }

    private static Pos dominantFace(double[] eye, double[] low) {
        double dx = eye[0] - (low[0] + 0.5);
        double dy = eye[1] - (low[1] + 0.5);
        double dz = eye[2] - (low[2] + 0.5);
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ax >= ay && ax >= az) {
            return new Pos(dx >= 0 ? 1 : -1, 0, 0);
        }
        if (az >= ay) {
            return new Pos(0, 0, dz >= 0 ? 1 : -1);
        }
        return new Pos(0, dy >= 0 ? 1 : -1, 0);
    }

    private static Pos offsetOf(int axis, int sign) {
        return switch (axis) {
            case 0 -> new Pos(sign, 0, 0);
            case 1 -> new Pos(0, sign, 0);
            default -> new Pos(0, 0, sign);
        };
    }
}
