package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaterGridTest {

    private static Pos at(int x, int z) {
        return new Pos(x, 64, z);
    }

    @Test
    void twoNetworksJoinedByAPipeBecomeOne() {
        WaterGrid apart = WaterGrid.of(List.of(WaterNode.pipe(at(0, 0)), WaterNode.pipe(at(2, 0))));
        assertThat(apart.networks()).hasSize(2);

        WaterGrid joined = WaterGrid.of(List.of(
                WaterNode.pipe(at(0, 0)), WaterNode.pipe(at(1, 0)), WaterNode.pipe(at(2, 0))));
        assertThat(joined.networks()).hasSize(1);
        assertThat(joined.networks().get(0)).containsExactly(at(0, 0), at(1, 0), at(2, 0));
    }

    @Test
    void pipeNextToAPumpJoinsIt() {
        WaterGrid grid = WaterGrid.of(List.of(WaterNode.pump(at(0, 0)), WaterNode.pipe(at(0, 1))));
        assertThat(grid.networks()).hasSize(1);
        assertThat(grid.connectedFaces(at(0, 0))).containsExactly(Face.SOUTH);
        assertThat(grid.networkOf(at(0, 1))).isEqualTo(grid.networkOf(at(0, 0)));
    }

    @Test
    void consumerJoinsItsPipeOnlyOnItsFaces() {
        WaterNode consumer = WaterNode.consumer(at(0, 0), Set.of(Face.NORTH));

        WaterGrid onFace = WaterGrid.of(List.of(consumer, WaterNode.pipe(at(0, -1))));
        assertThat(onFace.networkOf(at(0, -1))).isEqualTo(onFace.networkOf(at(0, 0)));

        WaterGrid offFace = WaterGrid.of(List.of(consumer, WaterNode.pipe(at(1, 0))));
        assertThat(offFace.networks()).hasSize(2);
    }

    @Test
    void splitterTJoinsOnlyItsInputAndArms() {
        WaterNode t = WaterNode.splitter(at(0, 0), WaterKind.SPLITTER_T, Face.NORTH);
        WaterGrid grid = WaterGrid.of(List.of(t, WaterNode.pipe(at(0, 1)), WaterNode.pipe(at(1, 0))));
        assertThat(grid.connectedFaces(at(0, 0))).containsExactly(Face.EAST);
        assertThat(grid.networkOf(at(0, 1))).isNotEqualTo(grid.networkOf(at(0, 0)));
    }

    @Test
    void pipeAttachedOnlyWhenAPipeIsOnAJoinedFace() {
        WaterNode generator = WaterNode.consumer(at(0, 0), EnumSet.allOf(Face.class));
        assertThat(WaterGrid.of(List.of(generator, WaterNode.pump(at(1, 0)))).pipeAttached(at(0, 0))).isFalse();
        assertThat(WaterGrid.of(List.of(generator, WaterNode.pipe(at(1, 0)))).pipeAttached(at(0, 0))).isTrue();
    }

    @Test
    void twoBlocksAtOnePositionAreRefused() {
        assertThatThrownBy(() -> WaterGrid.of(List.of(WaterNode.pipe(at(0, 0)), WaterNode.pump(at(0, 0)))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownPositionIsInNoNetwork() {
        WaterGrid grid = WaterGrid.of(List.of(WaterNode.pipe(at(0, 0))));
        assertThat(grid.networkOf(at(9, 9))).isEqualTo(-1);
        assertThat(grid.connectedFaces(at(9, 9))).isEmpty();
    }
}
