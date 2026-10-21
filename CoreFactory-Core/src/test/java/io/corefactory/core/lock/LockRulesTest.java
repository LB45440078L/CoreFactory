package io.corefactory.core.lock;

import io.corefactory.api.PinLock;
import io.corefactory.core.lock.LockRules.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LockRulesTest {

    private static final String PIN = "4821";

    private static PinLock lockedWith(String pin) {
        byte[] salt = PinHash.newSalt();
        return new PinLock(salt, PinHash.hash(salt, pin), true);
    }

    private static boolean pinOpens(PinLock lock, String pin) {
        return PinHash.matches(lock.salt(), lock.hash(), pin);
    }

    @Test
    void unlockedToLockedToUnlockedKeepingThePin() {
        LockRules.Step set = LockRules.setPin(true, null, null, PIN);
        assertThat(set.done()).isTrue();
        assertThat(set.lock().locked()).isTrue();
        assertThat(LockRules.refusesTriggers(set.lock())).isTrue();

        LockRules.Step opened = LockRules.unlock(set.lock(), PIN);
        assertThat(opened.done()).isTrue();
        assertThat(opened.lock().locked()).isFalse();
        assertThat(LockRules.refusesTriggers(opened.lock())).isFalse();
        assertThat(opened.lock().hash()).isEqualTo(set.lock().hash());
    }

    @Test
    void anUnlockedTntTakesANewPinWithoutTheOldOne() {
        PinLock open = lockedWith(PIN).withLocked(false);

        LockRules.Step relocked = LockRules.setPin(true, open, null, "1111");

        assertThat(relocked.done()).isTrue();
        assertThat(relocked.lock().locked()).isTrue();
        assertThat(pinOpens(relocked.lock(), "1111")).isTrue();
        assertThat(pinOpens(relocked.lock(), PIN)).isFalse();
    }

    @Test
    void changingAPinWhileLockedNeedsTheOldPin() {
        PinLock locked = lockedWith(PIN);

        assertThat(LockRules.setPin(true, locked, null, "1111").result()).isEqualTo(Result.NEEDS_PIN);
        assertThat(LockRules.setPin(true, locked, "0000", "1111").result()).isEqualTo(Result.WRONG_PIN);

        LockRules.Step changed = LockRules.setPin(true, locked, PIN, "1111");
        assertThat(changed.done()).isTrue();
        assertThat(changed.lock().locked()).isTrue();
        assertThat(pinOpens(changed.lock(), "1111")).isTrue();
        assertThat(pinOpens(changed.lock(), PIN)).isFalse();
    }

    @Test
    void onlyTheOwnerSetsAPin() {
        LockRules.Step step = LockRules.setPin(false, null, null, PIN);

        assertThat(step.result()).isEqualTo(Result.NOT_OWNER);
        assertThat(step.lock()).isNull();
    }

    @Test
    void aNewPinMustBeFourDigits() {
        assertThat(LockRules.setPin(true, null, null, "12").result()).isEqualTo(Result.INVALID_PIN);
        assertThat(LockRules.setPin(true, null, null, "abcd").result()).isEqualTo(Result.INVALID_PIN);
    }

    @Test
    void aWrongPinNeverUnlocks() {
        PinLock locked = lockedWith(PIN);

        LockRules.Step step = LockRules.unlock(locked, "0000");

        assertThat(step.result()).isEqualTo(Result.WRONG_PIN);
        assertThat(step.lock().locked()).isTrue();
        assertThat(LockRules.refusesTriggers(step.lock())).isTrue();
    }

    @Test
    void aTntWithNoLockHasNothingToUnlock() {
        assertThat(LockRules.unlock(null, PIN).result()).isEqualTo(Result.NO_LOCK);
    }

    @Test
    void removingAnUnlockedLockNeedsNoPin() {
        PinLock open = lockedWith(PIN).withLocked(false);

        LockRules.Step step = LockRules.remove(true, false, open, null);

        assertThat(step.done()).isTrue();
        assertThat(step.lock()).isNull();
    }

    @Test
    void removingALockedLockNeedsItsPin() {
        PinLock locked = lockedWith(PIN);

        assertThat(LockRules.remove(true, false, locked, null).result()).isEqualTo(Result.NEEDS_PIN);
        assertThat(LockRules.remove(true, false, locked, "0000").result()).isEqualTo(Result.WRONG_PIN);

        LockRules.Step removed = LockRules.remove(true, false, locked, PIN);
        assertThat(removed.done()).isTrue();
        assertThat(removed.lock()).isNull();
    }

    @Test
    void anAdminRemovesALockedLockWithoutThePin() {
        PinLock locked = lockedWith(PIN);

        LockRules.Step step = LockRules.remove(false, true, locked, null);

        assertThat(step.done()).isTrue();
        assertThat(step.lock()).isNull();
    }

    @Test
    void aNonOwnerWithoutTheAdminPermissionCannotRemoveEvenWithThePin() {
        PinLock locked = lockedWith(PIN);

        assertThat(LockRules.remove(false, false, locked, PIN).result()).isEqualTo(Result.NOT_PERMITTED);
        assertThat(LockRules.remove(false, false, locked.withLocked(false), null).result())
                .isEqualTo(Result.NOT_PERMITTED);
    }

    @Test
    void removingWhenThereIsNoLockSaysSo() {
        assertThat(LockRules.remove(true, false, null, null).result()).isEqualTo(Result.NO_LOCK);
    }

    @Test
    void theTriggerDecisionRefusesEveryTriggerOnlyWhileLocked() {
        assertThat(LockRules.refusesTriggers(null)).isFalse();
        assertThat(LockRules.refusesTriggers(lockedWith(PIN).withLocked(false))).isFalse();
        assertThat(LockRules.refusesTriggers(lockedWith(PIN))).isTrue();
    }
}
