package io.corefactory.core.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Player-facing text from the {@code messages:} section of {@code config.yml}. Colour codes use ampersands.
 */
public final class Messages {

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("prefix", "&8[&cCoreFactory&8] &7"),
            Map.entry("no-permission", "&cYou do not have permission to do that."),
            Map.entry("player-only", "&cThis command can only be used by a player."),
            Map.entry("unknown-id", "&cUnknown TNT or component: &f{id}"),
            Map.entry("player-offline", "&cNo online player named &f{player}&c."),
            Map.entry("given", "&7Gave &f{amount}x {name}&7 to &f{player}&7."),
            Map.entry("reloaded", "&aConfiguration reloaded: &f{tnts} &7TNT types, &f{components} &7components."),
            Map.entry("not-placed", "&cLook at a placed custom TNT block first."),
            Map.entry("aimed", "&7Direction set to &f{direction}&7."),
            Map.entry("target-set", "&7Teleport target set to &f{x}, {y}, {z}&7."),
            Map.entry("link-start", "&7Linked TNT selected. Right-click a second custom TNT to link them."),
            Map.entry("link-made", "&aFuse link created between two TNT blocks."),
            Map.entry("link-same", "&cA TNT cannot be linked to itself."),
            Map.entry("link-cleared", "&7All fuse links removed from this TNT."),
            Map.entry("disabled-world", "&cCoreFactory is disabled in this world."),
            Map.entry("placed-count", "&7Placed custom TNT: &f{count}&7."),
            Map.entry("info-header", "&6--- {name} &6---"),
            Map.entry("info-line", "&7{label}: &f{value}"),
            Map.entry("scan-result", "&7Scan of the marked region: &f{count} &7blocks, mostly {top}."),
            Map.entry("locked-tnt", "&cThis nuclear TNT is locked."),
            Map.entry("lock-set", "&aNuclear TNT locked. It will not detonate until it is unlocked."),
            Map.entry("lock-unlocked", "&aNuclear TNT unlocked."),
            Map.entry("lock-already", "&7This nuclear TNT is already unlocked."),
            Map.entry("lock-removed", "&aPIN lock removed. The TNT is unlocked and has no PIN."),
            Map.entry("lock-ask-set", "&7Enter a new 4-digit PIN for this nuclear TNT."),
            Map.entry("lock-confirm", "&7Enter the same 4 digits again to confirm."),
            Map.entry("lock-mismatch", "&cThe two entries did not match. Start again."),
            Map.entry("lock-ask-unlock", "&7Enter the 4-digit PIN to unlock this nuclear TNT."),
            Map.entry("lock-wrong", "&cWrong PIN. &f{left} &cattempts left before a lockout."),
            Map.entry("lock-lockout", "&cToo many wrong PINs. Try again in &f{seconds} s&c."),
            Map.entry("lock-not-owner", "&cOnly the owner can change this lock."),
            Map.entry("lock-not-owner-set", "&cOnly the owner can lock this TNT."),
            Map.entry("lock-needs-pin", "&cEnter the PIN first."),
            Map.entry("lock-none", "&7This TNT has no PIN lock."),
            Map.entry("lock-gone", "&cThe TNT is no longer there.")
    );

    private final Map<String, String> values = new HashMap<>();
    private final String prefix;

    public Messages(ConfigurationSection section) {
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            String configured = section == null ? null : section.getString(entry.getKey());
            values.put(entry.getKey(), color(configured != null ? configured : entry.getValue()));
        }
        this.prefix = values.get("prefix");
    }

    /** The message with {@code {placeholders}} replaced, prefixed. */
    public String get(String key, Object... replacements) {
        return prefix + raw(key, replacements);
    }

    /** The message with {@code {placeholders}} replaced, without the prefix. */
    public String raw(String key, Object... replacements) {
        String text = values.getOrDefault(key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace("{" + replacements[i] + "}", String.valueOf(replacements[i + 1]));
        }
        return text;
    }

    /** Converts ampersand colour codes. Public so item names and lore share the same rules. */
    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
