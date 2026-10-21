package io.corefactory.spigot.logistics;

import io.corefactory.spigot.logistics.DisplayBudget.Take;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayBudgetTest {

    @Test
    void displaysAreTakenUpToTheCap() {
        DisplayBudget budget = new DisplayBudget(2);
        assertThat(budget.take()).isEqualTo(Take.TAKEN);
        assertThat(budget.take()).isEqualTo(Take.TAKEN);
        assertThat(budget.used()).isEqualTo(2);
    }

    @Test
    void thePastTheCapWarnsOnceUntilARoomIsGivenBack() {
        DisplayBudget budget = new DisplayBudget(1);
        assertThat(budget.take()).isEqualTo(Take.TAKEN);
        assertThat(budget.take()).isEqualTo(Take.REFUSED_WARN);
        assertThat(budget.take()).isEqualTo(Take.REFUSED);
        assertThat(budget.take()).isEqualTo(Take.REFUSED);
        budget.giveBack();
        assertThat(budget.take()).isEqualTo(Take.TAKEN);
        assertThat(budget.take()).isEqualTo(Take.REFUSED_WARN);
    }

    @Test
    void aCapOfZeroShowsNothingAndWarnsOnce() {
        DisplayBudget budget = new DisplayBudget(0);
        assertThat(budget.take()).isEqualTo(Take.REFUSED_WARN);
        assertThat(budget.take()).isEqualTo(Take.REFUSED);
        assertThat(budget.used()).isZero();
    }

    @Test
    void giveBackNeverGoesBelowZero() {
        DisplayBudget budget = new DisplayBudget(3);
        budget.giveBack();
        assertThat(budget.used()).isZero();
        assertThat(budget.cap()).isEqualTo(3);
    }

    @Test
    void aNegativeCapIsRefused() {
        assertThatThrownBy(() -> new DisplayBudget(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
