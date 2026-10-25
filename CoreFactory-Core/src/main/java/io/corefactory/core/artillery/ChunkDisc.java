package io.corefactory.core.artillery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The chunks a disc on the ground covers: a disc of a radius around a point, in blocks, named by the coordinates of the chunks that hold it.
 * A chunk is a 16 by 16 block square, so a block coordinate rounds down into its chunk, negative ones too: the block at -1 is in chunk -1.
 *
 * <p>The chunks covered are the ones of the square that bounds the disc. That square is about 27 per cent larger than the disc, so the set
 * is a superset: no chunk the disc touches is missed, and a few that it only corners are included. Pure, so the layout can be tested without
 * a server.
 */
public final class ChunkDisc {

    /** The side of a chunk, in blocks. */
    static final int CHUNK_BLOCKS = 16;

    private ChunkDisc() {
    }

    /**
     * A chunk, named by its coordinates: the block coordinates shifted right by 4.
     *
     * @param x the chunk's x
     * @param z the chunk's z
     */
    public record Chunk(int x, int z) {
    }

    /**
     * The chunks a disc covers: every chunk from the one that holds the least corner of the bounding square to the one that holds the greatest,
     * on each axis.
     *
     * @param cx     the x of the disc's centre, in blocks
     * @param cz     the z of the disc's centre, in blocks
     * @param radius the disc's radius, in blocks, 0 or more
     * @return the chunks, in order of x and then z
     * @throws IllegalArgumentException when the centre or the radius is not finite, or the radius is below 0
     */
    public static List<Chunk> covering(double cx, double cz, double radius) {
        if (!Double.isFinite(cx) || !Double.isFinite(cz)) {
            throw new IllegalArgumentException("the centre must be finite: " + cx + ", " + cz);
        }
        if (!(radius >= 0) || !Double.isFinite(radius)) {
            throw new IllegalArgumentException("radius must be finite and 0 or more: " + radius);
        }
        int minX = chunkOf(cx - radius);
        int maxX = chunkOf(cx + radius);
        int minZ = chunkOf(cz - radius);
        int maxZ = chunkOf(cz + radius);
        List<Chunk> chunks = new ArrayList<>((maxX - minX + 1) * (maxZ - minZ + 1));
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                chunks.add(new Chunk(x, z));
            }
        }
        return List.copyOf(chunks);
    }

    /**
     * The chunks a disc covers, nearest its centre first. The distance of a chunk is the distance from the centre to the middle of the chunk;
     * chunks at the same distance are in order of x and then z. A cap on the chunks held keeps the first ones, so the chunks nearest the
     * centre are the ones kept.
     *
     * @param cx     the x of the disc's centre, in blocks
     * @param cz     the z of the disc's centre, in blocks
     * @param radius the disc's radius, in blocks, 0 or more
     * @return the chunks, nearest first
     * @throws IllegalArgumentException when the centre or the radius is not finite, or the radius is below 0
     */
    public static List<Chunk> nearestFirst(double cx, double cz, double radius) {
        List<Chunk> chunks = new ArrayList<>(covering(cx, cz, radius));
        chunks.sort(Comparator.<Chunk>comparingDouble(chunk -> distanceSquared(chunk, cx, cz))
                .thenComparingInt(Chunk::x)
                .thenComparingInt(Chunk::z));
        return List.copyOf(chunks);
    }

    /** The chunk that holds a block coordinate: the coordinate divided by 16, rounded down. */
    private static int chunkOf(double block) {
        return (int) Math.floor(block / CHUNK_BLOCKS);
    }

    private static double distanceSquared(Chunk chunk, double cx, double cz) {
        double midX = chunk.x() * CHUNK_BLOCKS + CHUNK_BLOCKS / 2.0;
        double midZ = chunk.z() * CHUNK_BLOCKS + CHUNK_BLOCKS / 2.0;
        double dx = midX - cx;
        double dz = midZ - cz;
        return dx * dx + dz * dz;
    }
}
