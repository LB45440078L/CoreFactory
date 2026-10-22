package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompassTest {

    @Test
    void eachOfTheEightDirectionsReadsItsPoint() {
        assertThat(Compass.point(0, -10)).isEqualTo("N");
        assertThat(Compass.point(10, -10)).isEqualTo("NE");
        assertThat(Compass.point(10, 0)).isEqualTo("E");
        assertThat(Compass.point(10, 10)).isEqualTo("SE");
        assertThat(Compass.point(0, 10)).isEqualTo("S");
        assertThat(Compass.point(-10, 10)).isEqualTo("SW");
        assertThat(Compass.point(-10, 0)).isEqualTo("W");
        assertThat(Compass.point(-10, -10)).isEqualTo("NW");
    }

    @Test
    void aZeroOffsetIsHere() {
        assertThat(Compass.point(0, 0)).isEqualTo(Compass.HERE);
    }

    @Test
    void theNorthwardOffsetIsNegativeZ() {
        assertThat(Compass.point(3, -40)).isEqualTo("N");
        assertThat(Compass.point(3, 40)).isEqualTo("S");
    }

    @Test
    void aFarOffsetReadsTheSameAsANearOneInTheSameDirection() {
        assertThat(Compass.point(12_000, -12_000)).isEqualTo("NE");
        assertThat(Compass.point(1, -1)).isEqualTo("NE");
    }
}
