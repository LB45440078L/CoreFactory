package io.corefactory.paper.water;

import org.bukkit.Location;

import java.util.Optional;

/**
 * What the machines and the nuclear generators ask of the water network. A lab machine asks for the water its process takes from its
 * bucket input, and takes it from its network when the network delivers it. A nuclear generator asks the same way and may run on the
 * network's flow instead of the underwater rule.
 */
public interface WaterFeed {

    /** A feed of a server with no water network: nothing is supplied, and a nuclear generator keeps the underwater rule. */
    WaterFeed NONE = new WaterFeed() {
        @Override
        public boolean canSupply(Location block, int processTicks) {
            return false;
        }

        @Override
        public Optional<String> waterLine(Location block) {
            return Optional.empty();
        }

        @Override
        public boolean nuclearMayRun(Location block, boolean underwaterRuleMet) {
            return underwaterRuleMet;
        }
    };

    /**
     * Whether the network delivers the water a run of a process takes to the machine at a block. It also declares the machine's request:
     * a run of {@code processTicks} ticks takes one unit of water, so the machine asks for that rate in units per second. Called once a
     * tick for each machine that has a process with a water bucket input.
     *
     * @param block        the machine's host block
     * @param processTicks the ticks of the process's run, at least 1
     * @return true when the delivery at the last solve covers the request
     */
    boolean canSupply(Location block, int processTicks);

    /**
     * The status line of a machine that has a connected pipe: the water it gets against the water it asks for. Empty when no pipe joins
     * the machine.
     *
     * @param block the machine's host block
     */
    Optional<String> waterLine(Location block);

    /**
     * Whether a nuclear generator may run: the underwater rule, or a pipe on any face with the network delivering water.
     *
     * @param block            the generator's block
     * @param underwaterRuleMet the caller's result of the underwater rule
     */
    boolean nuclearMayRun(Location block, boolean underwaterRuleMet);

    /** True when a water pipe is joined to the block at a location. */
    default boolean joinedToPipe(org.bukkit.Location block) {
        return false;
    }
}
