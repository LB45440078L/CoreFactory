package io.corefactory.paper.energy;

import io.corefactory.core.energy.DeviceKind;
import io.corefactory.core.energy.GeneratorEnvironment;
import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.GeneratorStep;
import io.corefactory.core.energy.Units;
import io.corefactory.core.water.Face;
import io.corefactory.paper.energy.StoredEnergy.StoredGenerator;
import io.corefactory.paper.screen.DeviceScreen;
import org.bukkit.Location;

import java.util.List;

/**
 * A generator device: an {@link EnergyDevice} of one of the six kinds. A charcoal or nuclear generator keeps its fuel count, the seconds
 * left on the item that burns, and the energy it has made in total. A petroleum or diesel generator keeps no fuel: each tick it draws
 * litres from the oil network, and it keeps the litres the network held at its last tick for its hologram and its menu. While its menu
 * is open the menu's fuel slot is the store: each tick reads the slot before it runs, and writes the slot after. While the menu is
 * closed the count saved here is the store. Nothing is kept twice, so no fuel item is duplicated or lost.
 */
public final class GeneratorDevice extends EnergyDevice {

    private static final double EPS = 1e-9;

    private final GeneratorKind generatorKind;
    private final GeneratorItems items;
    private int fuelItems;
    private double remainingSeconds;
    private double totalWh;
    private double outputW;
    private double networkLitres;
    private boolean openSite;
    private boolean underwater;
    private boolean piped;
    private GeneratorMenu menu;
    private Face front;
    private DeviceScreen screen;

    /**
     * A generator with no front kept, which faces north; see {@link #GeneratorDevice(String, GeneratorKind, Location, int, double, double,
     * GeneratorItems, Face)}.
     */
    public GeneratorDevice(String id, GeneratorKind kind, Location block, int fuel, double remainingSeconds, double totalWh,
                           GeneratorItems items) {
        this(id, kind, block, fuel, remainingSeconds, totalWh, items, null);
    }

    /**
     * @param id               the device id
     * @param kind             the generator kind
     * @param block            the block the generator stands in
     * @param fuel             the fuel items kept, capped at 64; ignored for a kind that takes no item
     * @param remainingSeconds the seconds left on the burning item; ignored for a kind that takes no item
     * @param totalWh          the energy made since it was placed, in Wh
     * @param items            the items of the generators, for the menu and the drops
     * @param front            the horizontal face its screen is on, or null for a generator saved before fronts were kept
     */
    public GeneratorDevice(String id, GeneratorKind kind, Location block, int fuel, double remainingSeconds, double totalWh,
                           GeneratorItems items, Face front) {
        super(id, DeviceKind.GENERATOR, block, 1);
        this.generatorKind = kind;
        this.items = items;
        this.front = front;
        setFuelItems(fuel);
        this.remainingSeconds = takesItem() ? Math.max(0.0, remainingSeconds) : 0.0;
        this.totalWh = Math.max(0.0, totalWh);
    }

    /** The horizontal face the screen is on, toward the player who placed the generator. North for a generator saved before fronts. */
    public Face front() {
        return front == null ? Face.NORTH : front;
    }

    /** The screen on the front face, or null when none is shown. */
    public DeviceScreen screen() {
        return screen;
    }

    void setScreen(DeviceScreen screen) {
        this.screen = screen;
    }

    /** The kind of this generator. */
    public GeneratorKind generatorKind() {
        return generatorKind;
    }

    /** The component id of the generator's item, such as charcoal_generator. */
    @Override
    public String componentId() {
        return GeneratorIds.componentId(generatorKind);
    }

    /** The fuel kept: the menu's slot while the menu is open, otherwise the count kept here. Zero for a kind that takes no item. */
    public int fuel() {
        return menu != null && menu.isOpen() ? menu.fuelCount() : fuelItems;
    }

    /** The fuel count kept while the menu is closed. */
    int fuelItems() {
        return fuelItems;
    }

    /** Sets the kept fuel count, capped; a kind that takes no item keeps none. */
    void setFuelItems(int count) {
        fuelItems = takesItem() ? GeneratorFuel.countOf(count) : 0;
    }

    /** True for a kind that burns an item: charcoal and nuclear. Wind, solar, petroleum and diesel take none. */
    boolean takesItem() {
        return GeneratorFuel.sourceOf(generatorKind).isPresent();
    }

    /**
     * Adds one fuel item to the count, as a belt or a picker delivers it. While the menu is open the slot is written too, so the
     * count the menu shows and the count the tick reads stay the same.
     *
     * @return false, and nothing changes, when the kind takes no item or the count is at the cap
     */
    public boolean addFuel() {
        int count = fuel();
        if (!takesItem() || count >= GeneratorFuel.CAP) {
            return false;
        }
        setFuelItems(count + 1);
        if (menu != null && menu.isOpen()) {
            menu.writeFuel(fuelItems);
        }
        return true;
    }

