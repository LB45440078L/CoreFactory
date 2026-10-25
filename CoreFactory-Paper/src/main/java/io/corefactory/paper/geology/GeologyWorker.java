package io.corefactory.paper.geology;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.geology.GeologyService;
import io.corefactory.core.geology.OreDefinition;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.logging.Logger;

/**
 * The ores of the server as the mining code reads them: the ore list, the host block each ore is placed as, and the
 * component a mined block drops. The ore blocks themselves are laid into the world by {@link DepositGenerator}.
 */
public final class GeologyWorker {

    private final CoreFactoryCore core;
    private final GeologyService service;
    private final Material[] hosts;

    /**
     * @param core    the core, which knows the components an ore may drop
     * @param service the ores of the server
     * @param log     receives a warning for each ore whose host is not a block or whose drop is unknown
     */
    public GeologyWorker(CoreFactoryCore core, GeologyService service, Logger log) {
        this.core = core;
        this.service = service;
        this.hosts = new Material[service.ores().size()];
        for (int index = 0; index < hosts.length; index++) {
            OreDefinition ore = service.ore(index);
            Material host = Material.matchMaterial(ore.host());
            if (host == null || !host.isBlock()) {
                log.warning("ores.yml: '" + ore.id() + "' host " + ore.host() + " is not a block; it is not placed.");
            } else {
                hosts[index] = host;
            }
            if (!ore.fluid() && !core.componentDefinitions().containsKey(ore.drop())) {
                log.warning("ores.yml: '" + ore.id() + "' drops unknown component '" + ore.drop() + "'.");
            }
        }
    }

    public GeologyService service() {
        return service;
    }

    /** True when the ore at this index is placed as this material. */
    public boolean hostMatches(int oreIndex, Material material) {
        return oreIndex >= 0 && oreIndex < hosts.length && hosts[oreIndex] == material;
    }

    /** The material the ore at this index is placed as, or null when its host is not a block. */
    public Material host(int oreIndex) {
        return hosts[oreIndex];
    }

    /**
     * Drops the ore's component at the block, the given number of items. A fluid ore, such as the crude petroleum deposit, drops
     * nothing: a petroleum drill pumps it into the oil network as litres.
     */
    public void dropFor(Block block, OreDefinition ore, int amount) {
        if (ore.fluid()) {
            return;
        }
        core.createComponentItem(ore.drop(), amount).ifPresent(item ->
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item));
    }
}
