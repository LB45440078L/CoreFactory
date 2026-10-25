package io.corefactory.paper.command;

import io.corefactory.core.text.Palette;
import io.corefactory.core.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StyleTest {

    @Test
    void eachRoleHasItsColour() {
        assertThat(Style.TITLE.of("x").color()).isEqualTo(Palette.ACCENT);
        assertThat(Style.COMMAND.of("x").color()).isEqualTo(Palette.COMMAND);
        assertThat(Style.ARGUMENT.of("x").color()).isEqualTo(Palette.ARGUMENT);
        assertThat(Style.DESCRIPTION.of("x").color()).isEqualTo(Palette.MUTED);
        assertThat(Style.HINT.of("x").color()).isEqualTo(Palette.HINT);
        assertThat(Style.SUCCESS.of("x").color()).isEqualTo(Palette.GOOD);
        assertThat(Style.ERROR.of("x").color()).isEqualTo(Palette.BAD);
        assertThat(Style.WARNING.of("x").color()).isEqualTo(Palette.WARN);
        assertThat(Style.ID.of("x").color()).isEqualTo(Palette.VALUE);
    }

    @Test
    void theTitleRoleIsBold() {
        assertThat(Style.TITLE.of("x").decoration(TextDecoration.BOLD)).isEqualTo(TextDecoration.State.TRUE);
    }

    @Test
    void aRoleTextIsPlain() {
        assertThat(Text.plain(Style.ARGUMENT.of("<id>"))).isEqualTo("<id>");
    }

    @Test
    void aUsageLineHasTheCommandArgumentsAndDescription() {
        Component line = Style.usage("/cf give", "<id> [player] [amount]", "give a TNT");

        assertThat(Text.plain(line)).isEqualTo("/cf give <id> [player] [amount] - give a TNT");
        assertThat(colourOf(line, "/cf give")).isEqualTo(Palette.COMMAND);
        assertThat(colourOf(line, "<id> [player] [amount]")).isEqualTo(Palette.ARGUMENT);
        assertThat(colourOf(line, "- ")).isEqualTo(Palette.HINT);
        assertThat(colourOf(line, "give a TNT")).isEqualTo(Palette.MUTED);
    }

    @Test
    void aUsageLineWithoutArgumentsHasNoPlaceholderSegment() {
        Component line = Style.usage("/cf list", "", "every id");

        assertThat(Text.plain(line)).isEqualTo("/cf list - every id");
        assertThat(colourOf(line, "/cf list")).isEqualTo(Palette.COMMAND);
        assertThat(colourOf(line, "every id")).isEqualTo(Palette.MUTED);
    }

    @Test
    void aUsageErrorStartsRedAndNamesTheCommand() {
        Component line = Style.usageError("/cf pylons show", "[seconds]");

        assertThat(Text.plain(line)).isEqualTo("Usage: /cf pylons show [seconds]");
        assertThat(colourOf(line, "Usage: ")).isEqualTo(Palette.BAD);
        assertThat(colourOf(line, "/cf pylons show")).isEqualTo(Palette.COMMAND);
        assertThat(colourOf(line, "[seconds]")).isEqualTo(Palette.ARGUMENT);
    }

    @Test
    void theTextOfAUsageLineIsPlain() {
        Component line = Style.usage("/cf research grant", "<node> [player]", "finish a node");

        assertThat(Text.plain(line)).isEqualTo("/cf research grant <node> [player] - finish a node");
    }

    /** The colour of the piece of the line whose text is {@code text}, or null when no piece has that text. */
    private static TextColor colourOf(Component line, String text) {
        if (line instanceof TextComponent piece && piece.content().equals(text)) {
            return piece.color();
        }
        for (Component child : line.children()) {
            TextColor found = colourOf(child, text);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
