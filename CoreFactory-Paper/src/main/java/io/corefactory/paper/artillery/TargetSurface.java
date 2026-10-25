package io.corefactory.paper.artillery;

import io.corefactory.paper.artillery.TerrainCache.ChunkId;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.map.MapPalette;

import java.awt.Color;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The terrain of a Command Center's map at one redraw, read from a world. The top of a column is coloured as the map colours a surface: the
 * colour of its top block, from a table of the common surfaces. A column in a loaded chunk is read from the world as it is now. A column in a
 * chunk that is not loaded is read from the shared {@link TerrainLoader}: the terrain sampled from that chunk before, or a loading tile while
 * the chunk is being read or generated. The map never keeps a chunk loaded: a chunk is loaded only for the loader's callback.
 *
 * <p>The game's own map colours are not in the Bukkit API, so the table here is an approximation of them by material. A surface that is not in
 * the table is drawn in a neutral grey.
 */
final class TargetSurface implements TargetFrame.Surface {

    /** The colour of a surface that is not in the table. */
    private static final Color DEFAULT = new Color(127, 127, 127);

    private static final Map<Material, Byte> CACHE = new EnumMap<>(Material.class);

    private final World world;
    private final TerrainLoader terrains;
    private final int phase;

    /**
     * @param world   the world the map shows
     * @param terrains the loader of the terrain of the chunks that are not loaded
     * @param tick    the server tick of the redraw, which sets the phase of the loading tiles
     */
    TargetSurface(World world, TerrainLoader terrains, long tick) {
        this.world = world;
        this.terrains = terrains;
        this.phase = LoadingAnimation.phase(tick);
    }

    @Override
    public int colourAt(int x, int z) {
        if (world.isChunkLoaded(x >> 4, z >> 4)) {
            return sampleColumn(world, x, z) & 0xFF;
        }
        return terrains.colourAt(new ChunkId(world.getUID(), x >> 4, z >> 4), x, z, phase);
    }

    /**
     * Asks the loader for the chunks of a view that are not loaded and not kept yet, nearest the view's centre first. Called once for a redraw
     * before the terrain is read.
     *
     * @param view the view
     * @param bpp  the blocks per pixel
     * @param tick the server tick of the redraw
     * @return true when a chunk of the view is still loading, so the view has loading tiles
     */
    boolean request(TargetView view, double bpp, long tick) {
        Set<ChunkId> chunks = new HashSet<>();
        int lastX = Integer.MIN_VALUE;
        int lastZ = Integer.MIN_VALUE;
        for (int py = 0; py < TargetView.PIXELS; py++) {
            for (int px = 0; px < TargetView.PIXELS; px++) {
                int x = ((int) Math.floor(view.worldX(px, bpp))) >> 4;
                int z = ((int) Math.floor(view.worldZ(py, bpp))) >> 4;
                if (x != lastX || z != lastZ) {
                    lastX = x;
                    lastZ = z;
                    chunks.add(new ChunkId(world.getUID(), x, z));
                }
            }
        }
        boolean loading = false;
        for (ChunkId id : chunks) {
            if (world.isChunkLoaded(id.x(), id.z())) {
                continue;
            }
            double dx = id.x() * 16 + 8 - view.centreX();
            double dz = id.z() * 16 + 8 - view.centreZ();
            loading |= terrains.want(id, dx * dx + dz * dz, tick);
        }
        return loading;
    }

    /**
     * The terrain of a loaded chunk: the colour of each of its columns, row by row (see {@link TerrainCache#index}). The chunk must be loaded,
     * and the call is made on the main thread.
     *
     * @param world the chunk's world
     * @param chunk the loaded chunk
     * @return {@link TerrainCache#COLUMNS} palette bytes
     */
    static byte[] sample(World world, Chunk chunk) {
        byte[] colours = new byte[TerrainCache.COLUMNS];
        int x0 = chunk.getX() << 4;
        int z0 = chunk.getZ() << 4;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                colours[TerrainCache.index(x, z)] = sampleColumn(world, x0 + x, z0 + z);
            }
        }
        return colours;
    }

    /** The map colour of the top of one column, of a loaded chunk. */
    private static byte sampleColumn(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        return colourOf(world.getBlockAt(x, y, z).getType());
    }

    /** The map colour of a block's material, cached per material. */
    static byte colourOf(Material material) {
        return CACHE.computeIfAbsent(material, m -> MapPalette.matchColor(colourOfName(m.name())));
    }

    /** The colour of a material name, by the first group of words in the table that it contains. */
    static Color colourOfName(String name) {
        if (name.contains("WATER") || name.contains("KELP") || name.contains("SEAGRASS")) {
            return new Color(64, 64, 255);
        }
        if (name.contains("ICE")) {
            return new Color(160, 160, 255);
        }
        if (name.contains("SNOW")) {
            return new Color(255, 255, 255);
        }
        if (name.contains("SAND") && !name.contains("SOUL")) {
            return new Color(247, 233, 163);
        }
        if (name.contains("LAVA") || name.contains("MAGMA") || name.contains("FIRE")) {
            return new Color(255, 85, 0);
        }
        if (name.contains("GRASS") || name.contains("MOSS") || name.contains("FERN") || name.contains("VINE")) {
            return new Color(127, 178, 56);
        }
        if (name.contains("LEAVES") || name.contains("AZALEA")) {
            return new Color(0, 124, 0);
        }
        if (name.contains("LOG") || name.contains("WOOD") || name.contains("PLANK") || name.contains("STEM")) {
            return new Color(143, 119, 72);
        }
        if (name.contains("DIRT") || name.contains("MUD") || name.contains("FARMLAND") || name.contains("PODZOL")
                || name.contains("ROOTED") || name.contains("MYCELIUM") || name.contains("CLAY")) {
            return new Color(151, 109, 77);
        }
        if (name.contains("STONE") || name.contains("COBBLE") || name.contains("GRAVEL") || name.contains("DEEPSLATE")
                || name.contains("ANDESITE") || name.contains("DIORITE") || name.contains("GRANITE") || name.contains("TUFF")
                || name.contains("BASALT") || name.contains("BEDROCK") || name.endsWith("_ORE")) {
            return new Color(112, 112, 112);
        }
        return DEFAULT;
    }
}
