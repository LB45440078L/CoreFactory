package io.corefactory.paper.turret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the damage line of {@code /cf turret info}: the total, the damage of the last second, and the hits of the last second. */
class TurretCommandTest {

    @Test
    void theDamageLineSaysTheTotalTheLastSecondAndTheHits() {
        assertThat(TurretCommand.damageLine(540.0, 270.0, 15))
                .isEqualTo("&7Damage dealt: &f540 &7total, &f270 &7in the last second, hits in the last second: &f15");
    }

    @Test
    void aFractionOfDamageIsShownToTwoPlaces() {
        assertThat(TurretCommand.damageLine(10.256, 0.0, 0))
                .isEqualTo("&7Damage dealt: &f10.26 &7total, &f0 &7in the last second, hits in the last second: &f0");
    }
}