    /** The seconds left on the fuel item that burns now, 0 when none does. */
    public double remainingSeconds() {
        return remainingSeconds;
    }

    /** The energy made since the generator was placed, in Wh. */
    public double totalWh() {
        return totalWh;
    }

    /** The output of the last tick, in W. */
    public double outputW() {
        return outputW;
    }

    /**
     * The litres of the generator's fluid the oil network held after its last draw, or 0 for a kind that burns no fluid. It is only a
     * reading for the hologram and the menu; the network keeps the oil.
     */
    public double networkLitres() {
        return networkLitres;
    }

    /** Records the litres the network holds of this generator's fluid, after the draw of a tick. */
    void setNetworkLitres(double litres) {
        this.networkLitres = Math.max(0.0, litres);
    }

    /** True when a fuel item is burning now. */
    public boolean burning() {
        return remainingSeconds > EPS;
    }

    /** True when the generator gave power in its last tick. */
    public boolean producing() {
        return outputW > 0;
    }

    /** Tells the generator whether a water pipe is joined to it, for its status. Set each tick, before it runs. */
    void setPiped(boolean piped) {
        this.piped = piped;
    }

    /** The status word of the generator now. */
    public GeneratorStatus status() {
        if (generatorKind == GeneratorKind.NUCLEAR && !underwater && piped) {
            return GeneratorStatus.NO_WATER;
        }
        return GeneratorStatus.of(generatorKind, openSite, underwater, producing(), burning(), fuel());
    }

    /** The lines of the generator's hologram now: its name, output and fuel or network litres on one line. */
    public List<String> hologramLines() {
        return GeneratorText.hologram(generatorKind, outputW, fuel(), status(), networkLitres);
    }

    /** The menu of this generator, made on first use. */
    public GeneratorMenu menu() {
        if (menu == null) {
            menu = new GeneratorMenu(this, items);
        }
        return menu;
    }

    /** The litres of fluid this generator wants from the network in a tick of {@code dtSeconds} at full output. Zero for a kind that burns no fluid. */
    public double litresWanted(double dtSeconds) {
        return GeneratorSnapshots.core(id(), pos(), generatorKind, 0, 0).litresWanted(dtSeconds);
    }

    /**
     * Runs one server tick of an item-fuelled kind of {@code dtSeconds}: the core generator gives its output and its next fuel state, the
     * energy made grows by the output over the tick, and the menu is kept in step.
     */
    void tick(GeneratorEnvironment env, double dtSeconds) {
        boolean viewed = menu != null && menu.isOpen();
        if (viewed) {
            setFuelItems(menu.fuelCount());
        }
        GeneratorStep step = GeneratorSnapshots.core(id(), pos(), generatorKind, fuelItems, remainingSeconds).tick(env, dtSeconds);
        record(env, dtSeconds, step, viewed);
    }

    /**
     * Runs one server tick of a fluid kind of {@code dtSeconds} with the litres it drew from the oil network. Its output is its rated
     * power for the share of the litres it wanted that it drew, and the energy made grows by that output over the tick.
     */
    void tickFluid(GeneratorEnvironment env, double dtSeconds, double litresDrawn) {
        GeneratorStep step = GeneratorSnapshots.core(id(), pos(), generatorKind, 0, 0).tickFluid(env, dtSeconds, litresDrawn);
        record(env, dtSeconds, step, false);
    }

    private void record(GeneratorEnvironment env, double dtSeconds, GeneratorStep step, boolean viewed) {
        fuelItems = step.state().fuelItems();
        remainingSeconds = step.state().remainingSeconds();
        outputW = step.outputW();
        totalWh += Units.wattHours(outputW, dtSeconds);
        openSite = env.openAir().open();
        underwater = env.underwater();
        if (viewed) {
            menu.writeFuel(fuelItems);
        }
    }

    /** Gives the menu's fuel back into the count and closes every viewer. Called when the device is removed. */
    void closeMenu() {
        if (menu != null) {
            menu.closeViewers();
            menu.absorb();
        }
    }

    /** Refreshes the menu's status items, when a player has the menu open. */
    void refreshMenu() {
        if (menu != null && menu.isOpen()) {
            menu.refresh();
        }
    }

    /** The saved form of this generator, with the fuel the menu holds if it is open. */
    public StoredGenerator toStored() {
        return new StoredGenerator(id(), world(), pos(), generatorKind, fuel(), remainingSeconds, totalWh, front());
    }
}
