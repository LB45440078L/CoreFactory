package io.corefactory.paper.artillery;

import io.corefactory.paper.chunks.ChunkKey;
import io.corefactory.paper.chunks.ChunkTickets;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The chunks the artillery holds through the plugin's chunk tickets. The pieces and the Command Centers hold their chunks together under
 * {@link #PIECES}, outside the device keeper's cap and order, so a piece stays loaded whatever the other devices need. Each Command Center
 * holds the target area of its aim under an owner of its own, {@link #targetOwner}, from the time the target is set until it is cleared, replaced
 * or the center is removed.
 *
 * <p>Both kinds of hold follow {@code chunks.keep-loaded}: when it is false, nothing here is held, as the device keeper holds nothing. The
 * holds are placed and removed through the same book as the other holders, so a chunk that the keeper or a shot also holds keeps its ticket.
 */
final class ArtilleryChunks {

    /** The owner of the chunks of the pieces and the Command Centers. */
    static final String PIECES = "artillery pieces";

    /** How far the target area reaches from the aim's chunk, in chunks: 1 is the aim's chunk and its eight neighbours, a 3 by 3 block. */
    static final int TARGET_REACH = 1;

    private final ChunkTickets tickets;
    private final boolean keepLoaded;

    /**
     * @param tickets    the plugin's chunk tickets
     * @param keepLoaded the value of {@code chunks.keep-loaded}; false holds nothing
     */
    ArtilleryChunks(ChunkTickets tickets, boolean keepLoaded) {
        this.tickets = tickets;
        this.keepLoaded = keepLoaded;
    }

    /** The owner of the target area of a Command Center, in the plugin's chunk tickets. */
    static String targetOwner(String centerId) {
        return "artillery target " + centerId;
    }

    /** The chunk a piece stands in. */
    static ChunkKey pieceChunk(ArtilleryDevice piece) {
        return ChunkKey.of(piece.world(), piece.pos().x(), piece.pos().z());
    }

    /**
     * The chunk an aim point is in. The aim is floored to a block first, as the firing rules floor it.
     *
     * @param world the id of the aim's world
     * @param aimX  the x of the aim, in blocks
     * @param aimZ  the z of the aim, in blocks
     */
    static ChunkKey aimChunk(UUID world, double aimX, double aimZ) {
        return ChunkKey.of(world, (int) Math.floor(aimX), (int) Math.floor(aimZ));
    }

    /**
     * The chunks of the target area of an aim: the chunk the aim is in, and the chunks within {@link #TARGET_REACH} of it on each side.
     *
     * @param world the id of the aim's world
     * @param aimX  the x of the aim, in blocks; floored, as the firing rules floor it
     * @param aimZ  the z of the aim, in blocks; floored, as the firing rules floor it
     * @return the chunks of the area, nine of them at the default reach
     */
    static Set<ChunkKey> targetArea(UUID world, double aimX, double aimZ) {
        ChunkKey aim = aimChunk(world, aimX, aimZ);
        Set<ChunkKey> area = new HashSet<>();
        for (int dx = -TARGET_REACH; dx <= TARGET_REACH; dx++) {
            for (int dz = -TARGET_REACH; dz <= TARGET_REACH; dz++) {
                area.add(new ChunkKey(world, aim.x() + dx, aim.z() + dz));
            }
        }
        return area;
    }

    /**
     * Makes the pieces' owner hold exactly the chunks given. The owner is not capped: every chunk given is held, whatever the device cap is.
     *
     * @param chunks the chunks of the pieces and the Command Centers
     */
    void holdPieces(Set<ChunkKey> chunks) {
        tickets.setOwned(PIECES, keepLoaded ? chunks : Set.of());
    }

    /**
     * Holds the target area of a center while it has an aim, and drops the area of the previous aim: a new aim replaces the old area. A center
     * with no aim holds nothing.
     *
     * @param center the center, with the aim it has now
     */
    void holdTarget(CommandCenter center) {
        if (!keepLoaded || !center.hasAim()) {
            tickets.release(targetOwner(center.id()));
            return;
        }
        tickets.setOwned(targetOwner(center.id()), targetArea(center.world(), center.aimX(), center.aimZ()));
    }

    /** Drops the target area of a center. Called when the target is cleared and when the center is removed. */
    void releaseTarget(String centerId) {
        tickets.release(targetOwner(centerId));
    }

    /** True when the plugin holds the chunk, so it loads again by itself. */
    boolean isHeld(ChunkKey key) {
        return tickets.isHeld(key);
    }
}
