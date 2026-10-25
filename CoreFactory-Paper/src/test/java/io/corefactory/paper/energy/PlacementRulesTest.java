package io.corefactory.paper.energy;

import io.corefactory.paper.energy.PlacementRules.Refusal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlacementRulesTest {

    @Test
    void aFreeAirSpotWithResearchDonePassesToTheProtectionCheck() {
        assertThat(PlacementRules.before(false, true, false, true)).isEmpty();
    }

    @Test
    void aDisabledWorldIsRefusedFirst() {
        assertThat(PlacementRules.before(true, false, true, false)).contains(Refusal.DISABLED_WORLD);
    }

    @Test
    void aTargetThatIsNotAirIsRefused() {
        assertThat(PlacementRules.before(false, false, false, true)).contains(Refusal.NOT_AIR);
    }

    @Test
    void aSpotWithAnotherDeviceIsRefused() {
        assertThat(PlacementRules.before(false, true, true, true)).contains(Refusal.OCCUPIED);
    }

    @Test
    void anUnfinishedResearchNodeIsRefused() {
        assertThat(PlacementRules.before(false, true, false, false)).contains(Refusal.LOCKED);
    }

    @Test
    void theFirstFailingCheckIsTheReason() {
        assertThat(PlacementRules.before(false, false, true, false)).contains(Refusal.NOT_AIR);
        assertThat(PlacementRules.before(false, true, true, false)).contains(Refusal.OCCUPIED);
    }
}
