package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;

import java.util.List;
import java.util.UUID;

/**
 * One player's listening session on a Command Center's wall: the view they are on, the strike it is for, the time of their last click, and
 * the button under their view. The map is drawn on the wall, which every player near it sees. A session exists only while its player is
 * targeting; {@link ListeningMode} creates it and ends it.
 */
final class TargetSession {

    /** The player who is targeting. */
    final UUID player;

    /** The center whose target is set. */
    final CommandCenter center;

    /** The shell chosen for the strike when the session began, or null when none is chosen. */
    final ArtilleryShell shell;

    /** The charge tier chosen for the strike when the session began. */
    final ChargeTier tier;

    /** The barrel's elevation the strike is set for, in degrees. It is stored with the target when the session is confirmed. */
    final double elevation;

    /** The horizontal direction on the ground from the first connected piece toward the aim, x component; 0 when there is none. */
    final double dirX;

    /** The horizontal direction, z component. */
    final double dirZ;

    /** The view the player holds: its centre and zoom. */
    TargetView view;

    /** The time of the player's last click, in milliseconds. */
    long lastActivityMillis;

    /** The tick of the player's last click, so the events of one click count once. */
    long lastClickTick = Long.MIN_VALUE;

    /** The button under the player's view, or null. */
    TargetButton hover;

    /** The terrain the frame is laid over, the view and the blocks per pixel it was read for, and the tick it was read at. */
    byte[] terrain;
    TargetView terrainView;
    double terrainBpp = Double.NaN;
    long terrainTick = Long.MIN_VALUE;

    /** True when the terrain has loading tiles, so it is read again at the animation's cadence. */
    boolean terrainLoading;

    /** The pixels the map shows now, and the values they were drawn for: the frame is drawn again only when they change. */
    byte[] frame;
    List<Object> frameKey = List.of();

    TargetSession(UUID player, CommandCenter center, ArtilleryShell shell, ChargeTier tier, double elevation, TargetView view,
                  double dirX, double dirZ, long nowMillis) {
        this.player = player;
        this.center = center;
        this.shell = shell;
        this.tier = tier;
        this.elevation = elevation;
        this.view = view;
        this.dirX = dirX;
        this.dirZ = dirZ;
        this.lastActivityMillis = nowMillis;
    }
}
