package io.corefactory.core.artillery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtilleryMagazineTest {

    @Test
    void loadingOneAddsOneShellAndOneCharge() {
        ArtilleryMagazine magazine = new ArtilleryMagazine(16, 0, 0);

        assertThat(magazine.load()).isEqualTo(ArtilleryMagazine.Verdict.OK);

        assertThat(magazine.shells()).isEqualTo(1);
        assertThat(magazine.charges()).isEqualTo(1);
        assertThat(magazine.holdsAny()).isTrue();
    }

    @Test
    void aLoadIntoAFullMagazineIsRefusedAndChangesNothing() {
        ArtilleryMagazine magazine = new ArtilleryMagazine(2, 0, 0);
        magazine.load();
        magazine.load();

        assertThat(magazine.full()).isTrue();
        assertThat(magazine.load()).isEqualTo(ArtilleryMagazine.Verdict.FULL);
        assertThat(magazine.shells()).isEqualTo(2);
        assertThat(magazine.charges()).isEqualTo(2);
    }

    @Test
    void unloadGivesEverythingBackAndEmptiesTheMagazine() {
        ArtilleryMagazine magazine = new ArtilleryMagazine(16, 0, 0);
        magazine.load();
        magazine.load();
        magazine.load();

        ArtilleryMagazine.Contents given = magazine.unload();

        assertThat(given).isEqualTo(new ArtilleryMagazine.Contents(3, 3));
        assertThat(magazine.shells()).isZero();
        assertThat(magazine.charges()).isZero();
        assertThat(magazine.holdsAny()).isFalse();
    }

    @Test
    void aFiredShotConsumesOneShellAndOneCharge() {
        ArtilleryMagazine magazine = new ArtilleryMagazine(16, 2, 2);

        assertThat(magazine.fire()).isEqualTo(ArtilleryMagazine.Verdict.OK);

        assertThat(magazine.shells()).isEqualTo(1);
        assertThat(magazine.charges()).isEqualTo(1);
    }

    @Test
    void aShotWithNoChargeIsRefusedAndChangesNothing() {
        ArtilleryMagazine magazine = new ArtilleryMagazine(16, 3, 0);

        assertThat(magazine.check()).isEqualTo(ArtilleryMagazine.Verdict.NO_CHARGE);
        assertThat(magazine.fire()).isEqualTo(ArtilleryMagazine.Verdict.NO_CHARGE);
        assertThat(magazine.shells()).isEqualTo(3);
        assertThat(magazine.charges()).isZero();
    }

    @Test
    void aShotWithNoShellIsRefusedAndChangesNothing() {
        ArtilleryMagazine magazine = new ArtilleryMagazine(16, 0, 3);

        assertThat(magazine.check()).isEqualTo(ArtilleryMagazine.Verdict.NO_SHELL);
        assertThat(magazine.fire()).isEqualTo(ArtilleryMagazine.Verdict.NO_SHELL);
        assertThat(magazine.shells()).isZero();
        assertThat(magazine.charges()).isEqualTo(3);
    }

    @Test
    void aShotFromAnEmptyMagazineIsEmpty() {
        ArtilleryMagazine magazine = new ArtilleryMagazine(16, 0, 0);

        assertThat(magazine.fire()).isEqualTo(ArtilleryMagazine.Verdict.EMPTY);
        assertThat(magazine.holdsAny()).isFalse();
    }

    @Test
    void aLoweredCapacityKeepsItsCountsRefusesMoreLoadsAndStillFires() {
        ArtilleryMagazine magazine = new ArtilleryMagazine(4, 10, 10);

        assertThat(magazine.full()).isTrue();
        assertThat(magazine.load()).isEqualTo(ArtilleryMagazine.Verdict.FULL);
        assertThat(magazine.fire()).isEqualTo(ArtilleryMagazine.Verdict.OK);
        assertThat(magazine.shells()).isEqualTo(9);
        assertThat(magazine.charges()).isEqualTo(9);
    }

    @Test
    void theCapacityAndTheCountsMustBeValid() {
        assertThatThrownBy(() -> new ArtilleryMagazine(0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ArtilleryMagazine(16, -1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ArtilleryMagazine(16, 0, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
