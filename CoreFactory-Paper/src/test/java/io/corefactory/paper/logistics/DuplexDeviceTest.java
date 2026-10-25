package io.corefactory.paper.logistics;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.logistics.LogisticsLevel;
import io.corefactory.core.logistics.PlacementReason;
import org.junit.jupiter.api.Test;
import org.joml.Vector3f;
import org.bukkit.util.Transformation;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The belt duplex as a placed device: its two cells, its display models and turn, its placement verdicts. Nothing here starts a server. */
class DuplexDeviceTest {

    private static final UUID WORLD = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

    @Test
    void aDuplexOccupiesItsAnchorAndTheCellOnTheRightOfItsFlow() {
        LogisticsDevice duplex = new LogisticsDevice("d", LogisticsIds.Kind.DUPLEX, WORLD, new Pos(10, 64, 10), LogisticsLevel.I,
                Dir.EAST, null);
        assertThat(duplex.cells()).containsExactly(new Pos(10, 64, 10), new Pos(10, 64, 11));
        assertThat(duplex.blockKey(new Pos(10, 64, 11))).isEqualTo(WORLD + ":10:64:11");
    }

    @Test
    void aBeltOrASplitterOccupiesOnlyItsBlock() {
        LogisticsDevice belt = new LogisticsDevice("b", LogisticsIds.Kind.BELT, WORLD, new Pos(10, 64, 10), LogisticsLevel.I,
                Dir.EAST, null);
        LogisticsDevice splitter = new LogisticsDevice("s", LogisticsIds.Kind.SPLITTER, WORLD, new Pos(10, 64, 10), LogisticsLevel.I,
                Dir.EAST, null);
        assertThat(belt.cells()).containsExactly(new Pos(10, 64, 10));
        assertThat(splitter.cells()).containsExactly(new Pos(10, 64, 10));
    }

    @Test
    void theModelsAreTheMachineKeysOfTheAnchorHalfAndTheRightHalf() {
        assertThat(BeltPath.duplexModelKeyOf("belt_duplex_1", false)).isEqualTo("machine/belt_duplex_1");
        assertThat(BeltPath.duplexModelKeyOf("belt_duplex_3", true)).isEqualTo("machine/belt_duplex_3_right");
    }

    @Test
    void aDuplexDisplayIsTurnedSoItsNorthFollowsTheFlowAndIsNotScaled() {
        for (Dir flow : Dir.values()) {
            double[] north = Orientation.rotate(Orientation.northYaw(flow), 0, -1);
            assertThat(north[0]).isCloseTo(Orientation.vectorOf(flow)[0], within());
            assertThat(north[1]).isCloseTo(Orientation.vectorOf(flow)[1], within());
            Transformation transform = LogisticsEntities.duplexTransform(flow);
            assertThat(transform.getScale()).isEqualTo(new Vector3f(1f, 1f, 1f));
            assertThat(transform.getTranslation()).isEqualTo(new Vector3f(0f, 0f, 0f));
        }
    }

    @Test
    void aPlacedDuplexFlowsAwayFromThePlayer() {
        assertThat(PlacementAim.plan(LogisticsIds.Kind.DUPLEX, new Pos(5, 64, 5), new Pos(0, 1, 0), 0.0))
                .map(PlacementAim.Plan::flow).contains(Dir.NORTH);
        assertThat(PlacementAim.plan(LogisticsIds.Kind.DUPLEX, new Pos(5, 64, 5), new Pos(1, 0, 0), 0.0))
                .map(PlacementAim.Plan::target).contains(new Pos(6, 64, 5));
    }

    @Test
    void aDuplexIsPlacedOnAnyFaceIncludingTheTopAndBottom() {
        assertThat(SidePlacement.decide(LogisticsIds.Kind.DUPLEX, new Pos(0, 1, 0), SidePlacement.Input.BELT_INTO, true,
                SidePlacement.Space.FREE, () -> PlacementReason.OK)).isEqualTo(SidePlacement.Verdict.OK);
    }

    @Test
    void aDuplexWithNoBeltBehindItIsRefused() {
        assertThat(SidePlacement.decide(LogisticsIds.Kind.DUPLEX, new Pos(0, 1, 0), SidePlacement.Input.OTHER, true,
                SidePlacement.Space.FREE, () -> PlacementReason.OK)).isEqualTo(SidePlacement.Verdict.NO_BACK_BELT);
    }

    @Test
    void aLockedDuplexIsRefusedBeforeItsInput() {
        assertThat(SidePlacement.decide(LogisticsIds.Kind.DUPLEX, new Pos(0, 1, 0), SidePlacement.Input.OTHER, false,
                SidePlacement.Space.FREE, () -> PlacementReason.OK)).isEqualTo(SidePlacement.Verdict.LOCKED);
    }

    @Test
    void aDuplexSpotThatIsNotFreeIsRefusedBeforeTheNetworkIsAsked() {
        assertThat(SidePlacement.decide(LogisticsIds.Kind.DUPLEX, new Pos(0, 1, 0), SidePlacement.Input.BELT_INTO, true,
                SidePlacement.Space.SOLID, () -> {
                    throw new AssertionError("the network must not be asked");
                })).isEqualTo(SidePlacement.Verdict.SOLID);
        assertThat(SidePlacement.decide(LogisticsIds.Kind.DUPLEX, new Pos(0, 1, 0), SidePlacement.Input.BELT_INTO, true,
                SidePlacement.Space.DEVICE, () -> PlacementReason.OK)).isEqualTo(SidePlacement.Verdict.OCCUPIED);
        assertThat(SidePlacement.decide(LogisticsIds.Kind.DUPLEX, new Pos(0, 1, 0), SidePlacement.Input.BELT_INTO, true,
                SidePlacement.Space.ENTITY, () -> PlacementReason.OK)).isEqualTo(SidePlacement.Verdict.ENTITY);
    }

    @Test
    void theNetworksReasonForAFreeSpotIsTheVerdict() {
        assertThat(SidePlacement.decide(LogisticsIds.Kind.DUPLEX, new Pos(0, 1, 0), SidePlacement.Input.BELT_INTO, true,
                SidePlacement.Space.FREE, () -> PlacementReason.MERGE)).isEqualTo(SidePlacement.Verdict.MERGE);
        assertThat(SidePlacement.decide(LogisticsIds.Kind.DUPLEX, new Pos(0, 1, 0), SidePlacement.Input.BELT_INTO, true,
                SidePlacement.Space.FREE, () -> PlacementReason.BLOCKED)).isEqualTo(SidePlacement.Verdict.BLOCKED);
    }

    @Test
    void aMachineBehindADuplexIsNotItsInput() {
        assertThat(SidePlacement.decide(LogisticsIds.Kind.DUPLEX, new Pos(0, 1, 0), SidePlacement.Input.PORT, true,
                SidePlacement.Space.FREE, () -> PlacementReason.OK)).isEqualTo(SidePlacement.Verdict.NO_BACK_BELT);
    }

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(1e-9);
    }
}
