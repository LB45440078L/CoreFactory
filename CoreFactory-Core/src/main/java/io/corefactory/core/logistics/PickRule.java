package io.corefactory.core.logistics;

/**
 * The take decision of a picker at its input port: whether it takes the item the port offers. A port with nothing to give
 * gives nothing. A port the claim check does not permit the picker's owner to take from refuses the take, so the picker waits and
 * the item stays where it is. The claim check itself is {@link io.corefactory.core.engine.ContainerAccess#permits}, asked by the
 * port for the owner of the picker that takes. Pure.
 */
public final class PickRule {

    /** What the picker does with its input port this pick. */
    public enum Verdict {
        /** The picker takes the offered item. */
        TAKE,
        /** The port offers nothing, so there is nothing to take. */
        EMPTY,
        /** The port offers an item, but the claim check does not let the picker's owner take from it. */
        CLAIMED
    }

    private PickRule() {
    }

    /**
     * The picker's decision for one pick.
     *
     * @param offered   the item the port offers next, or null when it offers none
     * @param permitted true when the claim check lets the picker's owner take from the container
     */
    public static Verdict decide(Item offered, boolean permitted) {
        if (offered == null) {
            return Verdict.EMPTY;
        }
        return permitted ? Verdict.TAKE : Verdict.CLAIMED;
    }
}
