package io.corefactory.spigot.command;

import org.bukkit.ChatColor;

/**
 * The colour style of the command text: help blocks, usage lines, replies that name a command, and the chat messages
 * that go with tab completion. Each constant is one role in the text, and each role has one colour everywhere, so a line
 * reads the same wherever it appears. The codes are section signs, which the server sends as they are.
 *
 * <p>Tab-completion suggestions cannot be coloured by the server, so they stay plain text.
 */
public enum Style {

    /** The heading of a help block: gold and bold. */
    TITLE(ChatColor.GOLD.toString() + ChatColor.BOLD),
    /** A command or subcommand name, as the player types it: aqua. */
    COMMAND(ChatColor.AQUA.toString()),
    /** An argument placeholder such as {@code <id>} or {@code [player]}: yellow. */
    ARGUMENT(ChatColor.YELLOW.toString()),
    /** The words that explain a command: gray. */
    DESCRIPTION(ChatColor.GRAY.toString()),
    /** A separator, a note or a hint that is secondary to the text beside it: dark gray. */
    HINT(ChatColor.DARK_GRAY.toString()),
    /** A confirmation that something worked: green. */
    SUCCESS(ChatColor.GREEN.toString()),
    /** A refusal or an error: red. */
    ERROR(ChatColor.RED.toString()),
    /** A caution that does not stop the command: gold. */
    WARNING(ChatColor.GOLD.toString()),
    /** An id or a name the plugin defines or the player typed: white. */
    ID(ChatColor.WHITE.toString());

    private final String code;

    Style(String code) {
        this.code = code;
    }

    /** The text in this style, with the colour reset after it so the next segment starts plain. */
    public String of(String text) {
        return code + text + ChatColor.RESET;
    }

    /**
     * A help line: the command, its arguments and what it does. Like {@code /cf give <id> - give a TNT}, with the
     * command in aqua, the arguments in yellow and the separator in dark gray.
     *
     * @param command     the command and its literal words, such as {@code /cf give}
     * @param arguments   the placeholders, such as {@code <id> [player]}; empty for none
     * @param description what the command does
     */
    public static String usage(String command, String arguments, String description) {
        return COMMAND.of(command) + argumentsSuffix(arguments) + " " + HINT.of("- ") + DESCRIPTION.of(description);
    }

    /**
     * The reply to a command that was given wrongly, such as {@code Usage: /cf pylons show [seconds]}.
     *
     * @param command   the command and its literal words
     * @param arguments the placeholders; empty for none
     */
    public static String usageError(String command, String arguments) {
        return ERROR.of("Usage: ") + COMMAND.of(command) + argumentsSuffix(arguments);
    }

    private static String argumentsSuffix(String arguments) {
        return arguments.isEmpty() ? "" : " " + ARGUMENT.of(arguments);
    }
}
