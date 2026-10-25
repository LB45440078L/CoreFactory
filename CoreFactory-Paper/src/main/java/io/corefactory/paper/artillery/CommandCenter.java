package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.energy.Pos;

import java.util.List;
import java.util.UUID;

/**
 * An Artillery Command Center placed in the world. It keeps its owner, its block, its name, the shell and the charge tier chosen, the aim on
 * the ground and the barrel's elevation it holds for the strike. It drives no power of its own: the artillery it commands draw theirs. Its
 * display, its screen and its interaction are its own handles and are made again when they go missing. Its saved form is written by
 * {@link ArtilleryStore}.
 */
public final class CommandCenter {

    /** The name a command center has until its owner gives it one. */
    public static final String DEFAULT_NAME = "Command Center";

    /** The elevation a command center holds until its owner sets one, in degrees. */
    public static final double DEFAULT_ELEVATION = 45.0;

    private final String id;
    private final UUID owner;
    private final UUID world;
    private final Pos pos;
    private String name = DEFAULT_NAME;
    private ArtilleryShell shell;
    private ChargeTier tier = ChargeTier.TIER_I;
    private double aimX = Double.NaN;
    private double aimZ = Double.NaN;
    private double elevation = DEFAULT_ELEVATION;
    private List<Integer> wallMaps = List.of();
    private UUID display;
    private UUID screen;
    private UUID interaction;
    private List<String> shown;

    /**
     * @param id    the command center's id
     * @param owner the player who placed it
     * @param world the id of the world it stands in
     * @param pos   the block it stands in
     */
    public CommandCenter(String id, UUID owner, UUID world, Pos pos) {
        this.id = id;
        this.owner = owner;
        this.world = world;
        this.pos = pos;
    }

    /** The command center's id. */
    public String id() {
        return id;
    }

    /** The player who placed it. */
    public UUID owner() {
        return owner;
    }

    /** The id of the world it stands in. */
    public UUID world() {
        return world;
    }

    /** The block it stands in: its anchor, on air. */
    public Pos pos() {
        return pos;
    }

    /** The name the owner gave it. */
    public String name() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    /** The shell chosen for the strike, or null when none is chosen. */
    public ArtilleryShell shell() {
        return shell;
    }

    /** The charge tier chosen for the strike. */
    public ChargeTier tier() {
        return tier;
    }

    void setLoad(ArtilleryShell shell, ChargeTier tier) {
        this.shell = shell;
        this.tier = tier;
    }

    /** The x of the aim point on the ground, NaN when no target is set. */
    public double aimX() {
        return aimX;
    }

    /** The z of the aim point on the ground, NaN when no target is set. */
    public double aimZ() {
        return aimZ;
    }

    /** True when a target is set. */
    public boolean hasAim() {
        return !Double.isNaN(aimX);
    }

    void setAim(double aimX, double aimZ) {
        this.aimX = aimX;
        this.aimZ = aimZ;
    }

    /** The barrel's elevation the strike is set for, in degrees. */
    public double elevation() {
        return elevation;
    }

    void setElevation(double elevation) {
        this.elevation = elevation;
    }

    /** The ids of the maps the wall's item frames hold, one for each frame, or none when no map has been made yet. */
    public List<Integer> wallMaps() {
        return wallMaps;
    }

    void setWallMaps(List<Integer> wallMaps) {
        this.wallMaps = List.copyOf(wallMaps);
    }

    /** The display of the model, or null when none is shown. */
    public UUID display() {
        return display;
    }

    void setDisplay(UUID display) {
        this.display = display;
    }

    /** The text display of the screen, or null when none is shown. */
    public UUID screen() {
        return screen;
    }

    void setScreen(UUID screen) {
        this.screen = screen;
        this.shown = null;
    }

    /** The interaction entity over the screen, or null when none is made. */
    public UUID interaction() {
        return interaction;
    }

    void setInteraction(UUID interaction) {
        this.interaction = interaction;
    }

    /** The lines the screen shows now, or null when it has shown none since its text was made. */
    List<String> shown() {
        return shown;
    }

    void setShown(List<String> shown) {
        this.shown = shown == null ? null : List.copyOf(shown);
    }
}
