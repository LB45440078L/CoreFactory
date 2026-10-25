package io.corefactory.core.artillery;

import io.corefactory.core.config.ConfigValues;

import java.util.List;
import java.util.Map;

/**
 * The typed form of the {@code command-center} section of config.yml: the Set Target listening mode, the map and the screen of the
 * Artillery Command Center. A missing key, or a value that is not finite or is out of range, gives its default, the value in
 * config.yml.
 *
 * @param listenTimeoutSeconds the seconds the Set Target listening mode stays open with no click, after which it ends
 * @param mapRadiusBlocks      the blocks from the centre of the map to its edge
 * @param zoomLevelsBlocks     the zoom levels of the map, in blocks per pixel, from the closest to the widest
 * @param screenRefreshTicks   the ticks between two refreshes of the screen
 * @param mapWallSize          the side of the map wall, in item frames: 3 or 5
 * @param terrainReadsInFlight the chunks the map reads from the server at once, without generating them: at least 1
 * @param terrainGenerationsInFlight the unexplored chunks the map generates at once: at least 1
 * @param terrainCacheChunks   the chunks whose terrain the map keeps, the least recently used dropped first: at least 1
 */
public record CommandCenterSettings(int listenTimeoutSeconds, double mapRadiusBlocks, List<Double> zoomLevelsBlocks,
                                    int screenRefreshTicks, int mapWallSize, int terrainReadsInFlight,
                                    int terrainGenerationsInFlight, int terrainCacheChunks) {

    /** The section's name in config.yml. */
    public static final String SECTION = "command-center";

    /** The listening timeout in config.yml, in seconds. */
    public static final int DEFAULT_LISTEN_TIMEOUT_SECONDS = 20;

    /** The map radius in config.yml, in blocks. */
    public static final double DEFAULT_MAP_RADIUS_BLOCKS = 64.0;

    /** The zoom levels in config.yml, in blocks per pixel. */
    public static final List<Double> DEFAULT_ZOOM_LEVELS_BLOCKS = List.of(1.0, 2.0, 4.0);

    /** The screen refresh in config.yml, in ticks. */
    public static final int DEFAULT_SCREEN_REFRESH_TICKS = 10;

    /** The side of the map wall in config.yml, in item frames. */
    public static final int DEFAULT_MAP_WALL_SIZE = 5;

    /** The chunks the map reads at once in config.yml. */
    public static final int DEFAULT_TERRAIN_READS_IN_FLIGHT = 8;

    /** The unexplored chunks the map generates at once in config.yml. */
    public static final int DEFAULT_TERRAIN_GENERATIONS_IN_FLIGHT = 4;

    /** The chunks whose terrain the map keeps in config.yml. */
    public static final int DEFAULT_TERRAIN_CACHE_CHUNKS = 4096;

    public CommandCenterSettings {
        zoomLevelsBlocks = List.copyOf(zoomLevelsBlocks);
        if (zoomLevelsBlocks.isEmpty()) {
            throw new IllegalArgumentException("the map needs at least one zoom level");
        }
        if (mapWallSize != 3 && mapWallSize != 5) {
            throw new IllegalArgumentException("the map wall is 3 or 5 item frames a side");
        }
        if (terrainReadsInFlight < 1 || terrainGenerationsInFlight < 1 || terrainCacheChunks < 1) {
            throw new IllegalArgumentException("the map's reads, generations and cache need at least one each");
        }
    }

    /** The settings the code runs with when config.yml gives none: every default of this record. */
    public static CommandCenterSettings defaults() {
        return from(null);
    }

    /**
     * Reads the {@code command-center} section.
     *
     * @param config the root of config.yml as plain data, or null for the defaults
     * @return the settings
     */
    public static CommandCenterSettings from(Map<String, Object> config) {
        Map<String, Object> section = ConfigValues.section(config, SECTION);
        return new CommandCenterSettings(
                positiveInt(section, "listen-timeout-seconds", DEFAULT_LISTEN_TIMEOUT_SECONDS),
                positive(section, "map-radius-blocks", DEFAULT_MAP_RADIUS_BLOCKS),
                zoomLevels(ConfigValues.numberList(section, "zoom-levels", DEFAULT_ZOOM_LEVELS_BLOCKS)),
                positiveInt(section, "screen-refresh-ticks", DEFAULT_SCREEN_REFRESH_TICKS),
                mapWallSize(section),
                positiveInt(section, "terrain-reads-in-flight", DEFAULT_TERRAIN_READS_IN_FLIGHT),
                positiveInt(section, "terrain-generations-in-flight", DEFAULT_TERRAIN_GENERATIONS_IN_FLIGHT),
                positiveInt(section, "terrain-cache-chunks", DEFAULT_TERRAIN_CACHE_CHUNKS));
    }

    /** The side of the map wall when it is 3 or 5 frames, and the default otherwise. */
    private static int mapWallSize(Map<String, Object> section) {
        int value = ConfigValues.integer(section, "map-wall-size", DEFAULT_MAP_WALL_SIZE);
        return value == 3 || value == 5 ? value : DEFAULT_MAP_WALL_SIZE;
    }

    /** The zoom levels when they are a non-empty, strictly increasing list of finite numbers above 0; the defaults otherwise. */
    private static List<Double> zoomLevels(List<Double> values) {
        boolean ok = !values.isEmpty();
        for (int i = 0; ok && i < values.size(); i++) {
            double value = values.get(i);
            ok = Double.isFinite(value) && value > 0 && (i == 0 || value > values.get(i - 1));
        }
        return ok ? values : DEFAULT_ZOOM_LEVELS_BLOCKS;
    }

    private static double positive(Map<String, Object> section, String path, double def) {
        double value = ConfigValues.number(section, path, def);
        return Double.isFinite(value) && value > 0 ? value : def;
    }

    private static int positiveInt(Map<String, Object> section, String path, int def) {
        int value = ConfigValues.integer(section, path, def);
        return value > 0 ? value : def;
    }
}
