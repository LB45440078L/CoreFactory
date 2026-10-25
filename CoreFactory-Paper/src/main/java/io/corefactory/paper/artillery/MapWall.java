package io.corefactory.paper.artillery;

import io.corefactory.core.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The map wall of one Command Center: the item frames that show its map, the barrier block behind each frame, and the map views the frames hold.
 * The wall is one object, so its cleanup is one call: {@link #remove} takes the frames away and puts back the block each barrier replaced.
 *
 * <p>The barriers make the frames stand on something, and they are invisible in survival. The frames are fixed, so a click does not turn
 * them. The wall is placed only when every chunk it reaches is loaded, and it is removed when the chunk of any of its blocks unloads.
 *
 * <p>The canvas is the map at the wall's scale. {@link #show} scales the map's pixels up to the wall, and the frames' renderers copy their
 * tiles from it. When the canvas changes, the maps whose tiles changed are pushed to the players near it with {@link Player#sendMap}, once for
 * each map. A map is kept by the center, so the same maps serve a wall that is placed again after a chunk unloads or the server restarts.
 */
final class MapWall {

    /** The marker on the frames of a wall, so the sweep of a chunk removes the frames that no wall owns. */
    static final NamespacedKey MARK = new NamespacedKey(ItemFactory.NAMESPACE, "command_center_wall");

    /** The distance in blocks within which players are sent the wall's maps. */
    static final double VIEW_BLOCKS = 128;

    /** A frame that is placed, the barrier behind it and the data of the block the barrier replaced. */
    private record Placed(UUID frame, Location backing, BlockData original) {
    }

    private final CommandCenter center;
    private final WallLayout layout;
    private final List<Placed> placed = new ArrayList<>();
    private MapView[] views = new MapView[0];
    private World world;
    private byte[] canvas;
    private byte[] lastFrame;
    private byte[] pushed;

    /**
     * @param center the center the wall belongs to
     * @param layout the wall's frames and their positions
     */
    MapWall(CommandCenter center, WallLayout layout) {
        this.center = center;
        this.layout = layout;
    }

    /** The center the wall belongs to. */
    CommandCenter center() {
        return center;
    }

    /** The wall's frames and their positions. */
    WallLayout layout() {
        return layout;
    }

    /** True while the frames and their barriers are placed. */
    boolean isPlaced() {
        return !placed.isEmpty();
    }

    /** The ids of the placed frames, for the sweep: a frame that no wall owns is removed. */
    Set<UUID> frames() {
        Set<UUID> ids = new HashSet<>();
        for (Placed frame : placed) {
            ids.add(frame.frame());
        }
        return ids;
    }

    /**
     * Places the frames, and a barrier behind each one. Nothing is placed when the wall is placed already, or when a chunk it reaches is not
     * loaded: the wall is never made by loading a chunk.
     *
     * @param world the world the center stands in
     */
    void place(World world) {
        if (isPlaced() || !chunksLoaded(world)) {
            return;
        }
        this.world = world;
        ensureViews(world);
        for (int index = 0; index < layout.frames(); index++) {
            int x = layout.frameX(index);
            int y = layout.frameY(index);
            Block backing = world.getBlockAt(x, y, layout.backingZ());
            BlockData original = backing.getBlockData();
            backing.setType(Material.BARRIER, false);
            ItemStack item = mapItem(views[index]);
            ItemFrame frame = world.spawn(new Location(world, x + 0.5, y + 0.5, layout.z0() + 0.5), ItemFrame.class, entity -> {
                entity.setFacingDirection(BlockFace.NORTH, true);
                entity.setItem(item, false);
                entity.setVisible(true);
                entity.setFixed(true);
                entity.setPersistent(true);
                entity.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, center.id());
            });
            placed.add(new Placed(frame.getUniqueId(), backing.getLocation(), original));
        }
        pushed = null;
        pushViewers();
    }

    /**
     * Shows a map on the wall: its pixels are scaled up to the wall, and the frames show them. The players near the wall are sent the new
     * maps when the picture changed. The canvas is kept when the wall is not placed, so it is shown when the wall is placed.
     *
     * @param frame the map's pixels, 128 by 128, row by row
     */
    void show(byte[] frame) {
        if (frame == lastFrame) {
            return;
        }
        lastFrame = frame;
        canvas = TargetFrame.upscale(frame, layout.tiles());
        if (isPlaced() && !Arrays.equals(canvas, pushed)) {
            pushViewers();
        }
    }

    /**
     * Takes the wall away: the frames go, and each barrier gives back the block it replaced. A barrier in a chunk that is not loaded is left,
     * which does not happen, because a wall is removed before its chunks unload.
     */
    void remove() {
        for (Placed frame : placed) {
            Entity entity = Bukkit.getEntity(frame.frame());
            if (entity != null) {
                entity.remove();
            }
            Location backing = frame.backing();
            World at = backing.getWorld();
            if (at != null && at.isChunkLoaded(backing.getBlockX() >> 4, backing.getBlockZ() >> 4)) {
                backing.getBlock().setBlockData(frame.original(), false);
            }
        }
        placed.clear();
    }

    /** The maps of the wall: one for each frame, made once, and kept by the center. A map that the center has no more is made again. */
    private void ensureViews(World world) {
        if (views.length == layout.frames()) {
            return;
        }
        List<Integer> saved = center.wallMaps();
        MapView[] made = new MapView[layout.frames()];
        List<Integer> ids = new ArrayList<>();
        for (int index = 0; index < made.length; index++) {
            MapView view = index < saved.size() ? Bukkit.getMap(saved.get(index)) : null;
            if (view == null) {
                view = Bukkit.createMap(world);
            }
            view.setTrackingPosition(false);
            view.setUnlimitedTracking(false);
            view.setLocked(true);
            for (MapRenderer renderer : new ArrayList<>(view.getRenderers())) {
                view.removeRenderer(renderer);
            }
            view.addRenderer(new WallTileRenderer(layout.tiles(), layout.column(index), layout.row(index), this::canvas));
            made[index] = view;
            ids.add(view.getId());
        }
        views = made;
        center.setWallMaps(ids);
    }

    /**
     * Sends the maps of the wall whose tiles changed since the canvas last sent to the players near it, and remembers the canvas they were sent.
     * A wall that was never sent, or was placed again, sends every map.
     */
    private void pushViewers() {
        if (world == null || canvas == null || views.length == 0) {
            return;
        }
        Location middle = new Location(world, layout.x0() + layout.tiles() / 2.0, layout.y0() + layout.tiles() / 2.0, layout.z0());
        Collection<Player> viewers = world.getNearbyPlayers(middle, VIEW_BLOCKS);
        for (int index = 0; index < views.length; index++) {
            if (!changed(index)) {
                continue;
            }
            for (Player viewer : viewers) {
                viewer.sendMap(views[index]);
            }
        }
        pushed = canvas;
    }

    /** True when the tile of a map differs from the canvas last sent, or when no canvas was sent yet. */
    private boolean changed(int index) {
        if (pushed == null) {
            return true;
        }
        int size = layout.tiles() * TargetView.PIXELS;
        int originX = layout.column(index) * TargetView.PIXELS;
        int originY = layout.row(index) * TargetView.PIXELS;
        for (int py = 0; py < TargetView.PIXELS; py++) {
            int start = (originY + py) * size + originX;
            if (!Arrays.equals(canvas, start, start + TargetView.PIXELS, pushed, start, start + TargetView.PIXELS)) {
                return true;
            }
        }
        return false;
    }

    /** The canvas the renderers copy from. */
    private byte[] canvas() {
        return canvas;
    }

    /** True when every chunk the wall reaches is loaded. */
    private boolean chunksLoaded(World world) {
        for (int cx = layout.minChunkX(); cx <= layout.maxChunkX(); cx++) {
            for (int cz = layout.minChunkZ(); cz <= layout.maxChunkZ(); cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** A filled map that shows the view. */
    private static ItemStack mapItem(MapView view) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) item.getItemMeta();
        meta.setMapView(view);
        item.setItemMeta(meta);
        return item;
    }
}
