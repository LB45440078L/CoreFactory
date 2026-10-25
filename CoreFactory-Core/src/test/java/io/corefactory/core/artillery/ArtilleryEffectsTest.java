package io.corefactory.core.artillery;

import io.corefactory.core.artillery.ArtilleryEffects.Column;
import io.corefactory.core.artillery.ArtilleryEffects.PhosphorusCloud;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ArtilleryEffectsTest {

    @Test
    void everySubmunitionLandsInsideTheRadius() {
        List<GroundPoint> points = ArtilleryEffects.clusterImpacts(100, -50, 30, 40, 7L, 4.0);
        assertThat(points).hasSize(40);
        for (GroundPoint p : points) {
            assertThat(Math.hypot(p.x() - 100, p.z() + 50)).isLessThanOrEqualTo(30.0 + 1e-9);
        }
    }

    @Test
    void everyPairOfSubmunitionsIsAtLeastTheMinimumSpacingApart() {
        List<GroundPoint> points = ArtilleryEffects.clusterImpacts(0, 0, 30, 40, 7L, 4.0);
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                double distance = Math.hypot(points.get(i).x() - points.get(j).x(), points.get(i).z() - points.get(j).z());
                assertThat(distance).isGreaterThanOrEqualTo(4.0 - 1e-9);
            }
        }
    }

    @Test
    void theSameSeedGivesTheSameSubmunitionsAndAnotherSeedGivesOthers() {
        List<GroundPoint> first = ArtilleryEffects.clusterImpacts(0, 0, 30, 40, 7L, 4.0);
        assertThat(ArtilleryEffects.clusterImpacts(0, 0, 30, 40, 7L, 4.0)).isEqualTo(first);
        assertThat(ArtilleryEffects.clusterImpacts(0, 0, 30, 40, 8L, 4.0)).isNotEqualTo(first);
        assertThat(ArtilleryEffects.clusterImpacts(0, 0, 30, 0, 7L, 4.0)).isEmpty();
    }

    @Test
    void aLayoutThatCannotFitIsRefused() {
        // Two points 5 blocks apart cannot fit in a disc of radius 1: the second finds no free spot in its tries.
        assertThatThrownBy(() -> ArtilleryEffects.clusterImpacts(0, 0, 1, 2, 1L, 5.0))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aNapalmBurstIsElongatedAlongTheShot() {
        // Length 30 along the shot (semi-axis 15), width 12 across it (semi-axis 6), shot going east.
        List<Column> burst = ArtilleryEffects.napalmBurst(0, 0, 1, 0, 30, 12);
        assertThat(burst).contains(new Column(14, 0));   // centre (14.5, 0.5): (14.5/15)^2 + (0.5/6)^2 = 0.94, inside
        assertThat(burst).contains(new Column(-14, 0));
        assertThat(burst).doesNotContain(new Column(0, 7));  // centre (0.5, 7.5): 7.5 > 6 across, outside
    }

    @Test
    void theLongAxisFollowsADiagonalShot() {
        // Shot going north-east, (1, 1): the ellipse runs along the diagonal.
        List<Column> burst = ArtilleryEffects.napalmBurst(0, 0, 1, 1, 30, 12);
        assertThat(burst).contains(new Column(10, 10));    // along 14.85 of 15, across 0: inside
        assertThat(burst).doesNotContain(new Column(10, -10)); // across 14.1 of 6: outside
        assertThat(burst).doesNotContain(new Column(14, 14));  // along 20.5 of 15: outside
    }

    @Test
    void withNoDirectionTheBurstIsACircleWhoseDiameterIsTheLength() {
        // Radius 15: (14, 0) centre (14.5, 0.5) is 14.5^2 + 0.5^2 = 210.5 <= 225, inside; (11, 11) centre (11.5, 11.5) is 264.5, outside.
        List<Column> burst = ArtilleryEffects.napalmBurst(0, 0, 0, 0, 30, 12);
        assertThat(burst).contains(new Column(14, 0), new Column(0, 14));
        assertThat(burst).doesNotContain(new Column(11, 11));
    }

    @Test
    void aNapalmBurstNeedsAPositiveLengthAndWidth() {
        assertThatThrownBy(() -> ArtilleryEffects.napalmBurst(0, 0, 1, 0, 0, 12))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void thePhosphorusCloudCoversTheDiscAndCarriesItsParticleCount() {
        PhosphorusCloud cloud = ArtilleryEffects.phosphorusCloud(0, 0, 8, 600);
        assertThat(cloud.lightParticles()).isEqualTo(600);
        // Column (0, 0) has its centre 0.71 from the middle; (-8, 0) has its centre (-7.5, 0.5) at 7.52 and is inside.
        assertThat(cloud.ground()).contains(new Column(0, 0), new Column(-8, 0));
        // Column (8, 0) has its centre (8.5, 0.5) at 8.52, outside; (-9, 0) is further out still.
        assertThat(cloud.ground()).doesNotContain(new Column(8, 0), new Column(-9, 0));
    }

    @Test
    void theExplosionRingIsEvenlySpacedOnItsCircle() {
        List<GroundPoint> ring = ArtilleryEffects.explosionRing(0, 0, 10, 4);
        assertThat(ring).hasSize(4);
        assertThat(ring.get(0).x()).isCloseTo(10.0, within(1e-9));
        assertThat(ring.get(0).z()).isCloseTo(0.0, within(1e-9));
        assertThat(ring.get(1).x()).isCloseTo(0.0, within(1e-9));
        assertThat(ring.get(1).z()).isCloseTo(10.0, within(1e-9));
        assertThat(ring.get(2).x()).isCloseTo(-10.0, within(1e-9));
        assertThatThrownBy(() -> ArtilleryEffects.explosionRing(0, 0, 10, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
