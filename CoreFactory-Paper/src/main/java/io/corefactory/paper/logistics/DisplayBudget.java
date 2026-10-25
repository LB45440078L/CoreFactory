package io.corefactory.paper.logistics;

/**
 * The count of item displays on belts, held under a cap. An item beyond the cap is still carried by the network, but it is not
 * shown. The first refusal after the count was last below the cap asks for one warning; the warning is armed again once a
 * display is given back. Pure.
 */
public final class DisplayBudget {

    /** The answer to a request for a display. */
    public enum Take {
        /** A display may be made. */
        TAKEN,
        /** No display may be made, and the warning was already given. */
        REFUSED,
        /** No display may be made, and this is the first refusal since the count last had room: warn once. */
        REFUSED_WARN
    }

    private final int cap;
    private int used;
    private boolean warned;

    /** @param cap the most displays at once, zero or more */
    public DisplayBudget(int cap) {
        if (cap < 0) {
            throw new IllegalArgumentException("display cap must not be negative: " + cap);
        }
        this.cap = cap;
    }

    /** Asks for one display. */
    public Take take() {
        if (used < cap) {
            used++;
            return Take.TAKEN;
        }
        if (warned) {
            return Take.REFUSED;
        }
        warned = true;
        return Take.REFUSED_WARN;
    }

    /** Gives back one display that was taken. Re-arms the warning. */
    public void giveBack() {
        if (used > 0) {
            used--;
        }
        if (used < cap) {
            warned = false;
        }
    }

    /** The displays in use. */
    public int used() {
        return used;
    }

    /** The most displays at once. */
    public int cap() {
        return cap;
    }
}
