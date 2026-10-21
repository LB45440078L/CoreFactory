package io.corefactory.core.turret;

import io.corefactory.core.turret.TurretAccess.Verdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TurretAccessTest {

    @Test
    void theOwnerWithThePermissionMayChangeTheTurret() {
        assertThat(TurretAccess.decide(true, true)).isEqualTo(Verdict.ALLOW);
    }

    @Test
    void theOwnerWithoutThePermissionMayNot() {
        assertThat(TurretAccess.decide(true, false)).isEqualTo(Verdict.NO_PERMISSION);
    }

    @Test
    void aPlayerWhoIsNotTheOwnerMayNotChangeTheTurretWhateverThePermission() {
        assertThat(TurretAccess.decide(false, true)).isEqualTo(Verdict.NOT_OWNER);
        assertThat(TurretAccess.decide(false, false)).isEqualTo(Verdict.NOT_OWNER);
    }
}
