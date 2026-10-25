package io.corefactory.paper.machine;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositSettings;
import io.corefactory.core.deposit.DepositStore;
import io.corefactory.core.deposit.DrillYield;
import io.corefactory.core.drill.DrillBox;
import io.corefactory.core.drill.DrillDefinition;
import io.corefactory.core.drill.DrillDeposits;
import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.drill.DrillPower;
import io.corefactory.core.drill.DrillRules;
import io.corefactory.core.drill.DrillState;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.drill.DrillStatus;
import io.corefactory.core.drill.DrillStorage;
import io.corefactory.core.drill.DrillTable;
import io.corefactory.core.drill.DrillText;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.hologram.HologramText;
import io.corefactory.core.hologram.Tone;
import io.corefactory.core.oil.Fluid;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import io.corefactory.core.text.Text;
import io.corefactory.paper.audio.MachineSounds;
import io.corefactory.paper.energy.EnergyService;
import io.corefactory.paper.energy.EnergyText;
import io.corefactory.paper.hologram.HologramService;
import io.corefactory.paper.oil.OilService;
import io.corefactory.paper.screen.ScreenText;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Runs every drill in a loaded chunk, once per server tick. A drill's power is the strongest of three sources: the redstone
 * its block receives, each daylight detector beside it, and charcoal in its fuel slot. A petroleum drill works the crude deposits that
 * touch its square, and pushes the petroleum it pumps into the oil network as litres: each tick, the accumulator turns its rate into
 * whole litres, which the network takes from it, and the deposit gives up the litres the network took. An ore drill works the other
 * deposits, one at a time: the one with the largest share left (what it has left of what it started with), so the deposits run out together. Each tick while it runs, it makes
 * {@code itemsPerSecond(level) * power} items per second, whole items at a time, and the whole items wait in a batch that is taken from the
 * deposit a few times a second, never more than the deposit holds and never more than the outputs have room for. An ore drill draws the
 * petroleum it burns from the oil network at its block on each tick it would run, and runs only when the network gives it all it wants. The fuel of a
 * petroleum drill burns once per fuel cycle of its level, while it runs. The hologram and the info item are refreshed every
 * {@link #TEXT_TICKS} ticks.
 */
public final class DrillTicker {

    private static final int DISPLAY_CHECK_TICKS = 20;
    private static final int MENU_PULL_TICKS = 5;
    private static final int TEXT_TICKS = 10;
    private static final int DEFAULT_STACK_LIMIT = 64;
    private static final double EPSILON = 1e-9;
    private static final BlockFace[] SIDES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

    private final MachineStore store;
    private final HologramService holograms;
    private final DepositStore deposits;
    private final DepositSettings settings;
    private final DrillYield yield;
    private final Map<String, OreDefinition> ores;
    private final EnergyService energy;
    private final OilService oil;
    private final Logger log;
    private final DrillAnimation animation = new DrillAnimation();

    /**
     * @param deposits the store of the planned deposits, which the drills take from
     * @param settings the deposit settings; the search reach is the largest radius a deposit may have, so it does not read this
     * @param yield    the rate of each level at full power: litres a second for a petroleum drill, items a second for an ore drill
     * @param ores     the ore definitions by id, which give a deposit's drop
     * @param oil      the oil network, which an ore drill draws its petroleum from and a petroleum drill pushes its petroleum into
     */
    public DrillTicker(MachineStore store, HologramService holograms, DepositStore deposits, DepositSettings settings,
                       DrillYield yield, Map<String, OreDefinition> ores, EnergyService energy, OilService oil, Logger log) {
        this.store = store;
        this.holograms = holograms;
        this.deposits = deposits;
        this.settings = settings;
        this.yield = yield;
        this.ores = Map.copyOf(ores);
        this.energy = energy;
        this.oil = oil;
        this.log = log;
    }

    /** Advances one drill by one tick. Call only for a drill whose chunk is loaded. */
    public void tick(PlacedMachine placed, long tick) {
        MachineMenu menu = placed.menu();
        DrillState drill = placed.drill();
        DrillMining mining = placed.mining();
        DrillDefinition definition = drill.definition();
        DrillKind kind = definition.kind();
        boolean open = menu.isOpen();
        if (open) {
            menu.push();
        }
        if (tick % DISPLAY_CHECK_TICKS == 0) {
            store.ensureDisplay(placed);
        }
        Optional<Stack> fuelSlot = menu.fuel().stream().findFirst();
        // The slot may be empty, so its reference may be null: compare with the reference the other way round.
        int fuel = fuelSlot.filter(slot -> kind.fuel().filter(ref -> ref.equals(slot.ref())).isPresent()).map(Stack::count).orElse(0);
        EnergyService.Reading reading = energy.reading(placed.block());
        if (mining.searchDue(tick)) {
            search(placed, tick);
        }
        boolean fed = drawsFuel(placed) && drawPetroleum(placed, definition);
        DrillPower local = power(placed, kind, fuel, fed);
        DrillPower power = kind == DrillKind.ORE ? local : DrillRules.withGrid(local, reading.satisfaction());
        boolean burns = kind == DrillKind.PETROLEUM && DrillRules.burnsCharcoal(power);
        MachineSounds.drillRun(power.powered(), kind, placed.block(), tick);
        if (kind == DrillKind.PETROLEUM && power.powered() && mining.current() != null) {
            pump(placed, power, tick);
        }
        if (status(placed, power) == DrillStatus.RUNNING) {
            if (kind == DrillKind.ORE) {
                produce(placed, power);
            }
            drill.advance(power.efficiency());
            if (drill.ready()) {
                drill.finishCycle(burns);
                if (burns && drill.fuelDue() && fuelSlot.isPresent()) {
                    fuelSlot.get().set(kind.fuel().orElseThrow(), fuel - 1);
                    drill.fuelPaid();
                }
            }
        }
        if (mining.batch().due() && !mining.takesBlocked(tick)) {
            take(placed, tick);
        }
        mining.batch().tick();
        menu.refresh((int) drill.progress(), definition.spec().cycleTicks(), false);
        animation.tick(placed, tick);
        if (tick % TEXT_TICKS == 0) {
            showStats(placed, power, fuel, reading);
        }
        if (open && tick % MENU_PULL_TICKS == 0) {
            menu.pull();
        }
    }

    /**
     * Draws the petroleum an ore drill burns this tick from the oil network at its block. The drill has the petroleum it wants only when
     * the network gave all of it.
     *
     * @return true when the network gave the drill the whole of its tick's petroleum
     */
    private boolean drawPetroleum(PlacedMachine placed, DrillDefinition definition) {
        double wanted = definition.kind().litresPerTick(definition.spec().cycleTicks());
        double drawn = oil.draw(placed.block(), Fluid.PETROLEUM, wanted);
        return drawn + EPSILON >= wanted;
    }

    /**
     * True when the drill draws its fluid fuel from the oil network on this tick: an ore drill that has room, a deposit in range and a
     * deposit to work on, so it would run if the petroleum came. It reads the deposit the search picked, so it runs after the search.
     */
    private boolean drawsFuel(PlacedMachine placed) {
        DrillMining mining = placed.mining();
        return DrillRules.drawsFuel(placed.drill().definition().kind(), freeSpace(placed) > 0, mining.targetInRange(),
                mining.current() != null);
    }

    /**
     * The power of a drill this cycle: petroleum from the network for an ore drill, which runs on that alone, and the strongest
     * local source for a petroleum drill (the grid is added by the caller).
     */
    private static DrillPower power(PlacedMachine placed, DrillKind kind, int fuel, boolean fed) {
        if (kind == DrillKind.ORE) {
            return DrillRules.orePower(fed);
        }
        Location site = placed.block();
        return DrillRules.petroleumPower(redstonePower(site), panelPowers(site), fuel > 0);
    }

    private static DrillBox boxOf(PlacedMachine placed) {
        Location site = placed.block();
        return DrillBox.around(site.getBlockX(), site.getBlockY(), site.getBlockZ(), placed.drill().definition().spec());
    }

    /**
     * Looks up the deposits in reach of the drill, and picks the one with the largest share left, as {@link DrillDeposits#choose} does.
     * When the pick is another deposit, the items it made for the old deposit are taken from it first, and it keeps the old deposit
     * until those items are in the outputs, so no item the outputs had no room for is dropped.
     */
    private void search(PlacedMachine placed, long tick) {
        DrillMining mining = placed.mining();
        Location site = placed.block();
        World world = site.getWorld();
        if (world == null) {
            mining.searched(tick, List.of(), false, null);
            return;
        }
        DrillDefinition definition = placed.drill().definition();
        List<Deposit> found;
        try {
            found = deposits.overlapping(world.getName(), site.getBlockX(), site.getBlockZ(),
                    DrillDeposits.reach(definition.spec()));
        } catch (RuntimeException e) {
            log.warning("Could not search the deposits of the drill at " + site.getBlockX() + " " + site.getBlockY() + " "
                    + site.getBlockZ() + ": " + e.getMessage());
            mining.searched(tick, mining.listed(), mining.targetInRange(), mining.current());
            return;
        }
        List<Deposit> listed = DrillDeposits.inSquare(found, boxOf(placed), definition.kind(), ores::containsKey);
        List<Deposit> mineable = DrillDeposits.mineable(listed, placed.selection());
        Deposit old = mining.current();
        Optional<Deposit> chosen = DrillDeposits.choose(mineable);
        boolean moving = old != null && (chosen.isEmpty() || chosen.get().id() != old.id());
        Optional<Deposit> next = chosen;
        if (moving) {
            take(placed, tick);
            if (mining.current() == null) {
                mining.batch().discard();
            }
            next = DrillDeposits.afterBatch(chosen, mining.current(), mining.batch().pending());
        }
        mining.searched(tick, listed, !mineable.isEmpty(), next.orElse(null));
    }

    /** A drill's background: green while it mines, red while it has no power, no room, no target or an exhausted deposit. */
    private static Tone drillTone(DrillStatus status) {
        return status == DrillStatus.RUNNING ? Tone.WORKING : Tone.STUCK;
    }

    /**
     * The status of a drill, from its power, its room for the next item and its deposit. Room is what the outputs can still take
     * after the items already made and waiting for a take. A petroleum drill's room is what the oil network takes of its petroleum.
     */
    private DrillStatus status(PlacedMachine placed, DrillPower power) {
        DrillMining mining = placed.mining();
        boolean idleWithoutFuel = placed.drill().definition().kind().fluid().isPresent() && !drawsFuel(placed);
        return DrillRules.status(power.powered() || idleWithoutFuel, freeSpace(placed) > 0, mining.targetInRange(),
                mining.current() != null);
    }

    /**
     * The room a drill has: for an ore drill, the items its outputs can still take of the product of the deposit it works on, less the
     * items waiting for a take; for a petroleum drill, unlimited unless the oil network refused its last push.
     */
    private long freeSpace(PlacedMachine placed) {
        DrillMining mining = placed.mining();
        if (placed.drill().definition().kind() == DrillKind.PETROLEUM) {
            return mining.networkRefused() ? 0 : Long.MAX_VALUE;
        }
        ItemRef product = product(mining.current());
        return DrillStorage.room(placed.menu().outputs(), product, limit(placed, product)) - mining.batch().pending();
    }

    /**
     * Makes the whole items of this tick from the drill's rate, and adds them to the batch. The batch never holds more than the
     * outputs can take, so the accumulator makes nothing the drill has no room for.
     */
    private void produce(PlacedMachine placed, DrillPower power) {
        DrillMining mining = placed.mining();
        Deposit deposit = mining.current();
        long available = deposit.remaining() - mining.batch().pending();
        long space = freeSpace(placed);
        if (available <= 0 || space <= 0) {
            return;
        }
        double rate = yield.itemsPerSecond(placed.drill().definition().spec().level()) * power.efficiency();
        long whole = mining.accumulator().step(rate, available);
        mining.batch().produce(Math.min(whole, space));
    }

    /**
     * Pumps the whole litres of this tick from a petroleum drill's deposit into the oil network. The accumulator turns the drill's rate
     * into whole litres, the network takes what it can of them, and the deposit gives up exactly the litres it took, so no petroleum is
     * drawn from the deposit that the network refused.
     */
    private void pump(PlacedMachine placed, DrillPower power, long tick) {
        DrillMining mining = placed.mining();
        if (mining.takesBlocked(tick)) {
            return;
        }
        Deposit deposit = mining.current();
        double rate = yield.itemsPerSecond(placed.drill().definition().spec().level()) * power.efficiency();
        long whole = mining.accumulator().step(rate, deposit.remaining());
        if (whole <= 0) {
            return;
        }
        double accepted = oil.supply(placed.block(), Fluid.PETROLEUM, whole);
        mining.setNetworkRefused(accepted <= 0);
        long litres = (long) Math.floor(accepted);
        if (litres <= 0) {
            return;
        }
        long taken;
        try {
            taken = deposits.take(deposit.id(), litres);
        } catch (RuntimeException e) {
            log.warning("Could not take " + litres + " from deposit " + deposit.id() + ": " + e.getMessage());
            mining.blockTakes(tick);
            return;
        }
        mining.took(tick, litres, taken);
    }

    /**
     * Takes the batch from the deposit the drill works on, as far as the outputs have room, and adds the taken items to the
     * outputs. A store that refuses the take holds takes back for a second, and keeps the batch.
     */
    private void take(PlacedMachine placed, long tick) {
        DrillMining mining = placed.mining();
        Deposit deposit = mining.current();
        ItemRef product = product(deposit);
        if (deposit == null || product == null) {
            mining.batch().discard();
            return;
        }
        int limit = limit(placed, product);
        List<Stack> outputs = placed.menu().outputs();
        long amount = Math.min(mining.batch().pending(), DrillStorage.room(outputs, product, limit));
        if (amount <= 0) {
            return;
        }
        long taken;
        try {
            taken = deposits.take(deposit.id(), amount);
        } catch (RuntimeException e) {
            log.warning("Could not take " + amount + " from deposit " + deposit.id() + ": " + e.getMessage());
            mining.blockTakes(tick);
            return;
        }
        if (taken > 0) {
            DrillStorage.add(outputs, product, (int) taken, limit);
        }
        mining.batch().settle(amount, taken);
        mining.took(tick, amount, taken);
    }

    /**
     * The item a deposit's ore gives: one per unit taken. Null for an ore without a definition, and for a fluid deposit such as the
     * crude petroleum deposit, which gives petroleum into the oil network instead of an item.
     */
    private ItemRef product(Deposit deposit) {
        if (deposit == null) {
            return null;
        }
        OreDefinition ore = ores.get(deposit.ore());
        return ore == null || ore.fluid() ? null : DrillRules.mined(ore.id(), ore.drop());
    }

    private int limit(PlacedMachine placed, ItemRef product) {
        return product == null ? DEFAULT_STACK_LIMIT : placed.menu().refs().limit(product);
    }

    /**
     * The redstone power the drill's block receives from its six neighbours, 0 to 15. It is read only while the chunks of all
     * six neighbours are loaded; otherwise it is 0, since a neighbour in an unloaded chunk cannot be read without loading it.
     */
    private static int redstonePower(Location site) {
        World world = site.getWorld();
        Block base = site.getBlock();
        for (BlockFace face : SIDES) {
            Block next = base.getRelative(face);
            if (!world.isChunkLoaded(next.getX() >> 4, next.getZ() >> 4)) {
                return 0;
            }
        }
        return base.getBlockPower();
    }

    /** The power of each daylight detector touching the drill, read from loaded chunks only. */
    private static List<Integer> panelPowers(Location site) {
        List<Integer> powers = new ArrayList<>();
        Block base = site.getBlock();
        for (BlockFace face : SIDES) {
            Block next = base.getRelative(face);
            if (!site.getWorld().isChunkLoaded(next.getX() >> 4, next.getZ() >> 4)) {
                continue;
            }
            if (next.getType() == Material.DAYLIGHT_DETECTOR && next.getBlockData() instanceof AnaloguePowerable panel) {
                powers.add(panel.getPower());
            }
        }
        return powers;
    }

    /**
     * Shows the drill's hologram while it has power and room, and refreshes its info item. The hologram shows the rate while the drill
     * runs, and its status when it does not, together with the deposit it works on. The screen shows the same lines, under its name and
     * its status.
     */
    private void showStats(PlacedMachine placed, DrillPower power, int fuel, EnergyService.Reading reading) {
        MachineMenu menu = placed.menu();
        DrillMining mining = placed.mining();
        DrillDefinition definition = placed.drill().definition();
        DrillKind kind = definition.kind();
        DrillStatus status = status(placed, power);
        animation.report(placed, status == DrillStatus.RUNNING);
        double rate = status == DrillStatus.RUNNING
                ? yield.itemsPerSecond(definition.spec().level()) * power.efficiency() : 0.0;
        Deposit current = mining.current();
        String depositLine = DrillText.deposit(oreName(current), current);
        ItemRef product = product(current);
        int capacity = kind == DrillKind.ORE ? DrillTable.OUTPUT_SLOTS * limit(placed, product) : 0;
        List<String> lines = EnergyText.withGrid(HologramText.drillLines(kind, definition.spec(), power, status, rate,
                depositLine, DrillStorage.stored(menu.outputs()), capacity, fuel),
                reading.voltageV(), reading.demandW(), reading.satisfaction());
        if (power.powered() && freeSpace(placed) > 0) {
            if (placed.hologram() == null) {
                holograms.create(HologramService.spotAbove(placed.block(), Footprint.heightOf(placed.machine().id())), lines)
                        .ifPresent(placed::setHologram);
            }
            if (placed.hologram() != null) {
                placed.hologram().setTone(drillTone(status));
                placed.hologram().setLines(lines);
            }
        } else {
            store.releaseHologram(placed);
        }
        if (placed.screen() != null) {
            placed.screen().setLines(ScreenText.rows(definition.name(), status.text(kind), drillTone(status), lines));
        }
        menu.showInfo(info(definition, power, status, rate, depositLine));
    }

    /** The display name of an ore for the deposit line, or the deposit's ore id when the ore has no definition. */
    private String oreName(Deposit deposit) {
        if (deposit == null) {
            return "";
        }
        OreDefinition ore = ores.get(deposit.ore());
        return ore == null ? deposit.ore() : ore.name();
    }

    /** The info item: the drill's level, area, depth, status, rate, deposit, power and what powers it. */
    private static ItemStack info(DrillDefinition definition, DrillPower power, DrillStatus status, double rate,
                                  String depositLine) {
        ItemStack item = new ItemStack(Material.COMPASS, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(definition.name()));
            meta.lore(Text.lore(DrillText.infoLines(definition.kind(), definition.spec(), power, status, rate, depositLine)));
            item.setItemMeta(meta);
        }
        return item;
    }
}
