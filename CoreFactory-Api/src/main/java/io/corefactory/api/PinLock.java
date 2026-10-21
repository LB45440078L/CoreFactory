package io.corefactory.api;

import java.util.Objects;

/**
 * The PIN lock of a placed nuclear TNT: the random salt of the lock, the salted SHA-256 hash of its PIN, and whether the
 * TNT is locked now. The PIN itself is never held here. An unlocked lock keeps its salt and hash, so its owner can lock it
 * again without a new PIN being chosen.
 *
 * <p>Instances are immutable. The arrays are copied on the way in and on the way out.
 *
 * @param salt   the random salt of this lock
 * @param hash   the salted SHA-256 hash of the PIN
 * @param locked true while the TNT refuses every trigger
 */
public record PinLock(byte[] salt, byte[] hash, boolean locked) {

    public PinLock {
        Objects.requireNonNull(salt, "salt");
        Objects.requireNonNull(hash, "hash");
        salt = salt.clone();
        hash = hash.clone();
    }

    /** A copy of the salt. */
    @Override
    public byte[] salt() {
        return salt.clone();
    }

    /** A copy of the hash. */
    @Override
    public byte[] hash() {
        return hash.clone();
    }

    /** The same salt and hash in the given state. */
    public PinLock withLocked(boolean locked) {
        return new PinLock(salt, hash, locked);
    }
}
