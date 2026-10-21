package io.corefactory.core.turret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MagazineTest {

    @Test
    void anEmptyMagazineTakesAnyRoundTypeUpToItsCapacity() {
        Magazine empty = Magazine.empty(10);

        assertThat(empty.isEmpty()).isTrue();
        assertThat(empty.roomFor(RoundType.ARMOR_PIERCING)).isEqualTo(10);
        assertThat(empty.add(RoundType.ARMOR_PIERCING, 4)).isEqualTo(new Magazine(RoundType.ARMOR_PIERCING, 4, 10));
    }

    @Test
    void aLoadedMagazineRefusesAnotherRoundTypeUntilItIsEmptied() {
        Magazine loaded = new Magazine(RoundType.REGULAR, 3, 10);

        assertThat(loaded.roomFor(RoundType.INCENDIARY)).isZero();
        assertThat(loaded.roomFor(RoundType.REGULAR)).isEqualTo(7);
        assertThat(loaded.fire(3).orElseThrow().roomFor(RoundType.INCENDIARY)).isEqualTo(10);
    }

    @Test
    void aLoadStopsAtTheCapacity() {
        Magazine nearlyFull = new Magazine(RoundType.REGULAR, 9, 10);

        assertThat(nearlyFull.roomFor(RoundType.REGULAR)).isEqualTo(1);
        assertThatThrownBy(() -> nearlyFull.add(RoundType.REGULAR, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThat(nearlyFull.add(RoundType.REGULAR, 1).count()).isEqualTo(10);
    }

    @Test
    void aShotUsesTheRoundsAndAFullyUsedMagazineForgetsItsType() {
        Magazine three = new Magazine(RoundType.DEPLETED_URANIUM, 3, 10);

        assertThat(three.canFire(2)).isTrue();
        assertThat(three.fire(2).orElseThrow().count()).isEqualTo(1);
        Magazine last = three.fire(3).orElseThrow();
        assertThat(last.count()).isZero();
        assertThat(last.type()).isNull();
        assertThat(three.fire(4)).isEmpty();
        assertThat(three.canFire(4)).isFalse();
    }

    @Test
    void ejectingEmptiesTheMagazineButKeepsTheCapacity() {
        Magazine ejected = new Magazine(RoundType.REGULAR, 5, 12).ejectAll();

        assertThat(ejected).isEqualTo(Magazine.empty(12));
    }

    @Test
    void aMagazineWithRoundsMustNameTheirTypeAndStayWithinItsCapacity() {
        assertThatThrownBy(() -> new Magazine(null, 2, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Magazine(RoundType.REGULAR, 11, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRoundItemsAreFoundByTheirIds() {
        assertThat(RoundType.ofItem("round_armor_piercing")).contains(RoundType.ARMOR_PIERCING);
        assertThat(RoundType.ofItem("bullet_regular")).isEmpty();
        assertThat(RoundType.ofConfigKey("Depleted-Uranium")).contains(RoundType.DEPLETED_URANIUM);
    }
}
