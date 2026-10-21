package io.corefactory.core.engine;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TntManagerCodecTest {

    @Test
    void directionRoundTripsThroughThePersistentDataFormat() {
        Vector direction = new Vector(0.5, -1.0, 0.25);

        Vector decoded = TntManager.decode(TntManager.encode(direction));

        assertThat(decoded).isEqualTo(direction);
    }

    @Test
    void missingOrMalformedDirectionDecodesToNull() {
        assertThat(TntManager.decode(null)).isNull();
        assertThat(TntManager.decode("1,2")).isNull();
        assertThat(TntManager.decode("a,b,c")).isNull();
    }
}
