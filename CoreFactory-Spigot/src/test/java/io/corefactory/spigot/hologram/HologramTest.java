package io.corefactory.spigot.hologram;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The lines of a hologram: one entity and one text each, stacked downward. Nothing here needs a world. */
class HologramTest {

    private static Hologram hologram(List<String> text) {
        List<Integer> ids = new ArrayList<>();
        List<UUID> uuids = new ArrayList<>();
        for (int line = 0; line < text.size(); line++) {
            ids.add(1000 - line);
            uuids.add(UUID.randomUUID());
        }
        return new Hologram(ids, uuids, new Location(null, 10.5, 70.0, -3.5), text);
    }

    @Test
    void aHologramHasOneEntityAndOneTextPerLine() {
        Hologram hologram = hologram(List.of("one", "two", "three"));

        assertThat(hologram.lineCount()).isEqualTo(3);
        assertThat(hologram.entityId(0)).isEqualTo(1000);
        assertThat(hologram.entityId(2)).isEqualTo(998);
        assertThat(hologram.text(1)).isEqualTo("two");
        assertThat(hologram.lines()).containsExactly("one", "two", "three");
    }

    @Test
    void theLinesStackDownwardAQuarterBlockApartFromThePosition() {
        Hologram hologram = hologram(List.of("a", "b", "c", "d"));

        assertThat(hologram.linePosition(0).getY()).isEqualTo(70.0);
        assertThat(hologram.linePosition(1).getY()).isEqualTo(69.75);
        assertThat(hologram.linePosition(3).getY()).isEqualTo(69.25);
        assertThat(hologram.linePosition(2).getX()).isEqualTo(10.5);
        assertThat(hologram.linePosition(2).getZ()).isEqualTo(-3.5);
        assertThat(Hologram.LINE_SPACING).isEqualTo(0.25);
    }

    @Test
    void thePositionItselfIsNotMoved() {
        Hologram hologram = hologram(List.of("a", "b"));
        hologram.linePosition(1);

        assertThat(hologram.position().getY()).isEqualTo(70.0);
    }

    @Test
    void setLinesKeepsTheNumberOfLinesAndPadsOrCutsTheText() {
        Hologram hologram = hologram(List.of("a", "b", "c"));

        hologram.setLines(List.of("x"));
        assertThat(hologram.lines()).containsExactly("x", "", "");

        hologram.setLines(List.of("p", "q", "r", "s"));
        assertThat(hologram.lines()).containsExactly("p", "q", "r");
        assertThat(hologram.lineCount()).isEqualTo(3);
    }

    @Test
    void aNullLineIsEmpty() {
        Hologram hologram = hologram(Arrays.asList("a", null));

        assertThat(hologram.text(1)).isEmpty();
    }
}
