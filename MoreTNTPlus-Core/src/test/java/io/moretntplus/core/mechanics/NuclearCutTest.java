package io.moretntplus.core.mechanics;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NuclearCutTest {

    @Test
    void saucerIsDeepestAtTheCentreAndLevelAtTheRim() {
        NuclearMechanic.Cut saucer = new NuclearMechanic.Cut(NuclearMechanic.Shape.SAUCER, 30, 50, 6.0, 0.0);

        assertThat(saucer.depthAt(0)).isEqualTo(6.0);
        assertThat(saucer.depthAt(15)).isBetween(4.0, 5.0);
        assertThat(saucer.depthAt(30)).isZero();
        assertThat(saucer.depthAt(45)).isZero();
    }

    @Test
    void sphereIsAHemisphereUnderTheSurface() {
        NuclearMechanic.Cut sphere = new NuclearMechanic.Cut(NuclearMechanic.Shape.SPHERE, 60, 100, 0.0, 6.0);

        assertThat(sphere.depthAt(0)).isEqualTo(60.0);
        assertThat(sphere.depthAt(60)).isZero();
        // Inside the sphere the wall is the circle x^2 + d^2 = r^2.
        assertThat(sphere.depthAt(36)).isEqualTo(48.0);
    }

    @Test
    void sphereRimSinksAndFadesOutAtTheReach() {
        NuclearMechanic.Cut sphere = new NuclearMechanic.Cut(NuclearMechanic.Shape.SPHERE, 60, 100, 0.0, 6.0);

        assertThat(sphere.depthAt(60.5)).isCloseTo(6.0, Offset.offset(0.2));
        assertThat(sphere.depthAt(80)).isBetween(1.0, 2.0);
        assertThat(sphere.depthAt(100)).isZero();
    }

    @Test
    void shapeNamesFallBackToSaucer() {
        assertThat(NuclearMechanic.Shape.of("sphere")).isEqualTo(NuclearMechanic.Shape.SPHERE);
        assertThat(NuclearMechanic.Shape.of("SPHERE")).isEqualTo(NuclearMechanic.Shape.SPHERE);
        assertThat(NuclearMechanic.Shape.of("saucer")).isEqualTo(NuclearMechanic.Shape.SAUCER);
        assertThat(NuclearMechanic.Shape.of("bogus")).isEqualTo(NuclearMechanic.Shape.SAUCER);
    }
}
