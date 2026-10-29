package io.corefactory.core.upgrade;

import java.util.OptionalInt;

/**
 * The levels of a lab machine: five levels (I to V), the kit that takes a machine up one level, the work pace of
 * each level and the model id each level is drawn with. A kit takes a machine up one level only, so a level I machine
 * takes the level II kit, then the level III kit, and so on up to the level V kit. Pure: the speed factors are given by the caller, read from config.yml.
 */
public final class LabLevels {

    /** The lowest level a lab machine has. */
    public static final int MIN_LEVEL = 1;

    /** The highest level a lab machine has. */
    public static final int MAX_LEVEL = 5;

    private static final String KIT_PREFIX = "machine_upgrade_";

    /** The outcome of asking whether a kit may take a machine to its next level. */
    public enum Upgrade {
        /** The kit takes the machine to the next level. */
        OK,
        /** The kit is not the one for the machine's next level. */
        WRONG_LEVEL,
        /** The machine is already at its highest level. */
        MAX_LEVEL
    }

    /**
     * One tick of pacing under a speed factor.
     *
     * @param credit the credit left after this tick, from 0 up to but not including 1
     * @param runs   the process ticks the machine runs this server tick, 0 or more
     */
    public record Pace(double credit, int runs) {
    }

    private final double[] speeds;

    /**
     * @param speeds the work pace factor of levels I to V, in order: each finite and above zero, and never lower for a
     *               higher level
     * @throws IllegalArgumentException when there are not three factors, or one of them is not valid
     */
    public LabLevels(double... speeds) {
        if (speeds == null || speeds.length != MAX_LEVEL) {
            throw new IllegalArgumentException("needs one speed factor for each of the " + MAX_LEVEL + " levels");
        }
        for (int i = 0; i < speeds.length; i++) {
            if (!Double.isFinite(speeds[i]) || speeds[i] <= 0) {
                throw new IllegalArgumentException("speed factor of level " + (i + 1) + " must be above zero");
            }
            if (i > 0 && speeds[i] < speeds[i - 1]) {
                throw new IllegalArgumentException("speed factor of level " + (i + 1) + " is lower than the level below");
            }
        }
        this.speeds = speeds.clone();
    }

    /** The speed factors of the shipped config: 1.0, 1.5, 2.0, 2.75 and 3.5. */
    public static LabLevels defaults() {
        return new LabLevels(1.0, 1.5, 2.0, 2.75, 3.5);
    }

    /** The work pace factor of a level, clamped to the levels that exist. */
    public double speed(int level) {
        return speeds[clamp(level) - MIN_LEVEL];
    }

    /**
     * Whether a kit takes a machine from its current level to the next one.
     *
     * @param current  the machine's level, from {@link #MIN_LEVEL} to {@link #MAX_LEVEL}
     * @param kitLevel the level the kit upgrades to
     * @return {@link Upgrade#OK} only when the kit is for the level above the machine's, and the machine is below the top level
     * @throws IllegalArgumentException when the machine's level is not a level a lab machine has
     */
    public static Upgrade canUpgrade(int current, int kitLevel) {
        if (current < MIN_LEVEL || current > MAX_LEVEL) {
            throw new IllegalArgumentException("a lab machine is level " + MIN_LEVEL + " to " + MAX_LEVEL + ", not " + current);
        }
        if (current == MAX_LEVEL) {
            return Upgrade.MAX_LEVEL;
        }
        return kitLevel == current + 1 ? Upgrade.OK : Upgrade.WRONG_LEVEL;
    }

    /**
     * The level a kit upgrades to, read from its component id: {@code machine_upgrade_2} is level II and {@code machine_upgrade_3}
     * is level III, up to {@code machine_upgrade_5} at level V. Empty for any other id.
     */
    public static OptionalInt kitLevel(String componentId) {
        if (componentId == null || !componentId.startsWith(KIT_PREFIX)) {
            return OptionalInt.empty();
        }
        String digits = componentId.substring(KIT_PREFIX.length());
        if (digits.length() != 1 || !Character.isDigit(digits.charAt(0))) {
            return OptionalInt.empty();
        }
        int level = digits.charAt(0) - '0';
        return level > MIN_LEVEL && level <= MAX_LEVEL ? OptionalInt.of(level) : OptionalInt.empty();
    }

    /**
     * The model id a lab machine is drawn with at a level: the machine id itself at level I, and the id with {@code _2} to
     * {@code _5} above it. The resource pack defines each as {@code machine/<model id>}.
     *
     * @param machineId the machine id, such as {@code assembler}
     * @param level     the level, from {@link #MIN_LEVEL} to {@link #MAX_LEVEL}
     */
    public static String modelId(String machineId, int level) {
        int clamped = clamp(level);
        return clamped == MIN_LEVEL ? machineId : machineId + "_" + clamped;
    }

    /** The roman numeral of a level: I to V. */
    public static String numeral(int level) {
        return switch (clamp(level)) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }

    /**
     * One tick of work pace: the satisfaction of the network times the level's speed factor is added to the credit, and the
     * machine runs one process tick for each whole unit of credit. At full satisfaction and a speed of 1.5 it runs 1, then 2,
     * then 1 process ticks, and so on. A satisfaction at or below zero, or NaN, adds nothing; above one it counts as one.
     *
     * @param credit       the credit before this tick
     * @param satisfaction the network's satisfaction, 0 to 1
     * @param speed        the level's speed factor, above zero
     */
    public static Pace pace(double credit, double satisfaction, double speed) {
        double share = satisfaction > 0 ? Math.min(1.0, satisfaction) : 0.0;
        double total = credit + share * speed;
        int runs = (int) Math.floor(total);
        return new Pace(total - runs, runs);
    }

    /** The level nearest to the given one, within the levels a lab machine has. */
    public static int clamp(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }
}
