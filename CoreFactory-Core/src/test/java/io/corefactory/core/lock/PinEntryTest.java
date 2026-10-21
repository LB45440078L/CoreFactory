package io.corefactory.core.lock;

import io.corefactory.core.lock.PinEntry.Outcome;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PinEntryTest {

    private static void type(PinEntry entry, String digits) {
        for (char c : digits.toCharArray()) {
            entry.press(c - '0');
        }
    }

    @Test
    void digitsStopAtFour() {
        PinEntry entry = new PinEntry(false);
        type(entry, "1234");

        assertThat(entry.press(5)).isFalse();
        assertThat(entry.length()).isEqualTo(4);
        assertThat(entry.complete()).isTrue();
    }

    @Test
    void onlyDecimalDigitsAreTyped() {
        PinEntry entry = new PinEntry(false);

        assertThat(entry.press(-1)).isFalse();
        assertThat(entry.press(10)).isFalse();
        assertThat(entry.length()).isZero();
    }

    @Test
    void backspaceRemovesTheLastDigitAndClearRemovesAll() {
        PinEntry entry = new PinEntry(false);
        type(entry, "123");

        entry.backspace();
        assertThat(entry.length()).isEqualTo(2);

        entry.clear();
        assertThat(entry.length()).isZero();
        entry.backspace();
        assertThat(entry.length()).isZero();
    }

    @Test
    void anIncompleteEntryIsNotSubmitted() {
        PinEntry entry = new PinEntry(false);
        type(entry, "12");

        PinEntry.Submission submission = entry.submit();

        assertThat(submission.outcome()).isEqualTo(Outcome.INCOMPLETE);
        assertThat(submission.pin()).isNull();
        assertThat(entry.length()).isEqualTo(2);
    }

    @Test
    void anUnlockEntryIsReadyAtOnce() {
        PinEntry entry = new PinEntry(false);
        type(entry, "1234");

        PinEntry.Submission submission = entry.submit();

        assertThat(submission.outcome()).isEqualTo(Outcome.READY);
        assertThat(submission.pin()).isEqualTo("1234");
        assertThat(entry.length()).isZero();
    }

    @Test
    void aSetEntryAsksForTheDigitsAgain() {
        PinEntry entry = new PinEntry(true);
        type(entry, "1234");

        assertThat(entry.submit().outcome()).isEqualTo(Outcome.FIRST_STORED);
        assertThat(entry.confirming()).isTrue();
        assertThat(entry.length()).isZero();

        type(entry, "1234");
        PinEntry.Submission confirmed = entry.submit();

        assertThat(confirmed.outcome()).isEqualTo(Outcome.READY);
        assertThat(confirmed.pin()).isEqualTo("1234");
    }

    @Test
    void aMismatchStartsTheEntryAgain() {
        PinEntry entry = new PinEntry(true);
        type(entry, "1234");
        entry.submit();
        type(entry, "1235");

        PinEntry.Submission mismatch = entry.submit();

        assertThat(mismatch.outcome()).isEqualTo(Outcome.MISMATCH);
        assertThat(mismatch.pin()).isNull();
        assertThat(entry.confirming()).isFalse();
        assertThat(entry.length()).isZero();

        type(entry, "5555");
        assertThat(entry.submit().outcome()).isEqualTo(Outcome.FIRST_STORED);
        type(entry, "5555");
        assertThat(entry.submit().pin()).isEqualTo("5555");
    }
}
