package io.corefactory.spigot.machine;

import io.corefactory.core.engine.Visuals;
import io.corefactory.core.hologram.HologramRules;
import io.corefactory.core.hologram.HologramText;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.spigot.audio.MachineSounds;
import io.corefactory.spigot.energy.EnergyService;
import io.corefactory.spigot.energy.EnergyText;
import io.corefactory.spigot.energy.WorkPace;
import io.corefactory.spigot.hologram.HologramService;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Advances every machine in a loaded chunk by one tick, once per server tick. A machine whose host block has gone (an
 * explosion, a piston, a mod) is removed, and its items drop where the block was. Machines in unloaded chunks do not run.
 * A machine that is running shows the steam of {@code machine_work}, at most once every {@link #WORK_INTERVAL_TICKS}
 * ticks. A machine whose pass completes shows its own lab effect, from {@link ProcessFamily#labVisual}.
 */
public final class MachineTicker implements Runnable {

    private static final int DISPLAY_CHECK_TICKS = 20;
    private static final int MENU_PULL_TICKS = 5;
    private static final long WORK_INTERVAL_TICKS = 40L;

    private final MachineStore store;
    private final ProcessCatalog catalog;
    private final ProcessEngine engine;
    private final DrillTicker drills;
    private final Visuals visuals;
    private final HologramService holograms;
    private final EnergyService energy;
    private final Random random = new Random();
    private final int saveIntervalTicks;
    private long tick;

    public MachineTicker(MachineStore store, ProcessCatalog catalog, ProcessEngine engine, DrillTicker drills,
                         Visuals visuals, HologramService holograms, EnergyService energy, int saveIntervalTicks) {
        this.store = store;
        this.catalog = catalog;
        this.engine = engine;
        this.drills = drills;
        this.visuals = visuals;
        this.holograms = holograms;
        this.energy = energy;
        this.saveIntervalTicks = Math.max(1, saveIntervalTicks);
    }

    @Override
    public void run() {
        tick++;
        for (PlacedMachine placed : store.all()) {
            if (!placed.isLoaded()) {
                continue;
            }
            store.settle(placed);
            if (!hostIntact(placed)) {
                store.remove(placed, true);
                continue;
            }
            if (placed.drill() != null) {
                drills.tick(placed, tick);
                continue;
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
            WorkPace pace = WorkPace.step(placed.workCredit(), grid);
            placed.setWorkCredit(pace.credit());
            ProcessEngine.Step step = pace.runTick() ? engine.tick(placed.state(), random) : ProcessEngine.Step.IDLE;
            int total = placed.state().processId() == null ? 0 : catalog.process(placed.state().processId())
                    .map(ProcessDefinition::ticks).orElse(0);
            placed.menu().refresh(placed.state().progress(), total, step == ProcessEngine.Step.BLOCKED);
            placed.menu().showFirebox(Firebox.lit(step == ProcessEngine.Step.RUNNING,
                    Firebox.fuelItems(placed.state().fuel(), ProcessEngine.FUEL)));
            updateHologram(placed, total, reading);
            if (open && tick % MENU_PULL_TICKS == 0) {
                placed.menu().pull();
            }
            if (step == ProcessEngine.Step.COMPLETED) {
                showComplete(placed);
            } else if (step == ProcessEngine.Step.RUNNING) {
                showWork(placed);
            }
        }
        if (tick % saveIntervalTicks == 0) {
            store.save();
        }
    }

    /** The lab effect of a machine id, or empty when the machine has none. */
    static Optional<String> labEffect(String machineId) {
        return ProcessFamily.labVisual(machineId).map(ProcessFamily.LabVisual::effect);
    }

    /**
     * Shows the machine's hologram while it is active, with its progress and its network line, and removes it once it is idle again.
     */
    private void updateHologram(PlacedMachine placed, int total, EnergyService.Reading reading) {
        if (!HologramRules.shouldShow(engine, placed.state())) {
            store.releaseHologram(placed);
            return;
        }
        List<String> lines = EnergyText.withGrid(HologramText.forMachine(placed.machine().name(), placed.state(), total),
                reading.voltageV(), reading.demandW(), reading.satisfaction());
        if (placed.hologram() == null) {
            holograms.create(HologramService.spotAbove(placed.block()), lines).ifPresent(placed::setHologram);
        }
        if (placed.hologram() != null) {
            placed.hologram().setLines(lines);
        }
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
