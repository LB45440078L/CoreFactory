package io.corefactory.spigot.machine;

import io.corefactory.core.config.Messages;
import io.corefactory.core.drill.DrillBox;
import io.corefactory.core.drill.DrillDefinition;
import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.drill.DrillPower;
import io.corefactory.core.drill.DrillRules;
import io.corefactory.core.drill.DrillState;
import io.corefactory.core.drill.DrillStatus;
import io.corefactory.core.drill.DrillStorage;
import io.corefactory.core.drill.DrillTable;
import io.corefactory.core.drill.DrillText;
import io.corefactory.core.drill.PetroleumVolume;
import io.corefactory.core.drill.PowerSource;
import io.corefactory.core.drill.TargetChoice;
import io.corefactory.core.geology.DepositVolumes;
import io.corefactory.core.geology.GeologyRegistry;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.geology.Placement;
import io.corefactory.core.hologram.HologramText;
import io.corefactory.core.ledger.ChunkBudgets;
import io.corefactory.core.ledger.MiningLedger;
import io.corefactory.core.ledger.OreBudget;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import io.corefactory.spigot.audio.MachineSounds;
import io.corefactory.spigot.energy.EnergyService;
import io.corefactory.spigot.energy.EnergyText;
import io.corefactory.spigot.geology.GeologyWorker;
import io.corefactory.spigot.geology.OreChunks;
import io.corefactory.spigot.hologram.HologramService;
import org.bukkit.Chunk;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Runs every drill in a loaded chunk, once per server tick. A drill's power is the strongest of three sources: the redstone
 * its block receives, each daylight detector beside it, and charcoal in its fuel slot. Its cycle advances by the efficiency
 * of that power, and a full cycle yields one petroleum or mines one ore block. Its items are in its barrel: the fuel slot
 * first, then the output grid. The block it works on is the nearest one its selection takes and its budget still holds.
 * Every mined unit is written to the drill's ledger and to the chunk's budget before the block changes. A cycle waits while
 * the outputs have no room; when no selected target is left in the box the drill stops until it is moved. The hologram and
 * the info item are refreshed every {@link #TEXT_TICKS} ticks.
 */
public final class DrillTicker {

    private static final int DISPLAY_CHECK_TICKS = 20;
    private static final int MENU_PULL_TICKS = 5;
    private static final int TEXT_TICKS = 10;
    private static final int ORE_STACK_LIMIT = 64;
    private static final String PETROLEUM = "petroleum";
    private static final BlockFace[] SIDES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

    private final MachineStore store;
    private final GeologyWorker geology;
    private final HologramService holograms;
    private final MiningLedger ledger;
    private final ChunkBudgets budgets;
    private final EnergyService energy;
    private final Logger log;
    private final int depositIndex;
    private final DrillAnimation animation = new DrillAnimation();

    public DrillTicker(MachineStore store, GeologyWorker geology, HologramService holograms, MiningLedger ledger,
                       ChunkBudgets budgets, EnergyService energy, Logger log) {
        this.store = store;
        this.geology = geology;
        this.holograms = holograms;
        this.ledger = ledger;
        this.budgets = budgets;
        this.energy = energy;
        this.log = log;
        this.depositIndex = geology.service().indexOf(DrillRules.DEPOSIT);
    }

    /** Advances one drill by one tick. Call only for a drill whose chunk is loaded. */
    public void tick(PlacedMachine placed, long tick) {
        MachineMenu menu = placed.menu();
        DrillState drill = placed.drill();
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
        double grid = reading.satisfaction();
        DrillPower power = DrillRules.withGrid(power(placed, kind, fuel), grid);
        boolean burns = kind == DrillKind.PETROLEUM ? DrillRules.burnsCharcoal(power)
                : power.source() == PowerSource.PETROLEUM;
        MachineSounds.drillRun(power.powered(), kind, placed.block(), tick);
        if (power.powered()) {
            drill.advance(power.efficiency());
            if (drill.ready() && yieldOne(placed)) {
                drill.finishCycle(burns);
                if (burns && drill.fuelDue()) {
                    fuelSlot.set(kind.fuel(), fuel - 1);
                    drill.fuelPaid();
                }
            }
        }
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

    /** The product of one full cycle: a petroleum pumped from a deposit, or an ore block mined. False when the cycle must wait. */
    private boolean yieldOne(PlacedMachine placed) {
        DrillBox box = boxOf(placed);
        Map<String, List<Placement>> byOre = candidates(placed, box);
        Optional<Placement> target = choose(placed, box, byOre);
        if (target.isEmpty()) {
            return false;
        }
        return placed.drill().definition().kind().removesTarget()
                ? mine(placed, target.get()) : pump(placed, target.get());
    }

    private static DrillBox boxOf(PlacedMachine placed) {
        Location site = placed.block();
        return DrillBox.around(site.getBlockX(), site.getBlockY(), site.getBlockZ(), placed.drill().definition().spec());
    }

    /** The registered blocks in the box that the drill's kind may take, grouped by ore id. */
    private Map<String, List<Placement>> candidates(PlacedMachine placed, DrillBox box) {
        World world = placed.block().getWorld();
        if (world == null) {
            return Map.of();
        }
        return DrillSite.byOre(world, box, geology.service(),
                DrillRules.targets(placed.drill().definition().kind(), depositIndex));
    }

    /** The nearest candidate the drill's selection takes. Each ore's remaining count in the box is its registered count. */
    private static Optional<Placement> choose(PlacedMachine placed, DrillBox box, Map<String, List<Placement>> byOre) {
        Location site = placed.block();
        return TargetChoice.choose(byOre, box, site.getBlockX(), site.getBlockY(), site.getBlockZ(), placed.selection(),
                ore -> byOre.getOrDefault(ore, List.of()).size());
    }

    /**
     * Pumps one petroleum from a deposit. The deposit's volume goes down by one; at zero the block becomes air and its
     * entry is dropped, so the drill looks for another deposit next time.
     */
    private boolean pump(PlacedMachine placed, Placement target) {
        World world = placed.block().getWorld();
        Block block = world.getBlockAt(target.x(), target.y(), target.z());
        Chunk chunk = world.getChunkAt(target.x() >> 4, target.z() >> 4);
        if (block.getType() != geology.host(target.oreIndex())) {
            forget(chunk, target);
            return false;
        }
        OreDefinition deposit = geology.service().ore(target.oreIndex());
        ItemRef petroleum = ItemRef.item(PETROLEUM);
        int limit = placed.menu().refs().limit(petroleum);
        if (DrillStorage.room(placed.menu().outputs(), petroleum, limit) < 1) {
            return false;
        }
        int[] volumes = OreChunks.volumes(chunk);
        int remaining = DepositVolumes.get(volumes, target.x(), target.y(), target.z(),
                PetroleumVolume.volumeOf(deposit.volume()));
        if (!record(placed, world, target, deposit.id(), chunk)) {
            return false;
        }
        DrillStorage.add(placed.menu().outputs(), petroleum, 1, limit);
        int left = PetroleumVolume.afterPump(remaining);
        if (PetroleumVolume.depleted(left)) {
            block.setType(Material.AIR, false);
            forget(chunk, target);
            OreChunks.storeVolumes(chunk, DepositVolumes.remove(volumes, target.x(), target.y(), target.z()));
        } else {
            OreChunks.storeVolumes(chunk, DepositVolumes.put(volumes, target.x(), target.y(), target.z(), left));
        }
        return true;
    }

    /**
     * Mines one registered ore block: the block becomes air without physics, its registry entry goes, and its raw items
     * go to the outputs. A block that is no longer the ore's host is only forgotten, and the cycle waits for the next target.
     */
    private boolean mine(PlacedMachine placed, Placement target) {
        World world = placed.block().getWorld();
        Block block = world.getBlockAt(target.x(), target.y(), target.z());
        Chunk chunk = world.getChunkAt(target.x() >> 4, target.z() >> 4);
        if (block.getType() != geology.host(target.oreIndex())) {
            forget(chunk, target);
            return false;
        }
        OreDefinition ore = geology.service().ore(target.oreIndex());
        ItemRef raw = DrillRules.mined(ore.id(), ore.drop());
        int limit = placed.menu().refs().limit(raw);
        if (DrillStorage.room(placed.menu().outputs(), raw, limit) < ore.dropMax()) {
            return false;
        }
        if (!record(placed, world, target, ore.id(), chunk)) {
            return false;
        }
        block.setType(Material.AIR, false);
        forget(chunk, target);
        int amount = ThreadLocalRandom.current().nextInt(ore.dropMin(), ore.dropMax() + 1);
        return DrillStorage.add(placed.menu().outputs(), raw, amount, limit);
    }

    /**
     * Checks the chunk's budget for the ore, then adds one unit to the drill's ledger and to the chunk's mined count. A
     * chunk with no budget row (laid before budgets) has nothing to check against, so it is allowed. Returns false, and
     * changes nothing, when the budget is spent or the database refuses the write.
     */
    private boolean record(PlacedMachine placed, World world, Placement target, String oreId, Chunk chunk) {
        String name = world.getName();
        try {
            Optional<OreBudget> budget = budgets.find(name, chunk.getX(), chunk.getZ(), oreId);
            if (budget.isPresent() && budget.get().remaining() <= 0) {
                log.warning("The " + oreId + " budget of chunk " + chunk.getX() + ", " + chunk.getZ() + " of " + name
                        + " is spent, but a block is still registered at " + target.x() + " " + target.y() + " "
                        + target.z() + ". The entry is dropped.");
                forget(chunk, target);
                return false;
            }
            String key = DrillSite.keyOf(placed.block());
            ledger.add(key, oreId, 1);
            budgets.recordMined(name, chunk.getX(), chunk.getZ(), oreId, 1);
            placed.setMinedBefore(true);
            return true;
        } catch (RuntimeException e) {
            log.warning("Could not record the mining of " + oreId + " at " + target.x() + " " + target.y() + " "
                    + target.z() + ": " + e.getMessage());
            return false;
        }
    }

    private static void forget(Chunk chunk, Placement target) {
        int[] registry = OreChunks.registry(chunk);
        OreChunks.storeRegistry(chunk, GeologyRegistry.remove(registry, target.x(), target.y(), target.z()));
    }

    /**
     * Whether the drill has ever mined, read from the ledger once and then kept on the drill. A read that fails counts as no.
     */
    private boolean minedBefore(PlacedMachine placed) {
        if (placed.minedBefore() == null) {
            try {
                placed.setMinedBefore(ledger.minedBy(DrillSite.keyOf(placed.block())).values().stream()
                        .anyMatch(count -> count > 0));
            } catch (RuntimeException e) {
                return false;
            }
        }
        return placed.minedBefore();
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
     * Shows the drill's hologram while it has power and room, and refreshes its info item. The hologram shows the rate while
     * the drill runs, and its status when it does not.
     */
    private void showStats(PlacedMachine placed, DrillPower power, int fuel, EnergyService.Reading reading) {
        MachineMenu menu = placed.menu();
        DrillDefinition definition = placed.drill().definition();
        DrillKind kind = definition.kind();
        ItemRef product = kind == DrillKind.PETROLEUM ? ItemRef.item(PETROLEUM) : null;
        int limit = product == null ? ORE_STACK_LIMIT : menu.refs().limit(product);
        boolean roomLeft = DrillStorage.room(menu.outputs(), product, limit) > 0;
        DrillBox box = boxOf(placed);
        Map<String, List<Placement>> byOre = candidates(placed, box);
        boolean targetInRange = choose(placed, box, byOre).isPresent();
        boolean hasHistory = !byOre.isEmpty() || minedBefore(placed);
        DrillStatus status = DrillRules.status(power.powered(), roomLeft, targetInRange, hasHistory);
        animation.report(placed, status == DrillStatus.RUNNING);
        if (power.powered() && roomLeft) {
            List<String> lines = EnergyText.withGrid(HologramText.drillLines(kind, definition.spec(), power, status,
                    DrillStorage.stored(menu.outputs()), DrillTable.OUTPUT_SLOTS * limit, fuel), reading.voltageV(),
                    reading.demandW(), reading.satisfaction());
            if (placed.hologram() == null) {
                holograms.create(HologramService.spotAbove(placed.block()), lines).ifPresent(placed::setHologram);
            }
            if (placed.hologram() != null) {
                placed.hologram().setLines(lines);
            }
        } else {
            store.releaseHologram(placed);
        }
        menu.showInfo(info(definition, power, status));
    }

    /** The info item: the drill's level, area, depth, status, rate, power and what powers it. */
    private static ItemStack info(DrillDefinition definition, DrillPower power, DrillStatus status) {
        ItemStack item = new ItemStack(Material.COMPASS, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(definition.name()));
            meta.setLore(DrillText.infoLines(definition.kind(), definition.spec(), power, status).stream()
                    .map(Messages::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
