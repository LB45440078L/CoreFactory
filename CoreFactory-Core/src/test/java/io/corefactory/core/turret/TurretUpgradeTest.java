package io.corefactory.core.turret;

import io.corefactory.core.turret.TurretUpgrade.Reason;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TurretUpgradeTest {

    @Test
    void everyPairOfNeighbouringLevelsUpgradesWithTheKitOfTheHigherLevel() {
        for (int current = 1; current < TurretLevels.MAX_LEVEL; current++) {
            assertThat(TurretUpgrade.canUpgrade(current, current + 1)).as("%d to %d", current, current + 1).isEqualTo(Reason.OK);
        }
    }

    @Test
    void aKitOfAnyOtherLevelIsRefused() {
        assertThat(TurretUpgrade.canUpgrade(1, 3)).isEqualTo(Reason.WRONG_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(1, 1)).isEqualTo(Reason.WRONG_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(2, 2)).isEqualTo(Reason.WRONG_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(2, 1)).isEqualTo(Reason.WRONG_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(3, 5)).isEqualTo(Reason.WRONG_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(4, 6)).isEqualTo(Reason.WRONG_LEVEL);
    }

    @Test
    void aTurretBelowLevelOneIsRefused() {
        assertThat(TurretUpgrade.canUpgrade(0, 1)).isEqualTo(Reason.WRONG_LEVEL);
    }

    @Test
    void aTurretAtTheHighestLevelTakesNoKit() {
        assertThat(TurretUpgrade.canUpgrade(5, 6)).isEqualTo(Reason.MAX_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(5, 5)).isEqualTo(Reason.MAX_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(5, 6, true)).isEqualTo(Reason.MAX_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(5, 6, false)).isEqualTo(Reason.MAX_LEVEL);
    }

    @Test
    void aTallLevelNeedsTheBlockAboveToBeFree() {
        assertThat(TurretUpgrade.canUpgrade(2, 3, false)).isEqualTo(Reason.BLOCKED_ABOVE);
        assertThat(TurretUpgrade.canUpgrade(2, 3, true)).isEqualTo(Reason.OK);
        assertThat(TurretUpgrade.canUpgrade(3, 4, false)).isEqualTo(Reason.BLOCKED_ABOVE);
        assertThat(TurretUpgrade.canUpgrade(3, 4, true)).isEqualTo(Reason.OK);
        assertThat(TurretUpgrade.canUpgrade(4, 5, false)).isEqualTo(Reason.BLOCKED_ABOVE);
        assertThat(TurretUpgrade.canUpgrade(4, 5, true)).isEqualTo(Reason.OK);
    }

    @Test
    void theFirstTwoLevelsDoNotNeedTheBlockAbove() {
        assertThat(TurretUpgrade.canUpgrade(1, 2, false)).isEqualTo(Reason.OK);
        assertThat(TurretUpgrade.canUpgrade(1, 2, true)).isEqualTo(Reason.OK);
    }

    @Test
    void theLevelRulesComeBeforeTheHeadroomRule() {
        assertThat(TurretUpgrade.canUpgrade(1, 3, false)).isEqualTo(Reason.WRONG_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(5, 6, false)).isEqualTo(Reason.MAX_LEVEL);
    }

    @Test
    void theTallLevelsAreTheLevelsFromTheRuleOn() {
        assertThat(TurretUpgrade.TALL_FROM).isEqualTo(3);
    }
}
