package io.corefactory.core.artillery;

/**
 * The levels of an artillery piece, I to VII. A higher level is a bigger piece: it fires further, reloads more slowly and
 * needs more power. The numbers of each level are read from the {@code artillery} section of config.yml (see
 * {@link ArtillerySettings}); this enum gives only the order and the numeral.
 */
public enum ArtilleryLevel {
    I, II, III, IV, V, VI, VII;

    /** The level's number, from 1 for level I to 7 for level VII. */
    public int number() {
        return ordinal() + 1;
    }

    /**
     * The level with a number.
     *
     * @param number the level's number, from 1 to 7
     * @return the level
     * @throws IllegalArgumentException when the number is out of range
     */
    public static ArtilleryLevel ofNumber(int number) {
        ArtilleryLevel[] levels = values();
        if (number < 1 || number > levels.length) {
            throw new IllegalArgumentException("artillery level must be from 1 to " + levels.length + ": " + number);
        }
        return levels[number - 1];
    }
}
