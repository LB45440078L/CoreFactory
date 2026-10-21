package io.corefactory.core.energy;

import java.util.Collection;

/**
 * The wire rules. A wire joins a generator to a pylon, or a pylon to another pylon, and is at most
 * {@link EnergyTable#WIRE_MAX_LENGTH} blocks long on x and z. Two generators, anything with a consumer or an accumulator,
 * a device to itself and a second wire between the same two ends are refused.
 */
public final class WireRules {

    private WireRules() {
    }

    /**
     * Checks a wire between two ends, in this order: the same device, an end that cannot take a wire, two generators, then
     * the length. The verdict does not depend on which end is first.
     *
     * @param a one end
     * @param b the other end
     * @return {@link WireCheck#OK} when the wire may be made, otherwise the first reason it may not
     */
    public static WireCheck validate(Endpoint a, Endpoint b) {
        if (a.id().equals(b.id())) {
            return WireCheck.SAME_DEVICE;
        }
        if (!a.kind().wirable() || !b.kind().wirable()) {
            return WireCheck.NOT_WIRABLE;
        }
        if (a.kind() == DeviceKind.GENERATOR && b.kind() == DeviceKind.GENERATOR) {
            return WireCheck.SAME_KIND;
        }
        long max = (long) EnergyTable.WIRE_MAX_LENGTH * EnergyTable.WIRE_MAX_LENGTH;
        if (a.pos().horizontalDistanceSquared(b.pos()) > max) {
            return WireCheck.TOO_LONG;
        }
        return WireCheck.OK;
    }

    /**
     * Checks a wire like {@link #validate(Endpoint, Endpoint)}, then refuses a second wire between the same two ends.
     *
     * @param a        one end
     * @param b        the other end
     * @param existing the wires already made
     */
    public static WireCheck validate(Endpoint a, Endpoint b, Collection<Wire> existing) {
        WireCheck check = validate(a, b);
        if (check != WireCheck.OK) {
            return check;
        }
        for (Wire wire : existing) {
            boolean forward = wire.from().equals(a.id()) && wire.to().equals(b.id());
            boolean backward = wire.from().equals(b.id()) && wire.to().equals(a.id());
            if (forward || backward) {
                return WireCheck.DUPLICATE;
            }
        }
        return WireCheck.OK;
    }
}
