package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.paper.energy.EnergyLevels;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The rows of the Shell dialog: the nine shells in the order a player meets them, and the three charge tiers. A shell that the connected
 * artillery cannot fire, because its minimum level is above their highest level, is a warning row with its reason. A tier that no connected
 * piece has loaded is a warning row too. Warnings do not stop a choice; the firing rules refuse a shot that breaks them. Pure.
 */
public final class ShellMenu {

    /**
     * One shell's row.
     *
     * @param shell  the shell
     * @param name   its name, such as {@code High explosive}
     * @param warn   true when the connected artillery cannot fire it
     * @param reason the reason, empty when {@code warn} is false
     */
    public record ShellRow(ArtilleryShell shell, String name, boolean warn, String reason) {
    }

    /**
     * One tier's row.
     *
     * @param tier   the charge tier
     * @param name   its name, such as {@code Tier II}
     * @param warn   true when no connected piece has this charge loaded
     * @param reason the reason, empty when {@code warn} is false
     */
    public record TierRow(ChargeTier tier, String name, boolean warn, String reason) {
    }

    private ShellMenu() {
    }

    /**
     * The shells in order: by the lowest level that fires them, then in the order the shells are declared.
     *
     * @return the nine shells
     */
    public static List<ArtilleryShell> ordered() {
        return Arrays.stream(ArtilleryShell.values())
                .sorted(Comparator.comparingInt((ArtilleryShell shell) -> shell.minimumLevel().number())
                        .thenComparingInt(ArtilleryShell::ordinal))
                .toList();
    }

    /**
     * The shell rows for the connected artillery.
     *
     * @param highestLevel the highest level among the connected artillery, or 0 when none is connected: then nothing is a warning
     * @return a row for every shell, in {@link #ordered} order
     */
    public static List<ShellRow> shells(int highestLevel) {
        return ordered().stream().map(shell -> {
            boolean warn = highestLevel > 0 && shell.minimumLevel().number() > highestLevel;
            String reason = warn ? "needs level " + EnergyLevels.roman(shell.minimumLevel().number())
                    + "; the highest connected piece is level " + EnergyLevels.roman(highestLevel) : "";
            return new ShellRow(shell, shellName(shell), warn, reason);
        }).toList();
    }

    /**
     * The tier rows for the connected artillery.
     *
     * @param loaded the charge tiers the connected artillery have loaded; empty when none is connected, then nothing is a warning
     * @return a row for each tier, from tier I to tier III
     */
    public static List<TierRow> tiers(Set<ChargeTier> loaded) {
        return Arrays.stream(ChargeTier.values()).map(tier -> {
            boolean warn = !loaded.isEmpty() && !loaded.contains(tier);
            String reason = warn ? "no connected piece has this charge loaded" : "";
            return new TierRow(tier, "Tier " + tierNumeral(tier), warn, reason);
        }).toList();
    }

    /**
     * The name of a shell for a player: its constant name in lower case with the first letter capitalised, such as
     * {@code Cluster enriched uranium}.
     */
    public static String shellName(ArtilleryShell shell) {
        String words = shell.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /** The numeral of a charge tier, I, II or III. */
    public static String tierNumeral(ChargeTier tier) {
        return EnergyLevels.roman(tier.ordinal() + 1);
    }

    /**
     * The label of a shell or tier button in MiniMessage. The chosen row gets a {@code > } marker and the accent colour; a row the connected
     * artillery cannot use is in the warning colour, which wins over the accent, and keeps the marker.
     *
     * @param name   the row's name, such as {@code High explosive} or {@code Tier II}
     * @param chosen true when the row is the one chosen now
     * @param warn   true when the row is a warning
     */
    public static String label(String name, boolean chosen, boolean warn) {
        String text = chosen ? "> " + name : name;
        if (warn) {
            return "<warn>" + text + "</warn>";
        }
        return chosen ? "<accent>" + text + "</accent>" : text;
    }

    /**
     * The shell and tier a load would use, as one line, such as {@code High explosive, tier II}, or {@code no shell chosen, tier I}.
     *
     * @param shell the shell chosen, or null when none is
     * @param tier  the charge tier chosen
     */
    public static String selection(ArtilleryShell shell, ChargeTier tier) {
        String name = shell == null ? "no shell chosen" : shellName(shell);
        return name + ", tier " + tierNumeral(tier);
    }
}
