package io.corefactory.core.railgun;

/**
 * The levels of a rail gun, I to III. A higher level hits harder, reaches further, needs more power, charges for less time and reloads a
 * little sooner. The numbers of each level are read from the {@code railgun} section of config.yml (see {@link RailgunSettings}); this enum
 * gives only the order and the numeral.
 */
public enum RailgunLevel {
    I, II, III;

    /** The level's number, from 1 for level I to 3 for level III. */
    public int number() {
        return ordinal() + 1;
    }

    /**
     * The level with a number.
     *
     * @param number the level's number, from 1 to 3
     * @return the level
     * @throws IllegalArgumentException when the number is out of range
     */
    public static RailgunLevel ofNumber(int number) {
        RailgunLevel[] levels = values();
        if (number < 1 || number > levels.length) {
            throw new IllegalArgumentException("railgun level must be from 1 to " + levels.length + ": " + number);
        }
        return levels[number - 1];
    }
}
