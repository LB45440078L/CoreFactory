package io.corefactory.core.turret;

import io.corefactory.core.config.ConfigValues;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The choice of targets for a machine gun with two cannons. The first cannon takes the nearest candidate that stands in range. The
 * second cannon takes the nearest other candidate in range that lies within the spacing of the first target, so the two cannons fire at
 * one area rather than at two far-apart mobs. With one cannon the rule gives one target. Each cannon gets one target, and a cannon with
 * no candidate in spacing is left idle. Pure.
 */
public final class DualTargeting {

    /** The default spacing between the two targets of a dual shot, in blocks, under {@code turrets.dual-spacing-blocks}. */
    public static final double DEFAULT_SPACING_BLOCKS = 6.0;

    /** The most cannons one turret has. */
    public static final int MAX_CANNONS = 2;

    private static final String KEY = "dual-spacing-blocks";
    private static final double MAX_SPACING_BLOCKS = 64.0;

    /**
     * One candidate target: what it is, where it stands, and whether it stands in range of the turret.
     *
     * @param target  the target, such as the entity it stands for
     * @param at      the target's place, such as the nearest point of its bounding box
     * @param inRange true when the turret can reach the target
     * @param <T>     the type of the target
     */
    public record Spot<T>(T target, TargetRules.Point at, boolean inRange) {
    }

    private DualTargeting() {
    }

    /**
     * The targets the cannons take, in cannon order: the first cannon's target first.
     *
     * @param candidates the candidates, nearest first
     * @param cannons    the cannons that fire; one or two, and any other count is clamped to that range
     * @param spacing    the most the second target may lie from the first, in blocks
     * @param <T>        the type of the target
     * @return up to {@code cannons} targets, never more than one per cannon, and empty when no candidate stands in range
     */
    public static <T> List<T> select(List<Spot<T>> candidates, int cannons, double spacing) {
        int barrels = Math.max(0, Math.min(cannons, MAX_CANNONS));
        int first = indexOfFirstInRange(candidates);
        if (barrels == 0 || first < 0) {
            return Collections.emptyList();
        }
        List<T> chosen = new ArrayList<>();
        chosen.add(candidates.get(first).target());
        if (barrels == 1) {
            return Collections.unmodifiableList(chosen);
        }
        double limit = Math.max(0.0, spacing);
        Spot<T> lead = candidates.get(first);
        for (int i = first + 1; i < candidates.size(); i++) {
            Spot<T> spot = candidates.get(i);
            if (spot.inRange() && lead.at().distanceSquaredTo(spot.at()) <= limit * limit) {
                chosen.add(spot.target());
                break;
            }
        }
        return Collections.unmodifiableList(chosen);
    }

    /**
     * The spacing between the two targets of a dual shot, read from {@code turrets.dual-spacing-blocks}. A missing key gives
     * {@link #DEFAULT_SPACING_BLOCKS}; the value is kept from 0 to 64 blocks.
     *
     * @param config the root configuration as plain data, or any section that holds a {@code turrets} section
     */
    public static double spacingOf(Map<String, Object> config) {
        double value = ConfigValues.number(ConfigValues.section(config, "turrets"), KEY, DEFAULT_SPACING_BLOCKS);
        if (Double.isNaN(value)) {
            return DEFAULT_SPACING_BLOCKS;
        }
        return Math.max(0.0, Math.min(MAX_SPACING_BLOCKS, value));
    }

    private static <T> int indexOfFirstInRange(List<Spot<T>> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).inRange()) {
                return i;
            }
        }
        return -1;
    }
}
