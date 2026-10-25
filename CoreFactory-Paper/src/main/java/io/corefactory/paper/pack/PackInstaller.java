package io.corefactory.paper.pack;

import io.corefactory.api.PackVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Decides whether the pack in {@code <folder>/pack.zip} should be replaced by the one bundled in the jar, and replaces
 * it when it should. It does not depend on Bukkit, so it can be tested with temporary folders.
 *
 * <p>The rule: a bundled pack replaces the installed one only if its version is greater. An equal version keeps the
 * installed file, and an installed pack that is newer than the jar is kept, so an older jar never downgrades a pack.
 * A file that is missing, unreadable or has no version is treated as outdated and replaced.
 */
public final class PackInstaller {

    /** The file name the pack is installed under. */
    public static final String FILE = "pack.zip";

    private static final String PART = FILE + ".part";

    /** What {@link #install} did, or would do. */
    public enum Action {
        /** There was no pack; the bundled one was copied in. */
        INSTALLED,
        /** The installed pack was older, missing, or unreadable; the bundled one replaced it. */
        UPDATED,
        /** The installed pack is the same version as the bundled one; nothing changed. */
        UP_TO_DATE,
        /** The installed pack is newer than the bundled one; nothing changed. */
        KEPT_NEWER,
        /** The jar has no bundled pack; nothing changed. */
        NO_BUNDLE,
        /** The bundled pack has no version, or the copy failed; nothing changed. */
        FAILED
    }

    /**
     * The outcome of an install.
     *
     * @param action    what happened, or would happen
     * @param bundled   the version of the pack in the jar, if it has one
     * @param installed the version of the pack in the folder before the call, if it has one
     * @param file      where the pack lives
     * @param detail    a sentence for the log or a command reply
     */
    public record Result(Action action, Optional<PackVersion> bundled, Optional<PackVersion> installed, Path file,
                         String detail) {

        public boolean changed() {
            return action == Action.INSTALLED || action == Action.UPDATED;
        }
    }

    private PackInstaller() {
    }

    /** Replaces the installed pack if the bundled one is newer, or installs it if there is none. */
    public static Result install(Path folder, byte[] bundled) {
        Result decision = inspect(folder, bundled);
        if (!decision.changed()) {
            return decision;
        }
        try {
            Files.createDirectories(folder);
            Path part = folder.resolve(PART);
            Files.write(part, bundled);
            Files.move(part, decision.file(), StandardCopyOption.REPLACE_EXISTING);
            return decision;
        } catch (IOException e) {
            return new Result(Action.FAILED, decision.bundled(), decision.installed(), decision.file(),
                    "Could not write " + decision.file() + ": " + e.getMessage()
                            + ". If the file is open in another program, close it and restart the server.");
        }
    }

    /** What {@link #install} would do, without writing anything. */
    public static Result inspect(Path folder, byte[] bundled) {
        Path target = folder.resolve(FILE);
        if (bundled == null) {
            return new Result(Action.NO_BUNDLE, Optional.empty(), installedVersion(target), target,
                    "This jar has no bundled resource pack.");
        }
        Optional<PackVersion> bundledVersion = PackVersion.read(bundled);
        if (bundledVersion.isEmpty()) {
            return new Result(Action.FAILED, Optional.empty(), installedVersion(target), target,
                    "The bundled resource pack has no version, so it was not installed.");
        }
        boolean present = Files.exists(target);
        Optional<PackVersion> installed = present ? installedVersion(target) : Optional.empty();
        PackVersion bundledNow = bundledVersion.get();

        if (!present) {
            return new Result(Action.INSTALLED, bundledVersion, installed, target,
                    "Resource pack " + bundledNow + " installed.");
        }
        if (installed.isEmpty()) {
            return new Result(Action.UPDATED, bundledVersion, installed, target,
                    "The installed resource pack has no readable version; replaced with " + bundledNow + ".");
        }
        int order = bundledNow.compareTo(installed.get());
        if (order > 0) {
            return new Result(Action.UPDATED, bundledVersion, installed, target,
                    "Resource pack updated from " + installed.get() + " to " + bundledNow + ".");
        }
        if (order == 0) {
            return new Result(Action.UP_TO_DATE, bundledVersion, installed, target,
                    "Resource pack " + bundledNow + " is already up to date.");
        }
        return new Result(Action.KEPT_NEWER, bundledVersion, installed, target,
                "The installed resource pack " + installed.get() + " is newer than this jar's " + bundledNow
                        + "; it was kept.");
    }

    /** Reads the version of the pack file at {@code target}. Empty if it is missing or unreadable. */
    static Optional<PackVersion> installedVersion(Path target) {
        if (!Files.isRegularFile(target)) {
            return Optional.empty();
        }
        try {
            return PackVersion.read(Files.readAllBytes(target));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
