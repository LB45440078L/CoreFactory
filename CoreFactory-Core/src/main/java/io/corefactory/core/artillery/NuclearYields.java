package io.corefactory.core.artillery;

import io.corefactory.core.config.ConfigValues;

import java.util.Map;

/**
 * The yields of the warheads the artillery shells copy, read from {@code tnts.yml} so the number lives in one place. A TNT type
 * gives its yield in megatons under {@code settings.yield-megatons}; a shell works in tons of TNT equivalent, and 1 megaton is
 * 1 000 000 tons. A missing or invalid entry takes the value shipped in tnts.yml, so the code runs without the file.
 *
 * @param littleBoyTons      the yield of the Little Boy warhead, in tons of TNT
 * @param davyCrockettTons   the yield of the Davy Crockett warhead, in tons of TNT
 */
public record NuclearYields(double littleBoyTons, double davyCrockettTons) {

    /** The id of the Little Boy in tnts.yml. */
    public static final String LITTLE_BOY = "nuke_little_boy";

    /** The id of the Davy Crockett in tnts.yml. */
    public static final String DAVY_CROCKETT = "nuke_davy_crockett";

    /** How many tons of TNT one megaton is. */
    public static final double TONS_PER_MEGATON = 1_000_000.0;

    /** The Little Boy's yield in tnts.yml, in megatons. */
    static final double DEFAULT_LITTLE_BOY_MEGATONS = 0.015;

    /** The Davy Crockett's yield in tnts.yml, in megatons. */
    static final double DEFAULT_DAVY_CROCKETT_MEGATONS = 0.00002;

    /**
     * Reads the two yields from the whole of {@code tnts.yml}, with its {@code tnts:} section.
     *
     * @param tnts the file as plain data, or null for the shipped values
     * @return the yields, in tons
     */
    public static NuclearYields from(Map<String, Object> tnts) {
        return new NuclearYields(
                tons(tnts, LITTLE_BOY, DEFAULT_LITTLE_BOY_MEGATONS),
                tons(tnts, DAVY_CROCKETT, DEFAULT_DAVY_CROCKETT_MEGATONS));
    }

    /** The yields shipped in tnts.yml, in tons: the reading of a missing file. */
    public static NuclearYields defaults() {
        return from(null);
    }

    private static double tons(Map<String, Object> tnts, String id, double defaultMegatons) {
        double megatons = ConfigValues.number(tnts, "tnts." + id + ".settings.yield-megatons", defaultMegatons);
        if (!Double.isFinite(megatons) || megatons <= 0) {
            megatons = defaultMegatons;
        }
        return megatons * TONS_PER_MEGATON;
    }
}
