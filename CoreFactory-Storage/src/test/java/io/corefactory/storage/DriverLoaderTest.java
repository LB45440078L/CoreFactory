package io.corefactory.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class DriverLoaderTest {

    @TempDir
    Path libraries;

    @Test
    void aDriverOnTheClasspathIsUsedAsIs() {
        assertThat(DriverLoader.isPresent(DriverLoader.SQLITE_CLASS)).isTrue();
        assertThat(DriverLoader.ensure(DriverLoader.SQLITE_CLASS, DriverLoader.SQLITE, libraries,
                Logger.getLogger("test"))).isNull();
    }

    @Test
    void aMissingClassIsReportedAsMissing() {
        assertThat(DriverLoader.isPresent("org.corefactory.NoSuchDriver")).isFalse();
    }

    @Test
    void theSha1OfAFileMatchesTheKnownDigest() throws Exception {
        Path file = libraries.resolve("abc.txt");
        Files.writeString(file, "abc");

        assertThat(DriverLoader.sha1Of(file)).isEqualTo("a9993e364706816aba3e25717850c26c9cd0d89d");
    }

    @Test
    void theArtifactNamesFollowMavenLayout() {
        assertThat(DriverLoader.SQLITE.fileName()).isEqualTo("sqlite-jdbc-3.46.1.3.jar");
        assertThat(DriverLoader.SQLITE.jarUri().toString())
                .isEqualTo("https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar");
        assertThat(DriverLoader.POSTGRESQL.sha1Uri().toString()).endsWith("postgresql-42.7.4.jar.sha1");
    }
}
