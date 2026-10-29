package io.corefactory.core.deposit;

import io.corefactory.core.geology.OreDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The ores a new solid deposit may hold, as the list {@link DepositPlanner} picks from uniformly. An ore appears once for
 * each point of its weight, so a uniform pick over the list is a pick weighted by the ores' weights. Pure, so the weights
 * are tested with plain definitions.
 */
public final class DepositPool {

    private DepositPool() {
    }

    /**
     * The solid ores, in file order, each repeated by its weight. The oil deposit is left out, since it is planned as oil.
     *
     * @param ores  the ores of the server, in file order
     * @param oilId the id of the oil deposit
     * @return the ore ids, one entry per point of weight; empty when no ore but the oil deposit is defined
     * @throws NullPointerException when {@code ores} is null
     */
    public static List<String> solid(List<OreDefinition> ores, String oilId) {
        Objects.requireNonNull(ores, "ores");
        List<String> pool = new ArrayList<>();
        for (OreDefinition ore : ores) {
            if (ore.id().equals(oilId)) {
                continue;
            }
            for (int point = 0; point < ore.weight(); point++) {
                pool.add(ore.id());
            }
        }
        return List.copyOf(pool);
    }
}
