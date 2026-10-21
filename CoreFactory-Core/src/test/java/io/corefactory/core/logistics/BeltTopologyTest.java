package io.corefactory.core.logistics;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.corefactory.core.logistics.Fixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BeltTopologyTest {

    private static final LogisticsLevel L = LogisticsLevel.I;

    @Test
    void aStraightLineOfFiveWestFlowingUnitsIsStraight() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder();
        for (int x = 0; x < 5; x++) {
            builder.belt(at(x, 0), Dir.WEST, L);
        }
        LogisticsNetwork network = builder.build();
        for (int x = 0; x < 5; x++) {
            assertThat(network.shapeOf(at(x, 0))).isEqualTo(BeltShape.STRAIGHT);
        }
    }

    @Test
    void theHeadOfAStraightLineTakesItsItemsFromBehind() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(4, 0), Dir.WEST, L)
                .belt(at(3, 0), Dir.WEST, L)
                .build();
        assertThat(network.inputSideOf(at(4, 0))).isEqualTo(Dir.EAST);
        assertThat(network.inputSideOf(at(3, 0))).isEqualTo(Dir.EAST);
    }

    @Test
    void aLoneBeltHasNoInputAndIsStraight() {
        LogisticsNetwork network = LogisticsNetwork.builder().belt(at(0, 0), Dir.NORTH, L).build();
        assertThat(network.shapeOf(at(0, 0))).isEqualTo(BeltShape.STRAIGHT);
        assertThat(network.inputSideOf(at(0, 0))).isEqualTo(Dir.SOUTH);
    }

    @Test
    void aFifthUnitFeedingTheHeadOfANorthLineMakesItACorner() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder();
        for (int z = 4; z >= 0; z--) {
            builder.belt(at(0, z), Dir.NORTH, L);
        }
        builder.belt(at(1, 4), Dir.WEST, L);
        LogisticsNetwork network = builder.build();
        assertThat(network.shapeOf(at(0, 4))).isEqualTo(BeltShape.CORNER_RIGHT);
        assertThat(network.inputSideOf(at(0, 4))).isEqualTo(Dir.EAST);
    }

    @Test
    void theCornerTurnsRightWhenItemsTravelWestIntoANorthFlow() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.NORTH, L)
                .belt(at(1, 0), Dir.WEST, L)
                .build();
        assertThat(network.shapeOf(at(0, 0))).isEqualTo(BeltShape.CORNER_RIGHT);
    }

    @Test
    void theMirroredCornerTurnsLeft() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 4), Dir.NORTH, L)
                .belt(at(-1, 4), Dir.EAST, L)
                .build();
        assertThat(network.shapeOf(at(0, 4))).isEqualTo(BeltShape.CORNER_LEFT);
        assertThat(network.inputSideOf(at(0, 4))).isEqualTo(Dir.WEST);
    }

    @Test
    void theUnitsBehindTheCornerStayStraight() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder();
        for (int z = 4; z >= 0; z--) {
            builder.belt(at(0, z), Dir.NORTH, L);
        }
        builder.belt(at(1, 4), Dir.WEST, L);
        LogisticsNetwork network = builder.build();
        for (int z = 0; z <= 3; z++) {
            assertThat(network.shapeOf(at(0, z))).isEqualTo(BeltShape.STRAIGHT);
        }
    }

    @Test
    void aPickerFeedingABeltFromTheSideMakesItACorner() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .picker("p", at(-1, 0), Dir.WEST, Dir.EAST, L)
                .belt(at(0, 0), Dir.SOUTH, L)
                .build();
        assertThat(network.shapeOf(at(0, 0))).isEqualTo(BeltShape.CORNER_RIGHT);
    }

    @Test
    void aPickerFeedingABeltFromBehindKeepsItStraight() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .picker("p", at(-1, 0), Dir.WEST, Dir.EAST, L)
                .belt(at(0, 0), Dir.EAST, L)
                .build();
        assertThat(network.shapeOf(at(0, 0))).isEqualTo(BeltShape.STRAIGHT);
        assertThat(network.inputSideOf(at(0, 0))).isEqualTo(Dir.WEST);
    }

    @Test
    void aSplitterOutputFeedingABeltFromBehindIsStraight() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)
                .belt(at(1, 0), Dir.EAST, L)
                .build();
        assertThat(network.shapeOf(at(1, 0))).isEqualTo(BeltShape.STRAIGHT);
        assertThat(network.inputSideOf(at(1, 0))).isEqualTo(Dir.WEST);
    }

    @Test
    void aSplitterRightOutputTurningEastIsALeftCorner() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)
                .belt(at(0, 1), Dir.EAST, L)
                .build();
        assertThat(network.shapeOf(at(0, 1))).isEqualTo(BeltShape.CORNER_LEFT);
        assertThat(network.inputSideOf(at(0, 1))).isEqualTo(Dir.NORTH);
    }

    @Test
    void aSplitterRightOutputContinuingSouthIsStraight() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)
                .belt(at(0, 1), Dir.SOUTH, L)
                .build();
        assertThat(network.shapeOf(at(0, 1))).isEqualTo(BeltShape.STRAIGHT);
        assertThat(network.inputSideOf(at(0, 1))).isEqualTo(Dir.NORTH);
    }

    @Test
    void aSplitterLeftOutputContinuingNorthIsStraight() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)
                .belt(at(0, -1), Dir.NORTH, L)
                .build();
        assertThat(network.shapeOf(at(0, -1))).isEqualTo(BeltShape.STRAIGHT);
        assertThat(network.inputSideOf(at(0, -1))).isEqualTo(Dir.SOUTH);
    }

    @Test
    void aBeltFedFromTheSideTurnsIntoItsFlow() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, L)
                .belt(at(0, 1), Dir.NORTH, L)
                .build();
        assertThat(network.inputSideOf(at(0, 0))).isEqualTo(Dir.SOUTH);
        assertThat(network.shapeOf(at(0, 0))).isEqualTo(BeltShape.CORNER_RIGHT);
    }

    @Test
    void aFourUnitLoopIsAllowedAndEveryUnitIsACorner() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, L)
                .belt(at(1, 0), Dir.SOUTH, L)
                .belt(at(1, 1), Dir.WEST, L)
                .belt(at(0, 1), Dir.NORTH, L)
                .build();
        assertThat(network.shapeOf(at(0, 0))).isEqualTo(BeltShape.CORNER_RIGHT);
        assertThat(network.shapeOf(at(1, 0))).isEqualTo(BeltShape.CORNER_RIGHT);
        assertThat(network.shapeOf(at(1, 1))).isEqualTo(BeltShape.CORNER_RIGHT);
        assertThat(network.shapeOf(at(0, 1))).isEqualTo(BeltShape.CORNER_RIGHT);
    }

    @Test
    void aBeltWithTwoInputsIsRefusedAsAMerge() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.WEST, L)
                .belt(at(1, 0), Dir.WEST, L);
        assertThatThrownBy(() -> builder.belt(at(0, -1), Dir.SOUTH, L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MERGE");
    }

    @Test
    void aBeltFedHeadOnIsRefusedAsBlocked() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder().belt(at(0, 0), Dir.WEST, L);
        assertThatThrownBy(() -> builder.belt(at(-1, 0), Dir.EAST, L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BLOCKED");
    }

    @Test
    void theUnitFedByANeighbourHasTheNeighbourAsItsOnlyInput() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(2, 0), Dir.WEST, L)
                .belt(at(1, 0), Dir.WEST, L)
                .build();
        assertThat(network.inputSideOf(at(1, 0))).isEqualTo(Dir.EAST);
        assertThat(network.inputSideOf(at(2, 0))).isEqualTo(Dir.EAST);
    }

    @Test
    void theShapeOfAnUnknownBeltIsAnError() {
        LogisticsNetwork network = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L).build();
        assertThatThrownBy(() -> network.shapeOf(at(9, 9))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theItemsOfAShapeDoNotChangeItsShape() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 4), Dir.NORTH, L)
                .belt(at(1, 4), Dir.WEST, L)
                .build();
        network.insertAtEntry(at(1, 4), Fixtures.A);
        assertThat(network.shapeOf(at(0, 4))).isEqualTo(BeltShape.CORNER_RIGHT);
    }
}
