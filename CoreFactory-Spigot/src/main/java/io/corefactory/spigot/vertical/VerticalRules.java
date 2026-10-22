package io.corefactory.spigot.vertical;

import io.corefactory.core.vertical.VerticalDir;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The decisions of the vertical belt runtime that need no server: which direction a menu button sets, whether a click may change
 * the direction, and which direction a new belt takes from the tower it joins. Pure.
 */
public final class VerticalRules {

    /** The size of the belt menu: three rows. */
    public static final int SIZE = 27;

    /** The slot of the UP button. */
    public static final int UP_SLOT = 11;

    /** The slot of the info item. */
    public static final int INFO_SLOT = 13;

    /** The slot of the DOWN button. */
    public static final int DOWN_SLOT = 15;

    /** What a click on a direction button does. */
    public enum Verdict {
        /** The tower's direction changes. */
        SET,
        /** The tower already has this direction: nothing changes. */
        SAME,
        /** The player may not change the tower. */
        DENIED
    }

    private VerticalRules() {
    }

    /** The direction a menu slot sets: UP in slot 11, DOWN in slot 15. Empty for any other slot. */
    public static Optional<VerticalDir> buttonAt(int slot) {
        if (slot == UP_SLOT) {
            return Optional.of(VerticalDir.UP);
        }
        if (slot == DOWN_SLOT) {
            return Optional.of(VerticalDir.DOWN);
        }
        return Optional.empty();
    }

    /**
     * Whether a click that asks for a direction changes the tower. Only the owner of the belt may change its tower's direction; a
     * belt saved with no owner may be changed by anyone.
     *
     * @param requested the direction the button sets
     * @param current   the tower's direction now
     * @param owner     the id of the player who placed the belt, or null when none is stored
     * @param player    the id of the player who clicked
     */
    public static Verdict decide(VerticalDir requested, VerticalDir current, UUID owner, UUID player) {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(player, "player");
        if (owner != null && !owner.equals(player)) {
            return Verdict.DENIED;
        }
        return requested == current ? Verdict.SAME : Verdict.SET;
    }

    /**
     * The direction a new belt takes when it is placed beside a tower. The first neighbour is the tower the player clicked, when the
     * new belt is placed against one; the second is the other tower it touches. A belt that touches no tower goes UP.
     *
     * @param first  the direction of the preferred neighbouring tower, or empty when none
     * @param second the direction of the other neighbouring tower, or empty when none
     */
    public static VerticalDir adoptDirection(Optional<VerticalDir> first, Optional<VerticalDir> second) {
        return first.or(() -> second).orElse(VerticalDir.UP);
    }
}
