package io.corefactory.core.oil;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * The rule that keeps two fluids apart. A network holds one fluid kind, so petroleum and diesel never share a tube: a tube that would join
 * a network with petroleum to a network with diesel is refused, and so is a diesel source placed on a petroleum network. The kinds a
 * network carries are the kinds of its sources. Pure.
 */
public final class FluidRules {

    private FluidRules() {
    }

    /**
     * True when a network whose sources give these fluids is allowed: no fluid, or one kind of fluid.
     *
     * @param sourceKinds the fluids of the network's sources
     */
    public static boolean allowed(Collection<Fluid> sourceKinds) {
        EnumSet<Fluid> kinds = EnumSet.noneOf(Fluid.class);
        kinds.addAll(sourceKinds);
        return kinds.size() <= 1;
    }

    /**
     * True when two networks may be joined: their fluids together make at most one kind.
     *
     * @param first  the fluids of one network
     * @param second the fluids of the other
     */
    public static boolean mayJoin(Set<Fluid> first, Set<Fluid> second) {
        EnumSet<Fluid> together = EnumSet.noneOf(Fluid.class);
        together.addAll(first);
        together.addAll(second);
        return together.size() <= 1;
    }
}
