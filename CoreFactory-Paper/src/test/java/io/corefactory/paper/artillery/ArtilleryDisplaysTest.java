package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.resourcepack.art.ArtilleryArt;
import io.corefactory.resourcepack.art.MachineArt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the displays of each level: a body and a barrel for every level, the upper model of level VII and its barrel three blocks up, and the
 * agreement with the art.
 */
class ArtilleryDisplaysTest {

    /** The trunnion of each level, in blocks above the floor of the block, from level I to VII. */
    private static final double[] TRUNNIONS = {1.125, 1.09375, 2.09375, 2.0625, 2.0625, 2.53125, 3.25};

    @Test
    void everyLevelButSevenHasABodyAndABarrelLiftedOneBlock() {
        for (ArtilleryLevel level : List.of(ArtilleryLevel.I, ArtilleryLevel.II, ArtilleryLevel.III, ArtilleryLevel.IV,
                ArtilleryLevel.V, ArtilleryLevel.VI)) {
            int number = level.number();
            assertThat(ArtilleryDisplays.layers(level)).containsExactly(
                    new ArtilleryDisplays.Layer("artillery_" + number + "_body", ArtilleryDisplays.Part.BODY, 1.0, 0.0),
                    new ArtilleryDisplays.Layer("artillery_" + number + "_barrel", ArtilleryDisplays.Part.BARREL, 1.0,
                            TRUNNIONS[number - 1]));
        }
    }

    @Test
    void levelSevenHasItsBodyItsBarrelAndItsUpperModelThreeBlocksAbove() {
        assertThat(ArtilleryDisplays.layers(ArtilleryLevel.VII)).containsExactly(
                new ArtilleryDisplays.Layer("artillery_7_body", ArtilleryDisplays.Part.BODY, 1.0, 0.0),
                new ArtilleryDisplays.Layer("artillery_7_barrel", ArtilleryDisplays.Part.BARREL, 4.0, 3.25),
                new ArtilleryDisplays.Layer("artillery_7_top", ArtilleryDisplays.Part.TOP, 4.0, 0.0));
        assertThat(ArtilleryDisplays.offset("artillery_7_body")).isZero();
        assertThat(ArtilleryDisplays.offset("artillery_7_top")).isEqualTo(3.0);
        assertThat(ArtilleryDisplays.offset("artillery_7_barrel")).isEqualTo(3.0);
    }

    @Test
    void theLiftIsOneBlockForEveryArtilleryModel() {
        for (int number = 1; number <= 7; number++) {
            assertThat(ArtilleryDisplays.lift("artillery_" + number + "_body")).isEqualTo(1.0);
            assertThat(ArtilleryDisplays.lift("artillery_" + number + "_barrel")).isEqualTo(1.0);
        }
        assertThat(ArtilleryDisplays.lift(ArtilleryDisplays.TOP_MODEL)).isEqualTo(1.0);
    }

    @Test
    void aModelThatIsNoArtilleryModelHasNoLift() {
        assertThatThrownBy(() -> ArtilleryDisplays.lift("command_center")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theNumbersAgreeWithTheResourcePackArt() {
        assertThat(ArtilleryDisplays.TOP_MODEL).isEqualTo(ArtilleryArt.LEVEL_SEVEN_TOP);
        assertThat(ArtilleryDisplays.LEVEL_SEVEN_BARREL).isEqualTo(ArtilleryArt.LEVEL_SEVEN_BARREL);
        for (ArtilleryLevel level : ArtilleryLevel.values()) {
            for (ArtilleryDisplays.Layer layer : ArtilleryDisplays.layers(level)) {
                assertThat(ArtilleryDisplays.lift(layer.model())).isEqualTo(ArtilleryArt.displayLift(layer.model()));
                assertThat(ArtilleryDisplays.offset(layer.model())).isEqualTo(ArtilleryArt.displayOffset(layer.model()));
                if (layer.part() == ArtilleryDisplays.Part.BARREL) {
                    assertThat(layer.trunnionBlocks()).isEqualTo(ArtilleryArt.trunnionBlocks(layer.model()));
                }
            }
        }
    }

    @Test
    void everyDisplayShowsAModelOfTheArt() {
        List<String> models = ArtilleryArt.ALL.stream().map(MachineArt.Spec::id).toList();
        for (ArtilleryLevel level : ArtilleryLevel.values()) {
            for (ArtilleryDisplays.Layer layer : ArtilleryDisplays.layers(level)) {
                assertThat(models).as(layer.model()).contains(layer.model());
            }
        }
    }
}
