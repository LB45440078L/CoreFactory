package io.corefactory.spigot.machine;

import io.corefactory.spigot.machine.InteractionRoute.Click;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The route of a click on a machine's interaction entity. Nothing here starts a server. */
class InteractionRouteTest {

    @Test
    void aRightClickFromTheMainHandOpensTheMachine() {
        assertThat(InteractionRoute.of(Click.RIGHT, true)).isEqualTo(InteractionRoute.OPEN);
    }

    @Test
    void aLeftClickFromTheMainHandBreaksTheMachine() {
        assertThat(InteractionRoute.of(Click.LEFT, true)).isEqualTo(InteractionRoute.BREAK);
    }

    @Test
    void anOffHandClickIsIgnored() {
        assertThat(InteractionRoute.of(Click.RIGHT, false)).isEqualTo(InteractionRoute.IGNORE);
        assertThat(InteractionRoute.of(Click.LEFT, false)).isEqualTo(InteractionRoute.IGNORE);
    }

    @Test
    void anUnknownClickIsIgnored() {
        assertThat(InteractionRoute.of(null, true)).isEqualTo(InteractionRoute.IGNORE);
    }
}
