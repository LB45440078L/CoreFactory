package io.corefactory.paper.artillery;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the pages of the artillery list: six to a page, one page when there are none, and the next and back availability. */
class MachinePagesTest {

    @Test
    void sixMachinesToAPageAndAtLeastOnePage() {
        assertThat(MachinePages.PER_PAGE).isEqualTo(6);
        assertThat(MachinePages.count(0)).isEqualTo(1);
        assertThat(MachinePages.count(1)).isEqualTo(1);
        assertThat(MachinePages.count(6)).isEqualTo(1);
        assertThat(MachinePages.count(7)).isEqualTo(2);
        assertThat(MachinePages.count(12)).isEqualTo(2);
        assertThat(MachinePages.count(13)).isEqualTo(3);
    }

    @Test
    void nextIsOfferedOnlyBeforeTheLastPageAndBackOnlyAfterTheFirst() {
        assertThat(MachinePages.hasNext(0, 0)).isFalse();
        assertThat(MachinePages.hasNext(0, 6)).isFalse();
        assertThat(MachinePages.hasNext(0, 7)).isTrue();
        assertThat(MachinePages.hasNext(1, 7)).isFalse();
        assertThat(MachinePages.hasBack(0)).isFalse();
        assertThat(MachinePages.hasBack(1)).isTrue();
    }

    @Test
    void aPageAskedForIsBroughtIntoRange() {
        assertThat(MachinePages.clamp(5, 7)).isEqualTo(1);
        assertThat(MachinePages.clamp(-1, 7)).isEqualTo(0);
        assertThat(MachinePages.clamp(3, 0)).isEqualTo(0);
    }

    @Test
    void aPageHoldsItsSixMachinesInOrder() {
        List<Integer> machines = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            machines.add(i);
        }
        assertThat(MachinePages.slice(machines, 0)).containsExactly(0, 1, 2, 3, 4, 5);
        assertThat(MachinePages.slice(machines, 1)).containsExactly(6, 7, 8, 9, 10, 11);
        assertThat(MachinePages.slice(machines, 2)).containsExactly(12);
        assertThat(MachinePages.slice(machines, 5)).isEmpty();
        assertThat(MachinePages.slice(List.<Integer>of(), 0)).isEmpty();
    }
}
