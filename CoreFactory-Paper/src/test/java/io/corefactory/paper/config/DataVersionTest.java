package io.corefactory.paper.config;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;

import static io.corefactory.paper.config.DataVersion.Action.KEEP;
import static io.corefactory.paper.config.DataVersion.Action.NEWER;
import static io.corefactory.paper.config.DataVersion.Action.REPLACE;
import static org.assertj.core.api.Assertions.assertThat;

/** The rule that decides whether a server's processes.yml is replaced. Nothing here starts a server. */
class DataVersionTest {

    @Test
    void aFileWithNoVersionIsReplaced() {
        assertThat(DataVersion.decide(OptionalInt.empty(), 7)).isEqualTo(REPLACE);
    }

    @Test
    void aFileWithAnOlderVersionIsReplaced() {
        assertThat(DataVersion.decide(OptionalInt.of(6), 7)).isEqualTo(REPLACE);
        assertThat(DataVersion.decide(OptionalInt.of(5), 7)).isEqualTo(REPLACE);
        assertThat(DataVersion.decide(OptionalInt.of(4), 7)).isEqualTo(REPLACE);
        assertThat(DataVersion.decide(OptionalInt.of(3), 7)).isEqualTo(REPLACE);
        assertThat(DataVersion.decide(OptionalInt.of(2), 7)).isEqualTo(REPLACE);
        assertThat(DataVersion.decide(OptionalInt.of(1), 7)).isEqualTo(REPLACE);
        assertThat(DataVersion.decide(OptionalInt.of(0), 7)).isEqualTo(REPLACE);
    }

    @Test
    void aFileWithTheSameVersionIsKept() {
        assertThat(DataVersion.decide(OptionalInt.of(7), 7)).isEqualTo(KEEP);
    }

    @Test
    void aFileWithANewerVersionIsLeftAlone() {
        assertThat(DataVersion.decide(OptionalInt.of(8), 7)).isEqualTo(NEWER);
    }

    @Test
    void theVersionLineIsRecognisedWithOrWithoutSpaces() {
        assertThat(DataVersion.parse("# data-version: 2")).isEqualTo(OptionalInt.of(2));
        assertThat(DataVersion.parse("#data-version:7")).isEqualTo(OptionalInt.of(7));
        assertThat(DataVersion.parse("﻿# data-version: 2  ")).isEqualTo(OptionalInt.of(2));
    }

    @Test
    void anotherFirstLineHasNoVersion() {
        assertThat(DataVersion.parse("# ====================")).isEmpty();
        assertThat(DataVersion.parse("machines:")).isEmpty();
        assertThat(DataVersion.parse("# data-version: two")).isEmpty();
        assertThat(DataVersion.parse("data-version: 2")).isEmpty();
    }

    @Test
    void theShippedProcessesFileIsStampedWithTheCurrentVersion() throws Exception {
        String firstLine;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                DataVersionTest.class.getClassLoader().getResourceAsStream("processes.yml"), StandardCharsets.UTF_8))) {
            firstLine = reader.readLine();
        }
        assertThat(firstLine).isEqualTo("# data-version: " + DataVersion.CURRENT);
        assertThat(DataVersion.parse(firstLine)).isEqualTo(OptionalInt.of(DataVersion.CURRENT));
    }
}
