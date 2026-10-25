package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.artillery.Position;

import java.util.UUID;

/**
 * A shell in flight. It is planned when it is fired, so its flight and its impact are known from the start; the tick action moves it along
 * its arc one tick at a time until it lands.
 */
final class ArtilleryShot {

    private final String pieceId;
    private final UUID world;
    private final UUID shooter;
    private final ArtilleryShell shell;
    private final ArtilleryLevel level;
    private final ChargeTier tier;
    private final ArtilleryPlan plan;
    private final ArtilleryImpactPlan impact;
    private int tick;

    /**
     * @param pieceId the id of the piece that fired the shell
     * @param world   the id of the world the shot is fired in
     * @param shooter the player who fired it
     * @param shell   the shell
     * @param level   the level of the piece
     * @param tier    the charge tier the piece loaded
     * @param plan    the flight
     * @param impact  the impact
     */
    ArtilleryShot(String pieceId, UUID world, UUID shooter, ArtilleryShell shell, ArtilleryLevel level, ChargeTier tier,
                  ArtilleryPlan plan, ArtilleryImpactPlan impact) {
        this.pieceId = pieceId;
        this.world = world;
        this.shooter = shooter;
        this.shell = shell;
        this.level = level;
        this.tier = tier;
        this.plan = plan;
        this.impact = impact;
    }

    String pieceId() {
        return pieceId;
    }

    UUID world() {
        return world;
    }

    UUID shooter() {
        return shooter;
    }

    ArtilleryShell shell() {
        return shell;
    }

    ArtilleryLevel level() {
        return level;
    }

    ChargeTier tier() {
        return tier;
    }

    ArtilleryImpactPlan impact() {
        return impact;
    }

    /** The ticks the shell has flown: 0 at the launch. */
    int tick() {
        return tick;
    }

    /** Where the shell is now, along its arc. */
    Position position() {
        return plan.positionAt(tick);
    }

    /** Where the shell was one tick ago, along its arc: the launch, on the first tick. */
    Position previous() {
        return plan.positionAt(Math.max(0, tick - 1));
    }

    /** True when the shell is at its landing tick, so it is due to land. */
    boolean landed() {
        return tick >= plan.lastTick();
    }

    /** Moves the shell on by one tick. */
    void advance() {
        tick++;
    }
}
