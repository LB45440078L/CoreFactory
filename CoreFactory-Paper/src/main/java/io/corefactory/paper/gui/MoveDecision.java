package io.corefactory.paper.gui;

/**
 * Whether a plugin menu may act on an item that a player drags or shift-clicks into one of its slots. Pure: it takes the kind of
 * menu, the kind of slot the item goes to, and whether that slot takes the item, and says allow or deny. A slot that takes nothing
 * from the player (an output, a button, a decoration, the outside of a splitter's lists) always denies, and so does any slot of a
 * menu that takes no items at all.
 */
public final class MoveDecision {

    /** The kind of menu the item is moved into. */
    public enum Holder {
        /** A splitter's filter panel. Its list slots take any item type that converts to one. */
        SPLITTER_PANEL,
        /** A machine's menu. Its input and fuel slots take what the machine's processes accept. */
        MACHINE,
        /** Any other plugin menu, such as the catalogue or a research console. It takes nothing from the player. */
        OTHER
    }

    /** The kind of slot the item is moved into. */
    public enum Slot {
        /** A list slot of a splitter's filter panel. */
        SPLITTER_LIST,
        /** Any other slot of a splitter's filter panel. */
        SPLITTER_OTHER,
        /** An input slot of a machine. */
        INPUT,
        /** A fuel slot of a machine. */
        FUEL,
        /** An output slot of a machine. */
        OUTPUT,
        /** Any other slot: a button, a decoration, a progress cell or a menu slot that takes no items. */
        OTHER
    }

    /** What the menu does with the item. */
    public enum Verdict {
        /** The menu may put the item into the slot. */
        ALLOW,
        /** The menu refuses the item; nothing changes. */
        DENY
    }

    private MoveDecision() {
    }

    /**
     * Decides one drop of an item into one slot.
     *
     * @param holder   the kind of menu
     * @param slot     the kind of slot the item goes to
     * @param accepted whether the slot takes the item: a splitter item that converts to a type, or an item a process accepts
     * @return {@link Verdict#ALLOW} only for a splitter list slot or a machine input or fuel slot that takes the item
     */
    public static Verdict decide(Holder holder, Slot slot, boolean accepted) {
        return switch (holder) {
            case SPLITTER_PANEL -> accepted && slot == Slot.SPLITTER_LIST ? Verdict.ALLOW : Verdict.DENY;
            case MACHINE -> accepted && (slot == Slot.INPUT || slot == Slot.FUEL) ? Verdict.ALLOW : Verdict.DENY;
            case OTHER -> Verdict.DENY;
        };
    }
}
