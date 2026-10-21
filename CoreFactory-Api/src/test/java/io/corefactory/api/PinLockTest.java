package io.corefactory.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PinLockTest {

    @Test
    void theArraysAreCopiedOnTheWayInAndOut() {
        byte[] salt = {1, 2, 3};
        byte[] hash = {4, 5, 6};
        PinLock lock = new PinLock(salt, hash, true);

        salt[0] = 9;
        lock.salt()[1] = 9;
        lock.hash()[0] = 9;

        assertThat(lock.salt()).containsExactly(1, 2, 3);
        assertThat(lock.hash()).containsExactly(4, 5, 6);
    }

    @Test
    void changingTheStateKeepsTheSaltAndHash() {
        PinLock locked = new PinLock(new byte[] {1, 2}, new byte[] {3, 4}, true);

        PinLock open = locked.withLocked(false);

        assertThat(open.locked()).isFalse();
        assertThat(open.salt()).containsExactly(1, 2);
        assertThat(open.hash()).containsExactly(3, 4);
        assertThat(locked.locked()).isTrue();
    }
}
