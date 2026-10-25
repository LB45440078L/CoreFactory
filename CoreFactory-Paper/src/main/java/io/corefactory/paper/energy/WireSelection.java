package io.corefactory.paper.energy;

import io.corefactory.core.energy.DeviceKind;
import io.corefactory.core.energy.Endpoint;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.energy.Wire;
import io.corefactory.core.energy.WireCheck;
import io.corefactory.core.energy.WireRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The wire tool as a state machine, per player. A first click on a wirable device starts a wire from it; a second click on
 * another device makes the wire when {@link WireRules} allows it, and otherwise says why and keeps the start. A sneaking click
 * cancels the start. A start is cleared when a wire is made, when the player quits, when its device is removed, and after
 * {@link #TIMEOUT_TICKS} ticks. A wire may join a generator to a pylon or a pylon to a pylon. Pure: the listener turns clicks into
 * {@link Device}s and sends the verdict as a message.
 */
public final class WireSelection {

    /** Ticks a start stays selected: 60 seconds. */
    public static final long TIMEOUT_TICKS = 60L * 20L;

    /** What a click did. */
    public enum Verdict {
        /** The device is now the start of a wire. */
        SELECTED,
        /** The start was cancelled by a sneaking click. */
        CANCELLED,
        /** A wire was made between the start and the clicked device. */
        MADE,
        /** The wire is not allowed; the reason is in {@link Result#check()}. The start is kept. */
        REFUSED,
        /** The two devices are in different worlds. The start is kept. */
        OTHER_WORLD,
        /** The two devices are already wired. The start is kept. */
        DUPLICATE,
        /** Nothing happened: a sneaking click with no start. */
        IDLE
    }

    /**
     * A device a wire can join.
     *
     * @param id    the device id
     * @param kind  the device kind
     * @param pos   the device's block
     * @param world the id of the device's world
     * @param label the name shown to the player, such as "Pylon I at 12 64 -3"
     */
    public record Device(String id, DeviceKind kind, Pos pos, UUID world, String label) {

        /** The device as an endpoint of {@link WireRules}. */
        public Endpoint endpoint() {
            return new Endpoint(id, kind, pos);
        }
    }

    /**
     * The result of a click.
     *
     * @param verdict what the click did
     * @param check   the wire rule's reason when the verdict is {@link Verdict#REFUSED}; otherwise {@link WireCheck#OK} or null
     * @param start   the start after the click, or null when there is none
     */
    public record Result(Verdict verdict, WireCheck check, Device start) {
    }

    private record Start(Device device, long tick) {
    }

    private final Map<UUID, Start> starts = new HashMap<>();

    /**
     * One click of a player on a device.
     *
     * @param player   the player who clicked
     * @param device   the device clicked
     * @param sneaking true when the player is sneaking
     * @param tick     the server tick of the click
     * @param existing the wires already made, which a new wire may not repeat
     */
    public Result click(UUID player, Device device, boolean sneaking, long tick, List<Wire> existing) {
        Start start = live(player, tick);
        if (sneaking) {
            if (start == null) {
                return new Result(Verdict.IDLE, null, null);
            }
            starts.remove(player);
            return new Result(Verdict.CANCELLED, null, start.device());
        }
        if (start == null) {
            if (!device.kind().wirable()) {
                return new Result(Verdict.REFUSED, WireCheck.NOT_WIRABLE, null);
            }
            starts.put(player, new Start(device, tick));
            return new Result(Verdict.SELECTED, null, device);
        }
        if (!start.device().world().equals(device.world())) {
            return new Result(Verdict.OTHER_WORLD, null, start.device());
        }
        WireCheck check = WireRules.validate(start.device().endpoint(), device.endpoint(), existing);
        if (check == WireCheck.DUPLICATE) {
            return new Result(Verdict.DUPLICATE, check, start.device());
        }
        if (check != WireCheck.OK) {
            return new Result(Verdict.REFUSED, check, start.device());
        }
        starts.remove(player);
        return new Result(Verdict.MADE, WireCheck.OK, start.device());
    }

    /** The start of a player, or empty when none is selected or it has timed out. */
    public Optional<Device> selected(UUID player, long tick) {
        Start start = live(player, tick);
        return start == null ? Optional.empty() : Optional.of(start.device());
    }

    /** Clears every start that has timed out by this tick, and returns the players whose start was cleared. */
    public List<UUID> expire(long tick) {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Start> entry : starts.entrySet()) {
            if (tick - entry.getValue().tick() >= TIMEOUT_TICKS) {
                expired.add(entry.getKey());
            }
        }
        expired.forEach(starts::remove);
        return expired;
    }

    /** Clears the start of a player who quit. */
    public void forget(UUID player) {
        starts.remove(player);
    }

    /** Clears every start that is the given device, when the device is removed. */
    public void forgetDevice(String deviceId) {
        starts.values().removeIf(start -> start.device().id().equals(deviceId));
    }

    private Start live(UUID player, long tick) {
        Start start = starts.get(player);
        if (start != null && tick - start.tick() >= TIMEOUT_TICKS) {
            starts.remove(player);
            return null;
        }
        return start;
    }
}
