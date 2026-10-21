package io.corefactory.core.turret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelKeysTest {

    @Test
    void levelOneUsesTheComponentsOwnModel() {
        assertThat(ModelKeys.of(TurretType.MACHINE_GUN, 1, false)).isEqualTo("machine/machine_gun_turret");
        assertThat(ModelKeys.of(TurretType.LASER, 1, false)).isEqualTo("machine/laser_turret");
    }

    @Test
    void eachHigherLevelHasItsOwnModel() {
        assertThat(ModelKeys.of(TurretType.MACHINE_GUN, 2, false)).isEqualTo("machine/machine_gun_turret_2");
        assertThat(ModelKeys.of(TurretType.MACHINE_GUN, 5, false)).isEqualTo("machine/machine_gun_turret_5");
        assertThat(ModelKeys.of(TurretType.LASER, 4, false)).isEqualTo("machine/laser_turret_4");
    }

    @Test
    void aTallLevelHasAnUpperHalfModel() {
        assertThat(ModelKeys.of(TurretType.MACHINE_GUN, 3, false)).isEqualTo("machine/machine_gun_turret_3");
        assertThat(ModelKeys.of(TurretType.MACHINE_GUN, 3, true)).isEqualTo("machine/machine_gun_turret_3_top");
        assertThat(ModelKeys.of(TurretType.LASER, 5, true)).isEqualTo("machine/laser_turret_5_top");
    }

    @Test
    void anUpperHalfOfAShortLevelIsRefused() {
        assertThatThrownBy(() -> ModelKeys.of(TurretType.MACHINE_GUN, 2, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModelKeys.of(TurretType.LASER, 1, true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aLevelOutsideOneToFiveIsRefused() {
        assertThatThrownBy(() -> ModelKeys.of(TurretType.MACHINE_GUN, 0, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModelKeys.of(TurretType.MACHINE_GUN, 6, false)).isInstanceOf(IllegalArgumentException.class);
    }
}
