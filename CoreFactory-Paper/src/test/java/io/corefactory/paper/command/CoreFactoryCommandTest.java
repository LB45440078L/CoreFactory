package io.corefactory.paper.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreFactoryCommandTest {

    @Test
    void aWholeAmountIsKept() {
        assertThat(CoreFactoryCommand.clampAmount(5)).isEqualTo(5);
        assertThat(CoreFactoryCommand.clampAmount(64)).isEqualTo(64);
    }

    @Test
    void anAmountAboveAStackIsCappedAtSixtyFour() {
        assertThat(CoreFactoryCommand.clampAmount(999)).isEqualTo(64);
    }

    @Test
    void zeroOrNegativeGivesOne() {
        assertThat(CoreFactoryCommand.clampAmount(0)).isEqualTo(1);
        assertThat(CoreFactoryCommand.clampAmount(-3)).isEqualTo(1);
    }
}
