package io.corefactory.paper.machine;

import io.corefactory.core.drill.DrillSelection;
import io.corefactory.core.drill.DrillState;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.MachineState;
import io.corefactory.core.upgrade.LabLevels;
import io.corefactory.core.water.Face;
import io.corefactory.paper.hologram.Hologram;
import io.corefactory.paper.screen.DeviceScreen;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One machine placed in the world: its host block, its menu and its engine state. The items are in the host block's barrel,
 * which the menu and the engine both read. A machine loaded from an older machines.yml may still hold stacks in the file;
 * they stay here until the barrel takes them, keyed by their menu slot.
 */
public final class PlacedMachine {

    private final Location block;
    private final Machine machine;
    private final Material host;
    private final MachineMenu menu;
    private final MachineState state;
    private final Map<Integer, ItemStack> legacy = new LinkedHashMap<>();
    private UUID display;
    private UUID topDisplay;
    private UUID interaction;
    private Hologram hologram;
    private DrillState drill;
    private DrillMining mining;
    private DrillSelection selection = new DrillSelection();
    private long lastWorkTick = Long.MIN_VALUE / 2;
    private boolean settled;
    private double workCredit;
    private int level = LabLevels.MIN_LEVEL;
    private UUID owner;
    private Face front;
    private DeviceScreen screen;

    PlacedMachine(Location block, Machine machine, Material host, MachineMenu menu) {
        this.block = block;
        this.machine = machine;
        this.host = host;
        this.menu = menu;
        this.state = new MachineState(machine.id(), menu.inputs(), menu.outputs(), menu.fuel());
    }

    /** The server tick this machine last showed its work effect on. */
    long lastWorkTick() {
        return lastWorkTick;
    }

    void markWork(long tick) {
        this.lastWorkTick = tick;
    }

    /** The player who placed the machine, or null when none is known (a machine saved before owners were kept). */
    public UUID owner() {
        return owner;
    }

    void setOwner(UUID owner) {
        this.owner = owner;
    }

    /**
     * The horizontal face the machine's screen is on, toward the player who placed it. A machine saved before fronts were kept faces north.
     */
    public Face front() {
        return front == null ? Face.NORTH : front;
    }

    void setFront(Face front) {
        this.front = front;
    }

    /** The screen on the front face, or null when none is kept. */
    public DeviceScreen screen() {
        return screen;
    }

    void setScreen(DeviceScreen screen) {
        this.screen = screen;
    }

    /** The host block's position, with whole block coordinates. */
    public Location block() {
        return block;
    }

    public Machine machine() {
        return machine;
    }

    /** The block the machine occupies, or null when the machine's host is not a block. */
    public Material host() {
        return host;
    }

    public MachineMenu menu() {
        return menu;
    }

    public MachineState state() {
        return state;
    }

    /** The stacks an older version kept in machines.yml that the barrel has not taken yet, by menu slot. */
    Map<Integer, ItemStack> legacy() {
        return legacy;
    }

    /** True once the machine has been brought onto the barrel storage. */
    boolean settled() {
        return settled;
    }

    void markSettled() {
        this.settled = true;
    }

    /** The process ticks a lab tool has earned under a partial network and not yet run, from 0 up to but not including 1. */
    double workCredit() {
        return workCredit;
    }

    void setWorkCredit(double workCredit) {
        this.workCredit = workCredit;
    }

    /**
     * The level of a lab machine, from {@link LabLevels#MIN_LEVEL} to {@link LabLevels#MAX_LEVEL}. It sets the machine's work pace
     * and its model. A drill is always at level I.
     */
    public int level() {
        return level;
    }

    void setLevel(int level) {
        if (level < LabLevels.MIN_LEVEL || level > LabLevels.MAX_LEVEL) {
            throw new IllegalArgumentException("a lab machine is level " + LabLevels.MIN_LEVEL + " to " + LabLevels.MAX_LEVEL);
        }
        this.level = level;
    }

    /** The item display drawn over the host block, or null when none is shown. */
    public UUID display() {
        return display;
    }

    /**
     * The item display of the block above the host block of a tall drill, or null when none is shown, which is always the case for a
     * machine one block tall.
     */
    public UUID topDisplay() {
        return topDisplay;
    }

    void setTopDisplay(UUID topDisplay) {
        this.topDisplay = topDisplay;
    }

    /** The progress hologram over the host block while the machine is active, or null when none is shown. */
    public Hologram hologram() {
        return hologram;
    }

    void setHologram(Hologram hologram) {
        this.hologram = hologram;
    }

    void setDisplay(UUID display) {
        this.display = display;
    }

    /**
     * The invisible interaction entity over the host block, which takes the clicks a concealed client sends to the barrel, or
     * null when none is made.
     */
    public UUID interaction() {
        return interaction;
    }

    void setInteraction(UUID interaction) {
        this.interaction = interaction;
    }

    /** The cycle state of a drill, or null for a lab tool. */
    public DrillState drill() {
        return drill;
    }

    void setDrill(DrillState drill) {
        this.drill = drill;
    }

    /** The mining state of a drill, or null for a lab tool. It is not saved. */
    DrillMining mining() {
        return mining;
    }

    void setMining(DrillMining mining) {
        this.mining = mining;
    }

    /** The ores a drill mines. Every ore is mined unless the player ignored it in the resources menu. */
    public DrillSelection selection() {
        return selection;
    }

    void setSelection(DrillSelection selection) {
        this.selection = selection;
    }

    /** True when the chunk of the host block is loaded, so the machine may run. */
    public boolean isLoaded() {
        World world = block.getWorld();
        return world != null && world.isChunkLoaded(block.getBlockX() >> 4, block.getBlockZ() >> 4);
    }
}
