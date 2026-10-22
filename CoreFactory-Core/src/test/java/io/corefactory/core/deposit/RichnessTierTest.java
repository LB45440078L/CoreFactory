package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RichnessTierTest {

    @Test
    void theThresholdsSplitThePoorRichAndExceptionalTiers() {
        assertThat(RichnessTier.of(999_999)).isEqualTo(RichnessTier.POOR);
        assertThat(RichnessTier.of(1_000_000)).isEqualTo(RichnessTier.RICH);
        assertThat(RichnessTier.of(9_999_999)).isEqualTo(RichnessTier.RICH);
        assertThat(RichnessTier.of(10_000_000)).isEqualTo(RichnessTier.EXCEPTIONAL);
    }

    @Test
    void anEmptyOrNegativeDepositIsPoor() {
        assertThat(RichnessTier.of(0)).isEqualTo(RichnessTier.POOR);
        assertThat(RichnessTier.of(-5)).isEqualTo(RichnessTier.POOR);
    }

    @Test
    void theColouredWordCarriesTheTierColour() {
        assertThat(RichnessTier.POOR.coloured()).isEqualTo("&7Poor");
        assertThat(RichnessTier.RICH.coloured()).isEqualTo("&6Rich");
        assertThat(RichnessTier.EXCEPTIONAL.coloured()).isEqualTo("&dExceptional");
        assertThat(RichnessTier.EXCEPTIONAL.label()).isEqualTo("Exceptional");
    }
}
