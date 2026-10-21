package io.corefactory.spigot.turret;

import io.corefactory.core.turret.Magazine;
import io.corefactory.core.turret.RoundType;
import io.corefactory.core.turret.TurretLevels;

import java.util.List;
import java.util.Locale;

/**
 * The words of the turrets: their names, their holograms, and their status and round lines in the menu. Colour codes use ampersands.
 */
public final class TurretText {

    /** The name of the machine gun turret. */
    public static final String NAME = "&6Machine Gun Turret";

    /** The name of the laser turret. */
    public static final String LASER_NAME = "&cLaser Turret";

    private TurretText() {
    }

    /** The name of a kind of turret. */
    public static String nameOf(TurretKind kind) {
        return kind == TurretKind.LASER ? LASER_NAME : NAME;
    }

    /** The level line of a turret: its level out of the highest. */
    public static String levelLine(int level) {
        return "&7Level &f" + level + "&7 of &f" + TurretLevels.MAX_LEVEL;
    }

    /**
     * The lines of a turret's hologram. A machine gun turret shows its name and level, its rounds, and its status with the power it gets. A
     * laser turret holds no rounds, so it shows its name and level, that it runs on power, and its status with the power it gets.
     */
    public static List<String> lines(TurretKind kind, int level, Magazine magazine, TurretStatus status, double satisfaction) {
        String name = nameOf(kind) + " &7L" + level;
        if (kind == TurretKind.LASER) {
            return List.of(name, "&7Runs on power, no rounds", statusLine(status, satisfaction));
        }
        return List.of(name, roundsLine(magazine), statusLine(status, satisfaction));
    }

    /** The rounds line: how many rounds are held, of which type. */
    public static String roundsLine(Magazine magazine) {
        String type = magazine.type() == null ? "no rounds loaded" : roundName(magazine.type());
        return "&7Rounds: &f" + magazine.count() + "&7/&f" + magazine.capacity() + " &8(" + type + "&8)";
    }

    /** The status line: the status word, and the share of the power it gets while it has rounds. */
    public static String statusLine(TurretStatus status, double satisfaction) {
        int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, satisfaction)) * 100.0);
        return "&7Status: &f" + status.text() + "&7 - power &f" + percent + "%";
    }

    /** The name of a round type as a player reads it, such as Armor-piercing. */
    public static String roundName(RoundType type) {
        String words = type.configKey().replace('-', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1).toLowerCase(Locale.ROOT);
    }
}
