package io.corefactory.core.process;

import java.util.List;

/**
 * One process of a machine: what it takes, how long it runs, and what it makes.
 *
 * @param id      the process id
 * @param machine the id of the machine that runs it
 * @param ticks   how many ticks one pass takes, at least one
 * @param fuel    true when each pass burns one fuel item
 * @param inputs  the items a pass consumes, all present in full before it starts
 * @param outputs the items a pass makes, each with its chance
 */
public record ProcessDefinition(String id, String machine, int ticks, boolean fuel,
                                List<Portion> inputs, List<Product> outputs) {

    public ProcessDefinition {
        if (ticks < 1) {
            throw new IllegalArgumentException("Process " + id + " needs at least one tick.");
        }
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }
}
