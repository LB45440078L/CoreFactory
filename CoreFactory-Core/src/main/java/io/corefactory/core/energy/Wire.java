package io.corefactory.core.energy;

/**
 * A permanent link between a generator and a pylon. The network checks each wire with {@link WireRules} before it
 * uses it; a wire that fails the check is ignored.
 *
 * @param id   the wire's id
 * @param from the id of one end: a generator or a pylon
 * @param to   the id of the other end: a generator or a pylon
 */
public record Wire(String id, String from, String to) {

    public Wire {
        Checks.id(id, "wire id");
        Checks.id(from, "wire from");
        Checks.id(to, "wire to");
    }
}
