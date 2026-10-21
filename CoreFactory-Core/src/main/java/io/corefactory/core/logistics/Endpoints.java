package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The non-belt nodes and the port cells that placement checks against.
 *
 * @param pickers  the pickers
 * @param splitters the splitters
 * @param ports    the cells that hold a port
 */
public record Endpoints(List<Picker> pickers, List<Splitter> splitters, Set<Pos> ports) {

    public Endpoints {
        pickers = List.copyOf(Objects.requireNonNull(pickers, "pickers"));
        splitters = List.copyOf(Objects.requireNonNull(splitters, "splitters"));
        ports = Set.copyOf(Objects.requireNonNull(ports, "ports"));
    }

    /** No pickers, splitters or ports. */
    public static Endpoints none() {
        return new Endpoints(List.of(), List.of(), Set.of());
    }
}
