package io.corefactory.paper.turret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the item ids of the upgrade kits: which level a kit makes, and which ids are no kit. */
class TurretIdsTest {

    @Test
    void aKitMakesTheLevelItsIdNames() {
        assertThat(TurretIds.kitLevelOf("turret_upgrade_2").getAsInt()).isEqualTo(2);
        assertThat(TurretIds.kitLevelOf("turret_upgrade_3").getAsInt()).isEqualTo(3);
        assertThat(TurretIds.kitLevelOf("turret_upgrade_4").getAsInt()).isEqualTo(4);
        assertThat(TurretIds.kitLevelOf("turret_upgrade_5").getAsInt()).isEqualTo(5);
    }

    @Test
    void anIdThatIsNoKitMakesNoLevel() {
        assertThat(TurretIds.kitLevelOf("machine_gun_turret").isPresent()).isFalse();
        assertThat(TurretIds.kitLevelOf("turret_upgrade_").isPresent()).isFalse();
        assertThat(TurretIds.kitLevelOf("turret_upgrade_x").isPresent()).isFalse();
        assertThat(TurretIds.kitLevelOf(null).isPresent()).isFalse();
    }
}
