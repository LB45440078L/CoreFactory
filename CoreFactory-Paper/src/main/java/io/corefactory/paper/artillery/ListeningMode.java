package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.CommandCenterSettings;
import io.corefactory.core.text.Text;
import io.corefactory.paper.TickScheduler;
import io.corefactory.paper.artillery.TerrainCache.ChunkId;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.map.MapPalette;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The Set Target mode: a player looks at the Command Center's map wall, and the ray of their view is traced onto the map. The map is drawn on
 * the wall, which every player near it sees, so the buttons are shown to all of them while the mode is open. A click on a button of the map (the
 * button under the player's view) zooms, moves the centre, opens the coordinates dialog, confirms or cancels.
 *
 * <p>Confirm stores the view's centre as the aim, with the center's elevation, on the center and its connected pieces, and the strike map stays
 * on the wall for everyone. Cancel stores nothing. A session also ends on its time limit, when its player quits, dies, changes world, or when
 * the center's chunk unloads, and on the plugin's shutdown. The tick action runs only while a session is open.
 *
 * <p>The terrain of a chunk that is not loaded comes from the shared {@link TerrainLoader}: an explored chunk is read without being generated,
 * and an unexplored one is generated, with a loading tile while it is. The map is drawn again at the loading tile's cadence while a tile of the
 * view is loading, and otherwise only when the view or the buttons change.
 */
public final class ListeningMode implements TargetingMode, Listener {

    /** The name of the tick action, which runs while at least one session is open. */
    static final String ACTION = "artillery-targeting";

    /** The ticks after which the terrain of the map is read again, even when the view has not moved, so a block change shows. */
    static final long REBUILD_TICKS = 100L;

    private static final ClickCallback.Options OPTIONS = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES)
            .lifetime(ClickCallback.DEFAULT_LIFETIME)
            .build();

    private static final int BUTTON_WIDTH = 200;

    private final CommandCenterService service;
    private final CommandCenterSettings settings;
    private final TerrainLoader terrains;
    private final Logger log;
    private final Map<UUID, TargetSession> sessions = new HashMap<>();
    private TickScheduler ticks;
    private long tickCount;

    /**
     * @param service  the Command Centers' runtime, whose walls show the map
     * @param settings the Command Center settings: the time limit, the map radius and the zoom levels
     * @param terrains the terrain of the chunks the maps show, shared by every session
     * @param log      receives the errors of the map
     */
    public ListeningMode(CommandCenterService service, CommandCenterSettings settings, TerrainLoader terrains, Logger log) {
        this.service = service;
        this.settings = settings;
        this.terrains = terrains;
        this.log = log;
    }

    /**
     * Gives the mode the tick scheduler its action is registered with. Call once, before any session begins.
     *
     * @param ticks the plugin's tick scheduler
     */
    public void attach(TickScheduler ticks) {
        this.ticks = ticks;
    }

    /** True while the player has a session open. */
    public boolean isTargeting(UUID player) {
        return sessions.containsKey(player);
    }

    @Override
    public void begin(Player player, CommandCenter center) {
        UUID id = player.getUniqueId();
        for (TargetSession other : List.copyOf(sessions.values())) {
            if (other.player.equals(id) || other.center.id().equals(center.id())) {
                end(other, false);
            }
        }
        World world = Bukkit.getWorld(center.world());
        Location block = service.centers().blockOf(center);
        if (world == null || block == null || !service.centers().isLoaded(center) || service.centers().byId(center.id()).isEmpty()) {
            player.sendMessage(Text.parse("&cThe command center is not loaded."));
            return;
        }
        TargetView start = startView(player, center);
        double dirX = 0;
        double dirZ = 0;
        List<ArtilleryDevice> pieces = service.connected(center);
        if (!pieces.isEmpty()) {
            ArtilleryDevice piece = pieces.get(0);
            double dx = start.centreX() - (piece.pos().x() + 0.5);
            double dz = start.centreZ() - (piece.pos().z() + 0.5);
            double length = Math.hypot(dx, dz);
            if (length > 0) {
                dirX = dx / length;
                dirZ = dz / length;
            }
        }
        TargetSession session = new TargetSession(id, center, center.shell(), center.tier(), center.elevation(), start, dirX, dirZ,
                System.currentTimeMillis());
        sessions.put(id, session);
        service.setTargeting(center, true);
        if (sessions.size() == 1 && ticks != null) {
            ticks.register(ACTION, 1, this::tick);
        }
        player.sendMessage(Text.parse("&7Look at the map on the command center. Click the green tick to set the target, or the X to cancel."));
    }

    /**
     * One tick of the open sessions: a session whose player is gone, whose center is gone or whose time is up ends; the others trace the
     * player's view onto the wall's map, and the map is drawn again when it changed.
     */
    void tick() {
        tickCount++;
        terrains.tick(tickCount);
        long now = System.currentTimeMillis();
        for (TargetSession session : List.copyOf(sessions.values())) {
            Player player = Bukkit.getPlayer(session.player);
            if (player == null || !player.isOnline()) {
                end(session, false);
                continue;
            }
            if (!live(session.center) || !session.center.world().equals(player.getWorld().getUID())) {
                end(session, false);
                continue;
            }
            if (TargetingTimeout.expired(session.lastActivityMillis, now, settings.listenTimeoutSeconds())) {
                player.sendMessage(Text.parse("&7Targeting timed out."));
                end(session, false);
                continue;
            }
            session.hover = buttonUnder(player, session.center).orElse(null);
            service.syncWall(session.center);
            try {
                redraw(session);
            } catch (IllegalStateException e) {
                log.warning("The map of a command center could not be drawn: " + e.getMessage());
            }
        }
        if (sessions.isEmpty() && ticks != null) {
            ticks.remove(ACTION);
        }
    }

    /**
     * Draws a session's map on the wall: the terrain again when the view moved, it is old, or it has loading tiles that are due a step, then the
     * strike area and the buttons over it. The map is drawn again only when the view, the hovered button or the terrain changed.
     */
    private void redraw(TargetSession session) {
        World world = Bukkit.getWorld(session.center.world());
        if (world == null) {
            return;
        }
        double bpp = blocksPerPixel(session.view);
        boolean animationDue = session.terrainLoading && tickCount - session.terrainTick >= LoadingAnimation.STEP_TICKS;
        if (session.terrain == null || !session.view.equals(session.terrainView) || session.terrainBpp != bpp
                || tickCount - session.terrainTick >= REBUILD_TICKS || animationDue) {
            TargetSurface surface = new TargetSurface(world, terrains, tickCount);
            session.terrainLoading = surface.request(session.view, bpp, tickCount);
            session.terrain = TargetFrame.terrain(surface, session.view, bpp, MapPalette.DARK_GRAY);
            session.terrainView = session.view;
            session.terrainBpp = bpp;
            session.terrainTick = tickCount;
        }
        List<Object> key = List.of(session.view.centreX(), session.view.centreZ(), session.view.zoomIndex(),
                String.valueOf(session.hover), session.terrainTick);
        if (session.frame != null && key.equals(session.frameKey)) {
            return;
        }
        TargetOverlay overlay = session.shell == null ? null : overlayOf(session);
        session.frame = TargetFrame.compose(session.terrain, session.view, bpp, overlay, true, session.hover);
        session.frameKey = key;
        service.showTargeting(session.center, session.frame);
    }

    /** The strike area of a session's shell at its view's centre, or null when the shell's layout cannot be laid out. */
    private TargetOverlay overlayOf(TargetSession session) {
        try {
            return TargetOverlay.of(service.artillery().settings(), service.artillery().rules(), session.shell,
                    session.view.centreX(), session.view.centreZ(), session.dirX, session.dirZ, session.center.id().hashCode());
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /** A click on the map, by a session's player: the button under their view acts. Any click resets the time limit. */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        TargetSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || event.getAction() == Action.PHYSICAL) {
            return;
        }
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        click(event.getPlayer(), session);
    }

    /** A click on an entity by a session's player is a click on the map, and is not an interaction with the entity. */
    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        TargetSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) {
            click(event.getPlayer(), session);
        }
    }

    /** A left click on a frame of the wall or on the screen by a session's player is a click on the map, and does not break the center. */
    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        TargetSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getEntity() instanceof Interaction || service.centers().isWallEntity(event.getEntity())) {
            click(player, session);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        TargetSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null) {
            end(session, false);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        TargetSession session = sessions.get(event.getEntity().getUniqueId());
        if (session != null) {
            end(session, false);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        TargetSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null) {
            end(session, false);
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (TargetSession session : List.copyOf(sessions.values())) {
            CommandCenter center = session.center;
            if (center.world().equals(event.getWorld().getUID()) && center.pos().x() >> 4 == event.getChunk().getX()
                    && center.pos().z() >> 4 == event.getChunk().getZ()) {
                end(session, false);
            }
        }
        Chunk chunk = event.getChunk();
        World world = event.getWorld();
        terrains.refresh(new ChunkId(world.getUID(), chunk.getX(), chunk.getZ()), () -> TargetSurface.sample(world, chunk));
    }

    /** A world that unloads takes its kept terrain with it. */
    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        terrains.forget(event.getWorld().getUID());
    }

    /** Ends every session at shutdown, without storing a target. */
    public void shutdown() {
        for (TargetSession session : List.copyOf(sessions.values())) {
            end(session, false);
        }
    }

    /**
     * A click: resets the time limit, and the button under the player's view acts. The events of one click, which may come as more than one
     * event in a tick, count once.
     */
    private void click(Player player, TargetSession session) {
        session.lastActivityMillis = System.currentTimeMillis();
        if (session.lastClickTick == tickCount) {
            return;
        }
        session.lastClickTick = tickCount;
        Optional<TargetButton> button = buttonUnder(player, session.center);
        if (button.isEmpty()) {
            return;
        }
        int levels = settings.zoomLevelsBlocks().size();
        World world = Bukkit.getWorld(session.center.world());
        double span = TargetView.spanBlocks(blocksPerPixel(session.view)) * TargetView.STEP_FRACTION;
        switch (button.get()) {
            case ZOOM_IN -> session.view = session.view.zoomedIn(levels);
            case ZOOM_OUT -> session.view = session.view.zoomedOut(levels);
            case NORTH -> session.view = session.view.moved(0, -span);
            case SOUTH -> session.view = session.view.moved(0, span);
            case WEST -> session.view = session.view.moved(-span, 0);
            case EAST -> session.view = session.view.moved(span, 0);
            case COORDINATES -> coordinates(player, session, world);
            case CONFIRM -> end(session, true);
            case CANCEL -> end(session, false);
        }
        if (world != null && button.get() != TargetButton.CONFIRM && button.get() != TargetButton.CANCEL
                && button.get() != TargetButton.COORDINATES) {
            session.view = clampToBorder(session.view, world);
        }
    }

    /** The coordinates dialog: two numbers, which set the centre of the map when the player submits them. */
    private void coordinates(Player player, TargetSession session, World world) {
        WorldBorder border = world == null ? null : world.getWorldBorder();
        float half = border == null ? 0f : (float) (border.getSize() / 2);
        float minX = border == null ? -30_000_000f : (float) border.getCenter().getX() - half;
        float maxX = border == null ? 30_000_000f : (float) border.getCenter().getX() + half;
        float minZ = border == null ? -30_000_000f : (float) border.getCenter().getZ() - half;
        float maxZ = border == null ? 30_000_000f : (float) border.getCenter().getZ() + half;
        List<DialogInput> inputs = List.of(
                DialogInput.numberRange("x", Text.parse("X"), minX, maxX).width(BUTTON_WIDTH).labelFormat("%s")
                        .initial((float) session.view.centreX()).step(1f).build(),
                DialogInput.numberRange("z", Text.parse("Z"), minZ, maxZ).width(BUTTON_WIDTH).labelFormat("%s")
                        .initial((float) session.view.centreZ()).step(1f).build());
        DialogActionCallback submit = (view, audience) -> {
            Float x = view == null ? null : view.getFloat("x");
            Float z = view == null ? null : view.getFloat("z");
            if (x == null || z == null || sessions.get(session.player) != session) {
                return;
            }
            session.lastActivityMillis = System.currentTimeMillis();
            session.view = clampToBorder(new TargetView(x, z, session.view.zoomIndex()), world);
        };
        ActionButton go = ActionButton.builder(Text.parse("Go to"))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(submit, OPTIONS))
                .build();
        ActionButton back = ActionButton.builder(Text.parse("Back"))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick((view, audience) -> {
                }, OPTIONS))
                .build();
        List<DialogBody> body = List.of(DialogBody.plainMessage(Text.parse("<muted>Set the centre of the map on the ground.</muted>")));
        player.showDialog(Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Text.parse("<accent>Coordinates</accent>")).canCloseWithEscape(true).body(body).inputs(inputs)
                        .build())
                .type(DialogType.multiAction(List.of(go)).exitAction(back).columns(1).build())));
    }

    /**
     * Ends a session: stores the target when asked, and takes the wall back to the strike, or away when the center has no target. The time
     * limit's message is sent before this.
     *
     * @param session the session
     * @param store   true to store the view's centre as the target
     */
    private void end(TargetSession session, boolean store) {
        if (sessions.remove(session.player) == null) {
            return;
        }
        if (sessions.isEmpty() && ticks != null) {
            ticks.remove(ACTION);
        }
        CommandCenter center = session.center;
        service.setTargeting(center, false);
        if (store && live(center)) {
            service.setStrike(center, session.view.centreX(), session.view.centreZ(), session.elevation);
        }
        service.centers().byId(center.id()).ifPresent(service::refresh);
    }

    /** The view a session opens on: the aim when the center has one, otherwise the player's own position, at the closest zoom. */
    private TargetView startView(Player player, CommandCenter center) {
        if (center.hasAim()) {
            return new TargetView(center.aimX(), center.aimZ(), 0);
        }
        return new TargetView(player.getX(), player.getZ(), 0);
    }

    /** The view with its centre held inside the world border. */
    private static TargetView clampToBorder(TargetView view, World world) {
        if (world == null) {
            return view;
        }
        WorldBorder border = world.getWorldBorder();
        double half = border.getSize() / 2;
        double cx = border.getCenter().getX();
        double cz = border.getCenter().getZ();
        return view.clampedTo(cx - half, cx + half, cz - half, cz + half);
    }

    private double blocksPerPixel(TargetView view) {
        return TargetView.blocksPerPixel(settings.zoomLevelsBlocks().get(view.zoomIndex()), settings.mapRadiusBlocks());
    }

    /** The button under the player's view, on the wall of the center: the ray of the eye, cut by the wall's plane. */
    private Optional<TargetButton> buttonUnder(Player player, CommandCenter center) {
        WallLayout layout = service.centers().wall(center).layout();
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection();
        return layout.pixelUnder(eye.getX(), eye.getY(), eye.getZ(), look.getX(), look.getY(), look.getZ())
                .flatMap(pixel -> layout.buttonAt(pixel[0], pixel[1]));
    }

    private boolean live(CommandCenter center) {
        return service.centers().byId(center.id()).isPresent() && service.centers().isLoaded(center);
    }
}
