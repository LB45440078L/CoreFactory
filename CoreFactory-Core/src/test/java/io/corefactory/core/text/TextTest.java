package io.corefactory.core.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextTest {

    @Test
    void ampersandCodesBecomeMiniMessageTags() {
        assertThat(Text.toMiniMessage("&cRed &lbold &r&7gray")).isEqualTo("<bad>Red <bold>bold <reset><muted>gray");
    }

    @Test
    void anAmpersandThatIsNotACodeStaysAsItIs() {
        assertThat(Text.toMiniMessage("Tom & Jerry &z")).isEqualTo("Tom & Jerry &z");
    }

    @Test
    void anAmpersandFollowedByNothingOrBySpaceOrAmpersandStaysAsItIs() {
        assertThat(Text.toMiniMessage("Fish & chips &")).isEqualTo("Fish & chips &");
        assertThat(Text.toMiniMessage("&& & &!")).isEqualTo("&& & &!");
        assertThat(Text.plain(Text.parse("Fish & chips &"))).isEqualTo("Fish & chips &");
    }

    @Test
    void anAmpersandHashWithoutSixHexDigitsStaysAsItIs() {
        assertThat(Text.toMiniMessage("&#zzzzzz")).isEqualTo("&#zzzzzz");
    }

    @Test
    void aHexCodeBecomesAHexTag() {
        assertThat(Text.toMiniMessage("&#ff8800hi")).isEqualTo("<#ff8800>hi");
    }

    @Test
    void bothSyntaxesReadTheSameWay() {
        assertThat(Text.plain(Text.parse("&cRed"))).isEqualTo("Red");
        assertThat(Text.parse("&cRed").color()).isEqualTo(Palette.BAD);
        assertThat(Text.parse("<bad>Red").color()).isEqualTo(Palette.BAD);
        assertThat(Text.parse("<#ff5f6d>Red").color()).isEqualTo(Palette.BAD);
    }

    @Test
    void theColourCodesAreThePaletteInRgb() {
        assertThat(Text.parse("&6x").color()).isEqualTo(Palette.ACCENT);
        assertThat(Text.parse("&ax").color()).isEqualTo(Palette.GOOD);
        assertThat(Text.parse("&fx").color()).isEqualTo(Palette.VALUE);
        assertThat(Text.parse("<research>x").color()).isEqualTo(Palette.RESEARCH);
    }

    @Test
    void parseReadsAmpersandCodesAndTagsInOneText() {
        assertThat(Text.plain(Text.parse("&cRed <bold>and</bold> &ax"))).isEqualTo("Red and x");
        assertThat(Text.parse("&cRed <bold>and</bold> &ax")).isEqualTo(Text.parse("<bad>Red <bold>and</bold> <good>x"));
        assertThat(Text.parse("<bold>x &cy")).isEqualTo(Text.parse("<bold>x <bad>y"));
    }

    @Test
    void anEscapedValueShowsAsTypedAndAddsNoColour() {
        String value = "<red>evil</red>";

        String text = Text.serialize(Text.mini("<gray>Name: " + Text.escape(value)));

        assertThat(Text.plain(Text.mini(text))).isEqualTo("Name: <red>evil</red>");
    }

    @Test
    void anEscapedValueWithTagsAndAmpersandCodesShowsAsTyped() {
        String value = "<red>x</red> & &c >";

        assertThat(Text.plain(Text.parse("Name: " + Text.escape(value)))).isEqualTo("Name: " + value);
    }

    @Test
    void escapingNothingGivesNothing() {
        assertThat(Text.escape(null)).isEmpty();
    }

    @Test
    void anItemNameIsNotItalicUnlessItSaysSo() {
        assertThat(Text.item("&7Name").decoration(TextDecoration.ITALIC)).isEqualTo(TextDecoration.State.FALSE);
    }

    @Test
    void anItemIsNotItalicWhateverItsOtherCodes() {
        assertThat(Text.item("Name").decoration(TextDecoration.ITALIC)).isEqualTo(TextDecoration.State.FALSE);
        assertThat(Text.item("&lName").decoration(TextDecoration.ITALIC)).isEqualTo(TextDecoration.State.FALSE);
        assertThat(Text.item("&lName").decoration(TextDecoration.BOLD)).isEqualTo(TextDecoration.State.TRUE);
    }

    @Test
    void plainKeepsOnlyTheTextOfAComponentWithAHoverAndAClick() {
        Component tooltip = Text.parse("&7Tooltip text");
        Component clickable = Text.suggest(Text.hover(Text.parse("&cName"), tooltip), "/cf info");

        assertThat(clickable.hoverEvent()).isNotNull();
        assertThat(clickable.clickEvent()).isNotNull();
        assertThat(Text.plain(clickable)).isEqualTo("Name");
        assertThat(Text.plain(Text.parse("&7Click ").append(clickable))).isEqualTo("Click Name");
    }

    @Test
    void loreKeepsItsLinesInOrder() {
        assertThat(Text.lore(java.util.List.of("&7one", "&7two")))
                .extracting(Text::plain)
                .containsExactly("one", "two");
    }
}
