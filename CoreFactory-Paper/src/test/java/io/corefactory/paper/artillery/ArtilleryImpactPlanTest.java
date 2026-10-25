package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryEffects.Column;
import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.artillery.ArtillerySettings;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.artillery.GroundPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the layout of a shot's impact: the scatter, the burst altitude, the submunitions, the bombs and the fire, and that a seed replays it. */
class ArtilleryImpactPlanTest {

    private static final GroundPoint AIM = new GroundPoint(100.0, 200.0);
    private static final ArtillerySettings SETTINGS = ArtillerySettings.defaults();

    private static ArtilleryImpactPlan plan(ArtilleryShell shell, ArtilleryLevel level, ChargeTier tier, long seed) {
        return ArtilleryImpactPlan.of(SETTINGS, shell, level, tier, AIM, 1000.0, 1.0, 0.0, new Random(seed));
    }

    @Test
    void aContactShellLandsNearItsAimWithNoBurst() {
        ArtilleryImpactPlan impact = plan(ArtilleryShell.EXPLOSIVE, ArtilleryLevel.I, ChargeTier.TIER_I, 1L);
        assertThat(impact.altitude()).isEqualTo(0.0);
        assertThat(impact.submunitions()).isEmpty();
        assertThat(impact.fire()).isEmpty();
        // The sigma at 1000 blocks for level I is 60 blocks; six sigma on each axis bounds any landing in practice.
        assertThat(Math.abs(impact.landing().x() - AIM.x())).isLessThan(360.0);
        assertThat(Math.abs(impact.landing().z() - AIM.z())).isLessThan(360.0);
    }

    @Test
    void anAirBurstGoesOffInItsAltitudeRange() {
        for (long seed = 0; seed < 50; seed++) {
            double altitude = plan(ArtilleryShell.WHITE_PHOSPHORUS, ArtilleryLevel.I, ChargeTier.TIER_I, seed).altitude();
            assertThat(altitude).isBetween(25.0, 35.0);
        }
    }

    @Test
    void aClusterScattersItsSubmunitionsApartAndWithinItsSpread() {
        ArtilleryImpactPlan impact = plan(ArtilleryShell.CLUSTER, ArtilleryLevel.II, ChargeTier.TIER_II, 7L);
        List<GroundPoint> points = impact.submunitions();
        assertThat(points).hasSize(SETTINGS.cluster().submunitions());
        for (int i = 0; i < points.size(); i++) {
            assertThat(distance(points.get(i), impact.landing())).isLessThanOrEqualTo(SETTINGS.cluster().spreadRadiusBlocks());
            for (int j = i + 1; j < points.size(); j++) {
                assertThat(distance(points.get(i), points.get(j))).isGreaterThanOrEqualTo(SETTINGS.cluster().minSpacingBlocks());
            }
        }
    }

    @Test
    void clusterEnrichedUraniumDropsItsBombsApartAtItsBurst() {
        ArtilleryImpactPlan impact = plan(ArtilleryShell.CLUSTER_ENRICHED_URANIUM, ArtilleryLevel.VII, ChargeTier.TIER_III, 3L);
        List<GroundPoint> bombs = impact.submunitions();
        assertThat(bombs).hasSize(SETTINGS.clusterEnriched().bombs());
        assertThat(impact.altitude()).isEqualTo(8.0);
        for (int i = 0; i < bombs.size(); i++) {
            assertThat(distance(bombs.get(i), impact.landing())).isLessThanOrEqualTo(SETTINGS.clusterEnriched().spreadRadiusBlocks());
            for (int j = i + 1; j < bombs.size(); j++) {
                assertThat(distance(bombs.get(i), bombs.get(j))).isGreaterThanOrEqualTo(SETTINGS.clusterEnriched().minSpacingBlocks());
            }
        }
    }

    @Test
    void napalmSetsFireToAnEllipseAlongTheShot() {
        ArtilleryImpactPlan impact = plan(ArtilleryShell.NAPALM, ArtilleryLevel.I, ChargeTier.TIER_I, 11L);
        List<Column> fire = impact.fire();
        assertThat(fire).isNotEmpty();
        double reach = SETTINGS.napalm().lengthBlocks() / 2.0 + 2.0;
        for (Column column : fire) {
            assertThat(Math.abs(column.x() + 0.5 - impact.landing().x())).isLessThanOrEqualTo(reach);
            assertThat(Math.abs(column.z() + 0.5 - impact.landing().z())).isLessThanOrEqualTo(reach);
        }
    }

    @Test
    void theSameSeedGivesTheSameImpact() {
        assertThat(plan(ArtilleryShell.CLUSTER, ArtilleryLevel.II, ChargeTier.TIER_II, 99L))
                .isEqualTo(plan(ArtilleryShell.CLUSTER, ArtilleryLevel.II, ChargeTier.TIER_II, 99L));
    }

    private static double distance(GroundPoint a, GroundPoint b) {
        return Math.hypot(a.x() - b.x(), a.z() - b.z());
    }
}
