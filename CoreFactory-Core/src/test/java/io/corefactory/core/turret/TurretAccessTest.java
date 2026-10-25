package io.corefactory.core.turret;

import io.corefactory.core.turret.TurretAccess.Verdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TurretAccessTest {

    @Test
    void anAllowedPlayerWithThePermissionMayChangeTheTurret() {
        assertThat(TurretAccess.decide(true, true)).isEqualTo(Verdict.ALLOW);
    }

    @Test
    void anAllowedPlayerWithoutThePermissionMayNot() {
        assertThat(TurretAccess.decide(true, false)).isEqualTo(Verdict.NO_PERMISSION);
    }

    @Test
    void aPlayerWhoIsNotAllowedMayNotChangeTheTurretWhateverThePermission() {
        assertThat(TurretAccess.decide(false, true)).isEqualTo(Verdict.NOT_ALLOWED);
        assertThat(TurretAccess.decide(false, false)).isEqualTo(Verdict.NOT_ALLOWED);
    }
}
