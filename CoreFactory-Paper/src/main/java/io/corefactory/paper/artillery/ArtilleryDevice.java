package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryLevel;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.energy.Pos;

import java.util.UUID;

/**
 * An artillery piece placed in the world. It keeps its level, its owner, its block, whether it is switched on, the shell and the charge tier
 * it is loaded with, the shells and the charges in its magazine, the last aim and elevation it fired at, and the time of its last shot, which
 * times its reload. Its pose is the yaw of its body and the pitch of its barrel: an aim turns the body to it and lays the barrel at its
 * elevation, and the registry returns the barrel to horizontal when the piece has fired and finished reloading. Its display and interaction
 * entities are its own handles and are made again when they go missing. Its saved form is written by {@link ArtilleryStore}.
 */
public final class ArtilleryDevice {

    /** The last-shot time of a piece that has not fired: it is never reloading. */
    static final long NEVER = Long.MIN_VALUE;

    /** The milliseconds of a server tick, which a reload is counted in. */
    static final long MILLIS_PER_TICK = 50L;

    /** The name a piece has until its owner gives it one. */
    public static final String DEFAULT_NAME = "Artillery";

    /** The longest name a piece may have. */
    public static final int MAX_NAME_LENGTH = 32;

    private final String id;
    private final UUID owner;
    private final UUID world;
    private final Pos pos;
    private final ArtilleryLevel level;
    private boolean on;
    private ArtilleryShell shell;
    private ChargeTier tier = ChargeTier.TIER_I;
    private int magazineShells;
    private int magazineCharges;
    private double aimX = Double.NaN;
    private double aimZ = Double.NaN;
    private double elevation = Double.NaN;
    private long lastShotMillis = NEVER;
    private String name = DEFAULT_NAME;
    private double bodyYaw = ArtilleryPose.NORTH_YAW;
    private double barrelPitch = ArtilleryPose.IDLE_PITCH;
    private double shownYaw = Double.NaN;
    private double shownPitch = Double.NaN;
    private UUID display;
    private UUID barrelDisplay;
    private UUID topDisplay;
    private UUID interaction;

    /**
     * @param id    the piece's id
     * @param owner the player who placed it
     * @param world the id of the world it stands in
     * @param pos   the block it stands in
     * @param level its level, from I to VII
     */
    public ArtilleryDevice(String id, UUID owner, UUID world, Pos pos, ArtilleryLevel level) {
        this.id = id;
        this.owner = owner;
        this.world = world;
        this.pos = pos;
        this.level = level;
    }

    /** The piece's id. */
    public String id() {
        return id;
    }

    /** The player who placed the piece. */
    public UUID owner() {
        return owner;
    }

    /** The id of the world the piece stands in. */
    public UUID world() {
        return world;
    }

    /** The block the piece stands in: its anchor, on air. */
    public Pos pos() {
        return pos;
    }

    /** The piece's level. */
    public ArtilleryLevel level() {
        return level;
    }

    /** True when the piece is switched on. A piece that is off draws no power and fires nothing. */
    public boolean on() {
        return on;
    }

    void setOn(boolean on) {
        this.on = on;
    }

    /** The shell chosen for the next shot, or null when none is chosen. */
    public ArtilleryShell shell() {
        return shell;
    }

    void setShell(ArtilleryShell shell) {
        this.shell = shell;
    }

    /** The charge tier loaded in the piece. A shell needs at least its required tier. */
    public ChargeTier tier() {
        return tier;
    }

    void setTier(ChargeTier tier) {
        this.tier = tier;
    }

    /** The shells loaded in the piece's magazine: each shot uses one. */
    public int magazineShells() {
        return magazineShells;
    }

    /** The charges loaded in the piece's magazine: each shot uses one, of the tier the piece is loaded with. */
    public int magazineCharges() {
        return magazineCharges;
    }

    void setMagazine(int shells, int charges) {
        this.magazineShells = shells;
        this.magazineCharges = charges;
    }

    /** The x of the last aim point on the ground, NaN when the piece has not fired. */
    public double aimX() {
        return aimX;
    }

    /** The z of the last aim point on the ground, NaN when the piece has not fired. */
    public double aimZ() {
        return aimZ;
    }

    /** The elevation of the barrel for the last shot, in degrees, NaN when the piece has not fired. */
    public double elevation() {
        return elevation;
    }

    /**
     * Sets the aim, and with it the pose: the body turns to the aim, and the barrel lays at the elevation. The body keeps its yaw when the aim is
     * over the piece.
     */
    void setAim(double aimX, double aimZ, double elevation) {
        this.aimX = aimX;
        this.aimZ = aimZ;
        this.elevation = elevation;
        this.bodyYaw = ArtilleryPose.bodyYaw(pos.x() + 0.5, pos.z() + 0.5, aimX, aimZ, bodyYaw);
        this.barrelPitch = elevation;
    }

    /** The yaw of the body, in degrees from 0 up to 360: the direction its front faces. A new piece faces north, at 180. */
    public double bodyYaw() {
        return bodyYaw;
    }

    void setBodyYaw(double bodyYaw) {
        this.bodyYaw = bodyYaw;
    }

    /**
     * The pitch of the barrel, in degrees: the elevation it is laid at, or the idle pitch of 0 when it lies horizontal. Positive raises the
     * muzzle.
     */
    public double barrelPitch() {
        return barrelPitch;
    }

    void setBarrelPitch(double barrelPitch) {
        this.barrelPitch = barrelPitch;
    }

    /** The body yaw the displays show, NaN until they are first drawn. The registry keeps it; it is not saved. */
    double shownYaw() {
        return shownYaw;
    }

    /** The barrel pitch the displays show, NaN until they are first drawn. The registry keeps it; it is not saved. */
    double shownPitch() {
        return shownPitch;
    }

    void setShown(double yaw, double pitch) {
        this.shownYaw = yaw;
        this.shownPitch = pitch;
    }

    /** True when the piece has an aim from a shot. */
    public boolean hasAim() {
        return !Double.isNaN(aimX);
    }

    /** The time of the last shot in milliseconds, or the value that means no shot was ever fired. */
    public long lastShotMillis() {
        return lastShotMillis;
    }

    void setLastShotMillis(long millis) {
        this.lastShotMillis = millis;
    }

    /**
     * True while the piece is reloading after its last shot: for the reload of its level and tier, in ticks, from the time of that shot.
     *
     * @param nowMillis   the current time, in milliseconds
     * @param reloadTicks the reload of the level and tier, in ticks
     */
    public boolean reloadingAt(long nowMillis, int reloadTicks) {
        return lastShotMillis != NEVER && nowMillis - lastShotMillis < (long) reloadTicks * MILLIS_PER_TICK;
    }

    /** The name the owner gave the piece. */
    public String name() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    /** The display's id, or null when none is shown. */
    public UUID display() {
        return display;
    }

    void setDisplay(UUID display) {
        this.display = display;
    }

    /** The id of the barrel's display, or null when none is shown. */
    public UUID barrelDisplay() {
        return barrelDisplay;
    }

    void setBarrelDisplay(UUID barrelDisplay) {
        this.barrelDisplay = barrelDisplay;
    }

    /** The id of the upper model's display, which only level VII has, or null when none is shown. */
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
}
