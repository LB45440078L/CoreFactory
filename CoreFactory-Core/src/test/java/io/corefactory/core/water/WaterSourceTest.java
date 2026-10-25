package io.corefactory.core.water;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaterSourceTest {

    private static boolean inCube(int x, int y, int z, int from, int to) {
        return x >= from && x <= to && y >= from && y <= to && z >= from && z <= to;
    }

    private static final WaterSource.BlockProbe CUBE = (x, y, z) -> inCube(x, y, z, 0, 2);

    @Test
    void threeByThreeCubeCountsTwentySeven() {
        assertThat(WaterSource.sourceBlocks(1, 1, 1, CUBE, 1000)).isEqualTo(27);
    }

    @Test
    void theCountStopsAtTheCap() {
        assertThat(WaterSource.sourceBlocks(1, 1, 1, CUBE, 10)).isEqualTo(10);
        assertThat(WaterSource.sourceBlocks(1, 1, 1, CUBE, 27)).isEqualTo(27);
    }

    @Test
    void onlyWaterConnectedToTheStartIsCounted() {
        WaterSource.BlockProbe twoCubes = (x, y, z) -> inCube(x, y, z, 0, 2) || inCube(x, y, z, 10, 12);
        assertThat(WaterSource.sourceBlocks(1, 1, 1, twoCubes, 1000)).isEqualTo(27);
    }

    @Test
    void aDryStartCountsNothing() {
        assertThat(WaterSource.sourceBlocks(5, 5, 5, CUBE, 100)).isEqualTo(0);
    }

    @Test
    void hasSourceNeedsTheMinimumCount() {
        assertThat(WaterSource.hasSource(1, 1, 1, CUBE, 27)).isTrue();
        assertThat(WaterSource.hasSource(1, 1, 1, CUBE, 28)).isFalse();
    }

    @Test
    void aBlockOnLandBesideWaterCountsTheWaterItTouches() {
        WaterSource.BlockProbe cube = (x, y, z) -> inCube(x, y, z, 0, 2);
        assertThat(WaterSource.sourceAround(5, 1, 1, cube, 1000)).isZero();
        assertThat(WaterSource.sourceAround(3, 1, 1, cube, 1000)).isEqualTo(27);
        assertThat(WaterSource.sourceAround(3, 1, 1, cube, 10)).isEqualTo(10);
    }

    @Test
    void aBlockInWaterCountsItsOwnWaterToo() {
        assertThat(WaterSource.sourceAround(1, 1, 1, CUBE, 1000)).isEqualTo(27);
    }

    @Test
    void capMustBeAtLeastOne() {
        assertThatThrownBy(() -> WaterSource.sourceBlocks(1, 1, 1, CUBE, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
