package io.corefactory.spigot.machine;

import io.corefactory.core.config.Messages;
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
import io.corefactory.core.drill.DrillStatus;
import io.corefactory.core.drill.DrillStorage;
import io.corefactory.core.drill.DrillTable;
import io.corefactory.core.drill.DrillText;
import io.corefactory.core.drill.PowerSource;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.hologram.HologramText;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import io.corefactory.spigot.audio.MachineSounds;
import io.corefactory.spigot.energy.EnergyService;
import io.corefactory.spigot.energy.EnergyText;
import io.corefactory.spigot.hologram.HologramService;
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
import java.util.OptionalLong;
import java.util.logging.Logger;

/**
 * Runs every drill in a loaded chunk, once per server tick. A drill's power is the strongest of three sources: the redstone
 * its block receives, each daylight detector beside it, and charcoal in its fuel slot. A drill works the deposits that touch
 * its square and that its kind and selection allow, one deposit at a time: the one with the most left, which it keeps until
 * that one is empty. Each tick while it runs, it makes {@code itemsPerSecond(level) * power} items per second, whole items
 * at a time, and the whole items wait in a batch that is taken from the deposit a few times a second, never more than the
 * deposit holds and never more than the outputs have room for. The fuel burns once per fuel cycle of the drill's level, while
 * it runs. The hologram and the info item are refreshed every {@link #TEXT_TICKS} ticks.
 */
public final class DrillTicker {

    private static final int DISPLAY_CHECK_TICKS = 20;
    private static final int MENU_PULL_TICKS = 5;
    private static final int TEXT_TICKS = 10;
    private static final int DEFAULT_STACK_LIMIT = 64;
    private static final BlockFace[] SIDES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

    private final MachineStore store;
    private final HologramService holograms;
    private final DepositStore deposits;
    private final DepositSettings settings;
    private final DrillYield yield;
    private final Map<String, OreDefinition> ores;
    private final EnergyService energy;
    private final Logger log;
    private final DrillAnimation animation = new DrillAnimation();

    /**
     * @param deposits the store of the planned deposits, which the drills take from
     * @param settings the deposit settings; the search reach is the largest radius a deposit may have, so it does not read this
     * @param yield    the items per second of each level at full power
     * @param ores     the ore definitions by id, which give a deposit's drop
     */
    public DrillTicker(MachineStore store, HologramService holograms, DepositStore deposits, DepositSettings settings,
                       DrillYield yield, Map<String, OreDefinition> ores, EnergyService energy, Logger log) {
        this.store = store;
        this.holograms = holograms;
        this.deposits = deposits;
        this.settings = settings;
        this.yield = yield;
        this.ores = Map.copyOf(ores);
        this.energy = energy;
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
        Stack fuelSlot = menu.fuel().get(0);
        int fuel = kind.fuel().equals(fuelSlot.ref()) ? fuelSlot.count() : 0;
        EnergyService.Reading reading = energy.reading(placed.block());
        DrillPower power = DrillRules.withGrid(power(placed, kind, fuel), reading.satisfaction());
        boolean burns = kind == DrillKind.PETROLEUM ? DrillRules.burnsCharcoal(power)
                : power.source() == PowerSource.PETROLEUM;
        MachineSounds.drillRun(power.powered(), kind, placed.block(), tick);
        if (mining.searchDue(tick)) {
            search(placed, tick);
        }
        if (status(placed, power) == DrillStatus.RUNNING) {
            produce(placed, power);
            drill.advance(power.efficiency());
            if (drill.ready()) {
                drill.finishCycle(burns);
                if (burns && drill.fuelDue()) {
                    fuelSlot.set(kind.fuel(), fuel - 1);
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

    /** The power of a drill this cycle: petroleum from its fuel for an ore drill, and the strongest source for a petroleum one. */
    private static DrillPower power(PlacedMachine placed, DrillKind kind, int fuel) {
        if (kind == DrillKind.ORE) {
            return DrillRules.orePower(fuel > 0);
        }
        Location site = placed.block();
        return DrillRules.petroleumPower(redstonePower(site), panelPowers(site), fuel > 0);
    }

    private static DrillBox boxOf(PlacedMachine placed) {
        Location site = placed.block();
        return DrillBox.around(site.getBlockX(), site.getBlockY(), site.getBlockZ(), placed.drill().definition().spec());
    }

    /**
     * Looks up the deposits in reach of the drill, and keeps the one it works on while that one is still mineable and has something
     * left. When it must move on, the items it made for the old deposit are taken from it first, and it keeps the old deposit until
     * those items are in the outputs, so no item the outputs had no room for is dropped.
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
        OptionalLong oldId = old == null ? OptionalLong.empty() : OptionalLong.of(old.id());
        Optional<Deposit> chosen = DrillDeposits.choose(mineable, oldId);
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

    /**
     * The status of a drill, from its power, its room for the next item and its deposit. Room is what the outputs can still take
     * after the items already made and waiting for a take.
     */
    private DrillStatus status(PlacedMachine placed, DrillPower power) {
        DrillMining mining = placed.mining();
        return DrillRules.status(power.powered(), freeSpace(placed) > 0, mining.targetInRange(), mining.current() != null);
    }

    /** The items the outputs can still take of the product of the deposit the drill works on, less the items waiting for a take. */
    private long freeSpace(PlacedMachine placed) {
        DrillMining mining = placed.mining();
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

    /** The item a deposit's ore gives: one per unit taken. Null for an ore without a definition. */
    private ItemRef product(Deposit deposit) {
        if (deposit == null) {
            return null;
        }
        OreDefinition ore = ores.get(deposit.ore());
        return ore == null ? null : DrillRules.mined(ore.id(), ore.drop());
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
     * runs, and its status when it does not, together with the deposit it works on.
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
        if (power.powered() && freeSpace(placed) > 0) {
            ItemRef product = product(current);
            List<String> lines = EnergyText.withGrid(HologramText.drillLines(kind, definition.spec(), power, status, rate,
                    depositLine, DrillStorage.stored(menu.outputs()), DrillTable.OUTPUT_SLOTS * limit(placed, product), fuel),
                    reading.voltageV(), reading.demandW(), reading.satisfaction());
            if (placed.hologram() == null) {
                holograms.create(HologramService.spotAbove(placed.block()), lines).ifPresent(placed::setHologram);
            }
            if (placed.hologram() != null) {
                placed.hologram().setLines(lines);
            }
        } else {
            store.releaseHologram(placed);
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
            meta.setDisplayName(Messages.color(definition.name()));
            meta.setLore(DrillText.infoLines(definition.kind(), definition.spec(), power, status, rate, depositLine).stream()
                    .map(Messages::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
