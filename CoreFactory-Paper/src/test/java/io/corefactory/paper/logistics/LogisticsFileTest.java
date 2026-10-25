package io.corefactory.paper.logistics;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.LogisticsLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LogisticsFileTest {

    private static final UUID WORLD = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final UUID OWNER = UUID.fromString("5b1f3c2e-8a7d-4e6b-9c10-2d4f6a8b0c12");

    private static LogisticsFile.State sample() {
        List<LogisticsFile.Belt> belts = List.of(
                new LogisticsFile.Belt(WORLD, new Pos(10, 64, -3), Dir.EAST, LogisticsLevel.I, OWNER),
                new LogisticsFile.Belt(WORLD, new Pos(11, 64, -3), Dir.SOUTH, LogisticsLevel.III));
        List<LogisticsFile.Picker> pickers = List.of(
                new LogisticsFile.Picker("picker-a", WORLD, new Pos(9, 64, -3), Dir.WEST, LogisticsLevel.II, OWNER));
        List<LogisticsFile.Splitter> splitters = List.of(
                new LogisticsFile.Splitter("split-a", WORLD, new Pos(12, 64, -3), Dir.EAST, LogisticsLevel.I,
                        List.of(Item.material("COAL"), Item.component("petroleum")), List.of(Item.material("IRON_INGOT")),
                        Item.component("belt_1"), OWNER),
                new LogisticsFile.Splitter("split-b", WORLD, new Pos(12, 64, 5), Dir.NORTH, LogisticsLevel.III,
                        List.of(), List.of(), null));
        List<LogisticsFile.Carried> items = List.of(
                new LogisticsFile.Carried(WORLD, new Pos(10, 64, -3), Item.material("COAL"), 0.375),
                new LogisticsFile.Carried(WORLD, new Pos(10, 64, -3), Item.component("petroleum"), 0.75),
                new LogisticsFile.Carried(WORLD, new Pos(11, 64, -3), Item.material("IRON_INGOT"), 1.0));
        return new LogisticsFile.State(belts, pickers, splitters, items);
    }

    @Test
    void aStateWithItemsOnBeltsSurvivesAWriteAndARead() {
        LogisticsFile.State state = sample();
        String text = LogisticsFile.write(state);
        List<String> warnings = new ArrayList<>();
        LogisticsFile.State read = LogisticsFile.read(text, warnings::add);
        assertThat(warnings).isEmpty();
        assertThat(read).isEqualTo(state);
    }

    @Test
    void theItemsOnABeltKeepTheirPositionsAndKinds() {
        LogisticsFile.State read = LogisticsFile.read(LogisticsFile.write(sample()), message -> {
        });
        assertThat(read.items()).hasSize(3);
        assertThat(read.items().get(0).position()).isEqualTo(0.375);
        assertThat(read.items().get(1).item()).isEqualTo(Item.component("petroleum"));
        assertThat(read.items().get(2).pos()).isEqualTo(new Pos(11, 64, -3));
    }

    @Test
    void aSplitterKeepsItsFilterAndItsHeldItem() {
        LogisticsFile.State read = LogisticsFile.read(LogisticsFile.write(sample()), message -> {
        });
        LogisticsFile.Splitter first = read.splitters().get(0);
        assertThat(first.left()).containsExactly(Item.material("COAL"), Item.component("petroleum"));
        assertThat(first.right()).containsExactly(Item.material("IRON_INGOT"));
        assertThat(first.held()).isEqualTo(Item.component("belt_1"));
        assertThat(read.splitters().get(1).held()).isNull();
    }

    @Test
    void anOwnerIsKeptForEachBeltPickerAndSplitter() {
        LogisticsFile.State read = LogisticsFile.read(LogisticsFile.write(sample()), message -> {
        });
        assertThat(read.belts().get(0).owner()).isEqualTo(OWNER);
        assertThat(read.pickers().get(0).owner()).isEqualTo(OWNER);
        assertThat(read.splitters().get(0).owner()).isEqualTo(OWNER);
    }

    @Test
    void aDeviceWithNoOwnerWritesNoOwnerKeyAndReadsWithNoOwner() {
        LogisticsFile.State state = new LogisticsFile.State(
                List.of(new LogisticsFile.Belt(WORLD, new Pos(1, 64, 1), Dir.EAST, LogisticsLevel.I)), List.of(), List.of(), List.of());
        String text = LogisticsFile.write(state);
        assertThat(text).doesNotContain("owner");
        assertThat(LogisticsFile.read(text, message -> {
        }).belts().get(0).owner()).isNull();
    }

    @Test
    void anEntrySavedBeforeOwnersWereKeptReadsWithNoOwner() {
        String text = """
                belts:
                  0:
                    world: %s
                    x: 1
                    y: 64
                    z: 1
                    flow: WEST
                    level: I
                """.formatted(WORLD);
        List<String> warnings = new ArrayList<>();
        LogisticsFile.State read = LogisticsFile.read(text, warnings::add);
        assertThat(warnings).isEmpty();
        assertThat(read.belts()).containsExactly(new LogisticsFile.Belt(WORLD, new Pos(1, 64, 1), Dir.WEST, LogisticsLevel.I));
        assertThat(read.belts().get(0).owner()).isNull();
    }

    @Test
    void anOwnerThatIsNotAnIdIsReadAsNoOwnerAndReported() {
        String text = """
                pickers:
                  0:
                    id: picker-a
                    world: %s
                    x: 9
                    y: 64
                    z: -3
                    in: WEST
                    level: II
                    owner: not-an-id
                """.formatted(WORLD);
        List<String> warnings = new ArrayList<>();
        LogisticsFile.State read = LogisticsFile.read(text, warnings::add);
        assertThat(read.pickers()).hasSize(1);
        assertThat(read.pickers().get(0).owner()).isNull();
        assertThat(warnings).hasSize(1);
    }

    @Test
    void anEmptyStateWritesAndReadsAsEmpty() {
        LogisticsFile.State read = LogisticsFile.read(LogisticsFile.write(LogisticsFile.State.empty()), message -> {
        });
        assertThat(read).isEqualTo(LogisticsFile.State.empty());
    }

    @Test
    void aBeltDuplexKeepsItsLevelDirectionHeldItemAndOwner() {
        LogisticsFile.State state = new LogisticsFile.State(
                List.of(), List.of(), List.of(),
                List.of(new LogisticsFile.Duplex("duplex-a", WORLD, new Pos(20, 64, 4), Dir.SOUTH, LogisticsLevel.II,
                        Item.component("belt_1"), OWNER),
                        new LogisticsFile.Duplex("duplex-b", WORLD, new Pos(21, 64, 4), Dir.WEST, LogisticsLevel.III, null, null)),
                List.of());
        String text = LogisticsFile.write(state);
        List<String> warnings = new ArrayList<>();
        LogisticsFile.State read = LogisticsFile.read(text, warnings::add);
        assertThat(warnings).isEmpty();
        assertThat(read).isEqualTo(state);
        LogisticsFile.Duplex first = read.duplexes().get(0);
        assertThat(first.flow()).isEqualTo(Dir.SOUTH);
        assertThat(first.level()).isEqualTo(LogisticsLevel.II);
        assertThat(first.held()).isEqualTo(Item.component("belt_1"));
        assertThat(first.owner()).isEqualTo(OWNER);
        assertThat(read.duplexes().get(1).held()).isNull();
    }

    @Test
    void aBeltDuplexEntryWithAnUnknownFlowIsSkippedAndReported() {
        String text = """
                duplexes:
                  0:
                    id: duplex-a
                    world: %s
                    x: 20
                    y: 64
                    z: 4
                    flow: SIDEWAYS
                    level: I
                  1:
                    id: duplex-b
                    world: %s
                    x: 30
                    y: 64
                    z: 4
                    flow: EAST
                    level: I
                """.formatted(WORLD, WORLD);
        List<String> warnings = new ArrayList<>();
        LogisticsFile.State read = LogisticsFile.read(text, warnings::add);
        assertThat(read.duplexes()).containsExactly(new LogisticsFile.Duplex("duplex-b", WORLD, new Pos(30, 64, 4), Dir.EAST,
                LogisticsLevel.I, null, null));
        assertThat(warnings).hasSize(1);
    }

    @Test
    void anEntryThatIsNotValidIsSkippedAndReported() {
        String text = """
                belts:
                  0:
                    world: %s
                    x: 1
                    y: 64
                    z: 1
                    flow: SIDEWAYS
                    level: I
                  1:
                    world: %s
                    x: 2
                    y: 64
                    z: 1
                    flow: WEST
                    level: II
                items:
                  0:
                    world: %s
                    x: 2
                    y: 64
                    z: 1
                    item: "MATERIAL:"
                    position: 0.5
                """.formatted(WORLD, WORLD, WORLD);
        List<String> warnings = new ArrayList<>();
        LogisticsFile.State read = LogisticsFile.read(text, warnings::add);
        assertThat(read.belts()).containsExactly(new LogisticsFile.Belt(WORLD, new Pos(2, 64, 1), Dir.WEST, LogisticsLevel.II));
        assertThat(read.items()).isEmpty();
        assertThat(warnings).hasSize(2);
    }
}
