package io.corefactory.paper.machine;

/**
 * What a right-click on a machine does, decided from the player's state and the item in hand. A plain right-click, or a click
 * with an empty hand, opens the machine's menu. A sneaking click with a block the player can place by hand places it against the
 * machine, when the block face the player aims at is known and the block there is free. Any other sneaking click passes through,
 * so nothing opens and nothing is placed. Pure: the listener works out the inputs from the server.
 */
public enum ClickDecision {

    /** Opens the machine's menu and takes the click. */
    OPEN_MENU,
    /** Places the held block against the machine and takes the click. */
    PLACE_BY_HAND,
    /** Takes nothing: the click is left to the game. */
    PASS_THROUGH;

    /** The kind of item in the player's main hand, as far as the decision needs it. */
    public enum Held {
        /** Nothing in hand. */
        EMPTY,
        /** A block the player may place by hand. */
        BLOCK,
        /** Any other item. */
        OTHER
    }

    /**
     * The decision for one click.
     *
     * @param sneaking   true when the player is sneaking
     * @param held       the kind of item in the main hand
     * @param faceKnown  true when the player aims at a face of the machine's host block
     * @param replaceable true when the block the held item would fill is free, air or liquid
     * @return the decision
     */
    public static ClickDecision of(boolean sneaking, Held held, boolean faceKnown, boolean replaceable) {
        if (!sneaking || held == Held.EMPTY) {
            return OPEN_MENU;
        }
        if (held == Held.OTHER) {
            return PASS_THROUGH;
        }
        return faceKnown && replaceable ? PLACE_BY_HAND : PASS_THROUGH;
    }
}
