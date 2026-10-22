package io.corefactory.core.turret;

import io.corefactory.core.turret.TargetAssignment.TurretSlot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The expected damage of two level V lasers on one Warden: 18 per hit, one hit every 4 ticks each, so 180 per second with both turrets at
 * full power on the same target. The Warden has 500 HP and no armour (an assumption: the repository does not state it).
 */
class LaserDamageAccountingTest {

    private static final double WARDEN_HP = 500.0;
    private static final UUID WARDEN = new UUID(0, 42);

    @Test
    void aLevelFiveLaserHitsForEighteenEveryFourTicksAtFullPower() {
        assertThat(LaserCannonModel.LEVEL_FIVE_DAMAGE).isEqualTo(18.0);
        assertThat(LaserCannonModel.LEVEL_FIVE_INTERVAL).isEqualTo(4);
        assertThat(FireRate.intervalTicks(LaserCannonModel.LEVEL_FIVE_INTERVAL, 1.0).getAsInt()).isEqualTo(4);
        assertThat(LaserCannonModel.firingTicks(4, 1.0, 20)).containsExactly(1, 5, 9, 13, 17);
    }

    @Test
    void twoLevelFiveLasersOnOneWardenDealOneHundredEightyPerSecondAtFullPower() {
        double oneTurret = LaserCannonModel.damagePerSecond(4, 1.0, 4, 18.0);
        double twoTurrets = 2 * oneTurret;

        assertThat(oneTurret).isEqualTo(90.0);
        assertThat(twoTurrets).isEqualTo(180.0);
    }

    @Test
    void theTwoTurretsOfTheSameWardenTakeTheSameTargetWhenItIsTheOnlyMob() {
        Map<String, List<UUID>> answer = TargetAssignment.assign(List.of(
                new TurretSlot("a", 1, List.of(), List.of(WARDEN)),
                new TurretSlot("b", 1, List.of(), List.of(WARDEN))));

        assertThat(answer.get("a")).containsExactly(WARDEN);
        assertThat(answer.get("b")).containsExactly(WARDEN);
    }

    @Test
    void theWardenDiesOnTheFourteenthVolleyAtTickFiftyThree() {
        List<Integer> volleys = LaserCannonModel.firingTicks(4, 1.0, 60);
        double hp = WARDEN_HP;
        int deadAt = -1;
        int volleysBeforeDeath = 0;
        for (int tick = 1; tick <= 60 && deadAt < 0; tick++) {
            if (volleys.contains(tick)) {
                hp -= 2 * 18.0;
                volleysBeforeDeath++;
                if (hp <= 0) {
                    deadAt = tick;
                }
            }
        }

        assertThat(volleysBeforeDeath).isEqualTo(14);
        assertThat(deadAt).isEqualTo(53);
        // 500 HP over 36 damage a volley is 13.9 volleys; the 14th, on tick 53 (2.65 s), is the kill, inside the 2.78 s estimate.
        assertThat(deadAt / 20.0).isLessThan(500.0 / 180.0);
    }

    @Test
    void theLaserDamageAtSixtyKilowattsOfSupplyIsFortyFivePerSecond() {
        // The power share, as the energy network gives it (see LaserPowerShareTest): the first turret gets 60 kW of 120 kW, the second none.
        double firstTurret = LaserCannonModel.damagePerSecond(4, 0.5, 4, 18.0);
        double secondTurret = LaserCannonModel.damagePerSecond(4, 0.0, 4, 18.0);

        assertThat(firstTurret).isEqualTo(45.0);
        assertThat(secondTurret).isZero();
        assertThat(180.0 / (firstTurret + secondTurret)).isEqualTo(4.0);
        assertThat(WARDEN_HP / (firstTurret + secondTurret)).isCloseTo(11.11, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void aShortfallOfPowerCostsItsShareOfTheDamageWhicheverTurretGetsIt() {
        // Two turrets at a quarter each, or one at a half and one at none: the same 45 per second, so the split does not change the total.
        double splitEvenly = 2 * LaserCannonModel.damagePerSecond(4, 0.25, 4, 18.0);
        double splitByOrder = LaserCannonModel.damagePerSecond(4, 0.5, 4, 18.0);

        assertThat(splitEvenly).isEqualTo(45.0);
        assertThat(splitByOrder).isEqualTo(45.0);
    }
}
