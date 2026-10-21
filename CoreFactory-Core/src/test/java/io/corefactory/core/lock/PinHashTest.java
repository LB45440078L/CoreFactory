package io.corefactory.core.lock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinHashTest {

    @Test
    void theSamePinWithDifferentSaltsHashesDifferently() {
        byte[] first = PinHash.newSalt();
        byte[] second = PinHash.newSalt();

        assertThat(first).isNotEqualTo(second);
        assertThat(PinHash.hash(first, "4821")).isNotEqualTo(PinHash.hash(second, "4821"));
    }

    @Test
    void saltIsSixteenBytesAndTheHashIsSha256() {
        assertThat(PinHash.newSalt()).hasSize(16);
        assertThat(PinHash.hash(PinHash.newSalt(), "0000")).hasSize(32);
    }

    @Test
    void theRightPinMatchesAndAWrongOneDoesNot() {
        byte[] salt = PinHash.newSalt();
        byte[] hash = PinHash.hash(salt, "4821");

        assertThat(PinHash.matches(salt, hash, "4821")).isTrue();
        assertThat(PinHash.matches(salt, hash, "4822")).isFalse();
        assertThat(PinHash.matches(salt, hash, "1111")).isFalse();
    }

    @Test
    void aHashDoesNotVerifyUnderAnotherSalt() {
        byte[] salt = PinHash.newSalt();
        byte[] hash = PinHash.hash(salt, "4821");

        assertThat(PinHash.matches(PinHash.newSalt(), hash, "4821")).isFalse();
    }

    @Test
    void aPinIsExactlyFourDigits() {
        assertThat(PinHash.validPin("0000")).isTrue();
        assertThat(PinHash.validPin("0123")).isTrue();
        assertThat(PinHash.validPin("9999")).isTrue();
        assertThat(PinHash.validPin("")).isFalse();
        assertThat(PinHash.validPin("123")).isFalse();
        assertThat(PinHash.validPin("12345")).isFalse();
        assertThat(PinHash.validPin("12a4")).isFalse();
        assertThat(PinHash.validPin(" 123")).isFalse();
        assertThat(PinHash.validPin("12-4")).isFalse();
        assertThat(PinHash.validPin(null)).isFalse();
    }

    @Test
    void aPinThatIsNotFourDigitsNeverMatchesAndCannotBeHashed() {
        byte[] salt = PinHash.newSalt();
        byte[] hash = PinHash.hash(salt, "4821");

        assertThat(PinHash.matches(salt, hash, "482")).isFalse();
        assertThat(PinHash.matches(salt, hash, null)).isFalse();
        assertThatThrownBy(() -> PinHash.hash(salt, "48215"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
