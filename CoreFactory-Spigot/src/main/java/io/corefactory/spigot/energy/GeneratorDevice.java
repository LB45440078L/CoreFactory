package io.corefactory.spigot.energy;

import io.corefactory.core.energy.DeviceKind;
import io.corefactory.core.energy.GeneratorEnvironment;
import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.GeneratorStep;
import io.corefactory.core.energy.Units;
import io.corefactory.spigot.energy.StoredEnergy.StoredGenerator;
import org.bukkit.Location;

import java.util.List;

/**
 * A generator device: an {@link EnergyDevice} of one of the six kinds. It keeps its fuel count, the seconds left on the item that
 * burns, the energy it has made in total, and the output and site flags of its last tick. While its menu is open the menu's fuel
 * slot is the store: each tick reads the slot before it runs, and writes the slot after. While the menu is closed the count saved
 * here is the store. Nothing is kept twice, so no fuel item is duplicated or lost.
 */
public final class GeneratorDevice extends EnergyDevice {

    private static final double EPS = 1e-9;

    private final GeneratorKind generatorKind;
    private final GeneratorItems items;
    private int fuelItems;
    private double remainingSeconds;
    private double totalWh;
    private double outputW;
    private boolean openSite;
    private boolean underwater;
    private GeneratorMenu menu;

    /**
     * @param id               the device id
     * @param kind             the generator kind
     * @param block            the block the generator stands in
     * @param fuel             the fuel items kept, capped at 64; ignored for a kind with no fuel
     * @param remainingSeconds the seconds left on the burning item
     * @param totalWh          the energy made since it was placed, in Wh
     * @param items            the items of the generators, for the menu and the drops
     */
    public GeneratorDevice(String id, GeneratorKind kind, Location block, int fuel, double remainingSeconds, double totalWh,
                           GeneratorItems items) {
        super(id, DeviceKind.GENERATOR, block, 1);
        this.generatorKind = kind;
        this.items = items;
        setFuelItems(fuel);
        this.remainingSeconds = kind.burnsFuel() ? Math.max(0.0, remainingSeconds) : 0.0;
        this.totalWh = Math.max(0.0, totalWh);
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

    /** The fuel kept: the menu's slot while the menu is open, otherwise the count kept here. */
    public int fuel() {
        return menu != null && menu.isOpen() ? menu.fuelCount() : fuelItems;
    }

    /** The fuel count kept while the menu is closed. */
    int fuelItems() {
        return fuelItems;
    }

    /** Sets the kept fuel count, capped; a kind with no fuel keeps none. */
    void setFuelItems(int count) {
        fuelItems = generatorKind.burnsFuel() ? GeneratorFuel.countOf(count) : 0;
    }

    /**
     * Adds one fuel item to the count, as a belt or a picker delivers it. While the menu is open the slot is written too, so the
     * count the menu shows and the count the tick reads stay the same.
     *
     * @return false, and nothing changes, when the kind takes no fuel or the count is at the cap
     */
    public boolean addFuel() {
        int count = fuel();
        if (!generatorKind.burnsFuel() || count >= GeneratorFuel.CAP) {
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

    /** True when a fuel item is burning now. */
    public boolean burning() {
        return remainingSeconds > EPS;
    }

    /** True when the generator gave power in its last tick. */
    public boolean producing() {
        return outputW > 0;
    }

    /** The status word of the generator now. */
    public GeneratorStatus status() {
        return GeneratorStatus.of(generatorKind, openSite, underwater, producing(), burning(), fuel());
    }

    /** The lines of the generator's hologram now: its name, output and fuel on one line. */
    public List<String> hologramLines() {
        return GeneratorText.hologram(generatorKind, outputW, fuel(), status());
    }

    /** The menu of this generator, made on first use. */
    public GeneratorMenu menu() {
        if (menu == null) {
            menu = new GeneratorMenu(this, items);
        }
        return menu;
    }

    /**
     * Runs one server tick of {@code dtSeconds}: the core generator gives its output and its next fuel state, the energy made grows
     * by the output over the tick, and the menu is kept in step.
     */
    void tick(GeneratorEnvironment env, double dtSeconds) {
        boolean viewed = menu != null && menu.isOpen();
        if (viewed) {
            setFuelItems(menu.fuelCount());
        }
        GeneratorStep step = GeneratorSnapshots.core(id(), pos(), generatorKind, fuelItems, remainingSeconds).tick(env, dtSeconds);
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
        return new StoredGenerator(id(), world(), pos(), generatorKind, fuel(), remainingSeconds, totalWh);
    }
}
