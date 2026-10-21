package io.corefactory.core.radiation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FalloutZoneTest {

    @Test
    void aBlastWithNoYieldLeavesNoFallout() {
        Fallout fallout = new Fallout();
        fallout.add(null, 0, 0, 30, 0.0, 0);

        assertThat(fallout.zones()).isEmpty();
    }

    @Test
    void aZoneHasItsRateAtTheCentreAndNoneOutsideItsRadius() {
        Fallout fallout = new Fallout();
        fallout.add(null, 100, 200, 90, 4.0, 0);
        List<Fallout.Zone> zones = fallout.zones();

        assertThat(zones).hasSize(1);
        Fallout.Zone zone = zones.get(0);
        assertThat(zone.rateAt(100, 200, 0)).isCloseTo(0.02, within(1e-12));
        assertThat(zone.rateAt(100 + 45, 200, 0)).isCloseTo(0.01, within(1e-12));
        assertThat(zone.rateAt(100 + 90, 200, 0)).isZero();
    }

    @Test
    void theRateDecaysOverTheZoneHalfLife() {
        Fallout fallout = new Fallout();
        fallout.add(null, 0, 0, 90, 4.0, 0);
        Fallout.Zone zone = fallout.zones().get(0);
        long halfLifeTicks = (long) (Fallout.ZONE_HALF_LIFE_SECONDS * 20);

        assertThat(zone.rateAt(0, 0, halfLifeTicks)).isCloseTo(0.01, within(1e-9));
    }

    @Test
    void aZoneIsSpentOnceItsCentreRateIsNegligible() {
        Fallout fallout = new Fallout();
        fallout.add(null, 0, 0, 90, 1.0, 0);
        Fallout.Zone zone = fallout.zones().get(0);

        assertThat(zone.spent(0)).isFalse();
        assertThat(zone.spent((long) (Fallout.ZONE_HALF_LIFE_SECONDS * 20 * 30))).isTrue();
    }
}
