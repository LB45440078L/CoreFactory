package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DepositFilterTest {

    private static final List<String> ORES = List.of("galena_ore", "coal_ore", "crude_petroleum_deposit");

    @Test
    void theFilterCyclesAllThenEachOreThenAllAgain() {
        String filter = DepositFilter.next(DepositFilter.ALL, ORES);
        assertThat(filter).isEqualTo("galena_ore");
        filter = DepositFilter.next(filter, ORES);
        assertThat(filter).isEqualTo("coal_ore");
        filter = DepositFilter.next(filter, ORES);
        assertThat(filter).isEqualTo("crude_petroleum_deposit");
        filter = DepositFilter.next(filter, ORES);
        assertThat(filter).isEqualTo(DepositFilter.ALL);
        filter = DepositFilter.next(filter, ORES);
        assertThat(filter).isEqualTo("galena_ore");
    }

    @Test
    void anUnknownOrNullFilterStartsTheCycle() {
        assertThat(DepositFilter.next(null, ORES)).isEqualTo("galena_ore");
        assertThat(DepositFilter.next("removed_ore", ORES)).isEqualTo("galena_ore");
    }

    @Test
    void withNoOresTheFilterStaysOnAll() {
        assertThat(DepositFilter.next(DepositFilter.ALL, List.of())).isEqualTo(DepositFilter.ALL);
    }

    @Test
    void allMatchesEveryOreAndAnIdMatchesOnlyItself() {
        assertThat(DepositFilter.matches(DepositFilter.ALL, "coal_ore")).isTrue();
        assertThat(DepositFilter.matches("coal_ore", "coal_ore")).isTrue();
        assertThat(DepositFilter.matches("coal_ore", "galena_ore")).isFalse();
    }
}
