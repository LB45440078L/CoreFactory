package io.corefactory.paper.machine;

/**
 * What a click on a machine's interaction hitbox does. A right-click from the main hand opens the machine's menu, as a right-click
 * on its host block does. A left-click from the main hand breaks the machine, as a break of its host block does. Any other click
 * does nothing. Pure: the listener maps its Bukkit event to a {@link Click} and says whether it came from the main hand.
 */
public enum InteractionRoute {

    /** Opens the machine's menu. */
    OPEN,
    /** Breaks the machine, through the break path of its host block. */
    BREAK,
    /** Does nothing. */
    IGNORE;

    /** The kind of click a player made on the hitbox. */
    public enum Click {
        RIGHT,
        LEFT
    }

    /**
     * The route of one click.
     *
     * @param click    the kind of click
     * @param mainHand true when the click came from the main hand; an off-hand click is ignored
     */
    public static InteractionRoute of(Click click, boolean mainHand) {
        if (click == null || !mainHand) {
            return IGNORE;
        }
        return switch (click) {
            case RIGHT -> OPEN;
            case LEFT -> BREAK;
        };
    }
}
