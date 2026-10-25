package io.corefactory.paper.turret;

import io.corefactory.core.turret.Magazine;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the damage counter of a turret: the total, and the damage and hits of the last second of ticks. */
class TurretDeviceTest {

    private static TurretDevice laser() {
        return new TurretDevice("laser", TurretKind.LASER, UUID.randomUUID(), new Location(null, 0, 64, 0), 0.0, Magazine.empty(1), 5);
    }

    /** One level 5 laser shot of 18 every fourth tick, from tick 0 through tick 36: ten hits. */
    private static void volley(TurretDevice turret) {
        for (long tick = 0; tick <= 36; tick += 4) {
            turret.countHit(tick, 18.0);
        }
    }

    @Test
    void theTotalIsTheSumOfEveryHitAndTheHitsAreCounted() {
        TurretDevice turret = laser();
        volley(turret);

        assertThat(turret.damageDealt()).isEqualTo(180.0);
        assertThat(turret.hitsDealt()).isEqualTo(10);
    }

    @Test
    void theLastSecondHoldsTheHitsOfTheLastTwentyTicks() {
        TurretDevice turret = laser();
        volley(turret);

        // At tick 39 the last second is ticks 20 to 39: the hits on 20, 24, 28, 32 and 36, which is five of 18.
        assertThat(turret.damageInLastSecond(39)).isEqualTo(90.0);
        assertThat(turret.hitsInLastSecond(39)).isEqualTo(5);
    }

    @Test
    void hitsOlderThanASecondLeaveTheLastSecondButNotTheTotal() {
        TurretDevice turret = laser();
        volley(turret);

        assertThat(turret.damageInLastSecond(60)).isZero();
        assertThat(turret.hitsInLastSecond(60)).isZero();
        assertThat(turret.damageDealt()).isEqualTo(180.0);
    }

    @Test
    void aHitTwentyTicksOnTakesTheSlotOfAnOlderHitAndTheOldOneLeaves() {
        TurretDevice turret = laser();
        turret.countHit(0, 18.0);
        turret.countHit(20, 7.0);

        assertThat(turret.damageInLastSecond(20)).isEqualTo(7.0);
        assertThat(turret.hitsInLastSecond(20)).isEqualTo(1);
        assertThat(turret.damageDealt()).isEqualTo(25.0);
    }

    @Test
    void threeLasersEngagedTogetherDealTwoHundredSeventyInTheLastSecond() {
        TurretDevice[] turrets = {laser(), laser(), laser()};
        for (TurretDevice turret : turrets) {
            volley(turret);
        }

        double dealt = 0.0;
        int hits = 0;
        for (TurretDevice turret : turrets) {
            dealt += turret.damageInLastSecond(39);
            hits += turret.hitsInLastSecond(39);
        }
        assertThat(dealt).isEqualTo(270.0);
        assertThat(hits).isEqualTo(15);
    }
}
