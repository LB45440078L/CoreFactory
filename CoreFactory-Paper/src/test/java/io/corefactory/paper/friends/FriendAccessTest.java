package io.corefactory.paper.friends;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class FriendAccessTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID FRIEND = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0000-000000000013");

    @TempDir
    File folder;

    private FriendAccess access(Friends friends) {
        return new FriendAccess(friends);
    }

    private Friends friends() {
        return new Friends(new FriendStore(new File(folder, "friends.yml"), Logger.getLogger("test")));
    }

    @Test
    void theOwnerMayUseTheirDevice() {
        assertThat(access(friends()).mayUse(OWNER, OWNER)).isTrue();
    }

    @Test
    void aFriendOfTheOwnerMayUseTheDevice() {
        Friends friends = friends();
        friends.add(OWNER, FRIEND, "Friend");
        assertThat(access(friends).mayUse(OWNER, FRIEND)).isTrue();
    }

    @Test
    void aStrangerMayNotUseTheDevice() {
        assertThat(access(friends()).mayUse(OWNER, STRANGER)).isFalse();
    }

    @Test
    void aFriendOfAnotherOwnerMayNotUseTheDevice() {
        Friends friends = friends();
        friends.add(STRANGER, FRIEND, "Friend");
        assertThat(access(friends).mayUse(OWNER, FRIEND)).isFalse();
    }

    @Test
    void aFriendWhoIsRemovedMayNotUseTheDevice() {
        Friends friends = friends();
        friends.add(OWNER, FRIEND, "Friend");
        friends.remove(OWNER, FRIEND);
        assertThat(access(friends).mayUse(OWNER, FRIEND)).isFalse();
    }

    @Test
    void aDeviceWithNoKnownOwnerMayBeUsedByAnyone() {
        assertThat(access(friends()).mayUse(null, STRANGER)).isTrue();
    }
}
