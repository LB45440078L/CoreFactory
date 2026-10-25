package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The non-belt nodes and the port cells that placement checks against.
 *
 * @param pickers   the pickers
 * @param splitters the splitters
 * @param duplexes  the belt duplexes, each occupying two cells
 * @param ports     the cells that hold a port
 */
public record Endpoints(List<Picker> pickers, List<Splitter> splitters, List<Duplex> duplexes, Set<Pos> ports) {

    public Endpoints {
        pickers = List.copyOf(Objects.requireNonNull(pickers, "pickers"));
        splitters = List.copyOf(Objects.requireNonNull(splitters, "splitters"));
        duplexes = List.copyOf(Objects.requireNonNull(duplexes, "duplexes"));
        ports = Set.copyOf(Objects.requireNonNull(ports, "ports"));
    }

    /** Pickers, splitters and ports, with no belt duplexes. */
    public Endpoints(List<Picker> pickers, List<Splitter> splitters, Set<Pos> ports) {
        this(pickers, splitters, List.of(), ports);
    }

    /** No pickers, splitters, duplexes or ports. */
    public static Endpoints none() {
        return new Endpoints(List.of(), List.of(), List.of(), Set.of());
    }
}
