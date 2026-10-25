package io.corefactory.paper.energy;

/**
 * How a lab tool advances under a partial network. A tool at full satisfaction runs every tick, as before. Below full, the
 * satisfaction is added to a credit each tick, and the tool runs one process tick each time the credit reaches 1, so its
 * progress advances by the satisfaction per tick on average and stops at 0. Pure.
 *
 * @param credit  the credit left after this tick, from 0 up to but not including 1 while the satisfaction is below full
 * @param runTick true when the tool runs one process tick this server tick
 */
public record WorkPace(double credit, boolean runTick) {

    /**
     * One tick of pacing.
     *
     * @param credit       the credit before this tick
     * @param satisfaction the network's satisfaction, 0 to 1; values outside are clamped, NaN counts as 0
     */
    public static WorkPace step(double credit, double satisfaction) {
        double s = satisfaction >= 1.0 ? 1.0 : satisfaction > 0 ? satisfaction : 0.0;
        if (s >= 1.0) {
            return new WorkPace(credit, true);
        }
        double next = credit + s;
        if (next >= 1.0) {
            return new WorkPace(next - 1.0, true);
        }
        return new WorkPace(next, false);
    }
}
