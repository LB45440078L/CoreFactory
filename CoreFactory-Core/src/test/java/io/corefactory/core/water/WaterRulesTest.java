package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaterRulesTest {

    private static final Pos ORIGIN = new Pos(0, 64, 0);

    private static boolean joins(WaterNode node, Face face) {
        return WaterRules.connects(node, face, WaterNode.pipe(face.step(node.pos())));
    }

    @Test
    void pipeConnectsToAPipeOnEveryFace() {
        WaterNode centre = WaterNode.pipe(ORIGIN);
        for (Face face : Face.values()) {
            assertThat(joins(centre, face)).as("pipe on %s", face).isTrue();
        }
    }

    @Test
    void pumpConnectsToAPipeAndAPumpOnEveryFace() {
        WaterNode pump = WaterNode.pump(ORIGIN);
        for (Face face : Face.values()) {
            Pos at = face.step(ORIGIN);
            assertThat(WaterRules.connects(pump, face, WaterNode.pipe(at))).as("pipe on %s", face).isTrue();
            assertThat(WaterRules.connects(pump, face, WaterNode.pump(at))).as("pump on %s", face).isTrue();
        }
    }

    @Test
    void consumerConnectsToAPipeAndNotToAPumpOrAnotherConsumer() {
        WaterNode consumer = WaterNode.consumer(ORIGIN, EnumSet.allOf(Face.class));
        for (Face face : Face.values()) {
            Pos at = face.step(ORIGIN);
            assertThat(WaterRules.connects(consumer, face, WaterNode.pipe(at))).as("pipe on %s", face).isTrue();
            assertThat(WaterRules.connects(consumer, face, WaterNode.pump(at))).as("pump on %s", face).isFalse();
            assertThat(WaterRules.connects(consumer, face, WaterNode.consumer(at, EnumSet.allOf(Face.class))))
                    .as("consumer on %s", face).isFalse();
        }
    }

    @Test
    void consumerConnectsToASplitterOnItsOutputButNotOnItsInput() {
        WaterNode consumer = WaterNode.consumer(ORIGIN, EnumSet.allOf(Face.class));
        for (Face face : List.of(Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST)) {
            Pos at = face.step(ORIGIN);
            // The T's line runs across the consumer's face: its input is across, so the consumer sits on an output side
            Face across = face == Face.NORTH || face == Face.SOUTH ? Face.EAST : Face.NORTH;
            // The T's input faces the consumer: water would flow into the consumer, so the two do not join
            WaterNode inputTowardConsumer = WaterNode.splitter(at, WaterKind.SPLITTER_T, face.opposite(), across);
            assertThat(WaterRules.connects(consumer, face, inputTowardConsumer)).as("input on %s", face).isFalse();
            // The T's output faces the consumer: the consumer joins it
            WaterNode outputTowardConsumer = WaterNode.splitter(at, WaterKind.SPLITTER_T, across, face.opposite());
            assertThat(WaterRules.connects(consumer, face, outputTowardConsumer)).as("output on %s", face).isTrue();
        }
    }

    @Test
    void consumerJoinsOnlyOnItsOwnFaces() {
        WaterNode consumer = WaterNode.consumer(ORIGIN, Set.of(Face.NORTH));
        assertThat(joins(consumer, Face.NORTH)).isTrue();
        assertThat(joins(consumer, Face.EAST)).isFalse();
        assertThat(joins(consumer, Face.UP)).isFalse();
    }

    @Test
    void tSplitterAllowsItsInputAndTheTwoArmsOnly() {
        assertThat(WaterRules.allowedFaces(WaterKind.SPLITTER_T, Face.NORTH))
                .containsExactlyInAnyOrder(Face.NORTH, Face.EAST, Face.WEST);
        assertThat(WaterRules.allowedFaces(WaterKind.SPLITTER_T, Face.EAST))
                .containsExactlyInAnyOrder(Face.EAST, Face.NORTH, Face.SOUTH);
    }

    @Test
    void tSplitterConnectsOnItsInputAndArmsButNotBehindOrVertically() {
        WaterNode t = WaterNode.splitter(ORIGIN, WaterKind.SPLITTER_T, Face.NORTH);
        assertThat(joins(t, Face.NORTH)).isTrue();
        assertThat(joins(t, Face.EAST)).isTrue();
        assertThat(joins(t, Face.WEST)).isTrue();
        assertThat(joins(t, Face.SOUTH)).isFalse();
        assertThat(joins(t, Face.UP)).isFalse();
        assertThat(joins(t, Face.DOWN)).isFalse();
    }

    @Test
    void xSplitterAllowsTheFourHorizontalFacesIncludingTheInputsOpposite() {
        assertThat(WaterRules.allowedFaces(WaterKind.SPLITTER_X, Face.NORTH))
                .containsExactlyInAnyOrder(Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST);
        WaterNode x = WaterNode.splitter(ORIGIN, WaterKind.SPLITTER_X, Face.NORTH);
        assertThat(joins(x, Face.NORTH)).isTrue();
        assertThat(joins(x, Face.SOUTH)).isTrue();
        assertThat(joins(x, Face.EAST)).isTrue();
        assertThat(joins(x, Face.WEST)).isTrue();
        assertThat(joins(x, Face.UP)).isFalse();
        assertThat(joins(x, Face.DOWN)).isFalse();
    }

    @Test
    void splitterInputMustBeHorizontal() {
        assertThatThrownBy(() -> WaterRules.allowedFaces(WaterKind.SPLITTER_T, Face.UP))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WaterRules.allowedFaces(WaterKind.SPLITTER_X, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WaterNode.splitter(ORIGIN, WaterKind.SPLITTER_X, Face.DOWN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nodeFacesMustMatchItsKind() {
        assertThatThrownBy(() -> new WaterNode(ORIGIN, WaterKind.PIPE, Face.NORTH, EnumSet.allOf(Face.class)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WaterNode(ORIGIN, WaterKind.SPLITTER_T, Face.NORTH, EnumSet.allOf(Face.class)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void kindRulesAreSymmetricAndOnlyPipesAndSplittersJoinAConsumer() {
        for (WaterKind a : WaterKind.values()) {
            for (WaterKind b : WaterKind.values()) {
                assertThat(WaterRules.kindsConnect(a, b)).as("%s-%s", a, b)
                        .isEqualTo(WaterRules.kindsConnect(b, a));
            }
        }
        assertThat(WaterRules.kindsConnect(WaterKind.PIPE, WaterKind.CONSUMER)).isTrue();
        assertThat(WaterRules.kindsConnect(WaterKind.CONSUMER, WaterKind.CONSUMER)).isFalse();
        assertThat(WaterRules.kindsConnect(WaterKind.CONSUMER, WaterKind.PUMP)).isFalse();
        assertThat(WaterRules.kindsConnect(WaterKind.CONSUMER, WaterKind.SPLITTER_T)).isTrue();
        assertThat(WaterRules.kindsConnect(WaterKind.PUMP, WaterKind.SPLITTER_X)).isTrue();
    }

    @Test
    void nuclearUnderwaterAloneRuns() {
        assertThat(WaterRules.nuclearMayRun(true, true, false, 0.0)).isTrue();
    }

    @Test
    void nuclearUnderwaterNeedsItsOwnBlockToBeWater() {
        assertThat(WaterRules.nuclearMayRun(false, true, false, 0.0)).isFalse();
    }

    @Test
    void nuclearPipeWithFlowRuns() {
        assertThat(WaterRules.nuclearMayRun(false, false, true, 3.0)).isTrue();
    }

    @Test
    void nuclearPipeWithoutFlowDoesNotRun() {
        assertThat(WaterRules.nuclearMayRun(false, false, true, 0.0)).isFalse();
    }

    @Test
    void nuclearWithNoPipeAndNoUnderwaterDoesNotRun() {
        assertThat(WaterRules.nuclearMayRun(false, false, false, 5.0)).isFalse();
    }
}
