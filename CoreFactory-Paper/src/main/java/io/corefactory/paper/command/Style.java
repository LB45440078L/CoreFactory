package io.corefactory.paper.command;

import io.corefactory.core.text.Palette;
import io.corefactory.core.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * The colour style of the command text: help blocks, usage lines and replies that name a command. Each constant is one role in the
 * text, and each role has one colour everywhere, so a line reads the same wherever it appears. A role is a component, so a line is
 * built by appending components and never by joining colour codes into a string.
 *
 * <p>Tab-completion suggestions cannot be coloured by the server, so they stay plain text.
 */
public enum Style {

    /** The heading of a help block: the accent colour, bold. */
    TITLE(Palette.ACCENT, TextDecoration.BOLD),
    /** A command or subcommand name, as the player types it. */
    COMMAND(Palette.COMMAND),
    /** An argument placeholder such as {@code <id>} or {@code [player]}. */
    ARGUMENT(Palette.ARGUMENT),
    /** The words that explain a command. */
    DESCRIPTION(Palette.MUTED),
    /** A separator, a note or a hint that is secondary to the text beside it. */
    HINT(Palette.HINT),
    /** A confirmation that something worked. */
    SUCCESS(Palette.GOOD),
    /** A refusal or an error. */
    ERROR(Palette.BAD),
    /** A caution that does not stop the command. */
    WARNING(Palette.WARN),
    /** An id or a name the plugin defines or the player typed. */
    ID(Palette.VALUE);

    private final TextColor color;
    private final TextDecoration decoration;

    Style(TextColor color) {
        this(color, null);
    }

    Style(TextColor color, TextDecoration decoration) {
        this.color = color;
        this.decoration = decoration;
    }

    /**
     * The text in this style. The text is plain: a {@code <} in it is shown as it is.
     *
     * @param text the words, such as a command name
     */
    public Component of(String text) {
        Component component = Component.text(text, color);
        return decoration == null ? component : component.decorate(decoration);
    }

    /**
     * A help line: the command, its arguments and what it does. Like {@code /cf give <id> - give a TNT}, with the command in the command
     * colour, the arguments in the argument colour and the separator in the hint colour. Clicking the command puts it into the chat line,
     * ready for the player to finish, and hovering over it says so.
     *
     * @param command     the command and its literal words, such as {@code /cf give}
     * @param arguments   the placeholders, such as {@code <id> [player]}; empty for none
     * @param description what the command does
     */
    public static Component usage(String command, String arguments, String description) {
        Component tip = DESCRIPTION.of("Click to type ").append(COMMAND.of(command));
        Component name = Text.hover(Text.suggest(COMMAND.of(command), command + " "), tip);
        return name.append(argumentsSuffix(arguments)).append(Component.text(" ")).append(HINT.of("- "))
                .append(DESCRIPTION.of(description));
    }

    /**
     * The reply to a command that was given wrongly, such as {@code Usage: /cf pylons show [seconds]}.
     *
     * @param command   the command and its literal words
     * @param arguments the placeholders; empty for none
     */
    public static Component usageError(String command, String arguments) {
        return ERROR.of("Usage: ").append(COMMAND.of(command)).append(argumentsSuffix(arguments));
    }

    private static Component argumentsSuffix(String arguments) {
        return arguments.isEmpty() ? Component.empty() : Component.text(" ").append(ARGUMENT.of(arguments));
    }
}
