package io.corefactory.core.hologram;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HologramLayoutTest {

    @Test
    void theCentreOfASingleLineIsTheBottomLine() {
        assertThat(HologramLayout.centreOffset(1)).isZero();
    }

    @Test
    void theCentreOfAStackSitsHalfItsHeightAboveTheBottomLine() {
        double spacing = HologramLayout.LINE_SPACING;

        assertThat(HologramLayout.centreOffset(3)).isEqualTo(2 * spacing / 2);
        assertThat(HologramLayout.centreOffset(5)).isEqualTo(4 * spacing / 2);
        assertThat(HologramLayout.centreOffset(0)).isZero();
    }

    @Test
    void aHologramIsDrawnAtHalfTheDefaultSize() {
        assertThat(HologramLayout.SCALE).isEqualTo(0.5f);
        assertThat(HologramLayout.LINE_SPACING).isEqualTo(0.125);
    }

    @Test
    void theViewRangeIsTheViewDistanceOverSixtyFourWithinBounds() {
        assertThat(HologramLayout.viewRange(64)).isEqualTo(1.0f);
        assertThat(HologramLayout.viewRange(32)).isEqualTo(0.5f);
        assertThat(HologramLayout.viewRange(1000)).isEqualTo(1.0f);
        assertThat(HologramLayout.viewRange(0)).isEqualTo(0.05f);
    }

    @Test
    void theLinesAreJoinedWithNewlines() {
        assertThat(HologramLayout.joined(List.of("a", "", "c"))).isEqualTo("a\n\nc");
        assertThat(HologramLayout.joined(List.of("only"))).isEqualTo("only");
    }
}
