package io.corefactory.paper.railgun;

import io.corefactory.core.railgun.RailgunLevel;
import io.corefactory.core.railgun.RailgunSettings;
import io.corefactory.core.turret.LaserRules;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the pure parts of the rail gun's shots: the end conditions, the chunk of a block and the damage a shot passes. The shots need a
 * running server for their world and their entities, so only these parts run here.
 */
class RailgunShotsTest {

    @Test
    void aShotContinuesOnlyWhileItsTargetLivesInItsWorldAndItsFrontierIsLoaded() {
        assertThat(RailgunShots.mayAdvance(true, true, true)).isTrue();
        assertThat(RailgunShots.mayAdvance(false, true, true)).isFalse();
        assertThat(RailgunShots.mayAdvance(true, false, true)).isFalse();
        assertThat(RailgunShots.mayAdvance(true, true, false)).isFalse();
        assertThat(RailgunShots.mayAdvance(false, false, false)).isFalse();
    }

    @Test
    void aBlockCoordinateIsInTheChunkOfItsFloorDividedBySixteen() {
        assertThat(RailgunShots.chunkOf(0.0)).isZero();
        assertThat(RailgunShots.chunkOf(15.99)).isZero();
        assertThat(RailgunShots.chunkOf(16.0)).isEqualTo(1);
        assertThat(RailgunShots.chunkOf(-0.5)).isEqualTo(-1);
        assertThat(RailgunShots.chunkOf(-16.0)).isEqualTo(-1);
        assertThat(RailgunShots.chunkOf(-16.5)).isEqualTo(-2);
    }

    @Test
    void theDamageIsTheLaserRuleAtTheLevelsDamage() {
        RailgunSettings settings = RailgunSettings.defaults();
        double armour = 20.0;
        double toughness = 2.0;

        assertThat(RailgunShots.damageOf(settings, RailgunLevel.I, armour, toughness))
                .isEqualTo(LaserRules.damageAfterArmour(settings.level(RailgunLevel.I).damage(), armour, toughness,
                        settings.armourBypass()));
        assertThat(RailgunShots.damageOf(settings, RailgunLevel.III, 0.0, 0.0))
                .isEqualTo(settings.level(RailgunLevel.III).damage());
    }

    @Test
    void aShotThatIgnoresAllArmourDealsTheWholeLevelDamage() {
        RailgunSettings settings = RailgunSettings.from(Map.of("railgun", Map.of("armour-bypass", 1.0)));

        assertThat(RailgunShots.damageOf(settings, RailgunLevel.II, 30.0, 4.0))
                .isEqualTo(settings.level(RailgunLevel.II).damage());
    }
}
