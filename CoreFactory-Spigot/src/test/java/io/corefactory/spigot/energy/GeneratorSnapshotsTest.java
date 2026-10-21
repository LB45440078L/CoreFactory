package io.corefactory.spigot.energy;

import io.corefactory.core.energy.Generator;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.GeneratorState;
import io.corefactory.core.energy.Pos;
import io.corefactory.spigot.energy.StoredEnergy.StoredGenerator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratorSnapshotsTest {

    private static final Pos POS = new Pos(4, 70, -2);

    @Test
    void aFuelKindKeepsItsCountAndItsBurningTime() {
        Generator core = GeneratorSnapshots.core("g", POS, GeneratorKind.DIESEL, 12, 500);
        assertThat(core.id()).isEqualTo("g");
        assertThat(core.pos()).isEqualTo(POS);
        assertThat(core.kind()).isEqualTo(GeneratorKind.DIESEL);
        assertThat(core.state()).isEqualTo(new GeneratorState(500, 12));
    }

    @Test
    void aSavedFuelCountAboveTheCapIsCappedAtSixtyFour() {
        assertThat(GeneratorSnapshots.core("g", POS, GeneratorKind.CHARCOAL, 900, 0).state().fuelItems()).isEqualTo(64);
        assertThat(GeneratorSnapshots.core("g", POS, GeneratorKind.CHARCOAL, -4, -1).state()).isEqualTo(GeneratorState.EMPTY);
    }

    @Test
    void windAndSolarStartEmptyWhateverWasSaved() {
        assertThat(GeneratorSnapshots.core("w", POS, GeneratorKind.WIND, 9, 9).state()).isEqualTo(GeneratorState.EMPTY);
        assertThat(GeneratorSnapshots.core("s", POS, GeneratorKind.SOLAR, 9, 9).state()).isEqualTo(GeneratorState.EMPTY);
    }

    @Test
    void aStoredGeneratorMapsToItsCoreGenerator() {
        StoredGenerator stored = new StoredGenerator("n", UUID.fromString("00000000-0000-0000-0000-000000000001"), POS,
                GeneratorKind.NUCLEAR, 3, 40, 1234.5);
        Generator core = GeneratorSnapshots.core(stored);
        assertThat(core).isEqualTo(new Generator("n", POS, GeneratorKind.NUCLEAR, new GeneratorState(40, 3)));
    }
}
