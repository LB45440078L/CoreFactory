package io.corefactory.core.drill;

import io.corefactory.core.geology.Placement;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DrillBoxTest {

    @Test
    void aTenWideSquareCentredOnTheDrillRunsFromMinusFiveToPlusFour() {
        DrillBox box = DrillBox.around(100, 60, -40, DrillTable.level(1));
        assertThat(box).isEqualTo(new DrillBox(95, 13, -45, 104, 60, -36));
    }

    @Test
    void theDepthGoesDownFromTheDrillsOwnLayer() {
        DrillBox box = DrillBox.around(0, 50, 0, DrillTable.level(1));
        assertThat(box.maxY()).isEqualTo(50);
        assertThat(box.minY()).isEqualTo(50 - 48 + 1);
        assertThat(box.maxY() - box.minY() + 1).isEqualTo(48);
    }

    @Test
    void theRegionOfTheLargestLevelIsFortyByFortyByTwoHundredAndEighty() {
        DrillBox box = DrillBox.around(0, 0, 0, DrillTable.level(5));
        assertThat(box.maxX() - box.minX() + 1).isEqualTo(40);
        assertThat(box.maxZ() - box.minZ() + 1).isEqualTo(40);
        assertThat(box.maxY() - box.minY() + 1).isEqualTo(280);
    }

    @Test
    void theEdgesAreInsideAndTheBlocksJustOutsideAreNot() {
        DrillBox box = DrillBox.around(0, 0, 0, DrillTable.level(1));
        assertThat(box.contains(-5, 0, -5)).isTrue();
        assertThat(box.contains(4, 0, 4)).isTrue();
        assertThat(box.contains(4, -15, 4)).isTrue();
        assertThat(box.contains(5, 0, 0)).isFalse();
        assertThat(box.contains(0, 0, -6)).isFalse();
        assertThat(box.contains(0, 1, 0)).isFalse();
        assertThat(box.contains(0, -48, 0)).isFalse();
    }

    @Test
    void aPlacementIsInsideWhenItsBlockIs() {
        DrillBox box = DrillBox.around(0, 0, 0, DrillTable.level(1));
        assertThat(box.contains(new Placement(3, -2, 3, 0))).isTrue();
        assertThat(box.contains(new Placement(30, -2, 3, 0))).isFalse();
    }
}
