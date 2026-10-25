package io.corefactory.core.artillery;

/**
 * The ammunition an artillery piece holds: the shells and the charges loaded into it. A magazine holds one shell kind and one charge tier, the
 * ones the piece is set to, and each shot uses one shell and one charge of them. A load adds one of each, and is refused when the magazine is
 * full; an unload gives back everything it holds; a shot is refused when there is no shell or no charge.
 *
 * <p>The counts may stand above the capacity when the capacity is lowered in config.yml: such a magazine takes no more loads, but a shot still
 * uses one of each. Pure: the items the counts stand for are moved by the caller.
 */
public final class ArtilleryMagazine {

    /** The outcome of a load or a shot. */
    public enum Verdict {
        /** The load was made, or the shot may be fired. */
        OK,
        /** The magazine is at its capacity in shells or in charges, so nothing more is loaded. */
        FULL,
        /** The magazine holds no shell, so a shot is refused. */
        NO_SHELL,
        /** The magazine holds no charge, so a shot is refused. */
        NO_CHARGE,
        /** The magazine holds neither a shell nor a charge. */
        EMPTY;

        /** True when the load was made or the shot may be fired. */
        public boolean ok() {
            return this == OK;
        }
    }

    /**
     * What an unload gives back.
     *
     * @param shells  the shells the magazine held
     * @param charges the charges the magazine held
     */
    public record Contents(int shells, int charges) {
    }

    private final int capacity;
    private int shells;
    private int charges;

    /**
     * @param capacity the most shells and the most charges a load brings the magazine to, at least 1
     * @param shells   the shells the magazine holds now, 0 or more
     * @param charges  the charges the magazine holds now, 0 or more
     * @throws IllegalArgumentException when the capacity is below 1 or a count is negative
     */
    public ArtilleryMagazine(int capacity, int shells, int charges) {
        if (capacity < 1) {
            throw new IllegalArgumentException("a magazine holds at least one shell and one charge");
        }
        if (shells < 0 || charges < 0) {
            throw new IllegalArgumentException("a magazine cannot hold a negative count");
        }
        this.capacity = capacity;
        this.shells = shells;
        this.charges = charges;
    }

    /** The most shells, and the most charges, that a load brings the magazine to. */
    public int capacity() {
        return capacity;
    }

    /** The shells the magazine holds. */
    public int shells() {
        return shells;
    }

    /** The charges the magazine holds. */
    public int charges() {
        return charges;
    }

    /** True when the magazine holds a shell or a charge. */
    public boolean holdsAny() {
        return shells > 0 || charges > 0;
    }

    /** True when a load is refused: the shells or the charges are at the capacity. */
    public boolean full() {
        return shells >= capacity || charges >= capacity;
    }

    /**
     * Adds one shell and one charge, when the magazine is not full. A refused load changes nothing.
     *
     * @return OK, or FULL
     */
    public Verdict load() {
        if (full()) {
            return Verdict.FULL;
        }
        shells++;
        charges++;
        return Verdict.OK;
    }

    /**
     * Gives back everything the magazine holds, and leaves it empty.
     *
     * @return the shells and the charges that were in it
     */
    public Contents unload() {
        Contents given = new Contents(shells, charges);
        shells = 0;
        charges = 0;
        return given;
    }

    /**
     * Whether a shot may use the magazine: it needs one shell and one charge. Changes nothing.
     *
     * @return OK, NO_SHELL, NO_CHARGE or EMPTY
     */
    public Verdict check() {
        if (shells == 0 && charges == 0) {
            return Verdict.EMPTY;
        }
        if (shells == 0) {
            return Verdict.NO_SHELL;
        }
        if (charges == 0) {
            return Verdict.NO_CHARGE;
        }
        return Verdict.OK;
    }

    /**
     * Uses one shell and one charge for a shot, when the magazine has both. A refused shot changes nothing.
     *
     * @return the verdict of {@link #check}, which is OK when a shell and a charge were used
     */
    public Verdict fire() {
        Verdict verdict = check();
        if (verdict.ok()) {
            shells--;
            charges--;
        }
        return verdict;
    }
}
