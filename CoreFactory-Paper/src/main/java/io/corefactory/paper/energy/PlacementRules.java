package io.corefactory.paper.energy;

import java.util.Optional;

/**
 * Whether a pylon or accumulator may be placed on a block face, before the protection check. The checks run in this order, and
 * the first that fails is the refusal. Pure: the listener reads the world, the device registry and the research.
 */
public final class PlacementRules {

    /** Why a placement is refused. */
    public enum Refusal {
        /** The world is one where CoreFactory is disabled. */
        DISABLED_WORLD,
        /** The block the device would fill is not air. */
        NOT_AIR,
        /** Another energy device already stands there. */
        OCCUPIED,
        /** The player has not finished the research node that unlocks the item. */
        LOCKED,
        /** A protection plugin refused the placement. Decided by the protection check, not by {@link #before}. */
        PROTECTED
    }

    private PlacementRules() {
    }

    /**
     * The first refusal that applies before the protection check.
     *
     * @param disabledWorld true when CoreFactory is disabled in the world
     * @param targetAir     true when the target block is air
     * @param occupied      true when an energy device stands at the target
     * @param unlocked      true when the player may place the item
     * @return the refusal, or empty when the placement may go on to the protection check
     */
    public static Optional<Refusal> before(boolean disabledWorld, boolean targetAir, boolean occupied, boolean unlocked) {
        if (disabledWorld) {
            return Optional.of(Refusal.DISABLED_WORLD);
        }
        if (!targetAir) {
            return Optional.of(Refusal.NOT_AIR);
        }
        if (occupied) {
            return Optional.of(Refusal.OCCUPIED);
        }
        if (!unlocked) {
            return Optional.of(Refusal.LOCKED);
        }
        return Optional.empty();
    }
}
