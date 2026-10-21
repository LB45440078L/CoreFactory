package io.corefactory.core.drill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DrillTableTest {

    @Test
    void theFiveLevelsHaveTheAreasDepthsAndCyclesTheDesignGives() {
        assertThat(DrillTable.level(1)).isEqualTo(new DrillSpec(1, 10, 48, 600));
        assertThat(DrillTable.level(2)).isEqualTo(new DrillSpec(2, 14, 96, 480));
        assertThat(DrillTable.level(3)).isEqualTo(new DrillSpec(3, 20, 144, 360));
        assertThat(DrillTable.level(4)).isEqualTo(new DrillSpec(4, 28, 200, 240));
        assertThat(DrillTable.level(5)).isEqualTo(new DrillSpec(5, 40, 280, 120));
    }

    @Test
    void levelsListsEveryLevelInOrder() {
        assertThat(DrillTable.levels()).hasSize(DrillTable.MAX_LEVEL);
        for (int level = 1; level <= DrillTable.MAX_LEVEL; level++) {
            assertThat(DrillTable.levels().get(level - 1).level()).isEqualTo(level);
        }
    }

    @Test
    void eachLevelIsWiderDeeperAndFasterThanTheLast() {
        for (int level = 2; level <= DrillTable.MAX_LEVEL; level++) {
            DrillSpec below = DrillTable.level(level - 1);
            DrillSpec spec = DrillTable.level(level);
            assertThat(spec.area()).isGreaterThan(below.area());
            assertThat(spec.depth()).isGreaterThan(below.depth());
            assertThat(spec.cycleTicks()).isLessThan(below.cycleTicks());
        }
    }

    @Test
    void everyAreaIsEvenSoTheSquareHasACentre() {
        for (DrillSpec spec : DrillTable.levels()) {
            assertThat(spec.area() % 2).as("area of level " + spec.level()).isZero();
        }
    }

    @Test
    void aLevelOutsideTheRangeIsRefused() {
        assertThatThrownBy(() -> DrillTable.level(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DrillTable.level(DrillTable.MAX_LEVEL + 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void everyDrillHasNineOutputSlots() {
        assertThat(DrillTable.OUTPUT_SLOTS).isEqualTo(9);
    }
}
