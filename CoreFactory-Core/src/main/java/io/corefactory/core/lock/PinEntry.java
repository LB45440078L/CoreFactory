package io.corefactory.core.lock;

/**
 * The digits typed on a keypad. A PIN is entered once to unlock, or twice to set: the first entry is kept, and the second must
 * match it, otherwise the entry starts again. Pure, so it is tested without a server. Closing the keypad discards the entry.
 */
public final class PinEntry {

    /** What a submission did. */
    public enum Outcome {
        /** Fewer than four digits were entered, so nothing was submitted. */
        INCOMPLETE,
        /** The first entry of a set was kept; the second entry is now asked for. */
        FIRST_STORED,
        /** The second entry did not match the first. The entry has started again. */
        MISMATCH,
        /** The PIN is ready to be used. */
        READY
    }

    /**
     * @param outcome what the submission did
     * @param pin     the PIN, when the outcome is {@link Outcome#READY}; otherwise null
     */
    public record Submission(Outcome outcome, String pin) {
    }

    private final boolean confirm;
    private final StringBuilder digits = new StringBuilder(PinHash.PIN_LENGTH);
    private String first;

    /**
     * @param confirm true to set a PIN, which is entered twice; false to enter one that already exists
     */
    public PinEntry(boolean confirm) {
        this.confirm = confirm;
    }

    /** Adds a digit when fewer than four are entered. Returns false otherwise, or when the digit is not 0 to 9. */
    public boolean press(int digit) {
        if (digit < 0 || digit > 9 || digits.length() >= PinHash.PIN_LENGTH) {
            return false;
        }
        digits.append((char) ('0' + digit));
        return true;
    }

    /** Removes the last digit typed. */
    public void backspace() {
        if (!digits.isEmpty()) {
            digits.setLength(digits.length() - 1);
        }
    }

    /** Removes every digit typed in this entry. */
    public void clear() {
        digits.setLength(0);
    }

    /** The number of digits typed in this entry. */
    public int length() {
        return digits.length();
    }

    /** True when four digits are typed, so the entry may be submitted. */
    public boolean complete() {
        return digits.length() == PinHash.PIN_LENGTH;
    }

    /** True when the first entry of a set is kept and the second is being typed. */
    public boolean confirming() {
        return first != null;
    }

    /** Submits the typed digits. The entry is cleared unless the outcome is {@link Outcome#INCOMPLETE} or {@link Outcome#READY}. */
    public Submission submit() {
        if (!complete()) {
            return new Submission(Outcome.INCOMPLETE, null);
        }
        String typed = digits.toString();
        if (!confirm) {
            digits.setLength(0);
            return new Submission(Outcome.READY, typed);
        }
        digits.setLength(0);
        if (first == null) {
            first = typed;
            return new Submission(Outcome.FIRST_STORED, null);
        }
        boolean same = first.equals(typed);
        first = null;
        return same ? new Submission(Outcome.READY, typed) : new Submission(Outcome.MISMATCH, null);
    }
}
