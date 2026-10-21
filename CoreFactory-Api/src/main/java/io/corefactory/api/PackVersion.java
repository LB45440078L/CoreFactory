package io.corefactory.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import java.util.zip.ZipInputStream;

/**
 * The version of a built resource pack. The pack builder writes it into the zip as {@link #ENTRY}, and the plugin
 * reads it back to decide whether the pack already in {@code plugins/CoreFactory/pack/} is outdated.
 *
 * <p>Packs compare by {@link #version} first, numerically per dotted part ({@code 1.10.0} is newer than {@code 1.9.0}),
 * and then by {@link #build}, a timestamp of the build. A pack with a higher build of the same version is newer, so
 * rebuilding without bumping the version still replaces the old pack.
 *
 * @param version the release version, dotted digits such as {@code 1.0.0}
 * @param build   the build stamp, {@code yyyyMMddHHmmss} from the build machine's clock, or any increasing number
 */
public record PackVersion(String version, long build) implements Comparable<PackVersion> {

    /** The name of the file inside the pack zip that holds the version. */
    public static final String ENTRY = "corefactory-pack.properties";

    public PackVersion {
        if (version == null || !version.matches("\\d+(\\.\\d+)*")) {
            throw new IllegalArgumentException("A pack version is dotted digits, such as 1.0.0: " + version);
        }
        if (build < 0) {
            throw new IllegalArgumentException("A pack build number cannot be negative: " + build);
        }
    }

    /** The text written into the zip: version, build and pack format, one per line. */
    public static String properties(String version, long build, int packFormat) {
        return "version=" + version + "\nbuild=" + build + "\npack-format=" + packFormat + "\n";
    }

    /** Reads the version from a pack zip. Empty if the zip has no version entry, or it cannot be read. */
    public static Optional<PackVersion> read(byte[] zip) {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (var entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                if (ENTRY.equals(entry.getName())) {
                    return parse(readText(in));
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /** Reads a {@code corefactory-pack.properties} text. Empty if a field is missing or malformed. */
    static Optional<PackVersion> parse(String text) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(text));
            String version = properties.getProperty("version");
            long build = Long.parseLong(properties.getProperty("build", "-1").trim());
            return Optional.of(new PackVersion(version.trim(), build));
        } catch (IOException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    @Override
    public int compareTo(PackVersion other) {
        String[] mine = version.split("\\.");
        String[] theirs = other.version.split("\\.");
        for (int i = 0; i < Math.max(mine.length, theirs.length); i++) {
            int a = i < mine.length ? Integer.parseInt(mine[i]) : 0;
            int b = i < theirs.length ? Integer.parseInt(theirs[i]) : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return Long.compare(build, other.build);
    }

    @Override
    public String toString() {
        return version + " (build " + build + ")";
    }

    private static String readText(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        in.transferTo(bytes);
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
