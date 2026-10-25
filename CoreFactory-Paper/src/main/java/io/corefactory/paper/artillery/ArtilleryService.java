package io.corefactory.paper.artillery;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.artillery.ArtilleryFiringRules;
import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.artillery.ArtilleryMagazine;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ArtillerySettings;
import io.corefactory.core.artillery.ArtilleryTable;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.artillery.FiringVerdict;
import io.corefactory.core.artillery.GroundPoint;
import io.corefactory.core.artillery.Position;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.paper.chunks.ChunkKey;
import io.corefactory.paper.energy.EnergyService;
import io.corefactory.paper.energy.EnergySnapshots;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The runtime of the artillery pieces: placement, power, the magazines, and the firing of shots. A piece is a consumer of the energy network: it
 * draws its power while it fires or reloads, and a small standby demand otherwise, so the network keeps it in view. A piece fires only from
 * its magazine: each shot uses one shell and one charge, and a magazine that lacks either refuses the shot. A shot is then checked by
 * {@link ArtilleryFiringRules} with the values of the piece, the player and the server, and a refused shot gives its reason back and does
 * nothing else. An accepted shot is planned, then flown one tick at a time by {@link #tickShots}, and lands with its shell's effect.
 *
 * <p>The piece's reload starts at its shot, and its shots are counted by the time of the shot, so a reload runs across a restart. The
 * Command Centers are saved in the same file, and their entities are kept by the same tick.
 */
public final class ArtilleryService implements EnergyService.ConsumerSource {

    /** How often the entities of the pieces are made again, when their chunks are loaded, in ticks. */
    private static final int ENSURE_TICKS = 20;

    /** How far in front of the barrel's trunnion the muzzle flash and the shell's start are, in blocks: about the length of the barrel's tip. */
    private static final double MUZZLE_OFFSET_BLOCKS = 0.75;

    private final CoreFactoryCore core;
    private final ArtilleryRegistry registry;
    private final CommandCenterRegistry centers;
    private final EnergyService energy;
    private final ArtillerySettings settings;
    private final ArtilleryFiringRules rules;
    private final ArtilleryTable table;
    private final ArtilleryStore store;
    private final ArtilleryImpacts impacts;
    private final ArtilleryChunks chunks;
    private final ArtilleryShells flights;
    private final MagazineItems items;
    private final Map<UUID, Long> antimatterShots;
    private final List<ArtilleryShot> shots = new ArrayList<>();
    /** The flight ticket of each shot in the air, by the shot's identity: released when the shot lands. */
    private final Map<ArtilleryShot, String> flightTickets = new IdentityHashMap<>();
    private final Random random;
    private final int saveTicks;
    private final Logger log;
    private long ticks;
    private long ticketSerial;

    /**
     * @param core            the core, for the items and the TNT manager
     * @param registry        the pieces and their entities
     * @param centers         the Command Centers and their entities, saved in the same file
     * @param energy          the energy network, which gives each piece its satisfaction
     * @param settings        the artillery settings
     * @param store           the file the pieces are saved in
     * @param impacts         what a shell does where it lands
     * @param chunks          the chunks the pieces, the centers and their target areas hold
     * @param antimatterShots the time of each player's last antimatter shot, in milliseconds, by unique id; kept and saved by the service
     * @param random          the random numbers of the shots; a seeded one makes the shots repeatable
     * @param saveTicks       how often the pieces are saved, in ticks: the save interval of the core
     * @param log             receives the dropped shots
     */
    public ArtilleryService(CoreFactoryCore core, ArtilleryRegistry registry, CommandCenterRegistry centers, EnergyService energy,
                            ArtillerySettings settings, ArtilleryStore store, ArtilleryImpacts impacts, ArtilleryChunks chunks,
                            Map<UUID, Long> antimatterShots, Random random, int saveTicks, Logger log) {
        this.core = core;
        this.registry = registry;
        this.centers = centers;
        this.energy = energy;
        this.settings = settings;
        this.rules = new ArtilleryFiringRules(settings);
        this.table = new ArtilleryTable(settings);
        this.store = store;
        this.impacts = impacts;
        this.chunks = chunks;
        this.flights = new ArtilleryShells(core, settings.presentation());
        this.items = new MagazineItems(core);
        this.antimatterShots = new HashMap<>(antimatterShots);
        this.random = random;
        this.saveTicks = Math.max(1, saveTicks);
        this.log = log;
    }

    /** The settings the pieces run with. */
    public ArtillerySettings settings() {
        return settings;
    }

    /** The firing rules the shots are checked with. */
    public ArtilleryFiringRules rules() {
        return rules;
    }

    /** The pieces and their entities. */
    public ArtilleryRegistry registry() {
        return registry;
    }

    /** The Command Centers and their entities. */
    public CommandCenterRegistry centers() {
        return centers;
    }

    /** The displays of the shells in the air. */
    ArtilleryShells flights() {
        return flights;
    }

    /** The chunks the pieces, the centers and their target areas hold. */
    ArtilleryChunks chunks() {
        return chunks;
    }

    /**
     * Holds the chunks of every piece and every Command Center, so that they stay loaded whatever the device keeper holds. The holds are set
     * exactly to the chunks the pieces and centers stand in now: a removed piece or center loses its hold at the next call.
     */
    public void holdChunks() {
        Set<ChunkKey> keys = new HashSet<>(registry.chunkKeys());
        keys.addAll(centers.chunkKeys());
        chunks.holdPieces(keys);
    }

    /**
     * Places a piece on a block, and saves.
     *
     * @param block the block it stands in, on air
     * @param owner the player who placed it
     * @param level the level of its item
     * @return the piece
     */
    public ArtilleryDevice place(Location block, UUID owner, ArtilleryLevel level) {
        ArtilleryDevice piece = registry.add(new ArtilleryDevice(UUID.randomUUID().toString(), owner,
                block.getWorld().getUID(), new Pos(block.getBlockX(), block.getBlockY(), block.getBlockZ()), level));
        save();
        return piece;
    }

    /**
     * Removes a piece, and saves. Its shots in flight still land. The shells and the charges in its magazine go back with it, so they are not lost.
     *
     * @return the items it gives back: its item at its level, and the shells and charges of its magazine
     */
    public List<ItemStack> remove(ArtilleryDevice piece) {
        List<ItemStack> drops = new ArrayList<>();
        ComponentDefinition definition = core.componentDefinitions().get(ArtilleryIds.itemOf(piece.level()));
        if (definition != null) {
            drops.add(core.items().component(definition, 1));
        }
        drops.addAll(magazineItems(piece, piece.magazineShells(), piece.magazineCharges()));
        registry.remove(piece);
        save();
        return drops;
    }

    /** The magazine of a piece: its loaded shells and charges, at the capacity the settings give. */
    public ArtilleryMagazine magazine(ArtilleryDevice piece) {
        return new ArtilleryMagazine(settings.magazineCapacity(), piece.magazineShells(), piece.magazineCharges());
    }

    /**
     * Loads one shell and one charge into a piece's magazine, taken from the player's inventory, and saves. The piece is set to the shell and the
     * tier given, which are the ones the magazine then holds: a magazine holds one shell kind and one tier, so a load of another kind is refused
     * while the magazine holds anything. A load is refused when the magazine is full, or the player has no shell or no charge of that kind.
     *
     * @param piece  the piece to load
     * @param shell  the shell the command center has chosen
     * @param tier   the charge tier the command center has chosen
     * @param player the player whose inventory gives the shell and the charge
     * @return the result: loaded, or refused with the line that tells why
     */
    public LoadResult load(ArtilleryDevice piece, ArtilleryShell shell, ChargeTier tier, Player player) {
        ArtilleryMagazine magazine = magazine(piece);
        if (magazine.holdsAny() && (piece.shell() != shell || piece.tier() != tier)) {
            return LoadResult.refused("&cThe magazine holds other ammunition: unload it first.");
        }
        if (magazine.full()) {
            return LoadResult.refused(ArtilleryVerdicts.message(ArtilleryMagazine.Verdict.FULL));
        }
        String shellItem = ArtilleryIds.shellItemOf(shell);
        String chargeItem = ArtilleryIds.chargeItemOf(tier);
        if (items.count(player, shellItem) < 1) {
            return LoadResult.refused("&cYou carry no " + ShellMenu.shellName(shell) + " shell.");
        }
        if (items.count(player, chargeItem) < 1) {
            return LoadResult.refused("&cYou carry no Tier " + ShellMenu.tierNumeral(tier) + " charge.");
        }
        items.take(player, shellItem, 1);
        items.take(player, chargeItem, 1);
        piece.setShell(shell);
        piece.setTier(tier);
        magazine.load();
        storeMagazine(piece, magazine);
        save();
        return LoadResult.made("&7Loaded one shell and one charge: "
                + magazine.shells() + " shells and " + magazine.charges() + " charges, " + magazine.capacity() + " at most each.");
    }

    /**
     * Gives a piece's magazine back to the player, as items, and empties it, and saves. Anything that does not fit in the player's inventory is
     * dropped at the player's feet.
     *
     * @param piece  the piece to unload
     * @param player the player who gets the shells and the charges
     * @return the result: unloaded, or refused when the magazine is empty
     */
    public LoadResult unload(ArtilleryDevice piece, Player player) {
        ArtilleryMagazine magazine = magazine(piece);
        if (!magazine.holdsAny()) {
            return LoadResult.refused("&7The magazine is empty.");
        }
        ArtilleryMagazine.Contents given = magazine.unload();
        List<ItemStack> stacks = magazineItems(piece, given.shells(), given.charges());
        for (ItemStack left : player.getInventory().addItem(stacks.toArray(ItemStack[]::new)).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
        storeMagazine(piece, magazine);
        save();
        return LoadResult.made("&7Unloaded " + given.shells() + " shells and " + given.charges() + " charges.");
    }

    /** Switches a piece on or off, and saves. A piece that is off draws no power and fires nothing. */
    public void setOn(ArtilleryDevice piece, boolean on) {
        piece.setOn(on);
        save();
    }

    /** Gives a piece its name, and saves. The name is trimmed and cut to the longest name, and a blank name keeps the old one. */
    public void rename(ArtilleryDevice piece, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String trimmed = name.trim();
        piece.setName(trimmed.length() > ArtilleryDevice.MAX_NAME_LENGTH
                ? trimmed.substring(0, ArtilleryDevice.MAX_NAME_LENGTH) : trimmed);
        save();
    }

    /**
     * The verdict of the firing rules for a shot: the same check a shot passes before it is fired, with the charge and the elevation given.
     * The piece's world must be loaded, and so must the chunk of the aim point, which the caller checks.
     *
     * @param piece            the piece that would fire
     * @param shell            the shell
     * @param tier             the charge tier the shot is set for
     * @param aimX             the x of the aim point, in blocks
     * @param aimZ             the z of the aim point, in blocks
     * @param elevationDegrees the elevation of the barrel, in degrees
     * @param playerPosition   the position of the player who fires
     * @param playerUuid       the player who fires
     * @param nowMillis        the current time, in milliseconds
     * @return OK, or the first rule the shot breaks
     * @throws IllegalStateException when the piece's world is not loaded
     */
    public FiringVerdict verdict(ArtilleryDevice piece, ArtilleryShell shell, ChargeTier tier, double aimX, double aimZ,
                                 double elevationDegrees, Position playerPosition, UUID playerUuid, long nowMillis) {
        World world = Bukkit.getWorld(piece.world());
        if (world == null) {
            throw new IllegalStateException("the piece's world is not loaded");
        }
        double groundY = groundY(world, (int) Math.floor(aimX), (int) Math.floor(aimZ));
        OptionalLong last = antimatterShots.containsKey(playerUuid)
                ? OptionalLong.of(antimatterShots.get(playerUuid)) : OptionalLong.empty();
        ArtilleryFiringRules.Shot check = new ArtilleryFiringRules.Shot(shell, piece.level(), tier, elevationDegrees,
                launchOf(piece), new Position(aimX, groundY, aimZ), playerPosition, playersIn(world),
                energy.satisfaction(consumerId(piece)), last, nowMillis);
        return rules.check(check);
    }

    /**
     * Fires a shot from a piece at an aim point on the ground, if its magazine and the firing rules allow it. The magazine is checked first: the
     * piece must hold the shell and the tier asked for, and a shell and a charge. A refused shot changes nothing, and its reason is returned for
     * the caller to show. An accepted one uses one shell and one charge, is planned and sent into the air, and the piece is busy for its reload.
     *
     * @param piece            the piece that fires
     * @param shell            the shell to fire
     * @param tier             the charge tier the shot is fired with
     * @param aimX             the x of the aim point, in blocks
     * @param aimZ             the z of the aim point, in blocks
     * @param elevationDegrees the elevation of the barrel, in degrees
     * @param playerPosition   the position of the player who fires
     * @param playerUuid       the player who fires
     * @param nowMillis        the current time, in milliseconds
     * @return the outcome: fired, or refused with the reason
     */
    public ShotResult request(ArtilleryDevice piece, ArtilleryShell shell, ChargeTier tier, double aimX, double aimZ,
                              double elevationDegrees, Position playerPosition, UUID playerUuid, long nowMillis) {
        if (!piece.on()) {
            return ShotResult.refused("&cThe piece is switched off.");
        }
        if (busy(piece, nowMillis)) {
            return ShotResult.refused("&cThe piece is still reloading.");
        }
        World world = Bukkit.getWorld(piece.world());
        if (world == null || !registry.isLoaded(piece)) {
            return ShotResult.refused(ArtilleryVerdicts.pieceNotLoaded(chunks.isHeld(ArtilleryChunks.pieceChunk(piece)),
                    piece.pos().x(), piece.pos().y(), piece.pos().z()));
        }
        int aimBlockX = (int) Math.floor(aimX);
        int aimBlockZ = (int) Math.floor(aimZ);
        if (!world.isChunkLoaded(aimBlockX >> 4, aimBlockZ >> 4)) {
            return ShotResult.refused(ArtilleryVerdicts.aimNotLoaded(chunks.isHeld(ArtilleryChunks.aimChunk(piece.world(), aimX, aimZ)),
                    aimBlockX, aimBlockZ));
        }
        ArtilleryMagazine magazine = magazine(piece);
        if (magazine.holdsAny() && (piece.shell() != shell || piece.tier() != tier)) {
            return ShotResult.refused("&cThe magazine holds other ammunition: unload it first.");
        }
        ArtilleryMagazine.Verdict ammunition = magazine.check();
        if (!ammunition.ok()) {
            return ShotResult.refused(ArtilleryVerdicts.message(ammunition));
        }
        FiringVerdict verdict = verdict(piece, shell, tier, aimX, aimZ, elevationDegrees, playerPosition, playerUuid, nowMillis);
        if (!verdict.ok()) {
            return ShotResult.refused(ArtilleryVerdicts.message(verdict));
        }
        Position launch = launchOf(piece);
        double groundY = groundY(world, aimBlockX, aimBlockZ);
        ArtilleryPlan plan;
        ArtilleryImpactPlan impact;
        try {
            ArtilleryPlan aimed = ArtilleryPlan.of(settings, launch, aimX, aimZ, groundY, elevationDegrees);
            impact = ArtilleryImpactPlan.of(settings, shell, piece.level(), tier, new GroundPoint(aimX, aimZ),
                    aimed.distance(), aimed.dirX(), aimed.dirZ(), new Random(random.nextLong()));
            // The scatter moves the landing from the aim, and the blast goes off at the landing, so the shell is flown to the landing
            // itself: it comes down where it bursts. The ground under the landing is the ground under the aim when its chunk is not loaded.
            GroundPoint landing = impact.landing();
            double landingGroundY = loadedAt(world, landing.x(), landing.z())
                    ? groundY(world, (int) Math.floor(landing.x()), (int) Math.floor(landing.z())) : groundY;
            plan = ArtilleryPlan.of(settings, launch, landing.x(), landing.z(), landingGroundY, elevationDegrees);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warning("A shell from piece " + piece.id() + " could not be planned: " + e.getMessage());
            return ShotResult.refused("&cThe shell cannot reach that spot from here.");
        }
        magazine.fire();
        storeMagazine(piece, magazine);
        piece.setAim(aimX, aimZ, elevationDegrees);
        piece.setShell(shell);
        piece.setTier(tier);
        piece.setLastShotMillis(nowMillis);
        if (shell == ArtilleryShell.CONDENSED_ANTIMATTER) {
            antimatterShots.put(playerUuid, nowMillis);
        }
        ArtilleryShot shot = new ArtilleryShot(piece.id(), piece.world(), playerUuid, shell, piece.level(), tier, plan, impact);
        String flight = "shot " + (++ticketSerial);
        Set<ChunkKey> held = flightChunks(world.getUID(), plan);
        held.addAll(impacts.chunksTouched(world.getUID(), shell, impact));
        impacts.tickets().hold(flight, held);
        flightTickets.put(shot, flight);
        shots.add(shot);
        Position tip = barrelTip(piece);
        ArtilleryCues.launch(new Location(world, tip.x(), tip.y(), tip.z()), settings.presentation().launchVolume(),
                settings.presentation().launchPitch());
        registry.recoil(piece);
        save();
        return ShotResult.accepted();
    }

    /**
     * Every piece that is on takes power while it fires or reloads, at the power of its level; otherwise it draws nothing. A consumer with no
     * demand is still solved, and reads as fully satisfied, so a piece that is on and idle can take its first shot at once.
     */
    @Override
    public Map<UUID, List<Consumer>> consumers() {
        Map<UUID, List<Consumer>> byWorld = new HashMap<>();
        long now = System.currentTimeMillis();
        for (ArtilleryDevice piece : registry.all()) {
            if (!piece.on() || !registry.isLoaded(piece)) {
                continue;
            }
            double demand = busy(piece, now) ? settings.powerOf(piece.level()) : 0.0;
            Pos pos = piece.pos();
            byWorld.computeIfAbsent(piece.world(), key -> new ArrayList<>())
                    .add(new Consumer(consumerId(piece), pos, demand));
        }
        return byWorld;
    }

    /**
     * One server tick of the pieces: every {@link #ENSURE_TICKS} ticks their entities are made again when their chunks are loaded, their chunks
     * are held again, and the chunk tickets of the shells are placed again where one went missing. Each tick the poses of the pieces are shown,
     * and the pieces are saved on their interval.
     */
    public void tick() {
        ticks++;
        if (ticks % ENSURE_TICKS == 0) {
            registry.ensureEntities();
            centers.ensureEntities();
            holdChunks();
            impacts.tickets().ensureAll();
        }
        long now = System.currentTimeMillis();
        for (ArtilleryDevice piece : registry.all()) {
            boolean ready = registry.refresh(piece, busy(piece, now));
            if (ready && registry.isLoaded(piece)) {
                ArtilleryCues.reloaded(registry.blockOf(piece).add(0.5, 0.5, 0.5), settings.presentation().reloadVolume());
            }
        }
        if (ticks % saveTicks == 0) {
            save();
        }
    }

    /**
     * Moves every shot in flight one tick along its arc, and draws it. A shot keeps flying whatever its chunk: its flight chunks are held by
     * tickets, so a chunk found unloaded has lost its ticket with its world, and its tickets are placed again. A shell that lands is given
     * its effect, and its chunks are held until the effect has run.
     */
    public void tickShots() {
        Iterator<ArtilleryShot> iterator = shots.iterator();
        while (iterator.hasNext()) {
            ArtilleryShot shot = iterator.next();
            World world = Bukkit.getWorld(shot.world());
            if (world == null) {
                log.info("Artillery shot from piece " + shot.pieceId() + " dropped: its world is not loaded.");
                impacts.tickets().release(flightTickets.remove(shot));
                flights.hide(shot);
                iterator.remove();
                continue;
            }
            Position at = shot.position();
            if (loadedAt(world, at.x(), at.z())) {
                flights.fly(shot, world, shot.previous(), at);
            } else {
                flights.hide(shot);
                impacts.tickets().ensure(flightTickets.get(shot));
            }
            if (shot.landed()) {
                iterator.remove();
                flights.hide(shot);
                land(world, shot);
            } else {
                shot.advance();
            }
        }
    }

    /** Saves the pieces, the Command Centers and the antimatter times to the store file. */
    public void save() {
        store.write(new ArtilleryStore.Saved(registry.snapshot(), centers.snapshot(), antimatterShots));
    }

    /** Saves, releases the entities of every piece and every Command Center, and releases every chunk ticket. Called when the plugin stops. */
    public void shutdown() {
        save();
        registry.releaseAll();
        centers.releaseAll();
        flights.releaseAll();
        flightTickets.clear();
        impacts.tickets().releaseAll();
    }

    /**
     * A shot has landed. Its impact's chunks are held under a new id before the flight's are let go, so no chunk unloads between the two; then
     * its effect runs. A landing in a chunk that has unloaded is loaded first, and the effect runs when the chunk is in, so no shell is lost.
     */
    private void land(World world, ArtilleryShot shot) {
        impacts.hold(world, "impact " + (++ticketSerial), shot.shell(), shot.impact());
        impacts.tickets().release(flightTickets.remove(shot));
        GroundPoint at = shot.impact().landing();
        Player shooter = shot.shooter() == null ? null : Bukkit.getPlayer(shot.shooter());
        if (loadedAt(world, at.x(), at.z())) {
            impacts.land(world, shooter, shot.shell(), shot.impact());
            return;
        }
        log.info("Artillery shell from piece " + shot.pieceId() + " waits for its chunk to load before it lands.");
        world.getChunkAtAsync((int) Math.floor(at.x()) >> 4, (int) Math.floor(at.z()) >> 4, true,
                chunk -> impacts.land(world, shooter, shot.shell(), shot.impact()));
    }

    /**
     * The chunks a flight passes over, from its launch to its landing, sampled at every tick of the arc. The shot holds them while it is in the
     * air, so the flight is never cut by an unloaded chunk.
     */
    private static Set<ChunkKey> flightChunks(UUID world, ArtilleryPlan plan) {
        Set<ChunkKey> chunks = new HashSet<>();
        for (int tick = 0; tick <= plan.lastTick(); tick++) {
            Position at = plan.positionAt(tick);
            chunks.add(ChunkKey.of(world, (int) Math.floor(at.x()), (int) Math.floor(at.z())));
        }
        return chunks;
    }

    /** The shells and charges of a magazine as items: the shell and the tier the piece is loaded with. Empty when the piece has no shell. */
    private List<ItemStack> magazineItems(ArtilleryDevice piece, int shells, int charges) {
        List<ItemStack> stacks = new ArrayList<>();
        if (piece.shell() == null) {
            return stacks;
        }
        stacks.addAll(items.stacks(ArtilleryIds.shellItemOf(piece.shell()), shells));
        stacks.addAll(items.stacks(ArtilleryIds.chargeItemOf(piece.tier()), charges));
        return stacks;
    }

    /** Writes the counts of a magazine into its piece. The caller saves. */
    private static void storeMagazine(ArtilleryDevice piece, ArtilleryMagazine magazine) {
        piece.setMagazine(magazine.shells(), magazine.charges());
    }

    /** True while the piece is reloading, or while a shot of it is still in the air. */
    private boolean busy(ArtilleryDevice piece, long nowMillis) {
        return piece.reloadingAt(nowMillis, table.reloadTicks(piece.level(), piece.tier())) || inFlight(piece);
    }

    private boolean inFlight(ArtilleryDevice piece) {
        for (ArtilleryShot shot : shots) {
            if (shot.pieceId().equals(piece.id())) {
                return true;
            }
        }
        return false;
    }

    /** The point the barrel fires from: above the middle of the piece's block, at the top of its model. */
    private Position launchOf(ArtilleryDevice piece) {
        return new Position(piece.pos().x() + 0.5, piece.pos().y() + settings.heightOf(piece.level()), piece.pos().z() + 0.5);
    }

    /** The point at the barrel's tip, a little in front of the launch along the barrel: where the muzzle flash and the smoke are. */
    private Position barrelTip(ArtilleryDevice piece) {
        Position launch = launchOf(piece);
        double[] muzzle = ArtilleryPose.muzzle(piece.bodyYaw(), piece.barrelPitch());
        return new Position(launch.x() + muzzle[0] * MUZZLE_OFFSET_BLOCKS, launch.y() + muzzle[1] * MUZZLE_OFFSET_BLOCKS,
                launch.z() + muzzle[2] * MUZZLE_OFFSET_BLOCKS);
    }

    /** The height of the ground at a column, just above its top block. */
    private static double groundY(World world, int x, int z) {
        return world.getHighestBlockYAt(x, z) + 1.0;
    }

    private static List<Position> playersIn(World world) {
        List<Position> players = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            players.add(new Position(player.getX(), player.getY(), player.getZ()));
        }
        return players;
    }

    private static boolean loadedAt(World world, double x, double z) {
        return world.isChunkLoaded((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
    }

    private static String consumerId(ArtilleryDevice piece) {
        Pos pos = piece.pos();
        return EnergySnapshots.consumerId(piece.world(), pos.x(), pos.y(), pos.z());
    }

    /**
     * What a shot request came to: fired, or refused with the line that tells the player why.
     *
     * @param fired  true when the shot was fired
     * @param reason the line of a refused shot, with its colour codes; null when the shot was fired
     */
    public record ShotResult(boolean fired, String reason) {

        /** A shot that was fired. */
        public static ShotResult accepted() {
            return new ShotResult(true, null);
        }

        /**
         * A shot that was refused.
         *
         * @param reason the line that tells why, with its colour codes
         */
        public static ShotResult refused(String reason) {
            return new ShotResult(false, reason);
        }
    }

    /**
     * What a load or an unload came to: made, or refused with the line that tells the player why.
     *
     * @param changed true when the magazine was changed
     * @param message the line for the player, with its colour codes
     */
    public record LoadResult(boolean changed, String message) {

        /**
         * A load or an unload that was made.
         *
         * @param message the line that says what was moved, with its colour codes
         */
        public static LoadResult made(String message) {
            return new LoadResult(true, message);
        }

        /**
         * A load or an unload that was refused.
         *
         * @param message the line that tells why, with its colour codes
         */
        public static LoadResult refused(String message) {
            return new LoadResult(false, message);
        }
    }
}
