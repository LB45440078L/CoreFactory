package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.FiringVerdict;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the screen's lines for given values, the power's unit, and that every refusal has its words on the screen. */
class CommandCenterScreenTest {

    private static final String HEADER = "<gradient:#FFB02E:#FFE070><b>ARTILLERY COMMAND</b></gradient>";

    @Test
    void theLinesOfAReadySettingShowEveryValue() {
        CommandCenterScreen.Readout readout = new CommandCenterScreen.Readout(1250.4, -340.6, "High explosive", "II", 45.0, 3,
                1_270_000.0 * 2, 0.8512, "OK", true);

        assertThat(CommandCenterScreen.lines(readout)).containsExactly(
                HEADER,
                "Impact <value>X 1250   Z -341</value>",
                "Shell <value>High explosive</value> <muted>tier II</muted>",
                "Elevation <value>45</value> deg",
                "Artillery <value>3</value> connected",
                "Needs <value>2.54 MW</value>",
                "Satisfied <warn>85%</warn>",
                "Safety <good>OK</good>");
    }

    @Test
    void anEmptySettingShowsWhatIsMissing() {
        CommandCenterScreen.Readout readout = new CommandCenterScreen.Readout(Double.NaN, Double.NaN, null, "I", 45.0, 0, 0.0,
                Double.NaN, "no shell chosen", false);

        assertThat(CommandCenterScreen.lines(readout)).containsExactly(
                HEADER,
                "Impact <hint>not set</hint>",
                "Shell <hint>none chosen</hint>",
                "Elevation <value>45</value> deg",
                "Artillery <bad>none connected</bad>",
                "Needs <value>0 W</value>",
                "Satisfied <bad>no grid</bad>",
                "Safety <bad>no shell chosen</bad>");
    }

    @Test
    void aFullGridIsGoodAndAPassingCheckIsGreen() {
        CommandCenterScreen.Readout readout = new CommandCenterScreen.Readout(0, 0, "Napalm", "I", 45.0, 1, 100_000.0, 1.0, "OK",
                true);

        List<String> lines = CommandCenterScreen.lines(readout);
        assertThat(lines.get(6)).isEqualTo("Satisfied <good>100%</good>");
        assertThat(lines.get(7)).isEqualTo("Safety <good>OK</good>");
    }

    @Test
    void thePowerIsShownInItsLargestUnitUnderAThousand() {
        assertThat(CommandCenterScreen.power(950)).isEqualTo("950 W");
        assertThat(CommandCenterScreen.power(100_000)).isEqualTo("100.0 kW");
        assertThat(CommandCenterScreen.power(1_270_000)).isEqualTo("1.27 MW");
        assertThat(CommandCenterScreen.power(1.5e9)).isEqualTo("1.50 GW");
    }

    @Test
    void everyVerdictHasItsWordsAndOnlyOkReadsOk() {
        for (FiringVerdict verdict : FiringVerdict.values()) {
            String words = CommandCenterScreen.reason(verdict);
            assertThat(words).isNotBlank();
            assertThat(words.equals("OK")).isEqualTo(verdict == FiringVerdict.OK);
        }
    }
}
