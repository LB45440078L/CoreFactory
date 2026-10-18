package io.moretntplus.resourcepack;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class TextureRendererTest {

    @Test
    void rendersASixteenByOneSixteenTextureInTheRequestedColours() {
        BufferedImage image = TextureRenderer.render("#000000", "#FFFFFF", "cross", "test");

        assertThat(image.getWidth()).isEqualTo(TextureRenderer.SIZE);
        assertThat(image.getHeight()).isEqualTo(TextureRenderer.SIZE);
        // The cross passes through the centre, so the centre pixel is bright and a corner is dark.
        assertThat(new java.awt.Color(image.getRGB(8, 8)).getRed()).isGreaterThan(128);
        assertThat(new java.awt.Color(image.getRGB(1, 1)).getRed()).isLessThan(128);
    }

    @Test
    void isDeterministicForTheSameSeed() {
        BufferedImage first = TextureRenderer.render("#123456", "#ABCDEF", "bio", "seed");
        BufferedImage second = TextureRenderer.render("#123456", "#ABCDEF", "bio", "seed");

        for (int y = 0; y < TextureRenderer.SIZE; y++) {
            for (int x = 0; x < TextureRenderer.SIZE; x++) {
                assertThat(first.getRGB(x, y)).isEqualTo(second.getRGB(x, y));
            }
        }
    }

    @Test
    void badColourFallsBackInsteadOfFailing() {
        assertThat(TextureRenderer.parse("not-a-colour", 0x102030)).containsExactly(0x10, 0x20, 0x30);
        assertThat(TextureRenderer.parse("#FF8000", 0)).containsExactly(0xFF, 0x80, 0x00);
    }
}
