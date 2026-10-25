package io.corefactory.paper.machine;

import io.corefactory.core.energy.EnergyTable;
import io.corefactory.core.hologram.Tone;
import io.corefactory.core.oil.Fluid;
import io.corefactory.core.oil.FluidPhysics;
import io.corefactory.core.oil.Refinery;
import io.corefactory.core.text.Text;
import io.corefactory.paper.oil.OilService;
import io.corefactory.paper.screen.ScreenText;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Runs the crude oil refinery once per server tick. It takes petroleum from every oil network at its block that holds petroleum, as far as it
 * can make diesel for the tick, and pushes the diesel into the first diesel output on any of its faces that takes it: a network that holds
 * diesel, or is empty with a diesel consumer on it. It never draws petroleum that the outputs cannot take: when no output takes the diesel,
 * the petroleum drawn shrinks to what they take. The refinery is working when it produces, and stuck when the network has no petroleum or no
 * output takes diesel. Its info item shows the petroleum in the network, the diesel in the outputs, the number of outputs and the state, and
 * its status item shows whether it runs.
 */
public final class RefineryTicker {

    /** The processes.yml id of the crude oil refinery. */
    public static final String MACHINE = "crude_oil_refinery";

    private static final double TICK_SECONDS = EnergyTable.SECONDS_PER_TICK;
    private static final int TEXT_TICKS = 10;

    /** The state of a refinery after its tick. */
    private enum State {
        /** It drew petroleum and pushed diesel. */
        RUNNING("&aRunning"),
        /** The oil network holds no petroleum for it. */
        NO_PETROLEUM("&cStuck: no petroleum in the network"),
        /** No diesel output takes diesel: none is joined, or each one is full of petroleum-free diesel it cannot take. */
        NO_OUTPUT("&cStuck: no output tube takes diesel");

        private final String text;

        State(String text) {
            this.text = text;
        }
    }

    private final OilService oil;

    /** @param oil the oil network the refinery draws petroleum from and pushes diesel into */
    public RefineryTicker(OilService oil) {
        this.oil = oil;
    }

    /** Advances one refinery by one tick. Call only for a refinery whose chunk is loaded. */
    public void tick(PlacedMachine placed, long tick) {
        Location block = placed.block();
        double wanted = Refinery.petroleumWanted(TICK_SECONDS);
        double available = oil.available(block, Fluid.PETROLEUM);
        double petroleum = Math.min(wanted, available);
        double diesel = Refinery.dieselFrom(petroleum);
        double accepted = oil.supply(block, Fluid.DIESEL, diesel);
        if (accepted < diesel) {
            petroleum = accepted / Refinery.DIESEL_PER_PETROLEUM;
        }
        double drawn = petroleum > 0 ? oil.draw(block, Fluid.PETROLEUM, petroleum) : 0.0;
        if (tick % TEXT_TICKS == 0) {
            show(placed, stateOf(drawn, available), drawn / TICK_SECONDS);
        }
    }

    private static State stateOf(double drawn, double available) {
        if (drawn > 0) {
            return State.RUNNING;
        }
        return available <= 0 ? State.NO_PETROLEUM : State.NO_OUTPUT;
    }

    /** Shows the info item and the status of the menu. Only a changed item is written. */
    private void show(PlacedMachine placed, State state, double petroleumPerSecond) {
        Location block = placed.block();
        double dieselInOutputs = oil.available(block, Fluid.DIESEL);
        int outputs = oil.outputs(block, Fluid.DIESEL);
        ItemStack item = new ItemStack(Material.CAULDRON, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(placed.machine().name()));
            meta.lore(Text.lore(List.of(
                    "&7Petroleum in the network &f" + FluidPhysics.volume(oil.available(block, Fluid.PETROLEUM)),
                    "&7Diesel in the outputs &f" + FluidPhysics.volume(dieselInOutputs),
                    "&7Petroleum taken &f" + FluidPhysics.rate(petroleumPerSecond),
                    "&7Diesel outputs &f" + outputs,
                    "&7State " + state.text)));
            item.setItemMeta(meta);
        }
        placed.menu().showInfo(item);
        boolean running = state == State.RUNNING;
        placed.menu().refresh(running ? 1 : 0, running ? 1 : 0, state == State.NO_OUTPUT);
        if (placed.screen() != null) {
            placed.screen().setLines(ScreenText.rows(placed.machine().name(), screenWord(state), running ? Tone.WORKING : Tone.STUCK,
                    List.of("&7Petroleum &f" + FluidPhysics.volume(oil.available(block, Fluid.PETROLEUM)),
                            "&7Diesel &f" + FluidPhysics.rate(Refinery.dieselFrom(petroleumPerSecond)))));
        }
    }

    /** The state a refinery's screen shows: working, no petroleum, or no output taking diesel. */
    private static String screenWord(State state) {
        return switch (state) {
            case RUNNING -> "Working";
            case NO_PETROLEUM -> "No petroleum";
            case NO_OUTPUT -> "Output blocked";
        };
    }
}
