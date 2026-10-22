package io.corefactory.core.vertical;

/** The direction a vertical transport belt carries items: UP toward larger y, DOWN toward smaller y. */
public enum VerticalDir {

    /** Toward larger y: the output end of a tower is its top belt. */
    UP(1),

    /** Toward smaller y: the output end of a tower is its bottom belt. */
    DOWN(-1);

    private final int dy;

    VerticalDir(int dy) {
        this.dy = dy;
    }

    /** The change in y for one block in this direction: +1 for UP, -1 for DOWN. */
    public int dy() {
        return dy;
    }

    /** The reverse direction. */
    public VerticalDir opposite() {
        return this == UP ? DOWN : UP;
    }
}
