package io.corefactory.core.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContainerAccessTest {

    @Test
    void theContainerLevelsLetAPlayerPutItemsIn() {
        assertThat(ContainerAccess.grantsContainer("Inventory")).isTrue();
        assertThat(ContainerAccess.grantsContainer("Container")).isTrue();
        assertThat(ContainerAccess.grantsContainer("Build")).isTrue();
        assertThat(ContainerAccess.grantsContainer("Edit")).isTrue();
        assertThat(ContainerAccess.grantsContainer("Manage")).isTrue();
    }

    @Test
    void accessAloneDoesNotLetAPlayerPutItemsIn() {
        assertThat(ContainerAccess.grantsContainer("Access")).isFalse();
        assertThat(ContainerAccess.grantsContainer("None")).isFalse();
        assertThat(ContainerAccess.grantsContainer(null)).isFalse();
        assertThat(ContainerAccess.grantsContainer("")).isFalse();
    }

    @Test
    void theLevelNameIsReadWithoutCase() {
        assertThat(ContainerAccess.grantsContainer("inventory")).isTrue();
        assertThat(ContainerAccess.grantsContainer("MANAGE")).isTrue();
    }

    @Test
    void anUnprotectedContainerIsOpenToEveryDevice() {
        assertThat(ContainerAccess.permits(false, false, false)).isTrue();
        assertThat(ContainerAccess.permits(false, true, false)).isTrue();
        assertThat(ContainerAccess.permits(false, true, true)).isTrue();
    }

    @Test
    void aProtectedContainerIsOpenOnlyToATrustedOwner() {
        assertThat(ContainerAccess.permits(true, true, true)).isTrue();
        assertThat(ContainerAccess.permits(true, true, false)).isFalse();
    }

    @Test
    void aDeviceWithNoOwnerIsRefusedWhereAClaimProtects() {
        assertThat(ContainerAccess.permits(true, false, false)).isFalse();
        assertThat(ContainerAccess.permits(true, false, true)).isFalse();
    }

    @Test
    void aDeviceWhoseOwnerOwnsTheClaimIsLetIntoIt() {
        assertThat(ContainerAccess.permits(true, true, true)).isTrue();
    }

    @Test
    void aDeviceWhoseOwnerIsAStrangerInTheClaimIsRefused() {
        assertThat(ContainerAccess.permits(true, true, ContainerAccess.grantsContainer(null))).isFalse();
    }

    @Test
    void aDeviceWhoseOwnerHoldsAContainerLevelIsLetInButAccessAloneIsNot() {
        assertThat(ContainerAccess.permits(true, true, ContainerAccess.grantsContainer("Container"))).isTrue();
        assertThat(ContainerAccess.permits(true, true, ContainerAccess.grantsContainer("Access"))).isFalse();
    }
}
