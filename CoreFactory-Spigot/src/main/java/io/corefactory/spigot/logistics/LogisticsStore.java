package io.corefactory.spigot.logistics;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes {@code logistics.yml} in the plugin's data folder. A write goes to a {@code .tmp} file that is then moved over
 * the real one, so a crash during a write leaves the last good file in place.
 */
public final class LogisticsStore {

    private final File file;
    private final Logger log;

    /**
     * @param file the logistics file, normally {@code logistics.yml} in the data folder
     * @param log  receives the skipped entries and any failed write
     */
    public LogisticsStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    /** The saved state, or an empty one when the file is missing or cannot be read. */
    public LogisticsFile.State read() {
        if (!file.isFile()) {
            return LogisticsFile.State.empty();
        }
        try {
            return LogisticsFile.read(Files.readString(file.toPath(), StandardCharsets.UTF_8), log::warning);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read " + file.getName() + ". Belts, pickers and splitters start empty.", e);
            return LogisticsFile.State.empty();
        }
    }

    /** Writes the state atomically: to a temporary file first, then moved over the logistics file. */
    public void write(LogisticsFile.State state) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        try {
            Files.writeString(temporary.toPath(), LogisticsFile.write(state), StandardCharsets.UTF_8);
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not save " + file.getName(), e);
        }
    }
}
