package io.corefactory.core.logistics;

/** Why a unit or endpoint can or cannot be placed. */
public enum PlacementReason {

    /** The placement is allowed. */
    OK,

    /** The cell already holds a belt, picker, splitter or port. */
    OCCUPIED,

    /** The placement would give a unit two inputs. The first version has no merges. */
    MERGE,

    /** The placement would feed a unit head-on, or feed a picker or splitter from a side it does not accept from. */
    BLOCKED
}
