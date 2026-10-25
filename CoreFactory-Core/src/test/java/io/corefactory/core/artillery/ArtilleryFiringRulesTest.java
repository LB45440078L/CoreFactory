package io.corefactory.core.artillery;

import io.corefactory.core.artillery.ArtilleryFiringRules.Shot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArtilleryFiringRulesTest {

    private static final long TWELVE_HOURS_MS = 12L * 3_600_000L;

    private final ArtilleryFiringRules rules = new ArtilleryFiringRules(ArtillerySettings.defaults());

    /** A shot from a piece at the origin to an aim point {@code distance} blocks east, with the firing player 100 blocks behind it. */
    private static Shot shotAt(ArtilleryShell shell, ArtilleryLevel level, ChargeTier tier, double elevation, double distance) {
        Position artillery = new Position(0, 64, 0);
        Position player = new Position(0, 64, -100);
        return new Shot(shell, level, tier, elevation, artillery, new Position(distance, 64, 0), player, List.of(player),
                1.0, OptionalLong.empty(), 0L);
    }

    @Test
    void theBlastRadiusScalesWithTheCubeRootOfTheYieldAndHasAFloor() {
        // 1.15 x cbrt(15000) = 1.15 x 24.6621 = 28.3614: the Little Boy's 28 blocks in tnts.yml.
        assertThat(rules.blastRadiusBlocks(15_000)).isCloseTo(28.3614, within(0.001));
        // 1.15 x cbrt(1000000) = 115.
        assertThat(rules.blastRadiusBlocks(1_000_000)).isCloseTo(115.0, within(1e-9));
        // 1.15 x cbrt(1) = 1.15, below the floor of 15 blocks.
        assertThat(rules.blastRadiusBlocks(1)).isCloseTo(15.0, within(1e-9));
    }

    @Test
    void theReachOfEachShellIsItsOwnEffect() {
        assertThat(rules.reachBlocks(ArtilleryShell.ENRICHED_URANIUM)).isCloseTo(28.3614, within(0.001));
        assertThat(rules.reachBlocks(ArtilleryShell.HYDROGEN_THERMONUCLEAR)).isCloseTo(115.0, within(1e-9));
        // Cluster: spread 30 plus the floor blast of one submunition, 15: 45.
        assertThat(rules.reachBlocks(ArtilleryShell.CLUSTER)).isCloseTo(45.0, within(1e-9));
        // Cluster enriched: spread 50 plus the floor blast of one 15-ton bomb, 15: 65.
        assertThat(rules.reachBlocks(ArtilleryShell.CLUSTER_ENRICHED_URANIUM)).isCloseTo(65.0, within(1e-9));
        // White phosphorus: its cloud, 8 blocks. Napalm: half its 30-block length, 15 blocks.
        assertThat(rules.reachBlocks(ArtilleryShell.WHITE_PHOSPHORUS)).isCloseTo(8.0, within(1e-9));
        assertThat(rules.reachBlocks(ArtilleryShell.NAPALM)).isCloseTo(15.0, within(1e-9));
        // Antimatter: 1.15 x cbrt(5e8) = 912.7556 blocks.
        assertThat(rules.reachBlocks(ArtilleryShell.CONDENSED_ANTIMATTER)).isCloseTo(912.7556, within(0.001));
    }

    @Test
    void theLeastDistanceIsFiftyForConventionalAndTwoHundredForNuclearAndTwoThousandForAntimatter() {
        assertThat(rules.minimumDistanceBlocks(ArtilleryShell.EXPLOSIVE)).isCloseTo(50.0, within(1e-9));
        assertThat(rules.minimumDistanceBlocks(ArtilleryShell.WHITE_PHOSPHORUS)).isCloseTo(50.0, within(1e-9));
        assertThat(rules.minimumDistanceBlocks(ArtilleryShell.ENRICHED_URANIUM)).isCloseTo(200.0, within(1e-9));
        assertThat(rules.minimumDistanceBlocks(ArtilleryShell.CLUSTER_ENRICHED_URANIUM)).isCloseTo(200.0, within(1e-9));
        assertThat(rules.minimumDistanceBlocks(ArtilleryShell.CONDENSED_ANTIMATTER)).isCloseTo(2000.0, within(1e-9));
    }

    @Test
    void aShellNeedsItsChargeTierOrAHigherOne() {
        assertThat(rules.charge(ArtilleryShell.EXPLOSIVE, ChargeTier.TIER_I)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.charge(ArtilleryShell.ENRICHED_URANIUM, ChargeTier.TIER_I)).isEqualTo(FiringVerdict.CHARGE_TOO_LOW);
        assertThat(rules.charge(ArtilleryShell.ENRICHED_URANIUM, ChargeTier.TIER_II)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.charge(ArtilleryShell.CLUSTER_ENRICHED_URANIUM, ChargeTier.TIER_II)).isEqualTo(FiringVerdict.CHARGE_TOO_LOW);
        assertThat(rules.charge(ArtilleryShell.CLUSTER_ENRICHED_URANIUM, ChargeTier.TIER_III)).isEqualTo(FiringVerdict.OK);
    }

    @Test
    void aShellNeedsItsArtilleryLevelOrAHigherOne() {
        assertThat(rules.level(ArtilleryShell.EXPLOSIVE, ArtilleryLevel.I)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.level(ArtilleryShell.ENRICHED_URANIUM, ArtilleryLevel.V)).isEqualTo(FiringVerdict.LEVEL_TOO_LOW);
        assertThat(rules.level(ArtilleryShell.ENRICHED_URANIUM, ArtilleryLevel.VI)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.level(ArtilleryShell.CLUSTER_ENRICHED_URANIUM, ArtilleryLevel.VI)).isEqualTo(FiringVerdict.LEVEL_TOO_LOW);
        assertThat(rules.level(ArtilleryShell.CLUSTER_ENRICHED_URANIUM, ArtilleryLevel.VII)).isEqualTo(FiringVerdict.OK);
    }

    @Test
    void theElevationIsFromFiveToEightyFiveDegreesAndTheClusterEnrichedShotNeedsTwentyFive() {
        assertThat(rules.elevation(ArtilleryShell.EXPLOSIVE, 4.999)).isEqualTo(FiringVerdict.ELEVATION_TOO_LOW);
        assertThat(rules.elevation(ArtilleryShell.EXPLOSIVE, 5.0)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.elevation(ArtilleryShell.EXPLOSIVE, 85.0)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.elevation(ArtilleryShell.EXPLOSIVE, 85.001)).isEqualTo(FiringVerdict.ELEVATION_TOO_HIGH);
        assertThat(rules.elevation(ArtilleryShell.EXPLOSIVE, Double.NaN)).isEqualTo(FiringVerdict.ELEVATION_TOO_LOW);

        assertThat(rules.elevation(ArtilleryShell.CLUSTER_ENRICHED_URANIUM, 24.999))
                .isEqualTo(FiringVerdict.CLUSTER_ELEVATION_TOO_LOW);
        assertThat(rules.elevation(ArtilleryShell.CLUSTER_ENRICHED_URANIUM, 25.0)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.elevation(ArtilleryShell.CLUSTER_ENRICHED_URANIUM, 4.9)).isEqualTo(FiringVerdict.ELEVATION_TOO_LOW);
    }

    @Test
    void theTargetMustBeWithinTheRangeOfTheLevelAndCharge() {
        // Level I, tier I: 1500 blocks. Tier II: 1500 x 1.25 = 1875 blocks.
        assertThat(rules.range(ArtilleryLevel.I, ChargeTier.TIER_I, 1500.0)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.range(ArtilleryLevel.I, ChargeTier.TIER_I, 1500.001)).isEqualTo(FiringVerdict.OUT_OF_RANGE);
        assertThat(rules.range(ArtilleryLevel.I, ChargeTier.TIER_II, 1875.0)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.range(ArtilleryLevel.I, ChargeTier.TIER_II, 1875.1)).isEqualTo(FiringVerdict.OUT_OF_RANGE);
    }

    @Test
    void theTargetMustBeAtLeastTheLeastDistanceAway() {
        assertThat(rules.minimumDistance(ArtilleryShell.EXPLOSIVE, 50.0)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.minimumDistance(ArtilleryShell.EXPLOSIVE, 49.999)).isEqualTo(FiringVerdict.TOO_CLOSE);
        assertThat(rules.minimumDistance(ArtilleryShell.ENRICHED_URANIUM, 200.0)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.minimumDistance(ArtilleryShell.ENRICHED_URANIUM, 199.999)).isEqualTo(FiringVerdict.TOO_CLOSE);
        assertThat(rules.minimumDistance(ArtilleryShell.CONDENSED_ANTIMATTER, 2000.0)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.minimumDistance(ArtilleryShell.CONDENSED_ANTIMATTER, 1999.999)).isEqualTo(FiringVerdict.TOO_CLOSE);
    }

    @Test
    void theBlastMustStopShortOfThePieceAndOfThePlayerWithItsMarginAndScatter() {
        // Explosive: reach 15, margin 20, no scatter: the aim must be more than 35 blocks from the piece and from the player.
        Position artillery = new Position(0, 0, 0);
        Position farPlayer = new Position(0, 0, -1000);
        assertThat(rules.blast(ArtilleryShell.EXPLOSIVE, new Position(35.0, 0, 0), artillery, farPlayer, 0.0))
                .isEqualTo(FiringVerdict.BLAST_REACHES_ARTILLERY);
        assertThat(rules.blast(ArtilleryShell.EXPLOSIVE, new Position(35.001, 0, 0), artillery, farPlayer, 0.0))
                .isEqualTo(FiringVerdict.OK);

        Position farPiece = new Position(-1000, 0, 0);
        assertThat(rules.blast(ArtilleryShell.EXPLOSIVE, new Position(0, 0, 0), farPiece, new Position(30, 0, 0), 0.0))
                .isEqualTo(FiringVerdict.BLAST_REACHES_PLAYER);
        assertThat(rules.blast(ArtilleryShell.EXPLOSIVE, new Position(0, 0, 0), farPiece, new Position(36, 0, 0), 0.0))
                .isEqualTo(FiringVerdict.OK);

        // With 10 blocks of scatter allowed, the clearance is 45: a player at 40 blocks is now inside it.
        assertThat(rules.blast(ArtilleryShell.EXPLOSIVE, new Position(0, 0, 0), farPiece, new Position(40, 0, 0), 10.0))
                .isEqualTo(FiringVerdict.BLAST_REACHES_PLAYER);
    }

    @Test
    void aNuclearShellMayNotBeAimedNearAPlayer() {
        // Enriched uranium: fallout radius 3 x 28.3614 = 85.08 blocks. A player at 80 blocks is inside it, one at 90 is not.
        Position aim = new Position(0, 0, 0);
        assertThat(rules.nuclearNearPlayers(ArtilleryShell.ENRICHED_URANIUM, aim, List.of(new Position(80, 0, 0)), 0.0))
                .isEqualTo(FiringVerdict.NUCLEAR_NEAR_PLAYER);
        assertThat(rules.nuclearNearPlayers(ArtilleryShell.ENRICHED_URANIUM, aim, List.of(new Position(90, 0, 0)), 0.0))
                .isEqualTo(FiringVerdict.OK);
        // The scatter allowance widens the radius to 95.08, so 90 blocks is inside it now.
        assertThat(rules.nuclearNearPlayers(ArtilleryShell.ENRICHED_URANIUM, aim, List.of(new Position(90, 0, 0)), 10.0))
                .isEqualTo(FiringVerdict.NUCLEAR_NEAR_PLAYER);
        // Any one player in the list is enough to refuse the shot.
        assertThat(rules.nuclearNearPlayers(ArtilleryShell.ENRICHED_URANIUM, aim,
                List.of(new Position(200, 0, 0), new Position(0, 0, 10)), 0.0))
                .isEqualTo(FiringVerdict.NUCLEAR_NEAR_PLAYER);
        // A conventional shell has no such limit.
        assertThat(rules.nuclearNearPlayers(ArtilleryShell.EXPLOSIVE, aim, List.of(new Position(1, 0, 0)), 0.0))
                .isEqualTo(FiringVerdict.OK);
    }

    @Test
    void theAntimatterCooldownIsTwelveHoursAndOnlyTheAntimatterShellHasOne() {
        assertThat(rules.cooldown(ArtilleryShell.CONDENSED_ANTIMATTER, OptionalLong.of(0L), TWELVE_HOURS_MS - 1))
                .isEqualTo(FiringVerdict.COOLDOWN_ACTIVE);
        assertThat(rules.cooldown(ArtilleryShell.CONDENSED_ANTIMATTER, OptionalLong.of(0L), TWELVE_HOURS_MS))
                .isEqualTo(FiringVerdict.OK);
        assertThat(rules.cooldown(ArtilleryShell.CONDENSED_ANTIMATTER, OptionalLong.empty(), 1L)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.cooldown(ArtilleryShell.EXPLOSIVE, OptionalLong.of(0L), 1L)).isEqualTo(FiringVerdict.OK);
    }

    @Test
    void thePieceNeedsItsFullPower() {
        assertThat(rules.power(1.0)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.power(0.9999995)).isEqualTo(FiringVerdict.OK);
        assertThat(rules.power(0.999)).isEqualTo(FiringVerdict.POWER_SHORT);
        assertThat(rules.power(0.0)).isEqualTo(FiringVerdict.POWER_SHORT);
    }

    @Test
    void aShotInsideEveryRuleIsAllowed() {
        assertThat(rules.check(shotAt(ArtilleryShell.EXPLOSIVE, ArtilleryLevel.I, ChargeTier.TIER_I, 45, 300)))
                .isEqualTo(FiringVerdict.OK);
    }

    @Test
    void aShotThatBreaksTwoRulesGetsTheFirstOne() {
        // Power short and too close: the least distance comes before the power in the order of the checks.
        Shot tooCloseAndUnpowered = new Shot(ArtilleryShell.EXPLOSIVE, ArtilleryLevel.I, ChargeTier.TIER_I, 45,
                new Position(0, 64, 0), new Position(20, 64, 0), new Position(0, 64, -100), List.of(),
                0.5, OptionalLong.empty(), 0L);
        assertThat(rules.check(tooCloseAndUnpowered)).isEqualTo(FiringVerdict.TOO_CLOSE);
    }

    @Test
    void theAntimatterShotIsRefusedWhenItIsTooCloseOrTooNearAPlayer() {
        // At 300 blocks it is inside the 2000-block minimum.
        assertThat(rules.check(shotAt(ArtilleryShell.CONDENSED_ANTIMATTER, ArtilleryLevel.VII, ChargeTier.TIER_III, 45, 300)))
                .isEqualTo(FiringVerdict.TOO_CLOSE);
        // At 2500 blocks the fallout radius is 3 x 912.76 = 2738 plus 3 sigma of scatter (8 x 1.5 x 2.5 x 3 = 90): the player 100
        // blocks behind the piece is 2502 blocks from the aim, inside it.
        assertThat(rules.check(shotAt(ArtilleryShell.CONDENSED_ANTIMATTER, ArtilleryLevel.VII, ChargeTier.TIER_III, 45, 2500)))
                .isEqualTo(FiringVerdict.NUCLEAR_NEAR_PLAYER);
        // At 3000 blocks the player is 3002 blocks away, outside 2738 + 108, and the shot is allowed.
        assertThat(rules.check(shotAt(ArtilleryShell.CONDENSED_ANTIMATTER, ArtilleryLevel.VII, ChargeTier.TIER_III, 45, 3000)))
                .isEqualTo(FiringVerdict.OK);
    }

    @Test
    void aClusterEnrichedShotNeedsItsSteepElevationAndAValidOneIsAllowed() {
        assertThat(rules.check(shotAt(ArtilleryShell.CLUSTER_ENRICHED_URANIUM, ArtilleryLevel.VII, ChargeTier.TIER_III, 20, 300)))
                .isEqualTo(FiringVerdict.CLUSTER_ELEVATION_TOO_LOW);
        assertThat(rules.check(shotAt(ArtilleryShell.CLUSTER_ENRICHED_URANIUM, ArtilleryLevel.VII, ChargeTier.TIER_III, 45, 300)))
                .isEqualTo(FiringVerdict.OK);
    }

    @Test
    void aShotIsRefusedWhileThePieceIsLowOnPowerAndWhileTheCooldownRuns() {
        Shot unpowered = new Shot(ArtilleryShell.EXPLOSIVE, ArtilleryLevel.I, ChargeTier.TIER_I, 45,
                new Position(0, 64, 0), new Position(300, 64, 0), new Position(0, 64, -100), List.of(),
                0.5, OptionalLong.empty(), 0L);
        assertThat(rules.check(unpowered)).isEqualTo(FiringVerdict.POWER_SHORT);

        Shot cooling = new Shot(ArtilleryShell.CONDENSED_ANTIMATTER, ArtilleryLevel.VII, ChargeTier.TIER_III, 45,
                new Position(0, 64, 0), new Position(3000, 64, 0), new Position(0, 64, -100), List.of(),
                1.0, OptionalLong.of(0L), TWELVE_HOURS_MS - 1);
        assertThat(rules.check(cooling)).isEqualTo(FiringVerdict.COOLDOWN_ACTIVE);
    }
}
