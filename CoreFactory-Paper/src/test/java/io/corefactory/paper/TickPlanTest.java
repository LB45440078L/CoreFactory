package io.corefactory.paper;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TickPlanTest {

    private final List<String> failed = new ArrayList<>();

    private TickPlan newPlan() {
        return new TickPlan((name, error) -> failed.add(name));
    }

    @Test
    void anActionWithPeriod6000RunsOnTick6000AndNotOnTick1() {
        TickPlan plan = newPlan();
        List<Long> runs = new ArrayList<>();
        plan.add("saver", 6000, 6000, () -> runs.add(plan.currentTick()));

        for (int i = 0; i < 6000; i++) {
            plan.tick();
        }
        assertThat(runs).containsExactly(6000L);

        for (int i = 0; i < 6000; i++) {
            plan.tick();
        }
        assertThat(runs).containsExactly(6000L, 12000L);
    }

    @Test
    void aPeriodOf40RunsEvery40Ticks() {
        TickPlan plan = newPlan();
        List<Long> runs = new ArrayList<>();
        plan.add("resources", 40, 40, () -> runs.add(plan.currentTick()));

        for (int i = 0; i < 200; i++) {
            plan.tick();
        }
        assertThat(runs).containsExactly(40L, 80L, 120L, 160L, 200L);
    }

    @Test
    void actionsDueOnOneTickRunInTheOrderTheyWereAdded() {
        TickPlan plan = newPlan();
        List<String> order = new ArrayList<>();
        plan.add("oil", 1, 1, () -> order.add("oil"));
        plan.add("energy", 1, 1, () -> order.add("energy"));
        plan.add("water", 1, 1, () -> order.add("water"));
        plan.add("machines", 1, 1, () -> order.add("machines"));

        plan.tick();
        assertThat(order).containsExactly("oil", "energy", "water", "machines");

        order.clear();
        plan.tick();
        assertThat(order).containsExactly("oil", "energy", "water", "machines");
    }

    @Test
    void aSlowActionKeepsItsPlaceInTheOrderOnTheTicksItRuns() {
        TickPlan plan = newPlan();
        List<String> order = new ArrayList<>();
        plan.add("first", 1, 1, () -> order.add("first"));
        plan.add("second", 2, 2, () -> order.add("second"));
        plan.add("third", 1, 1, () -> order.add("third"));

        plan.tick();
        assertThat(order).containsExactly("first", "third");

        order.clear();
        plan.tick();
        assertThat(order).containsExactly("first", "second", "third");
    }

    @Test
    void theFirstRunCanBeDelayedAndThenRepeatsEveryPeriod() {
        TickPlan plan = newPlan();
        List<Long> runs = new ArrayList<>();
        plan.add("late", 100, 40, () -> runs.add(plan.currentTick()));

        for (int i = 0; i < 180; i++) {
            plan.tick();
        }
        assertThat(runs).containsExactly(100L, 140L, 180L);
    }

    @Test
    void aDelayOf0RunsOnTheNextTick() {
        TickPlan plan = newPlan();
        List<Long> runs = new ArrayList<>();
        plan.add("now", 0, 10, () -> runs.add(plan.currentTick()));

        for (int i = 0; i < 25; i++) {
            plan.tick();
        }
        assertThat(runs).containsExactly(1L, 11L, 21L);
    }

    @Test
    void aPeriodOf0IsRefused() {
        TickPlan plan = newPlan();

        assertThatThrownBy(() -> plan.add("bad", 0, 0, () -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> plan.add("bad", 0, -5, () -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(plan.has("bad")).isFalse();
    }

    @Test
    void aNegativeDelayIsRefused() {
        TickPlan plan = newPlan();

        assertThatThrownBy(() -> plan.add("bad", -1, 10, () -> { }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNameCanBeUsedByOneActionOnly() {
        TickPlan plan = newPlan();
        plan.add("outline", 0, 10, () -> { });

        assertThatThrownBy(() -> plan.add("outline", 0, 10, () -> { }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anActionThatThrowsDoesNotStopTheOthers() {
        TickPlan plan = newPlan();
        List<Long> after = new ArrayList<>();
        plan.add("broken", 1, 1, () -> {
            throw new IllegalStateException("boom");
        });
        plan.add("after", 1, 1, () -> after.add(plan.currentTick()));

        plan.tick();
        plan.tick();

        assertThat(after).containsExactly(1L, 2L);
        assertThat(failed).containsExactly("broken", "broken");
    }

    @Test
    void anActionAddedDuringATickFirstRunsOnALaterTick() {
        TickPlan plan = newPlan();
        List<Long> dynamic = new ArrayList<>();
        plan.add("spawner", 5, 1000, () -> plan.add("dynamic", 0, 10, () -> dynamic.add(plan.currentTick())));

        for (int i = 0; i < 30; i++) {
            plan.tick();
        }
        assertThat(dynamic).containsExactly(6L, 16L, 26L);
    }

    @Test
    void aRemovedActionDoesNotRunAgain() {
        TickPlan plan = newPlan();
        List<Long> runs = new ArrayList<>();
        plan.add("outline", 1, 1, () -> runs.add(plan.currentTick()));

        plan.tick();
        assertThat(plan.remove("outline")).isTrue();
        assertThat(plan.has("outline")).isFalse();
        plan.tick();
        plan.tick();

        assertThat(runs).containsExactly(1L);
        assertThat(plan.remove("outline")).isFalse();
    }

    @Test
    void anActionCanRemoveItselfAndTheNameCanBeUsedAgain() {
        TickPlan plan = newPlan();
        List<Long> runs = new ArrayList<>();
        plan.add("once", 1, 1, () -> {
            runs.add(plan.currentTick());
            plan.remove("once");
        });

        for (int i = 0; i < 5; i++) {
            plan.tick();
        }
        assertThat(runs).containsExactly(1L);

        plan.add("once", 1, 1, () -> runs.add(plan.currentTick()));
        plan.tick();
        assertThat(runs).containsExactly(1L, 6L);
    }
}
