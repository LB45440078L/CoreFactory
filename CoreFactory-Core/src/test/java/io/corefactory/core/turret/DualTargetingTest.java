package io.corefactory.core.turret;

import io.corefactory.core.turret.DualTargeting.Spot;
import io.corefactory.core.turret.TargetRules.Point;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DualTargetingTest {

    private static Spot<String> spot(String name, double x, boolean inRange) {
        return new Spot<>(name, new Point(x, 0.0, 0.0), inRange);
    }

    @Test
    void oneCannonTakesTheNearestTargetInRange() {
        List<Spot<String>> candidates = List.of(spot("a", 2.0, true), spot("b", 3.0, true));

        assertThat(DualTargeting.select(candidates, 1, 6.0)).containsExactly("a");
    }

    @Test
    void oneCannonSkipsATargetOutOfRange() {
        List<Spot<String>> candidates = List.of(spot("a", 1.0, false), spot("b", 3.0, true));

        assertThat(DualTargeting.select(candidates, 1, 6.0)).containsExactly("b");
    }

    @Test
    void twoCannonsTakeTwoTargetsWithinTheSpacing() {
        List<Spot<String>> candidates = List.of(spot("a", 2.0, true), spot("b", 5.0, true));

        assertThat(DualTargeting.select(candidates, 2, 6.0)).containsExactly("a", "b");
    }

    @Test
    void twoCannonsLeaveTheSecondIdleWithoutATargetWithinTheSpacing() {
        List<Spot<String>> candidates = List.of(spot("a", 2.0, true), spot("b", 10.0, true));

        assertThat(DualTargeting.select(candidates, 2, 6.0)).containsExactly("a");
    }

    @Test
    void theSecondTargetIsTheNearestOneWithinTheSpacingInListOrder() {
        List<Spot<String>> candidates = List.of(spot("a", 2.0, true), spot("far", 9.0, true), spot("near", 5.0, true));

        assertThat(DualTargeting.select(candidates, 2, 6.0)).containsExactly("a", "near");
    }

    @Test
    void theSecondTargetMustBeInRangeToo() {
        List<Spot<String>> candidates = List.of(spot("a", 2.0, true), spot("out", 4.0, false), spot("in", 6.0, true));

        assertThat(DualTargeting.select(candidates, 2, 6.0)).containsExactly("a", "in");
    }

    @Test
    void theSpacingIsInclusive() {
        List<Spot<String>> candidates = List.of(spot("a", 0.0, true), spot("b", 6.0, true));

        assertThat(DualTargeting.select(candidates, 2, 6.0)).containsExactly("a", "b");
    }

    @Test
    void theFirstTargetIsTheNearestInRangeEvenIfAnOutOfRangeOneIsCloser() {
        List<Spot<String>> candidates = List.of(spot("out", 1.0, false), spot("in", 4.0, true), spot("other", 5.0, true));

        assertThat(DualTargeting.select(candidates, 2, 6.0)).containsExactly("in", "other");
    }

    @Test
    void noCannonsOrNoTargetInRangeGiveNoTarget() {
        List<Spot<String>> candidates = List.of(spot("out", 1.0, false));

        assertThat(DualTargeting.select(candidates, 2, 6.0)).isEmpty();
        assertThat(DualTargeting.select(List.of(spot("a", 1.0, true)), 0, 6.0)).isEmpty();
        assertThat(DualTargeting.select(List.<Spot<String>>of(), 2, 6.0)).isEmpty();
    }

    @Test
    void theCannonCountIsKeptToTwo() {
        List<Spot<String>> candidates = List.of(spot("a", 2.0, true), spot("b", 3.0, true), spot("c", 4.0, true));

        assertThat(DualTargeting.select(candidates, 9, 6.0)).containsExactly("a", "b");
    }

    @Test
    void theSpacingDefaultsToSixBlocks() {
        assertThat(DualTargeting.spacingOf(new MemoryConfiguration())).isEqualTo(6.0);
        assertThat(DualTargeting.DEFAULT_SPACING_BLOCKS).isEqualTo(6.0);
    }

    @Test
    void theSpacingIsReadFromTheTurretsSectionAndKeptInARange() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.dual-spacing-blocks", 4);
        assertThat(DualTargeting.spacingOf(config)).isEqualTo(4.0);

        config.set("turrets.dual-spacing-blocks", 500);
        assertThat(DualTargeting.spacingOf(config)).isEqualTo(64.0);

        config.set("turrets.dual-spacing-blocks", -3);
        assertThat(DualTargeting.spacingOf(config)).isZero();
    }
}
