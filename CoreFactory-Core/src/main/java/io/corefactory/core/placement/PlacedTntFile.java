package io.corefactory.core.placement;

import java.util.Map;

/**
 * Where the placed TNT registry is kept. Core hands over and receives plain data, a map with a {@code placed} list; the platform
 * decides the file and its format.
 */
public interface PlacedTntFile {

    /**
     * Reads the stored registry.
     *
     * @return the registry's data, or an empty map when nothing is stored yet
     */
    Map<String, Object> read();

    /**
     * Writes the registry. A failure is the platform's to report; the registry stays in memory either way.
     *
     * @param data the registry's data, as {@link PlacedTntStore#snapshot} gives it
     */
    void write(Map<String, Object> data);
}
