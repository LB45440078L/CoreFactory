package io.corefactory.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.Map;

/**
 * The colour logic of the plugin. Every piece of player-facing text becomes an Adventure {@link Component}, read from one of two
 * syntaxes: MiniMessage, such as {@code <red>Hi</red>}, or the ampersand codes that {@code config.yml} has always used, such as
 * {@code &cHi}. {@link #parse} reads both, so a string from a config file and one written in code look the same.
 *
 * <p>A value that comes from a player, such as a name or a typed id, goes through {@link #escape} before it is put into a text, so it
 * shows as typed and cannot add a colour or a click action.
 */
public final class Text {

    private static final MiniMessage MINI = MiniMessage.builder()
            .tags(TagResolver.resolver(TagResolver.standard(), Palette.TAGS))
            .build();

    /**
     * The ampersand codes and the MiniMessage tag each one stands for. The colour codes map to the palette, so a text written with
     * {@code &c} shows the same RGB red as one written with {@code <bad>}. The dark named colours have no palette entry and stay named.
     */
    private static final Map<Character, String> CODES = Map.ofEntries(
            Map.entry('0', "<black>"), Map.entry('1', "<dark_blue>"), Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"), Map.entry('4', "<dark_red>"), Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<accent>"), Map.entry('7', "<muted>"), Map.entry('8', "<hint>"),
            Map.entry('9', "<blue>"), Map.entry('a', "<good>"), Map.entry('b', "<command>"),
            Map.entry('c', "<bad>"), Map.entry('d', "<research>"), Map.entry('e', "<arg>"),
            Map.entry('f', "<value>"), Map.entry('k', "<obfuscated>"), Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"), Map.entry('n', "<underlined>"), Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>"));

    private Text() {
    }

    /**
     * Reads a text written in either syntax. Ampersand codes are turned into MiniMessage tags first, then the whole text is parsed.
     *
     * @param text the text, such as {@code &cRed} or {@code <red>Red}
     * @return the component; plain text stays plain
     */
    public static Component parse(String text) {
        return MINI.deserialize(toMiniMessage(text));
    }

    /**
     * Reads a text that is already MiniMessage. Ampersands are left alone, so a value that contains {@code &c} shows as typed. Use this
     * for a template after its values were escaped.
     */
    public static Component mini(String text) {
        return MINI.deserialize(text);
    }

    /**
     * Turns the ampersand codes of a text into MiniMessage tags. {@code &&} is not a code, and an ampersand that is not followed by a
     * code character stays as it is.
     *
     * @param text the text with ampersand codes
     * @return the same text in MiniMessage
     */
    public static String toMiniMessage(String text) {
        if (text == null || text.indexOf('&') < 0) {
            return text == null ? "" : text;
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && text.charAt(i + 1) == '&') {
                out.append('&');
                i++;
                continue;
            }
            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                String tag = CODES.get(code);
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
                if (code == '#' && i + 7 < text.length() && isHex(text, i + 2)) {
                    out.append('<').append(text, i + 1, i + 8).append('>');
                    i += 7;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * Escapes a value a player or the server supplies, so that it shows as typed. MiniMessage tags are escaped, and each ampersand is
     * marked, so an ampersand code in the value is not read as a colour. The value must then go through {@link #parse}, which reads the
     * marks; {@link #mini} would show them.
     *
     * @param value the raw value
     * @return the value, safe to put into a text that {@link #parse} reads
     */
    public static String escape(String value) {
        return MINI.escapeTags(value == null ? "" : value).replace("&", "\\&");
    }

    /**
     * The MiniMessage of a component, for putting it into a text that is parsed later.
     *
     * @param component the component
     * @return its MiniMessage
     */
    public static String serialize(Component component) {
        return MINI.serialize(component);
    }

    /**
     * The name or a lore line of an item, parsed. Vanilla shows an item's text in italics unless told otherwise, so the text is set
     * not italic.
     *
     * @param text the text, with either syntax
     */
    public static Component item(String text) {
        Component parsed = parse(text);
        return parsed.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET
                ? parsed.decoration(TextDecoration.ITALIC, false)
                : parsed;
    }

    /** The lore lines of an item, each parsed as {@link #item} does. */
    public static List<Component> lore(List<String> lines) {
        return lines.stream().map(Text::item).toList();
    }

    /** The text of a component with no colour or style, for comparing and logging. */
    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** The component with a tooltip shown when a player hovers over it. */
    public static Component hover(Component component, Component tooltip) {
        return component.hoverEvent(HoverEvent.showText(tooltip));
    }

    /** The component, which puts a command into the player's chat line when clicked, for the player to finish. */
    public static Component suggest(Component component, String command) {
        return component.clickEvent(ClickEvent.suggestCommand(command));
    }

    private static boolean isHex(String text, int from) {
        for (int i = from; i < from + 6; i++) {
            if (Character.digit(text.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
