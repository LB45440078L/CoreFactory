package io.corefactory.paper.machine;

import io.corefactory.core.device.Footprint;
import io.corefactory.core.engine.Visuals;
import io.corefactory.core.hologram.HologramRules;
import io.corefactory.core.hologram.HologramText;
import io.corefactory.core.hologram.Tone;
import io.corefactory.core.process.MachineState;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.core.upgrade.LabLevels;
import io.corefactory.paper.audio.MachineSounds;
import io.corefactory.paper.energy.EnergyService;
import io.corefactory.paper.energy.EnergyText;
import io.corefactory.paper.hologram.HologramService;
import io.corefactory.paper.screen.ScreenText;
import io.corefactory.paper.water.WaterFeed;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Advances every machine in a loaded chunk by one tick, once per server tick. A machine whose host block has gone (an
 * explosion, a piston, a mod) is removed, and its items drop where the block was. Machines in unloaded chunks do not run.
 * A machine that is running shows the steam of {@code machine_work}, at most once every {@link #WORK_INTERVAL_TICKS}
 * ticks. A machine whose pass completes shows its own lab effect, from {@link ProcessFamily#labVisual}.
 */
public final class MachineTicker implements Runnable {

    private static final int DISPLAY_CHECK_TICKS = 20;
    private static final int MENU_PULL_TICKS = 5;
    private static final int TEXT_TICKS = 10;
    private static final long WORK_INTERVAL_TICKS = 40L;

    private final MachineStore store;
    private final ProcessCatalog catalog;
    private final ProcessEngine engine;
    private final DrillTicker drills;
    private final Visuals visuals;
    private final HologramService holograms;
    private final EnergyService energy;
    private final LabLevels levels;
    private final WaterFeed water;
    private final RefineryTicker refineries;
    /** The machines whose failure has been logged, so each one is reported once. */
    private final Set<String> reported = new HashSet<>();
    private final Random random = new Random();
    private final int saveIntervalTicks;
    private long tick;

    public MachineTicker(MachineStore store, ProcessCatalog catalog, ProcessEngine engine, DrillTicker drills,
                         Visuals visuals, HologramService holograms, EnergyService energy, LabLevels levels,
                         int saveIntervalTicks, WaterFeed water, RefineryTicker refineries) {
        this.store = store;
        this.catalog = catalog;
        this.engine = engine;
        this.drills = drills;
        this.visuals = visuals;
        this.holograms = holograms;
        this.energy = energy;
        this.levels = levels;
        this.saveIntervalTicks = Math.max(1, saveIntervalTicks);
        this.water = water;
        this.refineries = refineries;
    }

    @Override
    public void run() {
        tick++;
        for (PlacedMachine placed : store.all()) {
            try {
                tickDevice(placed);
            } catch (RuntimeException error) {
                report(placed, error);
            }
        }
        if (tick % saveIntervalTicks == 0) {
            store.save();
        }
    }

    /**
     * One machine's work this tick. A failure in one machine is reported by {@link #report} and does not stop the others.
     */
    private void tickDevice(PlacedMachine placed) {
        if (!placed.isLoaded()) {
            return;
        }
        store.settle(placed);
        if (!hostIntact(placed)) {
            store.remove(placed, true);
            return;
        }
        if (placed.drill() != null) {
            drills.tick(placed, tick);
            return;
        }
        if (RefineryTicker.MACHINE.equals(placed.machine().id())) {
            if (tick % DISPLAY_CHECK_TICKS == 0) {
                store.ensureDisplay(placed);
            }
            refineries.tick(placed, tick);
            return;
        }
        boolean open = placed.menu().isOpen();
        if (open) {
            placed.menu().push();
        }
        if (tick % DISPLAY_CHECK_TICKS == 0) {
            store.ensureDisplay(placed);
        }
        EnergyService.Reading reading = energy.reading(placed.block());
        double grid = reading.satisfaction();
        LabLevels.Pace pace = LabLevels.pace(placed.workCredit(), grid, levels.speed(placed.level()));
        placed.setWorkCredit(pace.credit());
        ProcessDefinition waterProcess = waterProcess(placed);
        boolean watered = waterProcess != null && water.canSupply(placed.block(), waterProcess.ticks());
        ProcessEngine.Step step = runProcess(placed, pace.runs(), watered);
        int total = placed.state().processId() == null ? 0 : catalog.process(placed.state().processId())
                .map(ProcessDefinition::ticks).orElse(0);
        placed.menu().refresh(placed.state().progress(), total, step == ProcessEngine.Step.BLOCKED);
        placed.menu().showFirebox(Firebox.lit(step == ProcessEngine.Step.RUNNING,
                Firebox.fuelItems(placed.state().fuel(), ProcessEngine.FUEL)));
        Optional<String> waterWait = waterWait(placed, waterProcess, watered);
        List<String> lines = machineLines(placed, total, reading, waterWait);
        updateHologram(placed, lines, reading, waterWait);
        if (tick % TEXT_TICKS == 0) {
            updateScreen(placed, lines, reading, waterWait);
        }
        if (open && tick % MENU_PULL_TICKS == 0) {
            placed.menu().pull();
        }
        if (step == ProcessEngine.Step.COMPLETED) {
            showComplete(placed);
        } else if (step == ProcessEngine.Step.RUNNING) {
            showWork(placed);
        }
    }

    /** Logs a machine's failure once, with its position and type, so one bad device does not stop the others. */
    private void report(PlacedMachine placed, RuntimeException error) {
        String key = placed.machine().id() + " " + placed.block();
        if (reported.add(key)) {
            Logger.getLogger("CoreFactory").log(Level.SEVERE,
                    "The machine " + placed.machine().id() + " at " + placed.block() + " failed and is skipped each tick.", error);
        }
    }

    /**
     * The process of the machine that takes water from its bucket input: the current one, or else the first one of the machine that
     * takes water. Null when the machine has none.
     */
    private ProcessDefinition waterProcess(PlacedMachine placed) {
        if (placed.state().processId() != null) {
            return catalog.process(placed.state().processId()).filter(ProcessEngine::takesWater).orElse(null);
        }
        return catalog.processesFor(placed.machine().id()).stream().filter(ProcessEngine::takesWater).findFirst().orElse(null);
    }

    /** The water line of a machine that waits for water only: its bucket is the one input it lacks, and a pipe joins it. */
    private Optional<String> waterWait(PlacedMachine placed, ProcessDefinition waterProcess, boolean watered) {
        if (waterProcess == null || watered || !engine.supplied(waterProcess, placed.state(), true)
                || engine.supplied(waterProcess, placed.state())) {
            return Optional.empty();
        }
        return water.waterLine(placed.block());
    }

    /**
     * Runs the process ticks of one server tick. A machine of a higher level runs more than one in a tick. Stops early when the
     * machine goes idle or its outputs are full. The step is COMPLETED when any of the ticks finished a pass.
     */
    private ProcessEngine.Step runProcess(PlacedMachine placed, int runs, boolean watered) {
        ProcessEngine.Step step = ProcessEngine.Step.IDLE;
        boolean completed = false;
        for (int run = 0; run < runs; run++) {
            step = engine.tick(placed.state(), random, watered);
            completed |= step == ProcessEngine.Step.COMPLETED;
            if (step == ProcessEngine.Step.IDLE || step == ProcessEngine.Step.BLOCKED) {
                break;
            }
        }
        return completed ? ProcessEngine.Step.COMPLETED : step;
    }

    /** The lab effect of a machine id, or empty when the machine has none. */
    static Optional<String> labEffect(String machineId) {
        return ProcessFamily.labVisual(machineId).map(ProcessFamily.LabVisual::effect);
    }

    /**
     * The lines of a machine's hologram, which its screen shows too: its name and level with the process, its progress bar and its
     * percentage with the time left, the network line, and the water line when the machine waits for water.
     */
    private static List<String> machineLines(PlacedMachine placed, int total, EnergyService.Reading reading,
                                             Optional<String> waterWait) {
        String title = placed.machine().name() + " &7Level " + LabLevels.numeral(placed.level());
        List<String> lines = new ArrayList<>(EnergyText.withGrid(HologramText.forMachine(title, placed.state(), total),
                reading.voltageV(), reading.demandW(), reading.satisfaction()));
        waterWait.ifPresent(lines::add);
        return lines;
    }

    /**
     * Shows the machine's hologram while it is active, with its progress and its network line, and removes it once it is idle again.
     */
    private void updateHologram(PlacedMachine placed, List<String> lines, EnergyService.Reading reading,
                                Optional<String> waterWait) {
        if (!HologramRules.shouldShow(engine, placed.state()) && waterWait.isEmpty()) {
            store.releaseHologram(placed);
            return;
        }
        if (placed.hologram() == null) {
            holograms.create(HologramService.spotAbove(placed.block(), Footprint.heightOf(placed.machine().id())), lines)
                    .ifPresent(placed::setHologram);
        }
        if (placed.hologram() != null) {
            placed.hologram().setTone(machineTone(placed.state(), reading, waterWait));
            placed.hologram().setLines(lines);
        }
    }

    /**
     * Shows the machine's screen: its name, its status word and the lines of its hologram. The status word is Idle when no process runs,
     * waiting for water when the machine lacks only water, waiting for power when the network gives it less than all it asks, and working
     * otherwise.
     */
    private void updateScreen(PlacedMachine placed, List<String> lines, EnergyService.Reading reading, Optional<String> waterWait) {
        if (placed.screen() == null) {
            return;
        }
        MachineState state = placed.state();
        String status;
        if (state.processId() == null) {
            status = "Idle";
        } else if (waterWait.isPresent()) {
            status = "Waiting for water";
        } else if (reading.satisfaction() < 1.0) {
            status = "Waiting for power";
        } else {
            status = "Working";
        }
        placed.screen().setLines(ScreenText.rows(placed.machine().name(), status, machineTone(state, reading, waterWait), lines));
    }

    /**
     * A machine's background: idle machines show none, a running one is green, and one that waits for water or for power is red.
     */
    private static Tone machineTone(MachineState state, EnergyService.Reading reading, Optional<String> waterWait) {
        if (state.processId() == null) {
            return Tone.NEUTRAL;
        }
        return waterWait.isPresent() || reading.satisfaction() < 1.0 ? Tone.STUCK : Tone.WORKING;
    }

    private void showWork(PlacedMachine placed) {
        if (tick - placed.lastWorkTick() < WORK_INTERVAL_TICKS) {
            return;
        }
        placed.markWork(tick);
        visuals.play("machine_work", at(placed), 1.0);
        MachineSounds.working(placed.machine().id(), at(placed));
    }

    private void showComplete(PlacedMachine placed) {
        MachineSounds.complete(placed.machine().id(), at(placed));
        ProcessFamily.labVisual(placed.machine().id()).ifPresent(lab ->
                visuals.play(lab.effect(), at(placed), lab.scale()));
    }

    private static Location at(PlacedMachine placed) {
        return placed.block().clone().add(0.5, 1.0, 0.5);
    }

    private static boolean hostIntact(PlacedMachine placed) {
        if (placed.host() == null) {
            return true;
        }
        Location block = placed.block();
        return block.getBlock().getType() == placed.host();
    }
}
