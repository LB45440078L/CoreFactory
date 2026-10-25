package io.corefactory.paper.oil;

import io.corefactory.core.oil.Fluid;
import io.corefactory.core.oil.FluidRules;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * The decision whether oil networks may meet at one block. A tube, or a device, that touches several networks joins them into one, so the
 * fluids they hold must agree: two petroleum networks join, an empty network joins any, and a petroleum network with a diesel one is refused.
 * A block that touches no network joins nothing and is allowed. Pure, so the rule is tested without a server.
 */
final class OilJoins {

    private OilJoins() {
    }

    /**
     * True when the networks may all be joined: together their fluids make at most one kind.
     *
     * @param networks the fluids of each network the block touches; an empty set is a network that holds no fluid
     */
    static boolean mayJoinAll(Collection<? extends Set<Fluid>> networks) {
        Set<Fluid> together = EnumSet.noneOf(Fluid.class);
        for (Set<Fluid> network : networks) {
            if (!FluidRules.mayJoin(together, network)) {
                return false;
            }
            together.addAll(network);
        }
        return true;
    }
}
