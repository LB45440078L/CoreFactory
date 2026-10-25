package io.corefactory.paper.hologram;

import io.corefactory.core.hologram.HologramLayout;
import io.corefactory.core.hologram.Tone;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The lines of a hologram. One text display shows them all, so the hologram tells its service about a change only when the text differs.
 * Nothing here needs a world.
 */
class HologramTest {

    private static Hologram hologram(List<String> text, AtomicInteger changes) {
        return new Hologram(new Location(null, 10.5, 70.0, -3.5), text, false, changes::incrementAndGet);
    }

    @Test
    void aHologramKeepsItsLinesTopFirst() {
        Hologram hologram = hologram(List.of("one", "two", "three"), new AtomicInteger());

        assertThat(hologram.lineCount()).isEqualTo(3);
        assertThat(hologram.text(1)).isEqualTo("two");
        assertThat(hologram.lines()).containsExactly("one", "two", "three");
    }

    @Test
    void theCentreSitsHalfTheStackAboveTheBottomLine() {
        Hologram hologram = hologram(List.of("a", "b", "c", "d"), new AtomicInteger());

        assertThat(hologram.position().getY()).isEqualTo(70.0);
        assertThat(hologram.centre().getY()).isEqualTo(70.0 + 3 * HologramLayout.LINE_SPACING / 2);
        assertThat(hologram.centre().getX()).isEqualTo(10.5);
        assertThat(hologram.centre().getZ()).isEqualTo(-3.5);
    }

    @Test
    void setLinesKeepsTheNumberOfLinesAndPadsOrCutsTheText() {
        Hologram hologram = hologram(List.of("a", "b", "c"), new AtomicInteger());

        hologram.setLines(List.of("x"));
        assertThat(hologram.lines()).containsExactly("x", "", "");

        hologram.setLines(List.of("p", "q", "r", "s"));
        assertThat(hologram.lines()).containsExactly("p", "q", "r");
    }

    @Test
    void theServiceIsToldOnlyWhenTheTextChanges() {
        AtomicInteger changes = new AtomicInteger();
        Hologram hologram = hologram(List.of("a", "b"), changes);

        hologram.setLines(List.of("a", "b"));
        assertThat(changes.get()).isEqualTo(1);

        hologram.setShown(hologram.joined());
        hologram.setLines(List.of("a", "b"));
        assertThat(changes.get()).isEqualTo(1);

        hologram.setLines(List.of("a", "c"));
        assertThat(changes.get()).isEqualTo(2);
    }

    @Test
    void theServiceIsToldWhenTheToneChanges() {
        AtomicInteger changes = new AtomicInteger();
        Hologram hologram = hologram(List.of("a"), changes);

        hologram.setTone(Tone.NEUTRAL);
        assertThat(changes.get()).isZero();

        hologram.setTone(Tone.STUCK);
        assertThat(hologram.tone()).isEqualTo(Tone.STUCK);
        assertThat(changes.get()).isEqualTo(1);
    }
}
