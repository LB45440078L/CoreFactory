package io.corefactory.paper.energy;

import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.Units;
import io.corefactory.core.oil.FluidPhysics;

import java.util.List;
import java.util.Locale;

/**
 * The text of the generators in real units: their names, the hologram line, and the labels of the menu. Colours are ampersand codes.
 * Pure, so the wording is tested without a server.
 */
public final class GeneratorText {

    private static final double SECONDS_PER_HOUR = 3600.0;

    private GeneratorText() {
    }

    /** The name of a kind, as shown in its hologram, its menu and the wire messages. */
    public static String name(GeneratorKind kind) {
        return switch (kind) {
            case WIND -> "Wind generator";
            case SOLAR -> "Solar panel";
            case CHARCOAL -> "Charcoal generator";
            case PETROLEUM -> "Petroleum generator";
            case DIESEL -> "Diesel generator";
            case NUCLEAR -> "Nuclear generator";
        };
    }

    /** The name of the fuel a kind burns, such as Charcoal, or empty for a kind with no fuel. */
    public static String fuelName(GeneratorKind kind) {
        return switch (kind) {
            case WIND, SOLAR -> "";
            case CHARCOAL -> "Charcoal";
            case PETROLEUM -> "Petroleum";
            case DIESEL -> "Diesel";
            case NUCLEAR -> "Nuclear fuel rod";
        };
    }

    /**
     * The hologram line: the name, then the output with the fuel kept, such as {@code Charcoal generator  25.0 kW  fuel 14}, or the
     * status word in place of the output when the generator is not producing. A petroleum or diesel generator shows the litres its
     * network holds and the rate it burns at full output, such as {@code Diesel generator  150 kW  network 850 L  0.0 L/s}. It is one
     * line, so the hologram keeps one line.
     *
     * @param kind          the generator's kind
     * @param outputW       the output of the last tick, in W
     * @param fuel          the fuel items kept; not shown for a kind that takes no item
     * @param status        the status word
     * @param networkLitres the litres of the generator's fluid the network holds; not shown for a kind that burns no fluid
     */
    public static List<String> hologram(GeneratorKind kind, double outputW, int fuel, GeneratorStatus status, double networkLitres) {
        String middle = outputW > 0 ? "&a" + power(outputW) : colour(status) + status.word();
        String fuelPart = "";
        if (GeneratorFuel.sourceOf(kind).isPresent()) {
            fuelPart = "  &7fuel " + fuel;
        } else if (kind.fluid().isPresent()) {
            fuelPart = "  &7network " + FluidPhysics.volume(networkLitres) + "  &7" + FluidPhysics.rate(kind.litresPerSecond());
        }
        return List.of("&f" + name(kind) + "  " + middle + fuelPart);
    }

    /** The output of a generator with its unit, such as {@code 25.0 kW} or {@code 850 W}. */
    public static String power(double outputW) {
        return Units.powerText(outputW);
    }

    /** The colour code of a status: red for a missing fuel, yellow for the nuclear water rule, grey otherwise. */
    public static String colour(GeneratorStatus status) {
        return switch (status) {
            case RUNNING -> "&a";
            case NO_FUEL -> "&c";
            case NOT_UNDERWATER -> "&e";
            case NO_WATER -> "&c";
            case CLOSED, NIGHT -> "&7";
        };
    }

    /** The output line of the menu, such as {@code Output 25.0 kW}. */
    public static String outputTitle(double outputW) {
        return "&fOutput " + (outputW > 0 ? "&a" : "&7") + power(outputW);
    }

    /** The total line of the menu: the energy the generator has made since it was placed, such as {@code Produced 1.2 kWh}. */
    public static String producedTitle(double totalWh) {
        return "&fProduced &e" + Units.energyText(totalWh);
    }

    /**
     * The fuel line of the menu, such as {@code Fuel 14 items}. A petroleum or diesel generator takes no item, so its line reads
     * {@code No fuel item}, and wind and sun read {@code No fuel needed}.
     */
    public static String fuelTitle(GeneratorKind kind, int fuel) {
        if (kind.fluid().isPresent()) {
            return "&fNo fuel item";
        }
        if (GeneratorFuel.sourceOf(kind).isEmpty()) {
            return "&fNo fuel needed";
        }
        return "&fFuel &e" + fuel + (fuel == 1 ? " item" : " items");
    }

    /** The network line of the menu for a petroleum or diesel generator, such as {@code Network 850 L}. */
    public static String networkTitle(double networkLitres) {
        return "&fNetwork &e" + FluidPhysics.volume(networkLitres);
    }

    /**
     * The lore under the network line: the rate it burns at full output, how long a litre lasts, and where the fuel comes from. The rate
     * is the one {@link FluidPhysics#rate} gives, and the time a litre lasts is shown as well, since a rate this small reads as 0.0.
     */
    public static List<String> networkLore(GeneratorKind kind) {
        return List.of("&7Burns " + FluidPhysics.rate(kind.litresPerSecond()) + " at full power.",
                "&7One litre lasts " + duration(kind.burnSeconds()) + ".",
                "&8Drawn from the oil network while it runs.");
    }

    /** The lore under the fuel line: the item burning now, or the note that wind and sun cost nothing. */
    public static List<String> fuelLore(GeneratorKind kind, double remainingSeconds) {
        if (GeneratorFuel.sourceOf(kind).isEmpty()) {
            return List.of("&7Wind and sun cost nothing to run.");
        }
        String burning = remainingSeconds > 0 ? "&7Burning: " + duration(remainingSeconds) + " left on this item."
                : "&7No item is burning.";
        return List.of(burning, "&8The fuel slot is the store while this menu is open.");
    }

    /** The status line of the menu, such as {@code Status: running}, in the colour of the status. */
    public static String statusTitle(GeneratorStatus status) {
        return "&fStatus: " + colour(status) + status.word();
    }

    /**
     * A time in seconds: {@code h:mm:ss} from one hour, otherwise whole seconds such as {@code 96 s}. A part of a second counts as the
     * next whole second, so an item that is still burning never shows 0 s.
     *
     * @param seconds the seconds, not negative
     */
    public static String duration(double seconds) {
        long whole = (long) Math.ceil(Math.max(0.0, seconds));
        if (seconds >= SECONDS_PER_HOUR) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", whole / 3600, whole / 60 % 60, whole % 60);
        }
        return whole + " s";
    }
}
