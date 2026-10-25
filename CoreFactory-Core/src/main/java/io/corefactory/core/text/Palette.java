package io.corefactory.core.text;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * The colours of the plugin, in RGB. Each one is also a MiniMessage tag, so a text can say {@code <good>Working</good>} and get the
 * exact colour; the tag names are short words that never clash with the {@code <id>} or {@code [player]} placeholders of the help lines.
 */
public final class Palette {

    /** Headings and the names of things: a warm amber. */
    public static final TextColor ACCENT = TextColor.color(0xFFB02E);
    /** A command or subcommand as the player types it: a light cyan. */
    public static final TextColor COMMAND = TextColor.color(0x7FD8FF);
    /** An argument placeholder: a soft yellow. */
    public static final TextColor ARGUMENT = TextColor.color(0xF7E26B);
    /** A value the game computed, such as a count or a percentage: white. */
    public static final TextColor VALUE = TextColor.color(0xFFFFFF);
    /** Explanation text: a cool gray. */
    public static final TextColor MUTED = TextColor.color(0xA7B0BA);
    /** Secondary text, separators and hints: a dim gray. */
    public static final TextColor HINT = TextColor.color(0x6B7480);
    /** Something that works: green. */
    public static final TextColor GOOD = TextColor.color(0x5BE37D);
    /** Something refused, stuck or broken: red. */
    public static final TextColor BAD = TextColor.color(0xFF5F6D);
    /** A caution that does not stop anything: gold. */
    public static final TextColor WARN = TextColor.color(0xFFC857);
    /** Research: a violet. */
    public static final TextColor RESEARCH = TextColor.color(0xB48CFF);

    /** The palette as MiniMessage tags, added to the standard ones by {@link Text}. */
    static final TagResolver TAGS = TagResolver.resolver(
            tag("accent", ACCENT), tag("command", COMMAND), tag("arg", ARGUMENT), tag("value", VALUE),
            tag("muted", MUTED), tag("hint", HINT), tag("good", GOOD), tag("bad", BAD), tag("warn", WARN),
            tag("research", RESEARCH));

    private Palette() {
    }

    private static TagResolver tag(String name, TextColor color) {
        return TagResolver.resolver(name, Tag.styling(builder -> builder.color(color)));
    }
}
