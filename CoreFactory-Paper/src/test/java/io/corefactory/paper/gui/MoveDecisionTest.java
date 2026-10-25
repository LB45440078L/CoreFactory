package io.corefactory.paper.gui;

import io.corefactory.paper.gui.MoveDecision.Holder;
import io.corefactory.paper.gui.MoveDecision.Slot;
import io.corefactory.paper.gui.MoveDecision.Verdict;
import org.junit.jupiter.api.Test;

import static io.corefactory.paper.gui.MoveDecision.decide;
import static org.assertj.core.api.Assertions.assertThat;

class MoveDecisionTest {

    @Test
    void aSplitterListSlotTakesAnItemThatConverts() {
        assertThat(decide(Holder.SPLITTER_PANEL, Slot.SPLITTER_LIST, true)).isEqualTo(Verdict.ALLOW);
    }

    @Test
    void aSplitterListSlotRefusesAnItemThatDoesNotConvert() {
        assertThat(decide(Holder.SPLITTER_PANEL, Slot.SPLITTER_LIST, false)).isEqualTo(Verdict.DENY);
    }

    @Test
    void aSplitterSlotOutsideTheListsRefuses() {
        assertThat(decide(Holder.SPLITTER_PANEL, Slot.SPLITTER_OTHER, true)).isEqualTo(Verdict.DENY);
        assertThat(decide(Holder.SPLITTER_PANEL, Slot.SPLITTER_OTHER, false)).isEqualTo(Verdict.DENY);
    }

    @Test
    void aSeparatorInputTakesAnAcceptedItem() {
        assertThat(decide(Holder.MACHINE, Slot.INPUT, true)).isEqualTo(Verdict.ALLOW);
    }

    @Test
    void aSeparatorInputRefusesAnItemNoProcessAccepts() {
        assertThat(decide(Holder.MACHINE, Slot.INPUT, false)).isEqualTo(Verdict.DENY);
    }

    @Test
    void aSeparatorOutputRefusesEvenAnAcceptedItem() {
        assertThat(decide(Holder.MACHINE, Slot.OUTPUT, true)).isEqualTo(Verdict.DENY);
        assertThat(decide(Holder.MACHINE, Slot.OUTPUT, false)).isEqualTo(Verdict.DENY);
    }

    @Test
    void aMachineFuelSlotFollowsTheInputRule() {
        assertThat(decide(Holder.MACHINE, Slot.FUEL, true)).isEqualTo(Verdict.ALLOW);
        assertThat(decide(Holder.MACHINE, Slot.FUEL, false)).isEqualTo(Verdict.DENY);
    }

    @Test
    void anyOtherMachineSlotRefuses() {
        assertThat(decide(Holder.MACHINE, Slot.OTHER, true)).isEqualTo(Verdict.DENY);
        assertThat(decide(Holder.MACHINE, Slot.OTHER, false)).isEqualTo(Verdict.DENY);
    }

    @Test
    void theCatalogueRefusesEverySlot() {
        for (Slot slot : Slot.values()) {
            assertThat(decide(Holder.OTHER, slot, true)).isEqualTo(Verdict.DENY);
            assertThat(decide(Holder.OTHER, slot, false)).isEqualTo(Verdict.DENY);
        }
    }

    @Test
    void aSplitterPanelTakesNothingButItsLists() {
        for (Slot slot : Slot.values()) {
            if (slot == Slot.SPLITTER_LIST) {
                continue;
            }
            assertThat(decide(Holder.SPLITTER_PANEL, slot, true)).isEqualTo(Verdict.DENY);
        }
    }

    @Test
    void aMachineTakesNothingInItsListSlotsOrItsPanel() {
        assertThat(decide(Holder.MACHINE, Slot.SPLITTER_LIST, true)).isEqualTo(Verdict.DENY);
        assertThat(decide(Holder.MACHINE, Slot.SPLITTER_OTHER, true)).isEqualTo(Verdict.DENY);
    }
}
