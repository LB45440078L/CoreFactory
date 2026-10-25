package io.corefactory.core.process;

import io.corefactory.core.config.ConfigValues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The machines and processes of {@code processes.yml}. A bad entry is logged and left out. A process whose machine does
 * not exist, or that needs more slots than its machine has, is left out too, so the rest still runs.
 */
public final class ProcessCatalog {

    private final Map<String, Machine> machines;
    private final Map<String, ProcessDefinition> processes;
    private final Map<String, List<ProcessDefinition>> byMachine = new LinkedHashMap<>();
    private final Map<String, Set<ItemRef>> acceptedInputs = new LinkedHashMap<>();

    private ProcessCatalog(Map<String, Machine> machines, Map<String, ProcessDefinition> processes) {
        this.machines = Collections.unmodifiableMap(machines);
        this.processes = Collections.unmodifiableMap(processes);
        for (ProcessDefinition process : processes.values()) {
            byMachine.computeIfAbsent(process.machine(), key -> new ArrayList<>()).add(process);
            Set<ItemRef> accepted = acceptedInputs.computeIfAbsent(process.machine(), key -> new HashSet<>());
            for (Portion input : process.inputs()) {
                accepted.add(input.ref());
            }
        }
        byMachine.replaceAll((id, list) -> List.copyOf(list));
    }

    /**
     * Reads {@code machines:} and {@code processes:} from processes.yml.
     *
     * @param root the root of the file as plain data, or null for an empty catalog
     * @param log  receives one warning per skipped entry
     */
    public static ProcessCatalog parse(Map<String, Object> root, Logger log) {
        Map<String, Machine> machines = new LinkedHashMap<>();
        Map<String, Object> machineSection = ConfigValues.optionalSection(root, "machines");
        if (machineSection != null) {
            for (String id : machineSection.keySet()) {
                Machine machine = readMachine(id, ConfigValues.optionalSection(machineSection, id), log);
                if (machine != null) {
                    machines.put(id, machine);
                }
            }
        }
        Map<String, ProcessDefinition> processes = new LinkedHashMap<>();
        Map<String, Object> processSection = ConfigValues.optionalSection(root, "processes");
        if (processSection != null) {
            for (String id : processSection.keySet()) {
                ProcessDefinition process = readProcess(id, ConfigValues.optionalSection(processSection, id), machines, log);
                if (process != null) {
                    processes.put(id, process);
                }
            }
        }
        return new ProcessCatalog(machines, processes);
    }

    /** Every machine, in the order of the file. */
    public Collection<Machine> machines() {
        return machines.values();
    }

    public Optional<Machine> machine(String id) {
        return Optional.ofNullable(machines.get(id));
    }

    /** Every process, in the order of the file. */
    public Collection<ProcessDefinition> processes() {
        return processes.values();
    }

    public Optional<ProcessDefinition> process(String id) {
        return Optional.ofNullable(processes.get(id));
    }

    /** The processes a machine runs, in the order of the file. Empty for a machine with no processes. */
    public List<ProcessDefinition> processesFor(String machineId) {
        return byMachine.getOrDefault(machineId, List.of());
    }

    /** True when some process of the machine takes this item as an input. */
    public boolean acceptsInput(String machineId, ItemRef ref) {
        return acceptedInputs.getOrDefault(machineId, Set.of()).contains(ref);
    }

    /** Every item some process of the machine takes as an input. */
    public Set<ItemRef> inputsOf(String machineId) {
        return Collections.unmodifiableSet(acceptedInputs.getOrDefault(machineId, Set.of()));
    }

