package io.corefactory.core.lock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * The PIN of a nuclear TNT lock: a 4-digit number, stored as the SHA-256 of a random salt followed by the digits. The
 * digits are never kept; only the salt and the hash are. Pure, so it is tested without a server.
 */
public final class PinHash {

    /** The exact length of a PIN. */
    public static final int PIN_LENGTH = 4;
    /** The bytes of salt drawn for each lock. */
    public static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PinHash() {
    }

    /** True when the text is exactly {@link #PIN_LENGTH} decimal digits. */
    public static boolean validPin(String pin) {
        if (pin == null || pin.length() != PIN_LENGTH) {
            return false;
        }
        for (int i = 0; i < pin.length(); i++) {
            char c = pin.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /** A new random salt of {@link #SALT_BYTES} bytes. */
    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * The salted hash of a PIN.
     *
     * @throws IllegalArgumentException when the PIN is not {@link #validPin valid}
     */
    public static byte[] hash(byte[] salt, String pin) {
        if (!validPin(pin)) {
            throw new IllegalArgumentException("A PIN is exactly " + PIN_LENGTH + " digits.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            digest.update(pin.getBytes(StandardCharsets.US_ASCII));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM.", e);
        }
    }

    /**
     * True when the PIN hashes to {@code hash} under {@code salt}. A PIN that is not valid never matches. The comparison
     * takes the same time whether or not the digits agree.
     */
    public static boolean matches(byte[] salt, byte[] hash, String pin) {
        if (!validPin(pin) || salt == null || hash == null) {
            return false;
        }
        return MessageDigest.isEqual(hash(salt, pin), hash);
    }
}
