package io.corefactory.paper.energy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads and writes {@code energy.yml} in the plugin's data folder. A write goes to a {@code .tmp} file that is then moved over the
 * real one, so a crash during a write leaves the last good file in place.
 */
public final class EnergyStore {

    private final File file;
    private final Logger log;

    /**
     * @param file the energy file, normally {@code energy.yml} in the data folder
     * @param log  receives the skipped entries and any failed write
     */
    public EnergyStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    /** The saved state, or an empty one when the file is missing or cannot be read. */
    public StoredEnergy read() {
        if (!file.isFile()) {
            return StoredEnergy.empty();
        }
        try {
            return EnergyFile.read(Files.readString(file.toPath(), StandardCharsets.UTF_8), log::warning);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not read " + file.getName() + ". Energy devices start empty.", e);
            return StoredEnergy.empty();
        }
    }

    /** Writes the state atomically: to a temporary file first, then moved over the energy file. */
    public void write(StoredEnergy energy) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        try {
            Files.writeString(temporary.toPath(), EnergyFile.write(energy), StandardCharsets.UTF_8);
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
