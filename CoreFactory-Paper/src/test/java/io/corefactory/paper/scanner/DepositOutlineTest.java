package io.corefactory.paper.scanner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DepositOutlineTest {

    @Test
    void theOreColourIsTheFirstColourCodeOfItsName() {
        assertThat(DepositOutline.colourOf("&7Galena Ore")).isEqualTo(0xAAAAAA);
        assertThat(DepositOutline.colourOf("&cRed &9Blue")).isEqualTo(0xFF5555);
        assertThat(DepositOutline.colourOf("&6Gold &kmagic")).isEqualTo(0xFFAA00);
    }

    @Test
    void aNameWithNoColourCodeIsWhite() {
        assertThat(DepositOutline.colourOf("Coal")).isEqualTo(0xFFFFFF);
        assertThat(DepositOutline.colourOf("&zodd code")).isEqualTo(0xFFFFFF);
    }

    @Test
    void aRingHasAboutOnePointPerFourBlocksWithinItsLimits() {
        assertThat(DepositOutline.points(1)).isEqualTo(12);
        assertThat(DepositOutline.points(24)).isEqualTo(38);
        assertThat(DepositOutline.points(48)).isEqualTo(48);
        assertThat(DepositOutline.points(256)).isEqualTo(48);
    }
}
