package io.corefactory.core.logistics;

/**
 * The levels of belts, pickers, splitters and duplexes. Belts, splitters and duplexes go up to level III; pickers go up to
 * level V. Their numbers are in {@link LogisticsTable}.
 */
public enum LogisticsLevel {

    /** Level I, the cheapest. */
    I,

    /** Level II. */
    II,

    /** Level III, the fastest belt, splitter or duplex. */
    III,

    /** Level IV, a picker only. */
    IV,

    /** Level V, the fastest picker. */
    V
}
