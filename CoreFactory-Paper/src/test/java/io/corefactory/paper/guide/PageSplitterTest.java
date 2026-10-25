package io.corefactory.paper.guide;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageSplitterTest {

    @Test
    void aShortParagraphIsOneLine() {
        assertThat(PageSplitter.wrap("Short text.")).containsExactly("Short text.");
    }

    @Test
    void wordsWrapToTheLineWidthAndKeepTheirOrder() {
        String text = "Uranium must be concentrated, turned into a gas, and enriched until its U-235 is a larger share.";

        List<String> lines = PageSplitter.wrap(text);

        assertThat(lines).allSatisfy(line -> assertThat(PageSplitter.visibleLength(line))
                .isLessThanOrEqualTo(PageSplitter.LINE_WIDTH));
        assertThat(String.join(" ", lines)).isEqualTo(text);
    }

    @Test
    void aWordLongerThanALineIsCutToFit() {
        List<String> lines = PageSplitter.wrap("Extraordinarilylongword end");

        assertThat(lines).allSatisfy(line -> assertThat(PageSplitter.visibleLength(line))
                .isLessThanOrEqualTo(PageSplitter.LINE_WIDTH));
        assertThat(String.join(" ", lines).replace(" ", "")).isEqualTo("Extraordinarilylongwordend");
    }

    @Test
    void theLeadingColourCodeIsRepeatedOnEveryLine() {
        List<String> lines = PageSplitter.wrap("&6&lA heading long enough to need several lines of its own");

        assertThat(lines).hasSizeGreaterThan(1);
        assertThat(lines).allSatisfy(line -> assertThat(line).startsWith("&6&l"));
    }

    @Test
    void colourCodesDoNotCountTowardTheWidth() {
        assertThat(PageSplitter.visibleLength("&6&lAbc&r")).isEqualTo(3);
    }

    @Test
    void anEmptyParagraphIsABlankLine() {
        assertThat(PageSplitter.wrap("")).containsExactly("");
    }

    @Test
    void noPageHoldsMoreLinesThanAPageCan() {
        List<String> paragraphs = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            paragraphs.add("Paragraph " + i + " has enough words in it to wrap onto a second line.");
            paragraphs.add("");
        }

        List<String> pages = PageSplitter.paginate(paragraphs);

        assertThat(pages).hasSizeGreaterThan(1);
        for (String page : pages) {
            assertThat(page.split("\n", -1)).hasSizeLessThanOrEqualTo(PageSplitter.LINES_PER_PAGE);
        }
    }

    @Test
    void thePagesHoldEveryLineInOrder() {
        List<String> paragraphs = List.of("One two three four five six seven eight nine ten eleven twelve.", "",
                "Last paragraph.");

        List<String> pages = PageSplitter.paginate(paragraphs);
        List<String> lines = new ArrayList<>();
        pages.forEach(page -> lines.addAll(Arrays.asList(page.split("\n", -1))));

        List<String> expected = new ArrayList<>();
        paragraphs.forEach(paragraph -> expected.addAll(PageSplitter.wrap(paragraph)));
        assertThat(lines).isEqualTo(expected);
    }

    @Test
    void aShortChapterIsOnePage() {
        assertThat(PageSplitter.paginate(List.of("&6&lTitle", "", "Body."))).hasSize(1);
    }
}
