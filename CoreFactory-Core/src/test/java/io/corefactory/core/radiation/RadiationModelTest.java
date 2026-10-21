package io.corefactory.core.radiation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RadiationModelTest {

    @Test
    void theCentreRateGrowsWithTheSquareRootOfTheYield() {
        assertThat(RadiationModel.centreRate(0)).isZero();
        assertThat(RadiationModel.centreRate(4)).isCloseTo(0.02, within(1e-12));
        assertThat(RadiationModel.centreRate(-3)).isZero();
    }

    @Test
    void theRateFallsLinearlyToZeroAtTheEdge() {
        double centre = 0.01;
        assertThat(RadiationModel.rateAt(centre, 30, 0, 0, 1000)).isCloseTo(centre, within(1e-12));
        assertThat(RadiationModel.rateAt(centre, 30, 15, 0, 1000)).isCloseTo(centre / 2, within(1e-12));
        assertThat(RadiationModel.rateAt(centre, 30, 30, 0, 1000)).isZero();
        assertThat(RadiationModel.rateAt(centre, 30, 45, 0, 1000)).isZero();
    }

    @Test
    void aZoneHalvesEachHalfLife() {
        assertThat(RadiationModel.decay(0, 600)).isEqualTo(1.0);
        assertThat(RadiationModel.decay(600, 600)).isCloseTo(0.5, within(1e-12));
        assertThat(RadiationModel.decay(1200, 600)).isCloseTo(0.25, within(1e-12));
    }

    @Test
    void theDoseAccumulatesAndRecoversAfterTheHalfLife() {
        double dose = RadiationModel.absorb(0.0, 0.5, 4.0);
        assertThat(dose).isCloseTo(2.0, within(1e-12));

        double recovered = RadiationModel.recover(dose, RadiationModel.PLAYER_HALF_LIFE_SECONDS);
        assertThat(recovered).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void damageStartsAtTwoSievertsAndIsSevereFromFive() {
        assertThat(RadiationModel.damagePerSecond(1.9)).isZero();
        assertThat(RadiationModel.damagePerSecond(2.0)).isEqualTo(0.25);
        assertThat(RadiationModel.damagePerSecond(4.9)).isEqualTo(0.25);
        assertThat(RadiationModel.damagePerSecond(5.0)).isEqualTo(1.0);
    }

    @Test
    void nauseaStartsAtOneSievert() {
        assertThat(RadiationModel.causesNausea(0.99)).isFalse();
        assertThat(RadiationModel.causesNausea(1.0)).isTrue();
    }

    @Test
    void theColourFollowsTheSeverity() {
        assertThat(RadiationModel.severityColour(0.05)).isEqualTo("&a");
        assertThat(RadiationModel.severityColour(5)).isEqualTo("&e");
        assertThat(RadiationModel.severityColour(500)).isEqualTo("&6");
        assertThat(RadiationModel.severityColour(2000)).isEqualTo("&c");
    }

    @Test
    void theRateIsShownInTheUnitAGeigerCounterUses() {
        double perSecondOf05 = 0.5 / RadiationModel.SECONDS_PER_HOUR / RadiationModel.MSV_PER_SV;
        assertThat(RadiationModel.formatRate(perSecondOf05)).isEqualTo("500.00 uSv/h");
        double perSecondOf12 = 12.3 / RadiationModel.SECONDS_PER_HOUR / RadiationModel.MSV_PER_SV;
        assertThat(RadiationModel.formatRate(perSecondOf12)).isEqualTo("12.3 mSv/h");
        assertThat(RadiationModel.formatRate(RadiationModel.BACKGROUND_SV_PER_SECOND)).isEqualTo("0.10 uSv/h");
    }
}
