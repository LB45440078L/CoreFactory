package io.corefactory.spigot.hologram;

import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.drill.DrillRules;
import io.corefactory.core.drill.DrillStatus;
import io.corefactory.core.drill.DrillTable;
import io.corefactory.core.drill.DrillText;
import io.corefactory.core.hologram.HologramText;
import io.corefactory.core.process.MachineState;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.spigot.ShippedContent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HologramJsonTest {

    @Test
    void plainTextHasNoColour() {
        assertThat(HologramJson.of("plain"))
                .isEqualTo("{\"text\":\"\",\"extra\":[{\"text\":\"plain\"}]}");
    }

    @Test
    void eachColourCodeStartsANewPartInItsColour() {
        assertThat(HologramJson.of("&aOn&8off"))
                .isEqualTo("{\"text\":\"\",\"extra\":["
                        + "{\"text\":\"On\",\"color\":\"green\"},"
                        + "{\"text\":\"off\",\"color\":\"dark_gray\"}]}");
    }

    @Test
    void resetClearsTheColour() {
        assertThat(HologramJson.of("&cRed&rplain"))
                .isEqualTo("{\"text\":\"\",\"extra\":["
                        + "{\"text\":\"Red\",\"color\":\"red\"},"
                        + "{\"text\":\"plain\"}]}");
    }

    @Test
    void emptyTextHasNoParts() {
        assertThat(HologramJson.of("")).isEqualTo("{\"text\":\"\",\"extra\":[]}");
        assertThat(HologramJson.of("&a")).isEqualTo("{\"text\":\"\",\"extra\":[]}");
    }

    @Test
    void quotesAndBackslashesAreEscaped() {
        assertThat(HologramJson.of("say \"hi\" \\ now"))
                .isEqualTo("{\"text\":\"\",\"extra\":[{\"text\":\"say \\\"hi\\\" \\\\ now\"}]}");
    }

    @Test
    void segmentsAreKeptAsTheyAre() {
        assertThat(HologramJson.of("&a■■ 50%"))
                .isEqualTo("{\"text\":\"\",\"extra\":[{\"text\":\"■■ 50%\",\"color\":\"green\"}]}");
    }

    @Test
    void ampersandNotFollowedByAColourIsKept() {
        assertThat(HologramJson.of("Tom & Jerry &z"))
                .isEqualTo("{\"text\":\"\",\"extra\":[{\"text\":\"Tom & Jerry &z\"}]}");
    }

    @Test
    void everyLineOfALabToolAndADrillBecomesPlainJsonWithNoCodeLeft() {
        MachineState state = new MachineState("crusher", List.of(), List.of(), List.of());
        state.start("crush_uraninite");
        state.restore("crush_uraninite", 40);
        List<List<String>> hologramLines = List.of(
                HologramText.forMachine("&7Crusher", state, 100),
                HologramText.drillLines(DrillKind.PETROLEUM, DrillTable.level(2),
                        DrillRules.petroleumPower(0, List.of(9), true), DrillStatus.RUNNING, 0.4,
                        DrillText.deposit("&7Crude Petroleum Deposit", null), 12, 576, 3),
                HologramText.drillLines(DrillKind.ORE, DrillTable.level(4), DrillRules.orePower(true),
                        DrillStatus.NO_TARGET, 0.0, "&7Deposit &cnone in range", 0, 576, 0));
        for (List<String> lines : hologramLines) {
            for (String line : lines) {
                assertThat(HologramJson.of(line)).as(line).startsWith("{\"text\":\"\",\"extra\":[").doesNotContain("&");
            }
        }
    }

    @Test
    void everyShippedProcessNamesItsLineInThirtyTwoCharacters() {
        for (ProcessDefinition process : ShippedContent.load().processes().processes()) {
            MachineState state = new MachineState(process.machine(), List.of(), List.of(), List.of());
            state.start(process.id());
            String line = HologramText.forMachine("&7Machine", state, process.ticks()).get(0);
            assertThat(line.replaceAll("&[0-9a-fk-or]", "").length()).as(process.id()).isLessThanOrEqualTo(32);
        }
    }
}