    private static Machine readMachine(String id, Map<String, Object> section, Logger log) {
        if (section == null) {
            warn(log, "machine '" + id + "' is not a section and is skipped.");
            return null;
        }
        String host = ConfigValues.string(section, "host", "").trim();
        if (host.isEmpty()) {
            warn(log, "machine '" + id + "' has no host block and is skipped.");
            return null;
        }
        Map<String, Object> slots = ConfigValues.optionalSection(section, "slots");
        try {
            return new Machine(id, ConfigValues.string(section, "name", id), host,
                    ConfigValues.integer(slots, "input", 0),
                    ConfigValues.integer(slots, "output", 0),
                    ConfigValues.integer(slots, "fuel", 0),
                    ConfigValues.integer(section, "power", 0));
        } catch (IllegalArgumentException e) {
            warn(log, e.getMessage() + " It is skipped.");
            return null;
        }
    }

    private static ProcessDefinition readProcess(String id, Map<String, Object> section,
                                                 Map<String, Machine> machines, Logger log) {
        if (section == null) {
            warn(log, "process '" + id + "' is not a section and is skipped.");
            return null;
        }
        String machineId = ConfigValues.string(section, "machine", "");
        Machine machine = machines.get(machineId);
        if (machine == null) {
            warn(log, "process '" + id + "' names the unknown machine '" + machineId + "' and is skipped.");
            return null;
        }
        int ticks = ConfigValues.integer(section, "ticks", 0);
        if (ticks < 1) {
            warn(log, "process '" + id + "' needs ticks of at least 1 and is skipped.");
            return null;
        }
        boolean fuel = ConfigValues.bool(section, "fuel", false);
        if (fuel && machine.fuelSlots() == 0) {
            warn(log, "process '" + id + "' burns fuel but machine '" + machineId + "' has no fuel slot and it is skipped.");
            return null;
        }
        try {
            List<Portion> inputs = readInputs(ConfigValues.stringList(section, "inputs", List.of()));
            List<Product> outputs = readOutputs(ConfigValues.mapList(section, "outputs"));
            if (inputs.isEmpty() || outputs.isEmpty()) {
                warn(log, "process '" + id + "' needs at least one input and one output and is skipped.");
                return null;
            }
            long distinctInputs = inputs.stream().map(Portion::ref).distinct().count();
            long distinctOutputs = outputs.stream().map(Product::ref).distinct().count();
            if (distinctInputs > machine.inputSlots() || distinctOutputs > machine.outputSlots()) {
                warn(log, "process '" + id + "' needs more slots than machine '" + machineId + "' has and is skipped.");
                return null;
            }
            return new ProcessDefinition(id, machineId, ticks, fuel, inputs, outputs);
        } catch (IllegalArgumentException e) {
            warn(log, "process '" + id + "': " + e.getMessage() + " It is skipped.");
            return null;
        }
    }

    private static List<Portion> readInputs(List<String> entries) {
        List<Portion> inputs = new ArrayList<>();
        for (String entry : entries) {
            int split = entry.lastIndexOf(':');
            ItemRef ref = ItemRef.parse(split < 0 ? entry : entry.substring(0, split))
                    .orElseThrow(() -> new IllegalArgumentException("Bad input '" + entry + "'."));
            int amount = split < 0 ? 0 : parseInt(entry.substring(split + 1));
            if (amount < 1) {
                throw new IllegalArgumentException("Input '" + entry + "' needs an amount of at least 1.");
            }
            inputs.add(new Portion(ref, amount));
        }
        return inputs;
    }

    private static List<Product> readOutputs(List<Map<String, Object>> entries) {
        List<Product> outputs = new ArrayList<>();
        for (Map<String, Object> entry : entries) {
            if (!(entry.get("item") instanceof String text)) {
                throw new IllegalArgumentException("An output has no item.");
            }
            ItemRef ref = ItemRef.parse(text).orElseThrow(() -> new IllegalArgumentException("Bad output '" + text + "'."));
            int amount = entry.get("amount") instanceof Number n ? n.intValue() : 1;
            double chance = entry.get("chance") instanceof Number n ? n.doubleValue() : 1.0;
            outputs.add(new Product(ref, amount, chance));
        }
        return outputs;
    }

    private static int parseInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void warn(Logger log, String message) {
        if (log != null) {
            log.warning("processes.yml: " + message);
        }
    }
}
