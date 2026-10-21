package io.corefactory.core.energy;

/** The three charts of a grid: its satisfaction, its production and its storage. */
public enum ChartMode {

    /** The satisfaction, in percent, on a fixed scale from 0 to 100. */
    SATISFACTION,

    /** The supply, in kW, on a scale from 0 to the highest supply in view. */
    PRODUCTION,

    /** The stored energy, in percent of the capacity, on a fixed scale from 0 to 100. */
    STORAGE
}
