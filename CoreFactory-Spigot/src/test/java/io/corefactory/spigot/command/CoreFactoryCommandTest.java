package io.corefactory.spigot.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreFactoryCommandTest {

    @Test
    void aWholeAmountIsKept() {
        assertThat(CoreFactoryCommand.amountArgument("5")).isEqualTo(5);
        assertThat(CoreFactoryCommand.amountArgument("64")).isEqualTo(64);
    }

    @Test
    void anAmountAboveAStackIsCappedAtSixtyFour() {
        assertThat(CoreFactoryCommand.amountArgument("999")).isEqualTo(64);
    }

    @Test
    void zeroNegativeOrNotANumberGivesOne() {
        assertThat(CoreFactoryCommand.amountArgument("0")).isEqualTo(1);
        assertThat(CoreFactoryCommand.amountArgument("-3")).isEqualTo(1);
        assertThat(CoreFactoryCommand.amountArgument("many")).isEqualTo(1);
        assertThat(CoreFactoryCommand.amountArgument("")).isEqualTo(1);
    }
}
