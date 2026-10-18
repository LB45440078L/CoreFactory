package io.moretntplus.core.engine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShapesTest {

    @Test
    void sphereOfRadiusTwoHasThirtyThreeBlocks() {
        List<int[]> blocks = new ArrayList<>();
        Shapes.sphere(0, 0, 0, 2, -64, 320, (x, y, z) -> blocks.add(new int[]{x, y, z}));

        // Lattice points with x^2 + y^2 + z^2 <= 4.
        assertThat(blocks).hasSize(33);
        assertThat(blocks).allSatisfy(p -> assertThat(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]).isLessThanOrEqualTo(4));
    }

    @Test
    void sphereIsClampedToTheWorldHeight() {
        List<int[]> blocks = new ArrayList<>();
        // Only the y = 0 layer is inside [0, 0]: a disc of radius 2 has 13 lattice points.
        Shapes.sphere(0, 0, 0, 2, 0, 0, (x, y, z) -> blocks.add(new int[]{x, y, z}));

        assertThat(blocks).hasSize(13);
        assertThat(blocks).allSatisfy(p -> assertThat(p[1]).isZero());
    }

    @Test
    void scanOnlyVisitsOffsetsThatPassTheFilter() {
        List<int[]> blocks = new ArrayList<>();
        // Keep only the positive-x half of a cube of reach 1.
        Shapes.scan(0, 0, 0, 1, -64, 320, (dx, dy, dz) -> dx > 0,
                (x, y, z) -> blocks.add(new int[]{x, y, z}));

        assertThat(blocks).hasSize(9);
        assertThat(blocks).allSatisfy(p -> assertThat(p[0]).isEqualTo(1));
    }

    @Test
    void scanRespectsTheVerticalBounds() {
        List<int[]> blocks = new ArrayList<>();
        Shapes.scan(0, 5, 0, 3, 4, 6, (dx, dy, dz) -> true, (x, y, z) -> blocks.add(new int[]{x, y, z}));

        assertThat(blocks).allSatisfy(p -> assertThat(p[1]).isBetween(4, 6));
    }
}
