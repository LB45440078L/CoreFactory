package io.corefactory.paper.lock;

import io.corefactory.api.PinLock;
import io.corefactory.api.PlacedTnt;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.lock.LockRules;
import io.corefactory.core.lock.PinAttempts;
import io.corefactory.core.placement.PlacedTntStore;
import io.corefactory.paper.lock.LockKeypad.Purpose;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The PIN locks of placed nuclear TNT, as the players and the keypads use them. It decides what a click asks for, sends the
 * messages, saves the registry after every change, and keeps the wrong-PIN count of each TNT in memory. The rules themselves
 * (what a PIN may change, and when a remove is allowed) live in {@link LockRules}.
 */
public final class TntLocks {

    /** Set, change and remove a PIN lock on your own nuclear TNT. */
    public static final String PERMISSION = "corefactory.lock";
    /** Remove a lock from any nuclear TNT without its PIN, for recovery. Each such removal is logged. */
    public static final String ADMIN_PERMISSION = "corefactory.lock.admin";

    /** What a keypad submission leaves open: the UNLOCK keypad is closed, or kept open for another try. */
    public enum Attempt {
        /** The TNT is unlocked. */
        UNLOCKED,
        /** The PIN was wrong, and the keypad may be used again. */
        RETRY,
        /** Nothing more can be done here: no lock, the TNT is gone, or the keypad is locked out. */
        STOP
    }

    private final CoreFactoryCore core;
    private final Map<String, PinAttempts> attempts = new HashMap<>();

    public TntLocks(CoreFactoryCore core) {
        this.core = core;
    }

    /** Sneak-right-click with an empty hand on a nuclear TNT: the owner's lock, or the admin's remove. */
    public void manage(Player player, PlacedTnt placed) {
        PinLock lock = placed.lock();
        boolean owner = isOwner(player, placed);
        boolean admin = player.hasPermission(ADMIN_PERMISSION);
        if (LockRules.refusesTriggers(lock)) {
            if (owner) {
                if (!player.hasPermission(PERMISSION)) {
                    player.sendMessage(core.messages().get("no-permission"));
                    return;
                }
                openUnlock(player, placed.location(), admin, true);
            } else if (admin) {
                openUnlock(player, placed.location(), true, false);
            } else {
                player.sendMessage(message("lock-not-owner"));
            }
            return;
        }
        if (owner) {
            if (!player.hasPermission(PERMISSION)) {
                player.sendMessage(core.messages().get("no-permission"));
                return;
            }
            new LockKeypad(this, placed.location(), Purpose.SET_PIN, lock != null, false).open(player);
        } else if (admin && lock != null) {
            openUnlock(player, placed.location(), true, false);
        } else {
            player.sendMessage(message(lock == null ? "lock-not-owner-set" : "lock-not-owner"));
        }
    }

    /** A plain right-click on a locked nuclear TNT, with any item or none: the UNLOCK keypad. */
    public void promptUnlock(Player player, PlacedTnt placed) {
        openUnlock(player, placed.location(), false, false);
    }

    /**
     * Opens the UNLOCK keypad, unless the TNT is locked out. A locked-out keypad says how long is left and does not open.
     *
     * @param removeOffered whether the keypad offers an admin's remove
     * @param manageAfter   whether a correct PIN opens the owner's SET PIN keypad next
     */
    void openUnlock(Player player, Location at, boolean removeOffered, boolean manageAfter) {
        long wait = lockoutRemaining(at, System.currentTimeMillis());
        if (wait > 0) {
            player.sendMessage(message("lock-lockout", "seconds", seconds(wait)));
            return;
        }
        new LockKeypad(this, at, Purpose.UNLOCK, removeOffered, manageAfter).open(player);
    }

    /** Opens the SET PIN keypad for a TNT, with the remove button when {@code removeOffered}. */
    void openSetPin(Player player, Location at, boolean removeOffered) {
        new LockKeypad(this, at, Purpose.SET_PIN, removeOffered, false).open(player);
    }

    /** Sets the PIN of an unlocked TNT, or a new one, and locks it. Only the owner, with the permission. */
    void setPin(Player player, Location at, String pin) {
        Optional<PlacedTnt> found = core.placedAt(at);
        if (found.isEmpty()) {
            player.sendMessage(message("lock-gone"));
            return;
        }
        PlacedTnt placed = found.get();
        if (!isOwner(player, placed)) {
            player.sendMessage(message(placed.lock() == null ? "lock-not-owner-set" : "lock-not-owner"));
            return;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(core.messages().get("no-permission"));
            return;
        }
        LockRules.Step step = LockRules.setPin(true, placed.lock(), null, pin);
        if (!step.done()) {
            player.sendMessage(message("lock-needs-pin"));
            return;
        }
        placed.setLock(step.lock());
        core.savePlaced();
        player.sendMessage(message("lock-set"));
    }

