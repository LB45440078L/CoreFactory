package io.corefactory.core.access;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceAccessTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FRIEND = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void aDeviceWithNoKnownOwnerMayBeUsedByAnyone() {
        assertThat(DeviceAccess.mayUse(null, STRANGER, false)).isTrue();
    }

    @Test
    void theOwnerMayUseTheDevice() {
        assertThat(DeviceAccess.mayUse(OWNER, OWNER, false)).isTrue();
    }

    @Test
    void aFriendOfTheOwnerMayUseTheDevice() {
        assertThat(DeviceAccess.mayUse(OWNER, FRIEND, true)).isTrue();
    }

    @Test
    void aStrangerMayNotUseTheDevice() {
        assertThat(DeviceAccess.mayUse(OWNER, STRANGER, false)).isFalse();
    }
}
