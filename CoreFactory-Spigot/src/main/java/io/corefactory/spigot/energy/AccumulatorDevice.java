package io.corefactory.spigot.energy;

import io.corefactory.core.energy.AccumulatorLevel;
import io.corefactory.core.energy.DeviceKind;
import org.bukkit.Location;

/**
 * An accumulator device: an {@link EnergyDevice} that holds energy. Its stored energy and discharging flag are updated from the
 * solver each tick and saved in {@code energy.yml}. It also keeps the charging and discharging of the last tick for its hologram,
 * and the tick until which its flash line reads after an activation.
 */
public final class AccumulatorDevice extends EnergyDevice {

    private double stored;
    private boolean charging;
    private boolean discharging;
    private long flashUntil = Long.MIN_VALUE / 2;

    /**
     * @param id          the device id
     * @param block       the block the accumulator stands in
     * @param level       the level, 1 to 3
     * @param stored      the energy held, in Wh
     * @param discharging true when it was discharging at the last save
     */
    public AccumulatorDevice(String id, Location block, int level, double stored, boolean discharging) {
        super(id, DeviceKind.ACCUMULATOR, block, level);
        this.stored = Math.max(0.0, stored);
        this.discharging = discharging;
    }

    /** The accumulator level. */
    public AccumulatorLevel accumulatorLevel() {
        return EnergyLevels.accumulatorLevel(level());
    }

    /** The energy held, in Wh. */
    public double stored() {
        return stored;
    }

    /** True when it took energy in the last tick. */
    public boolean charging() {
        return charging;
    }

    /** True when it gave energy out in the last tick. */
    public boolean discharging() {
        return discharging;
    }

    /** Takes the solver's state for this tick. */
    void update(double stored, boolean charging, boolean discharging) {
        this.stored = Math.max(0.0, stored);
        this.charging = charging;
        this.discharging = discharging;
    }

    /** Starts the flash line, which reads until the given tick. */
    void flashUntil(long tick) {
        this.flashUntil = tick;
    }

    /** True while the flash line should read at this tick. */
    public boolean flashing(long tick) {
        return tick < flashUntil;
    }
}
