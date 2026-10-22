package io.corefactory.spigot.logistics;

import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Port;
import io.corefactory.spigot.energy.DeviceRegistry;
import io.corefactory.spigot.energy.GeneratorDevice;
import io.corefactory.spigot.hook.ContainerProtection;
import io.corefactory.spigot.machine.DrillPort;
import io.corefactory.spigot.machine.ItemRefs;
import io.corefactory.spigot.machine.LabPort;
import io.corefactory.spigot.machine.MachineStore;
import io.corefactory.spigot.machine.PlacedMachine;
import io.corefactory.spigot.machine.VanillaContainerPort;
import io.corefactory.spigot.turret.TurretPort;
import io.corefactory.spigot.turret.TurretRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;
import java.util.UUID;

/**
 * The ports at a block: a drill's output slots, a lab tool's barrel, a generator's fuel for a kind that burns fuel, a turret's ammunition,
 * and a vanilla storage block (a chest, a trapped chest, a barrel, a shulker box or a copper chest) as a sink and as a picker's source. The
 * plugin's own devices are tried first, and a block a device occupies never falls through to the vanilla storage. Any other block has no
 * port, so a belt or a picker that points at it waits. A block in a world that is not loaded has none. A vanilla container that a claim
 * system protects takes items from a belt, a picker or a splitter, and gives items to a picker, only when the owner of that device is
 * trusted there; a device with no owner is refused.
 */
final class LogisticsPorts {

    private final MachineStore machines;
    private final DeviceRegistry energy;
    private final TurretRegistry turrets;
    private final ItemRefs refs;
    private final ContainerProtection containers;

    LogisticsPorts(MachineStore machines, DeviceRegistry energy, TurretRegistry turrets, ItemRefs refs,
                   ContainerProtection containers) {
        this.machines = machines;
        this.energy = energy;
        this.turrets = turrets;
        this.refs = refs;
        this.containers = containers;
    }

    /**
     * The port at a block of a world, or empty when the block has none or its chunk is not loaded.
     *
     * @param putOwner  the owner of the devices that put items into the block, or null when none is known; it is only used for a vanilla
     *                  container
     * @param takeOwner the owner of the pickers that take items from the block, or null when none is known; it is only used for a vanilla
     *                  container
     */
    Optional<Port> at(UUID world, Pos cell, UUID putOwner, UUID takeOwner) {
        World loaded = Bukkit.getWorld(world);
        if (loaded == null || !loaded.isChunkLoaded(cell.x() >> 4, cell.z() >> 4)) {
            return Optional.empty();
        }
        Location block = new Location(loaded, cell.x(), cell.y(), cell.z());
        Optional<Port> machine = machines.at(block).flatMap(this::machinePort);
        if (machine.isPresent()) {
            return machine;
        }
        Optional<Port> turret = turrets.at(block).map(TurretPort::new);
        if (turret.isPresent()) {
            return turret;
        }
        Optional<Port> generator = energy.at(block).filter(device -> device instanceof GeneratorDevice)
                .filter(device -> GeneratorFuel.sourceOf(((GeneratorDevice) device).generatorKind()).isPresent())
                .map(device -> fuelPort((GeneratorDevice) device));
        if (generator.isPresent()) {
            return generator;
        }
        if (machines.at(block).isPresent() || turrets.at(block).isPresent() || energy.at(block).isPresent()) {
            return Optional.empty();
        }
        return VanillaContainerPort.of(loaded.getBlockAt(cell.x(), cell.y(), cell.z()), refs, containers, putOwner, takeOwner)
                .map(port -> port);
    }

    /** The port of a placed machine: a drill's output slots, or a lab tool's barrel. */
    private Optional<Port> machinePort(PlacedMachine placed) {
        if (placed.drill() != null) {
            return DrillPort.of(placed).map(port -> port);
        }
        return LabPort.of(placed, refs).map(port -> port);
    }

    private static Port fuelPort(GeneratorDevice generator) {
        return new FuelPort(generator.generatorKind(), new FuelPort.Tank() {
            @Override
            public int count() {
                return generator.fuel();
            }

            @Override
            public void addOne() {
                generator.addFuel();
            }
        });
    }
}
