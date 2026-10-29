package io.corefactory.paper.logistics;

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
        assertThat(LogisticsIds.of("picker_4")).contains(new LogisticsIds.Device(LogisticsIds.Kind.PICKER, LogisticsLevel.IV));
        assertThat(LogisticsIds.of("picker_5")).contains(new LogisticsIds.Device(LogisticsIds.Kind.PICKER, LogisticsLevel.V));
        assertThat(LogisticsIds.of("picker_6")).isEmpty();
        assertThat(LogisticsIds.of("belt_4")).isEmpty();
        assertThat(LogisticsIds.of("splitter_4")).isEmpty();
    }

    @Test
    void aBeltDuplexIdReadsBackAsADuplexAndNotAsABelt() {
        assertThat(LogisticsIds.componentId(LogisticsIds.Kind.DUPLEX, LogisticsLevel.II)).isEqualTo("belt_duplex_2");
        assertThat(LogisticsIds.of("belt_duplex_1")).contains(new LogisticsIds.Device(LogisticsIds.Kind.DUPLEX, LogisticsLevel.I));
        assertThat(LogisticsIds.of("belt_duplex_3")).contains(new LogisticsIds.Device(LogisticsIds.Kind.DUPLEX, LogisticsLevel.III));
        assertThat(LogisticsIds.of("belt_duplex_4")).isEmpty();
        assertThat(LogisticsIds.of("belt_duplex")).isEmpty();
    }

    @Test
    void onlyPickersAndSplittersTakeASideFace() {
        assertThat(LogisticsIds.takesSide(LogisticsIds.Kind.PICKER)).isTrue();
        assertThat(LogisticsIds.takesSide(LogisticsIds.Kind.SPLITTER)).isTrue();
        assertThat(LogisticsIds.takesSide(LogisticsIds.Kind.BELT)).isFalse();
        assertThat(LogisticsIds.takesSide(LogisticsIds.Kind.DUPLEX)).isFalse();
        assertThat(LogisticsIds.takesSideAim("belt_duplex_2")).isFalse();
        assertThat(LogisticsIds.takesSideAim("splitter_2")).isTrue();
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
