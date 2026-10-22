package io.corefactory.core.deposit;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OreBlocksTest {

    private static final long DEPOSIT = 0x5EED_0001L;

    @Test
    void theSameBlockGivesTheSameAnswerEveryTime() {
        for (int x = -40; x <= 40; x += 3) {
            for (int y = -64; y <= 96; y += 5) {
                boolean first = OreBlocks.shows(DEPOSIT, x, y, 7, 0.3);

                assertThat(OreBlocks.shows(DEPOSIT, x, y, 7, 0.3)).isEqualTo(first);
            }
        }
    }

    @Test
    void theShareOfBlocksIsCloseToTheDensity() {
        int shown = 0;
        int total = 0;
        for (int x = 0; x < 120; x++) {
            for (int y = -64; y < 56; y++) {
                for (int z = 0; z < 120; z++) {
                    total++;
                    if (OreBlocks.shows(DEPOSIT, x, y, z, 0.3)) {
                        shown++;
                    }
                }
            }
        }

        assertThat((double) shown / total).isCloseTo(0.3, Offset.offset(0.005));
    }

    @Test
    void theShareFollowsOtherDensities() {
        int shown = 0;
        for (int x = 0; x < 200; x++) {
            for (int z = 0; z < 200; z++) {
                if (OreBlocks.shows(DEPOSIT, x, 10, z, 0.75)) {
                    shown++;
                }
            }
        }

        assertThat(shown / 40_000.0).isCloseTo(0.75, Offset.offset(0.01));
    }

    @Test
    void zeroShowsNothingAndOneShowsEverything() {
        for (int x = 0; x < 50; x++) {
            assertThat(OreBlocks.shows(DEPOSIT, x, 0, x, 0.0)).isFalse();
            assertThat(OreBlocks.shows(DEPOSIT, x, 0, x, 1.0)).isTrue();
        }
    }

    @Test
    void twoDepositsGiveDifferentPatterns() {
        int differ = 0;
        for (int x = 0; x < 100; x++) {
            if (OreBlocks.shows(DEPOSIT, x, 5, 9, 0.5) != OreBlocks.shows(DEPOSIT + 1, x, 5, 9, 0.5)) {
                differ++;
            }
        }

        assertThat(differ).isPositive();
    }
}