    /**
     * Unlocks the TNT with a PIN. A wrong PIN counts against the TNT, and after the allowed number of them the keypad is locked
     * out for the configured time. A correct PIN keeps the salt and hash, so the owner can lock the TNT again.
     */
    Attempt unlock(Player player, Location at, String pin) {
        Optional<PlacedTnt> found = core.placedAt(at);
        if (found.isEmpty()) {
            player.sendMessage(message("lock-gone"));
            return Attempt.STOP;
        }
        PlacedTnt placed = found.get();
        PinLock lock = placed.lock();
        if (lock == null) {
            player.sendMessage(message("lock-none"));
            return Attempt.STOP;
        }
        if (!lock.locked()) {
            player.sendMessage(message("lock-already"));
            return Attempt.UNLOCKED;
        }
        String key = PlacedTntStore.key(placed.location());
        long now = System.currentTimeMillis();
        PinAttempts tries = attempts.computeIfAbsent(key, k -> new PinAttempts(
                core.settings().lockMaxAttempts(), core.settings().lockoutSeconds() * 1000L));
        long wait = tries.remainingMillis(now);
        if (wait > 0) {
            player.sendMessage(message("lock-lockout", "seconds", seconds(wait)));
            return Attempt.STOP;
        }
        LockRules.Step step = LockRules.unlock(lock, pin);
        if (step.done()) {
            attempts.remove(key);
            placed.setLock(step.lock());
            core.savePlaced();
            player.sendMessage(message("lock-unlocked"));
            return Attempt.UNLOCKED;
        }
        tries.wrong(now);
        long locked = tries.remainingMillis(now);
        if (locked > 0) {
            player.sendMessage(message("lock-lockout", "seconds", seconds(locked)));
            return Attempt.STOP;
        }
        player.sendMessage(message("lock-wrong", "left", tries.attemptsLeft()));
        return Attempt.RETRY;
    }

    /**
     * Removes the lock entirely. The owner removes an unlocked lock, or a locked one with the PIN they just gave. An admin removes
     * any lock without the PIN, and every such removal is logged.
     */
    void remove(Player player, Location at) {
        Optional<PlacedTnt> found = core.placedAt(at);
        if (found.isEmpty()) {
            player.sendMessage(message("lock-gone"));
            return;
        }
        PlacedTnt placed = found.get();
        PinLock before = placed.lock();
        boolean owner = isOwner(player, placed);
        boolean admin = player.hasPermission(ADMIN_PERMISSION);
        if (!owner && !admin) {
            player.sendMessage(message("lock-not-owner"));
            return;
        }
        if (!admin && !player.hasPermission(PERMISSION)) {
            player.sendMessage(core.messages().get("no-permission"));
            return;
        }
        LockRules.Step step = LockRules.remove(owner, admin, before, null);
        switch (step.result()) {
            case DONE -> {
                placed.setLock(null);
                attempts.remove(PlacedTntStore.key(placed.location()));
                core.savePlaced();
                if (admin && (!owner || LockRules.refusesTriggers(before))) {
                    logAdminRemoval(player, placed);
                }
                player.sendMessage(message("lock-removed"));
            }
            case NO_LOCK -> player.sendMessage(message("lock-none"));
            case NEEDS_PIN -> player.sendMessage(message("lock-needs-pin"));
            default -> player.sendMessage(message("lock-not-owner"));
        }
    }

    /** The text of a message from the configuration, with its prefix. */
    Component message(String key, Object... replacements) {
        return core.messages().get(key, replacements);
    }

    private long lockoutRemaining(Location at, long now) {
        PinAttempts tries = attempts.get(PlacedTntStore.key(at));
        return tries == null ? 0 : tries.remainingMillis(now);
    }

    private void logAdminRemoval(Player admin, PlacedTnt placed) {
        Logger log = core.plugin().getLogger();
        log.info("Operator " + admin.getName() + " (" + admin.getUniqueId() + ") removed the PIN lock of "
                + placed.tntId() + " at " + placed.location().getWorld().getName() + " "
                + placed.location().getBlockX() + "," + placed.location().getBlockY() + ","
                + placed.location().getBlockZ() + ", owned by " + placed.owner() + ".");
    }

    private static boolean isOwner(Player player, PlacedTnt placed) {
        return placed.owner() != null && placed.owner().equals(player.getUniqueId());
    }

    private static long seconds(long millis) {
        return (millis + 999) / 1000;
    }
}
