package io.corefactory.spigot.energy;

import io.corefactory.core.energy.GeneratorKind;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorLookTest {

    @Test
    void aCharcoalGeneratorSmokesOnlyWhileItBurns() {
        assertThat(GeneratorLook.puffFor(GeneratorKind.CHARCOAL, true, true))
                .contains(new GeneratorLook.Puff(Particle.SMOKE, 3));
        assertThat(GeneratorLook.puffFor(GeneratorKind.CHARCOAL, false, false)).isEmpty();
    }

    @Test
    void aPetroleumOrDieselGeneratorGivesSparseCosySmokeWhileItBurns() {
        assertThat(GeneratorLook.puffFor(GeneratorKind.PETROLEUM, true, true))
                .contains(new GeneratorLook.Puff(Particle.CAMPFIRE_COSY_SMOKE, 1));
        assertThat(GeneratorLook.puffFor(GeneratorKind.DIESEL, true, true))
                .contains(new GeneratorLook.Puff(Particle.CAMPFIRE_COSY_SMOKE, 1));
        assertThat(GeneratorLook.puffFor(GeneratorKind.DIESEL, false, false)).isEmpty();
    }

    @Test
    void aNuclearGeneratorSparklesWhileItRuns() {
        assertThat(GeneratorLook.puffFor(GeneratorKind.NUCLEAR, false, true))
                .contains(new GeneratorLook.Puff(Particle.END_ROD, 1));
        assertThat(GeneratorLook.puffFor(GeneratorKind.NUCLEAR, false, false)).isEmpty();
    }

    @Test
    void windAndSolarGiveNoParticles() {
        assertThat(GeneratorLook.puffFor(GeneratorKind.WIND, true, true)).isEmpty();
        assertThat(GeneratorLook.puffFor(GeneratorKind.SOLAR, true, true)).isEmpty();
    }

    @Test
    void theViewIsFortyEightBlocksAndThePuffsComeEveryTenTicks() {
        assertThat(GeneratorLook.VIEW_BLOCKS).isEqualTo(48.0);
        assertThat(GeneratorLook.PERIOD_TICKS).isEqualTo(10);
    }
}
