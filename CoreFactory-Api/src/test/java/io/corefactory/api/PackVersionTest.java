package io.corefactory.api;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PackVersionTest {

    @Test
    void comparesVersionsNumericallyPerPart() {
        assertThat(new PackVersion("1.10.0", 1).compareTo(new PackVersion("1.9.0", 1))).isPositive();
        assertThat(new PackVersion("2.0.0", 1).compareTo(new PackVersion("1.99.99", 1))).isPositive();
        assertThat(new PackVersion("1.0", 1).compareTo(new PackVersion("1.0.0", 1))).isZero();
    }

    @Test
    void usesTheBuildToBreakATieBetweenEqualVersions() {
        PackVersion older = new PackVersion("1.0.0", 20260101000000L);
        PackVersion newer = new PackVersion("1.0.0", 20261019120000L);

        assertThat(newer.compareTo(older)).isPositive();
        assertThat(older.compareTo(newer)).isNegative();
        assertThat(newer.compareTo(new PackVersion("1.0.0", 20261019120000L))).isZero();
    }

    @Test
    void aHigherVersionWinsOverAHigherBuild() {
        PackVersion release = new PackVersion("1.1.0", 1L);
        PackVersion laterBuild = new PackVersion("1.0.0", 99_999_999_999_999L);

        assertThat(release.compareTo(laterBuild)).isPositive();
    }

    @Test
    void readsTheVersionBackFromAPackZip() throws Exception {
        byte[] zip = zipWith(PackVersion.ENTRY, PackVersion.properties("1.2.3", 20261019120000L, 97));

        assertThat(PackVersion.read(zip)).contains(new PackVersion("1.2.3", 20261019120000L));
    }

    @Test
    void aZipWithoutTheVersionEntryHasNoVersion() throws Exception {
        assertThat(PackVersion.read(zipWith("pack.mcmeta", "{}"))).isEmpty();
    }

    @Test
    void bytesThatAreNotAZipHaveNoVersion() {
        assertThat(PackVersion.read("not a zip".getBytes(StandardCharsets.UTF_8))).isEmpty();
    }

    @Test
    void aMalformedVersionEntryHasNoVersion() throws Exception {
        assertThat(PackVersion.read(zipWith(PackVersion.ENTRY, "version=oops\nbuild=1\n"))).isEmpty();
        assertThat(PackVersion.read(zipWith(PackVersion.ENTRY, "version=1.0.0\n"))).isEmpty();
    }

    @Test
    void rejectsAVersionThatIsNotDottedDigits() {
        assertThatThrownBy(() -> new PackVersion("v1", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PackVersion("1.0.0-SNAPSHOT", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PackVersion("1.0.0", -1)).isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] zipWith(String name, String text) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(text.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
