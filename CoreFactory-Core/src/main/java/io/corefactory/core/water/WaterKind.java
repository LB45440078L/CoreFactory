package io.corefactory.core.water;

/**
 * The kinds of block in a water network. PIPE, PUMP and the two splitters carry water; CONSUMER takes it.
 */
public enum WaterKind {
    PIPE,
    PUMP,
    SPLITTER_T,
    SPLITTER_X,
    CONSUMER;

    /** True for SPLITTER_T and SPLITTER_X. */
    public boolean isSplitter() {
        return this == SPLITTER_T || this == SPLITTER_X;
    }
}
