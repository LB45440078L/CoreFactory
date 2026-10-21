package io.corefactory.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.logging.Logger;

/**
 * Makes a JDBC driver available when it is not on the classpath. The driver jar is downloaded once from Maven Central
 * into the plugin's libraries folder, checked against the SHA-1 that Maven Central publishes beside it, and loaded in a
 * class loader of its own. Nothing is bundled into the plugin jar, so the native SQLite libraries are never shaded.
 */
final class DriverLoader {

    static final String SQLITE_CLASS = "org.sqlite.JDBC";
    static final String POSTGRESQL_CLASS = "org.postgresql.Driver";

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    /**
     * A driver artifact in Maven Central.
     *
     * @param coordinates the group path, artifact and version, for example {@code org/xerial/sqlite-jdbc/3.46.1.3}
     * @param artifact    the artifact id, which names the jar
     * @param version     the version, which names the jar
     */
    record Artifact(String coordinates, String artifact, String version) {

        String fileName() {
            return artifact + "-" + version + ".jar";
        }

        URI jarUri() {
            return URI.create(MAVEN_CENTRAL + coordinates + "/" + fileName());
        }

        URI sha1Uri() {
            return URI.create(jarUri() + ".sha1");
        }
    }

    static final Artifact SQLITE = new Artifact("org/xerial/sqlite-jdbc/3.46.1.3", "sqlite-jdbc", "3.46.1.3");
    static final Artifact POSTGRESQL = new Artifact("org/postgresql/postgresql/42.7.4", "postgresql", "42.7.4");

    private DriverLoader() {
    }

    /**
     * Returns a class loader that holds the driver, or null when the driver is already on the classpath.
     *
     * @param driverClass the driver class name, for example {@code org.sqlite.JDBC}
     * @param artifact    where the driver is published
     * @param libraries   the folder the driver jar is kept in
     * @throws StorageException when the driver is missing and cannot be downloaded or verified
     */
    static ClassLoader ensure(String driverClass, Artifact artifact, Path libraries, Logger log) {
        if (isPresent(driverClass)) {
            return null;
        }
        Path jar = libraries.resolve(artifact.fileName());
        try {
            Files.createDirectories(libraries);
            if (!Files.isRegularFile(jar) || !verified(jar, artifact)) {
                Files.deleteIfExists(jar);
                download(artifact, jar, log);
            }
            return new URLClassLoader(new URL[] {jar.toUri().toURL()}, DriverLoader.class.getClassLoader());
        } catch (IOException e) {
            throw new StorageException("The " + artifact.artifact() + " driver is not on the classpath and could not be "
                    + "downloaded to " + jar + ". Check the connection to " + MAVEN_CENTRAL + ".", e);
        }
    }

    static boolean isPresent(String driverClass) {
        try {
            Class.forName(driverClass, false, DriverLoader.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** The SHA-1 of a file, in lower-case hex. */
    static String sha1Of(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[1 << 16];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available.", e);
        }
    }

    private static boolean verified(Path jar, Artifact artifact) throws IOException {
        Path marker = jar.resolveSibling(artifact.fileName() + ".sha1");
        if (!Files.isRegularFile(marker)) {
            return false;
        }
        return sha1Of(jar).equalsIgnoreCase(Files.readString(marker).trim());
    }

    private static void download(Artifact artifact, Path jar, Logger logger) throws IOException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        String expected = fetchText(client, artifact.sha1Uri()).trim().split("\\s+")[0];
        logger.info("Downloading the " + artifact.artifact() + " driver from Maven Central.");
        Path temp = jar.resolveSibling(artifact.fileName() + ".part");
        HttpResponse<InputStream> response = send(client, HttpRequest.newBuilder(artifact.jarUri())
                .timeout(TIMEOUT).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            Files.copy(body, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!sha1Of(temp).equalsIgnoreCase(expected)) {
            Files.deleteIfExists(temp);
            throw new IOException("The downloaded " + artifact.fileName() + " does not match its published SHA-1.");
        }
        Files.move(temp, jar, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(jar.resolveSibling(artifact.fileName() + ".sha1"), expected);
    }

    private static String fetchText(HttpClient client, URI uri) throws IOException {
        return send(client, HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
    }

    private static <T> HttpResponse<T> send(HttpClient client, HttpRequest request,
                                            HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            HttpResponse<T> response = client.send(request, handler);
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " for " + request.uri());
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading " + request.uri(), e);
        }
    }
}
