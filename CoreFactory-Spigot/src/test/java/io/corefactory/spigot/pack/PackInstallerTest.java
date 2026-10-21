package io.corefactory.spigot.pack;

import io.corefactory.api.PackVersion;
import io.corefactory.spigot.pack.PackInstaller.Action;
import io.corefactory.spigot.pack.PackInstaller.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PackInstallerTest {

    @TempDir
    Path folder;

    @Test
    void installsWhenThereIsNoPackYet() throws IOException {
        byte[] bundled = pack("1.0.0", 1L);

        Result result = PackInstaller.install(folder, bundled);

        assertThat(result.action()).isEqualTo(Action.INSTALLED);
        assertThat(result.changed()).isTrue();
        assertThat(Files.readAllBytes(folder.resolve(PackInstaller.FILE))).isEqualTo(bundled);
    }

    @Test
    void keepsAnInstalledPackOfTheSameVersion() throws IOException {
        byte[] installed = withMarker();
        Files.write(folder.resolve(PackInstaller.FILE), installed);

        Result result = PackInstaller.install(folder, pack("1.0.0", 1L));

        assertThat(result.action()).isEqualTo(Action.UP_TO_DATE);
        assertThat(result.changed()).isFalse();
        assertThat(Files.readAllBytes(folder.resolve(PackInstaller.FILE))).isEqualTo(installed);
    }

    @Test
    void replacesAnOlderVersion() throws IOException {
        Files.write(folder.resolve(PackInstaller.FILE), pack("1.0.0", 1L));
        byte[] bundled = pack("1.1.0", 1L);

        Result result = PackInstaller.install(folder, bundled);

        assertThat(result.action()).isEqualTo(Action.UPDATED);
        assertThat(result.installed()).contains(new PackVersion("1.0.0", 1L));
        assertThat(Files.readAllBytes(folder.resolve(PackInstaller.FILE))).isEqualTo(bundled);
    }

    @Test
    void replacesTheSameVersionWhenTheBuildIsNewer() throws IOException {
        Files.write(folder.resolve(PackInstaller.FILE), pack("1.0.0", 20260101000000L));
        byte[] bundled = pack("1.0.0", 20261019120000L);

        Result result = PackInstaller.install(folder, bundled);

        assertThat(result.action()).isEqualTo(Action.UPDATED);
        assertThat(Files.readAllBytes(folder.resolve(PackInstaller.FILE))).isEqualTo(bundled);
    }

    @Test
    void neverDowngradesANewerInstalledPack() throws IOException {
        byte[] installed = pack("2.0.0", 1L);
        Files.write(folder.resolve(PackInstaller.FILE), installed);

        Result result = PackInstaller.install(folder, pack("1.9.9", 99L));

        assertThat(result.action()).isEqualTo(Action.KEPT_NEWER);
        assertThat(Files.readAllBytes(folder.resolve(PackInstaller.FILE))).isEqualTo(installed);
    }

    @Test
    void replacesAPackThatHasNoVersion() throws IOException {
        Files.write(folder.resolve(PackInstaller.FILE), zip(Map.of("pack.mcmeta", "{}")));
        byte[] bundled = pack("1.0.0", 1L);

        Result result = PackInstaller.install(folder, bundled);

        assertThat(result.action()).isEqualTo(Action.UPDATED);
        assertThat(Files.readAllBytes(folder.resolve(PackInstaller.FILE))).isEqualTo(bundled);
    }

    @Test
    void replacesAPackThatIsNotAZip() throws IOException {
        Files.write(folder.resolve(PackInstaller.FILE), "garbage".getBytes(StandardCharsets.UTF_8));

        Result result = PackInstaller.install(folder, pack("1.0.0", 1L));

        assertThat(result.action()).isEqualTo(Action.UPDATED);
    }

    @Test
    void doesNothingWhenTheJarHasNoPack() {
        Result result = PackInstaller.install(folder, null);

        assertThat(result.action()).isEqualTo(Action.NO_BUNDLE);
        assertThat(Files.exists(folder.resolve(PackInstaller.FILE))).isFalse();
    }

    @Test
    void refusesABundledPackThatHasNoVersion() {
        Result result = PackInstaller.install(folder, zip(Map.of("pack.mcmeta", "{}")));

        assertThat(result.action()).isEqualTo(Action.FAILED);
        assertThat(Files.exists(folder.resolve(PackInstaller.FILE))).isFalse();
    }

    @Test
    void inspectReportsWhatWouldHappenAndWritesNothing() throws IOException {
        byte[] installed = pack("1.0.0", 1L);
        Files.write(folder.resolve(PackInstaller.FILE), installed);

        Result result = PackInstaller.inspect(folder, pack("1.1.0", 1L));

        assertThat(result.action()).isEqualTo(Action.UPDATED);
        assertThat(Files.readAllBytes(folder.resolve(PackInstaller.FILE))).isEqualTo(installed);
    }

    /** The same pack with an extra entry, so a copy can be told apart from the original. */
    private static byte[] withMarker() {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(PackVersion.ENTRY, PackVersion.properties("1.0.0", 1L, 97));
        entries.put("installed.marker", "kept");
        return zip(entries);
    }

    private static byte[] pack(String version, long build) {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("pack.mcmeta", "{}");
        entries.put(PackVersion.ENTRY, PackVersion.properties(version, build, 97));
        return zip(entries);
    }

    private static byte[] zip(Map<String, String> entries) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }
}
