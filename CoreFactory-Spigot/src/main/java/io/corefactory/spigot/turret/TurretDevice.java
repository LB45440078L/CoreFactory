package io.corefactory.spigot.turret;

import io.corefactory.core.device.Footprint;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.turret.DualTargeting;
import io.corefactory.core.turret.Magazine;
import io.corefactory.core.turret.TargetPolicy;
import io.corefactory.core.turret.TurretUpgrade;
import io.corefactory.spigot.hologram.Hologram;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * A turret placed on air: an item display for its look, an interaction entity for its clicks, and an optional hologram for its status. A
 * tall turret (level 3 and up) has a second display over the block above, see {@link #height}. It keeps its kind, its level, its aim and its
 * magazine, the target each cannon holds, and the countdowns of its search and its fire. A machine gun has one or two cannons, each with its
 * own aim, target and cooldown; the barrel of the first cannon is the turret's own, and it is the one the model shows. A laser turret holds no
 * rounds, so its magazine stays empty, and it has one cannon. Its saved form is a {@link StoredTurret}; its entities are made again when they
 * go missing. The barrel is the point a turret aims and fires from.
 */
public final class TurretDevice {

    /** The most cannons a turret has, and so the number of its aims, targets and cooldowns. */
    public static final int CANNONS = DualTargeting.MAX_CANNONS;

    private final String id;
    private final TurretKind kind;
    private final UUID owner;
    private final Location block;
    private final double facing;
    private Magazine magazine;
    private TargetPolicy policy;
    private int level;
    private final double[] yaws = new double[CANNONS];
    private final double[] pitches = new double[CANNONS];
    private final UUID[] targets = new UUID[CANNONS];
    private final int[] cooldowns = new int[CANNONS];
    private boolean sweepingForward = true;
    private UUID display;
    private UUID topDisplay;
    private UUID interaction;
    private Hologram hologram;
    private int searchIn;
    private int rotateSoundIn;
    private int textIn;
    private int fireSoundIn;
    private int chargeSoundIn;
    private double shownYaw = Double.NaN;
    private double shownPitch = Double.NaN;
    private TurretStatus status = TurretStatus.NO_ROUNDS;
    private double satisfaction;
    private TurretMenu menu;

    /**
     * @param id       the turret's id
     * @param kind     the kind of turret
     * @param owner    the player who placed it
     * @param block    the block it stands in; copied to whole block coordinates
     * @param facing   the yaw it faces when placed
     * @param magazine the rounds it holds
     * @param level    its upgrade level, from 1 to 5
     */
    public TurretDevice(String id, TurretKind kind, UUID owner, Location block, double facing, Magazine magazine, int level) {
        this.id = id;
        this.kind = kind;
        this.owner = owner;
        this.block = new Location(block.getWorld(), block.getBlockX(), block.getBlockY(), block.getBlockZ());
        this.facing = facing;
        this.magazine = magazine;
        this.level = level;
        this.policy = TargetPolicy.DEFAULT;
        for (int cannon = 0; cannon < CANNONS; cannon++) {
            yaws[cannon] = facing;
        }
    }

    /** The turret's id. */
    public String id() {
        return id;
    }

    /** The kind of turret: machine gun or laser. */
    public TurretKind kind() {
        return kind;
    }

    /** The player who placed the turret. */
    public UUID owner() {
        return owner;
    }

    /** A copy of the block the turret stands in: its lower half. */
    public Location block() {
        return block.clone();
    }

    /** The id of the world the turret stands in. */
    public UUID world() {
        return block.getWorld().getUID();
    }

    /** The turret's block as a position: the lower half, which is the turret's anchor. */
    public Pos pos() {
        return new Pos(block.getBlockX(), block.getBlockY(), block.getBlockZ());
    }

    /** The yaw the turret faced when it was placed: the centre of its arc. */
    public double facing() {
        return facing;
    }

    /** The point the turret aims and fires from, above the centre of its block. */
    public Location barrel() {
        return block.clone().add(0.5, 0.75, 0.5);
    }

    /** The world the turret stands in, or null when that world is not loaded. */
    public World loadedWorld() {
        return block.getWorld();
    }

    /** True when the chunk the turret stands in is loaded, so it can act and its entities exist. */
    public boolean isLoaded() {
        World world = block.getWorld();
        return world != null && world.isChunkLoaded(block.getBlockX() >> 4, block.getBlockZ() >> 4);
    }

    /** True when the turret stands in the given chunk. */
    public boolean inChunk(Chunk chunk) {
        return block.getWorld() != null && block.getWorld().equals(chunk.getWorld())
                && block.getBlockX() >> 4 == chunk.getX() && block.getBlockZ() >> 4 == chunk.getZ();
    }

    /** The upgrade level, from 1 to 5. */
    public int level() {
        return level;
    }

    void setLevel(int level) {
        this.level = level;
    }

    /**
     * The number of blocks the turret stands: one, or two from level 3 on, when the block above is its upper half.
     */
    public int height() {
        return level >= TurretUpgrade.TALL_FROM ? Footprint.TALL : Footprint.SHORT;
    }

    /** The rounds the turret holds. */
    public Magazine magazine() {
        return magazine;
    }

    void setMagazine(Magazine magazine) {
        this.magazine = magazine;
    }

    /** The targeting choices of the turret: its friendly-fire switch, its overrides, its player stances and its team mode. */
    public TargetPolicy policy() {
        return policy;
    }

    void setPolicy(TargetPolicy policy) {
        this.policy = policy;
    }

    /** The current yaw of a cannon's barrel. Cannon 0 is the turret's own barrel, which the model shows. */
    public double yaw(int cannon) {
        return yaws[cannon];
    }

    /** The current pitch of a cannon's barrel; positive looks down. */
    public double pitch(int cannon) {
        return pitches[cannon];
    }

    void aim(int cannon, double yaw, double pitch) {
        yaws[cannon] = yaw;
        pitches[cannon] = pitch;
    }

    /** True when the turret is heading toward the positive end of its arc while it sweeps. */
    public boolean sweepingForward() {
        return sweepingForward;
    }

    void setSweepingForward(boolean forward) {
        this.sweepingForward = forward;
    }

    /** The item display's id of the lower half, or null when none is shown. */
    public UUID display() {
        return display;
    }

    void setDisplay(UUID display) {
        this.display = display;
    }

    /** The item display's id of the upper half of a tall turret, or null when none is shown. */
    public UUID topDisplay() {
        return topDisplay;
    }

    void setTopDisplay(UUID topDisplay) {
        this.topDisplay = topDisplay;
    }

    /** The interaction entity's id, or null when none is made. */
    public UUID interaction() {
        return interaction;
    }

    void setInteraction(UUID interaction) {
        this.interaction = interaction;
    }

    /** The hologram over the turret, or null when it has none. */
    public Hologram hologram() {
        return hologram;
    }

    void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }

    /** The entity a cannon holds as its target, or null. */
    public UUID target(int cannon) {
        return targets[cannon];
    }

    void setTarget(int cannon, UUID target) {
        targets[cannon] = target;
    }

    int searchIn() {
        return searchIn;
    }

    void setSearchIn(int ticks) {
        this.searchIn = ticks;
    }

    /** The ticks until a cannon may fire again. */
    public int cooldown(int cannon) {
        return cooldowns[cannon];
    }

    void setCooldown(int cannon, int ticks) {
        cooldowns[cannon] = ticks;
    }

    int rotateSoundIn() {
        return rotateSoundIn;
    }

    void setRotateSoundIn(int ticks) {
        this.rotateSoundIn = ticks;
    }

    int textIn() {
        return textIn;
    }

    void setTextIn(int ticks) {
        this.textIn = ticks;
    }

    int fireSoundIn() {
        return fireSoundIn;
    }

    void setFireSoundIn(int ticks) {
        this.fireSoundIn = ticks;
    }

    int chargeSoundIn() {
        return chargeSoundIn;
    }

    void setChargeSoundIn(int ticks) {
        this.chargeSoundIn = ticks;
    }

    /** The yaw the display was last drawn at, NaN before the first draw. */
    double shownYaw() {
        return shownYaw;
    }

    /** The pitch the display was last drawn at, NaN before the first draw. */
    double shownPitch() {
        return shownPitch;
    }

    void setShown(double yaw, double pitch) {
        this.shownYaw = yaw;
        this.shownPitch = pitch;
    }

    /** The status of the last tick. */
    public TurretStatus status() {
        return status;
    }

    void setStatus(TurretStatus status) {
        this.status = status;
    }

    /** The share of its power demand the turret received at the last solve, 0 to 1. */
    public double satisfaction() {
        return satisfaction;
    }

    void setSatisfaction(double satisfaction) {
        this.satisfaction = satisfaction;
    }

    /** The menu of this turret, made on first use. */
    TurretMenu menu(TurretService service) {
        if (menu == null) {
            menu = new TurretMenu(service, this);
        }
        return menu;
    }

    /** The menu, when it has been made. */
    TurretMenu existingMenu() {
        return menu;
    }

    /** The saved form of this turret. */
    public StoredTurret toStored() {
        return new StoredTurret(id, kind, owner, world(), pos(), facing, magazine.type(), magazine.count(), level, policy);
    }
}
