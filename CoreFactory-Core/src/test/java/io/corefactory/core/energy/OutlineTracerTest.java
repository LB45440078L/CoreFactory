package io.corefactory.core.energy;

import io.corefactory.core.energy.OutlineTracer.Cell;
import io.corefactory.core.energy.OutlineTracer.Corner;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutlineTracerTest {

    private static Corner c(int x, int z) {
        return new Corner(x, z);
    }

    private static Set<Cell> cells(Cell... cells) {
        return Set.of(cells);
    }

    @Test
    void aSingleCellHasAFourCornerLoop() {
        List<List<Corner>> loops = OutlineTracer.trace(cells(new Cell(0, 0)));
        assertThat(loops).hasSize(1);
        assertThat(loops.get(0)).containsExactly(c(0, 0), c(1, 0), c(1, 1), c(0, 1));
    }

    @Test
    void aPylonSquareHasAFourCornerLoop() {
        Set<Cell> square = OutlineTracer.square(0, 0, 5);
        assertThat(square).hasSize(121);
        List<List<Corner>> loops = OutlineTracer.trace(square);
        assertThat(loops).hasSize(1);
        assertThat(loops.get(0)).containsExactly(c(-5, -5), c(6, -5), c(6, 6), c(-5, 6));
    }

    @Test
    void twoOverlappingSquaresGiveOneLoopOfTheUnionWithNoInteriorEdge() {
        Set<Cell> union = OutlineTracer.coverOf(List.of(
                new Pylon("a", new Pos(0, 64, 0), PylonType.SMALL),
                new Pylon("b", new Pos(4, 64, 0), PylonType.SMALL)));
        List<List<Corner>> loops = OutlineTracer.trace(union);
        assertThat(loops).hasSize(1);
        assertThat(loops.get(0)).containsExactly(c(-5, -5), c(10, -5), c(10, 6), c(-5, 6));
    }

    @Test
    void twoOverlappingCellBlocksGiveTheUnionRectangle() {
        Set<Cell> both = new HashSet<>(OutlineTracer.square(0, 0, 1));
        both.addAll(OutlineTracer.square(1, 0, 1));
        List<List<Corner>> loops = OutlineTracer.trace(both);
        assertThat(loops).hasSize(1);
        assertThat(loops.get(0)).containsExactly(c(-1, -1), c(3, -1), c(3, 2), c(-1, 2));
    }

    @Test
    void twoSquaresTouchingOnlyAtACornerGiveTwoLoops() {
        List<List<Corner>> loops = OutlineTracer.trace(cells(new Cell(0, 0), new Cell(1, 1)));
        assertThat(loops).hasSize(2);
        assertThat(loops.get(0)).containsExactly(c(0, 0), c(1, 0), c(1, 1), c(0, 1));
        assertThat(loops.get(1)).containsExactly(c(1, 1), c(2, 1), c(2, 2), c(1, 2));
    }

    @Test
    void aSquareWithAHoleGivesAnOuterLoopAndAHoleLoop() {
        Set<Cell> ring = OutlineTracer.square(1, 1, 1);
        ring.remove(new Cell(1, 1));
        List<List<Corner>> loops = OutlineTracer.trace(ring);
        assertThat(loops).hasSize(2);
        assertThat(loops.get(0)).containsExactly(c(0, 0), c(3, 0), c(3, 3), c(0, 3));
        assertThat(loops.get(1)).containsExactly(c(1, 1), c(1, 2), c(2, 2), c(2, 1));
    }

    @Test
    void anLShapeIsOneSixCornerLoop() {
        List<List<Corner>> loops = OutlineTracer.trace(cells(new Cell(0, 0), new Cell(1, 0), new Cell(0, 1)));
        assertThat(loops).hasSize(1);
        assertThat(loops.get(0)).containsExactly(c(0, 0), c(2, 0), c(2, 1), c(1, 1), c(1, 2), c(0, 2));
    }

    @Test
    void noCellsGiveNoLoops() {
        assertThat(OutlineTracer.trace(Set.of())).isEmpty();
    }

    @Test
    void aNegativeHalfSideIsRefused() {
        assertThatThrownBy(() -> OutlineTracer.square(0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
