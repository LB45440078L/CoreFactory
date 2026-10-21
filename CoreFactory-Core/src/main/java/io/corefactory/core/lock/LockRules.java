package io.corefactory.core.lock;

import io.corefactory.api.PinLock;

/**
 * The state machine of a nuclear TNT's PIN lock. A TNT is unlocked (no lock, or a lock that is open) or locked. Setting a PIN
 * locks it. Unlocking needs the PIN. Changing a PIN while it is locked needs the old one. Removing a lock needs the PIN while
 * it is locked, unless the actor is an admin. Every rule returns a {@link Step}: the outcome and the lock that results, which
 * is the given lock when the step is refused. Pure, so it is tested without a server.
 */
public final class LockRules {

    /** What a step did. */
    public enum Result {
        /** The step was taken, and {@link Step#lock()} is the new lock. */
        DONE,
        /** There is no lock to act on. */
        NO_LOCK,
        /** The PIN given does not match the lock. */
        WRONG_PIN,
        /** The lock is locked and no PIN was given. */
        NEEDS_PIN,
        /** The new PIN is not four digits. */
        INVALID_PIN,
        /** The actor does not own the TNT. */
        NOT_OWNER,
        /** The actor is neither the owner nor an admin. */
        NOT_PERMITTED
    }

    /**
     * The outcome of a step.
     *
     * @param result what happened
     * @param lock   the lock after the step; the lock given in when the step was refused, or null when it was removed
     */
    public record Step(Result result, PinLock lock) {

        public boolean done() {
            return result == Result.DONE;
        }
    }

    private LockRules() {
    }

    /**
     * The trigger decision: a placed TNT with a locked lock refuses every trigger, and an unlocked or unlocked-with-PIN TNT
     * allows them. Breaking a placed TNT by a player is refused by the same rule.
     */
    public static boolean refusesTriggers(PinLock lock) {
        return lock != null && lock.locked();
    }

    /**
     * Sets a new PIN. An unlocked TNT (or one with no lock) takes the PIN with no old PIN needed. A locked TNT needs its old PIN
     * to change it. The result is locked. Only the owner may set a PIN.
     *
     * @param owner  whether the actor owns the TNT
     * @param oldPin the current PIN, needed only while the lock is locked
     * @param newPin the PIN to set, four digits
     */
    public static Step setPin(boolean owner, PinLock current, String oldPin, String newPin) {
        if (!owner) {
            return new Step(Result.NOT_OWNER, current);
        }
        if (!PinHash.validPin(newPin)) {
            return new Step(Result.INVALID_PIN, current);
        }
        if (refusesTriggers(current)) {
            if (oldPin == null) {
                return new Step(Result.NEEDS_PIN, current);
            }
            if (!PinHash.matches(current.salt(), current.hash(), oldPin)) {
                return new Step(Result.WRONG_PIN, current);
            }
        }
        byte[] salt = PinHash.newSalt();
        return new Step(Result.DONE, new PinLock(salt, PinHash.hash(salt, newPin), true));
    }

    /**
     * Unlocks a locked TNT with its PIN. The salt and hash are kept, so the owner can lock it again. An unlocked TNT stays
     * unlocked, and a TNT with no lock has nothing to unlock.
     */
    public static Step unlock(PinLock current, String pin) {
        if (current == null) {
            return new Step(Result.NO_LOCK, null);
        }
        if (!current.locked()) {
            return new Step(Result.DONE, current);
        }
        if (!PinHash.matches(current.salt(), current.hash(), pin)) {
            return new Step(Result.WRONG_PIN, current);
        }
        return new Step(Result.DONE, current.withLocked(false));
    }

    /**
     * Removes the lock entirely: the TNT returns to the default unlocked state, with no PIN. The owner may remove an unlocked lock
     * freely, and a locked one with its PIN. An admin may remove any lock without the PIN, for recovery. Anyone else is refused.
     *
     * @param owner whether the actor owns the TNT
     * @param admin whether the actor holds the admin permission
     * @param pin   the PIN, needed only when the owner removes a locked lock
     */
    public static Step remove(boolean owner, boolean admin, PinLock current, String pin) {
        if (!owner && !admin) {
            return new Step(Result.NOT_PERMITTED, current);
        }
        if (current == null) {
            return new Step(Result.NO_LOCK, null);
        }
        if (current.locked() && !admin) {
            if (pin == null) {
                return new Step(Result.NEEDS_PIN, current);
            }
            if (!PinHash.matches(current.salt(), current.hash(), pin)) {
                return new Step(Result.WRONG_PIN, current);
            }
        }
        return new Step(Result.DONE, null);
    }
}
