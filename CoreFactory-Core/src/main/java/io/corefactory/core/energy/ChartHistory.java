package io.corefactory.core.energy;

import java.util.ArrayList;
import java.util.List;

/**
 * A ring buffer of the last {@link #CAPACITY} samples of one grid, one per second. It is kept in memory only.
 */
public final class ChartHistory {

    /** The most samples kept. */
    public static final int CAPACITY = 48;

    /**
     * One sample of a grid.
     *
     * @param supplyW     the grid's supply, in W
     * @param loadW       the grid's load, in W
     * @param satisfaction the grid's satisfaction, from 0 to 1
     * @param storedWh    the energy stored in the grid's accumulators, in Wh
     * @param capacityWh  the capacity of the grid's accumulators, in Wh
     */
    public record Sample(double supplyW, double loadW, double satisfaction, double storedWh, double capacityWh) {

        public Sample {
            Checks.finiteNonNegative(supplyW, "sample supply");
            Checks.finiteNonNegative(loadW, "sample load");
            Checks.finiteNonNegative(storedWh, "sample stored");
            Checks.finiteNonNegative(capacityWh, "sample capacity");
            if (!Double.isFinite(satisfaction) || satisfaction < 0 || satisfaction > 1) {
                throw new IllegalArgumentException("sample satisfaction must be from 0 to 1: " + satisfaction);
            }
        }
    }

    private final Sample[] ring = new Sample[CAPACITY];
    private int next;
    private int size;

    /**
     * Adds a sample, dropping the oldest one when the buffer is full.
     *
     * @param sample the sample
     */
    public void add(Sample sample) {
        ring[next] = sample;
        next = (next + 1) % CAPACITY;
        size = Math.min(CAPACITY, size + 1);
    }

    /** The number of samples kept. */
    public int size() {
        return size;
    }

    /** The samples kept, oldest first. */
    public List<Sample> samples() {
        List<Sample> samples = new ArrayList<>(size);
        int start = (next - size + CAPACITY) % CAPACITY;
        for (int i = 0; i < size; i++) {
            samples.add(ring[(start + i) % CAPACITY]);
        }
        return List.copyOf(samples);
    }
}
