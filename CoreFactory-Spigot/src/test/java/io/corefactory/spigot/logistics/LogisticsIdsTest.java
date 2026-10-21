package io.corefactory.spigot.logistics;

import io.corefactory.core.logistics.LogisticsLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogisticsIdsTest {

    @Test
    void theComponentIdNamesTheKindAndTheLevel() {
        assertThat(LogisticsIds.componentId(LogisticsIds.Kind.BELT, LogisticsLevel.I)).isEqualTo("belt_1");
        assertThat(LogisticsIds.componentId(LogisticsIds.Kind.PICKER, LogisticsLevel.II)).isEqualTo("picker_2");
        assertThat(LogisticsIds.componentId(LogisticsIds.Kind.SPLITTER, LogisticsLevel.III)).isEqualTo("splitter_3");
    }

    @Test
    void aLogisticsIdReadsBackItsKindAndLevel() {
        assertThat(LogisticsIds.of("belt_2")).contains(new LogisticsIds.Device(LogisticsIds.Kind.BELT, LogisticsLevel.II));
        assertThat(LogisticsIds.of("picker_3")).contains(new LogisticsIds.Device(LogisticsIds.Kind.PICKER, LogisticsLevel.III));
        assertThat(LogisticsIds.of("splitter_1")).contains(new LogisticsIds.Device(LogisticsIds.Kind.SPLITTER, LogisticsLevel.I));
    }

    @Test
    void anotherIdIsNotALogisticsDevice() {
        assertThat(LogisticsIds.of("belt_4")).isEmpty();
        assertThat(LogisticsIds.of("belt")).isEmpty();
        assertThat(LogisticsIds.of("belt_12")).isEmpty();
        assertThat(LogisticsIds.of("pylon_1")).isEmpty();
        assertThat(LogisticsIds.of(null)).isEmpty();
    }
}
