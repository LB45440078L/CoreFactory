package io.corefactory.paper.turret;

import io.corefactory.core.railgun.RailgunCycle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The lines of a rail gun's screen: its power, its satisfaction, its charge bar, and its switch with what it is doing. */
class RailgunScreenTest {

    @Test
    void theScreenShowsFiveLinesTopFirst() {
        List<String> lines = RailgunScreen.lines(new RailgunScreen.Readout(1.2e6, 2e6, 1.0, 0.5, RailgunCycle.Phase.CHARGING, true));

        assertThat(lines).hasSize(5);
        assertThat(lines.get(0)).contains("RAIL GUN");
        assertThat(lines.get(1)).isEqualTo("Power <value>1.20 MW</value> <muted>of 2.00 MW</muted>");
        assertThat(lines.get(2)).isEqualTo("Satisfied <good>100%</good>");
        assertThat(lines.get(3)).isEqualTo("Charge <good>|||||</good><muted>.....</muted> <value>50%</value>");
        assertThat(lines.get(4)).isEqualTo("Switch <good>ON</good> <muted>charging</muted>");
    }

    @Test
    void theChargeBarFillsWithTheShareOfTheCharge() {
        assertThat(RailgunScreen.charge(0.0, true)).isEqualTo("Charge <good></good><muted>..........</muted> <value>0%</value>");
        assertThat(RailgunScreen.charge(1.0, true)).isEqualTo("Charge <good>||||||||||</good><muted></muted> <value>100%</value>");
        assertThat(RailgunScreen.charge(0.34, true)).isEqualTo("Charge <good>|||</good><muted>.......</muted> <value>34%</value>");
    }

    @Test
    void aShareOutOfRangeOrNotANumberIsClampedForTheBar() {
        assertThat(RailgunScreen.charge(3.0, true)).contains("<value>100%</value>");
        assertThat(RailgunScreen.charge(-1.0, true)).contains("<value>0%</value>");
        assertThat(RailgunScreen.charge(Double.NaN, true)).contains("<value>0%</value>");
    }

    @Test
    void aSwitchedOffRailGunShowsOffAndNoCharge() {
        assertThat(RailgunScreen.state(RailgunCycle.Phase.IDLE, false)).isEqualTo("Switch <bad>OFF</bad>");
        assertThat(RailgunScreen.charge(0.8, false)).isEqualTo("Charge <muted>off</muted>");
    }

    @Test
    void theSwitchNamesWhatTheCycleIsDoing() {
        assertThat(RailgunScreen.state(RailgunCycle.Phase.IDLE, true)).isEqualTo("Switch <good>ON</good> <muted>idle</muted>");
        assertThat(RailgunScreen.state(RailgunCycle.Phase.CHARGING, true)).isEqualTo("Switch <good>ON</good> <muted>charging</muted>");
        assertThat(RailgunScreen.state(RailgunCycle.Phase.RELOADING, true)).isEqualTo("Switch <good>ON</good> <muted>reloading</muted>");
    }

    @Test
    void theSatisfactionIsGoodOnlyWhenItIsWhole() {
        assertThat(RailgunScreen.satisfied(0.5)).isEqualTo("Satisfied <warn>50%</warn>");
        assertThat(RailgunScreen.satisfied(0.9999)).isEqualTo("Satisfied <good>100%</good>");
        assertThat(RailgunScreen.satisfied(Double.NaN)).isEqualTo("Satisfied <bad>no grid</bad>");
    }

    @Test
    void thePowerLineShowsTheWattsUsedAndAskedFor() {
        assertThat(RailgunScreen.power(0.0, 1.0)).isEqualTo("Power <value>0 W</value> <muted>of 1 W</muted>");
        assertThat(RailgunScreen.power(2e6, 2e6)).isEqualTo("Power <value>2.00 MW</value> <muted>of 2.00 MW</muted>");
    }
}
