package io.corefactory.paper.artillery;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.artillery.CommandCenterSettings;
import io.corefactory.core.artillery.FiringVerdict;
import io.corefactory.core.artillery.Position;
import io.corefactory.core.energy.GridResult;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.text.Text;
import io.corefactory.paper.chunks.ChunkKey;
import io.corefactory.paper.energy.EnergyService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * The runtime of the Command Centers: placement, the settings a center holds, the artillery it commands, its safety check, its launch, and its
 * screen.
 *
 * <p>The artillery connected to a center are the artillery of its owner that stand in the same energy grid. A grid is found for a block with
 * {@link EnergyService#gridCovering}: the smallest id of the grids whose pylons cover the block. A center draws no power of its own, so its
 * screen reads the satisfaction of its grid, from {@link EnergyService#grid}, rather than its own consumer reading, which is 1 with no demand.
 */
public final class CommandCenterService {

    private final CoreFactoryCore core;
    private final ArtilleryService artillery;
    private final CommandCenterRegistry centers;
    private final EnergyService energy;
    private final int refreshTicks;

    /** The ids of the centers that a targeting session is open on. Their walls are placed only while a session is open. */
    private final Set<String> targeted = new HashSet<>();

    private long ticks;

    /**
     * @param core      the core, for the item a center gives back
     * @param artillery the artillery runtime, which saves the centers with the pieces and checks the firing rules
     * @param centers   the centers and their entities
     * @param energy    the energy network, for the grids
     * @param settings  the Command Center's settings, for the refresh of the screen
     */
    public CommandCenterService(CoreFactoryCore core, ArtilleryService artillery, CommandCenterRegistry centers, EnergyService energy,
                                CommandCenterSettings settings) {
        this.core = core;
        this.artillery = artillery;
        this.centers = centers;
        this.energy = energy;
        this.refreshTicks = Math.max(1, settings.screenRefreshTicks());
    }

    /** The pieces' runtime the centers command. */
    public ArtilleryService artillery() {
        return artillery;
    }

    /** The centers and their entities. */
    public CommandCenterRegistry centers() {
        return centers;
    }

    /**
     * Stores a target on a center and on its connected pieces, and saves. The pieces take the aim and the elevation, and each one is checked
     * with the firing rules when it fires; no rule runs here. The target area of the aim is held from now on, and the area of the previous
     * target is let go. The map wall is taken away once the target is set: the target stays on the center and on its screen.
     *
     * @param center    the center
     * @param aimX      the x of the aim point, in blocks
     * @param aimZ      the z of the aim point, in blocks
     * @param elevation the barrel's elevation, in degrees
     */
    public void setStrike(CommandCenter center, double aimX, double aimZ, double elevation) {
        center.setAim(aimX, aimZ);
        center.setElevation(elevation);
        for (ArtilleryDevice piece : connected(center)) {
            piece.setAim(aimX, aimZ, elevation);
        }
        artillery.chunks().holdTarget(center);
        centers.applyStrikeView(center);
        syncWall(center);
        artillery.save();
        refresh(center);
    }

    /**
     * Clears a center's target, and saves: the strike map leaves the wall, and the black screen returns. The target area is let go. The pieces
     * keep the target they were last given.
     *
     * @param center the center
     */
    public void clearStrike(CommandCenter center) {
        center.setAim(Double.NaN, Double.NaN);
        artillery.chunks().releaseTarget(center.id());
        centers.applyStrikeView(center);
        syncWall(center);
        artillery.save();
        refresh(center);
    }

    /**
     * Marks a center as being targeted, or not. While it is, its map wall is placed and shows the targeting map; when it stops, the wall is
     * taken away.
     *
     * @param center the center
     * @param on     true when a player's targeting session on the center begins; false when it ends
     */
    void setTargeting(CommandCenter center, boolean on) {
        if (on) {
            targeted.add(center.id());
        } else {
            targeted.remove(center.id());
        }
        syncWall(center);
    }

    /** Places a center's map wall while a targeting session is open on it, and takes it away when none is. */
    void syncWall(CommandCenter center) {
        if (targeted.contains(center.id())) {
            centers.placeWall(center);
        } else {
            centers.removeWall(center);
        }
    }

    /** Shows the targeting map of a session on a center's wall. */
    void showTargeting(CommandCenter center, byte[] frame) {
        centers.showWall(center, frame);
    }

    /**
     * Fires the strike of a center from every connected piece. Each piece is asked to fire the center's shell and charge tier at the center's aim
     * and elevation, from its magazine: a piece whose magazine is empty, or holds other ammunition, is refused with its reason. The owner gets one
     * summary line, then one line for each refused piece. Nothing happens when the center has no aim. When the target area is not loaded, the
     * launch is refused with one line that says whether the area is loading. A nuclear strike is refused as a whole when its safety check is not OK.
     *
     * @param player the player who fires: the owner, or a friend of the owner
     * @param center the center
     */
    public void launch(Player player, CommandCenter center) {
        if (!center.hasAim()) {
            return;
        }
        ArtilleryShell shell = center.shell();
        if (shell == null) {
            player.sendMessage(Text.parse("&cChoose a shell first."));
            return;
        }
        List<ArtilleryDevice> pieces = connected(center);
        if (pieces.isEmpty()) {
            player.sendMessage(Text.parse("&cNo artillery is connected to this command center."));
            return;
        }
        World world = Bukkit.getWorld(center.world());
        if (world == null || !aimLoaded(center, world)) {
            boolean held = artillery.chunks().isHeld(ArtilleryChunks.aimChunk(center.world(), center.aimX(), center.aimZ()));
            player.sendMessage(Text.parse(ArtilleryVerdicts.aimNotLoaded(held,
                    (int) Math.floor(center.aimX()), (int) Math.floor(center.aimZ()))));
            return;
        }
        Position at = new Position(player.getX(), player.getY(), player.getZ());
        if (shell.nuclear()) {
            Safety safety = safety(center, pieces, at);
            if (!safety.ok()) {
                player.sendMessage(Text.parse(safety.message()));
                return;
            }
        }
        long now = System.currentTimeMillis();
        List<LaunchSummary.Attempt> attempts = new ArrayList<>();
        for (ArtilleryDevice piece : pieces) {
            ArtilleryService.ShotResult result = artillery.request(piece, shell, center.tier(), center.aimX(), center.aimZ(),
                    center.elevation(), at, player.getUniqueId(), now);
            attempts.add(new LaunchSummary.Attempt(label(piece), result));
        }
        for (String line : LaunchSummary.of(attempts).lines()) {
            player.sendMessage(Text.parse(line));
        }
        refresh(center);
    }

    /**
     * Places a center on a block, and saves.
     *
     * @param block the block it stands in, on air
     * @param owner the player who placed it
     * @return the center
     */
    public CommandCenter place(Location block, UUID owner) {
        CommandCenter center = centers.add(new CommandCenter(UUID.randomUUID().toString(), owner, block.getWorld().getUID(),
                new Pos(block.getBlockX(), block.getBlockY(), block.getBlockZ())));
        artillery.save();
        return center;
    }

    /**
     * Removes a center, and saves.
     *
     * @return the item it gives back: the command center's item
     */
    public List<ItemStack> remove(CommandCenter center) {
        List<ItemStack> drops = new ArrayList<>();
        ComponentDefinition definition = core.componentDefinitions().get(ArtilleryIds.COMMAND_CENTER);
        if (definition != null) {
            drops.add(core.items().component(definition, 1));
        }
        centers.remove(center);
        targeted.remove(center.id());
        artillery.chunks().releaseTarget(center.id());
        artillery.save();
        return drops;
    }

    /** Sets the shell and the charge tier of a center's strike, and saves. */
    public void setLoad(CommandCenter center, ArtilleryShell shell, ChargeTier tier) {
        center.setLoad(shell, tier);
        artillery.save();
    }

    /** Sets the aim point of a center's strike on the ground, and saves. */
    public void setAim(CommandCenter center, double aimX, double aimZ) {
        center.setAim(aimX, aimZ);
        artillery.chunks().holdTarget(center);
        artillery.save();
    }

    /** Gives a center its name, and saves. A blank name keeps the old one, and a long one is cut. */
    public void rename(CommandCenter center, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String trimmed = name.trim();
        center.setName(trimmed.length() > ArtilleryDevice.MAX_NAME_LENGTH
                ? trimmed.substring(0, ArtilleryDevice.MAX_NAME_LENGTH) : trimmed);
        artillery.save();
    }

    /**
     * The artillery connected to a center: the pieces of its owner that stand in the same energy grid. A center on no grid has none.
     *
     * @param center the center
     * @return the pieces, in the order the registry holds them; empty when the center is on no grid
     */
    public List<ArtilleryDevice> connected(CommandCenter center) {
        OptionalInt grid = gridAt(center.world(), center.pos());
        List<ArtilleryDevice> found = new ArrayList<>();
        if (grid.isEmpty()) {
            return found;
        }
        for (ArtilleryDevice piece : artillery.registry().all()) {
            if (piece.owner().equals(center.owner()) && piece.world().equals(center.world())
                    && grid.equals(gridAt(piece.world(), piece.pos()))) {
                found.add(piece);
            }
        }
        return found;
    }

    /** The highest level among pieces, or 0 when there are none. */
    public static int highestLevel(List<ArtilleryDevice> pieces) {
        return pieces.stream().mapToInt(piece -> piece.level().number()).max().orElse(0);
    }

    /** The charge tiers the pieces have loaded: empty when there are none. */
    public static Set<ChargeTier> loadedTiers(List<ArtilleryDevice> pieces) {
        Set<ChargeTier> tiers = EnumSet.noneOf(ChargeTier.class);
        for (ArtilleryDevice piece : pieces) {
            tiers.add(piece.tier());
        }
        return tiers;
    }

    /** The power the pieces need together, in W: the sum of their levels' powers. */
    public double wattsNeeded(List<ArtilleryDevice> pieces) {
        return pieces.stream().mapToDouble(piece -> artillery.settings().powerOf(piece.level())).sum();
    }

    /**
     * The grid's satisfaction at a center: from the grid's last solve, 0 to 1. NaN when the center is on no grid.
     *
     * @param center the center
     */
    public double satisfaction(CommandCenter center) {
        OptionalInt grid = gridAt(center.world(), center.pos());
        if (grid.isEmpty()) {
            return Double.NaN;
        }
        return energy.grid(grid.getAsInt()).map(GridResult::satisfaction).orElse(Double.NaN);
    }

    /**
     * The safety check of a center's current settings, against the first connected piece. The check is the firing rules' verdict, with the
     * center's shell, charge tier, aim and elevation. A check the rules have not reached is refused with its reason.
     *
     * @param center    the center
     * @param connected the pieces connected to it, from {@link #connected}
     * @param firer     where the player who fires stands, for the check of the blast against that player
     * @return the result
     */
    public Safety safety(CommandCenter center, List<ArtilleryDevice> connected, Position firer) {
        if (center.shell() == null) {
            return new Safety(false, "no shell chosen", "&cChoose a shell first.");
        }
        if (!center.hasAim()) {
            return new Safety(false, "no target set", "&cSet a target first.");
        }
        if (connected.isEmpty()) {
            return new Safety(false, "no artillery connected", "&cNo artillery is connected to this command center.");
        }
        ArtilleryDevice piece = connected.get(0);
        World world = Bukkit.getWorld(piece.world());
        if (!artillery.registry().isLoaded(piece) || world == null) {
            boolean held = artillery.chunks().isHeld(ArtilleryChunks.pieceChunk(piece));
            return new Safety(false, "piece not loaded",
                    ArtilleryVerdicts.pieceNotLoaded(held, piece.pos().x(), piece.pos().y(), piece.pos().z()));
        }
        if (!aimLoaded(center, world)) {
            boolean held = artillery.chunks().isHeld(ArtilleryChunks.aimChunk(center.world(), center.aimX(), center.aimZ()));
            return new Safety(false, held ? "target loading" : "target not loaded",
                    ArtilleryVerdicts.aimNotLoaded(held, (int) Math.floor(center.aimX()), (int) Math.floor(center.aimZ())));
        }
        FiringVerdict verdict = artillery.verdict(piece, center.shell(), center.tier(), center.aimX(), center.aimZ(),
                center.elevation(), firer, center.owner(), System.currentTimeMillis());
        if (verdict.ok()) {
            return new Safety(true, "OK", "&aThe strike is safe: the first connected piece may fire it.");
        }
        return new Safety(false, CommandCenterScreen.reason(verdict), ArtilleryVerdicts.message(verdict));
    }

    /**
     * Runs the safety check for the player who asks, and tells that player the result in chat, then shows it on the screen.
     *
     * @param center the center
     * @param player the player who asks: the owner, or a friend of the owner
     * @return the result
     */
    public Safety checkFor(CommandCenter center, Player player) {
        Safety result = safety(center, connected(center), new Position(player.getX(), player.getY(), player.getZ()));
        player.sendMessage(Text.parse(result.message()));
        refresh(center);
        return result;
    }

    /** The screen's values for a center, from its settings, its grid and the check. */
    public CommandCenterScreen.Readout readout(CommandCenter center) {
        List<ArtilleryDevice> connected = connected(center);
        Safety safety = safety(center, connected, viewerOf(center));
        return new CommandCenterScreen.Readout(center.aimX(), center.aimZ(),
                center.shell() == null ? null : ShellMenu.shellName(center.shell()), ShellMenu.tierNumeral(center.tier()),
                center.elevation(), connected.size(), wattsNeeded(connected), satisfaction(center), safety.label(), safety.ok());
    }

    /**
     * One server tick: every {@code screen-refresh-ticks} ticks, every loaded center's screen is refreshed.
     */
    public void tick() {
        ticks++;
        if (ticks % refreshTicks == 0) {
            for (CommandCenter center : centers.all()) {
                if (centers.isLoaded(center)) {
                    refresh(center);
                }
            }
        }
    }

    /** Shows a center's readout on its screen now. The text is written only when it changes. */
    public void refresh(CommandCenter center) {
        centers.show(center, CommandCenterScreen.lines(readout(center)));
    }

    /** The grid covering a block, or empty when no grid covers it. */
    private OptionalInt gridAt(UUID world, Pos pos) {
        return energy.gridCovering(world, pos);
    }

    /** True when the chunk of a center's aim is loaded in the given world. */
    private static boolean aimLoaded(CommandCenter center, World world) {
        ChunkKey aim = ArtilleryChunks.aimChunk(center.world(), center.aimX(), center.aimZ());
        return world.isChunkLoaded(aim.x(), aim.z());
    }

    /** The name of a piece and its block, as the launch summary names it. */
    private static String label(ArtilleryDevice piece) {
        return piece.name() + " at " + piece.pos().x() + ", " + piece.pos().y() + ", " + piece.pos().z();
    }

    /** Where the owner stands, for the check of the blast against the player; the center's own block when the owner is offline. */
    private Position viewerOf(CommandCenter center) {
        Player owner = Bukkit.getPlayer(center.owner());
        if (owner != null) {
            return new Position(owner.getX(), owner.getY(), owner.getZ());
        }
        return new Position(center.pos().x() + 0.5, center.pos().y(), center.pos().z() + 0.5);
    }

    /**
     * The result of a safety check.
     *
     * @param ok      true when the strike may be fired
     * @param label   the short words for the screen: OK, or the reason
     * @param message the line for the chat of the player who asks, with its colour codes
     */
    public record Safety(boolean ok, String label, String message) {
    }
}
