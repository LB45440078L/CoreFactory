package io.corefactory.paper.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns paragraphs of book text into pages. A written book shows about {@value #LINES_PER_PAGE} lines of about
 * {@value #LINE_WIDTH} characters, so each paragraph is wrapped to that width and the lines are packed into pages.
 * A paragraph's leading colour codes are repeated on each of its lines. Colour codes do not count toward the width.
 */
public final class PageSplitter {

    /** The most lines one page holds. */
    public static final int LINES_PER_PAGE = 14;

    /** The most visible characters on one line. */
    public static final int LINE_WIDTH = 19;

    private static final Pattern LEADING_CODES = Pattern.compile("^((?:&[0-9a-fk-orA-FK-OR])*)(.*)$", Pattern.DOTALL);
    private static final Pattern CODE = Pattern.compile("&[0-9a-fk-orA-FK-OR]");

    private PageSplitter() {
    }

    /**
     * Lays the paragraphs out as pages, in order. An empty paragraph is a blank line.
     *
     * @return the pages, each a newline-joined block of at most {@link #LINES_PER_PAGE} lines
     */
    public static List<String> paginate(List<String> paragraphs) {
        List<String> pages = new ArrayList<>();
        List<String> page = new ArrayList<>();
        for (String paragraph : paragraphs) {
            for (String line : wrap(paragraph)) {
                if (page.size() == LINES_PER_PAGE) {
                    pages.add(String.join("\n", page));
                    page = new ArrayList<>();
                }
                page.add(line);
            }
        }
        if (!page.isEmpty()) {
            pages.add(String.join("\n", page));
        }
        return pages;
    }

    /** Wraps one paragraph to the line width. Words longer than a line are cut to fit. */
    public static List<String> wrap(String paragraph) {
        Matcher codes = LEADING_CODES.matcher(paragraph);
        String prefix = codes.matches() ? codes.group(1) : "";
        String body = codes.matches() ? codes.group(2) : paragraph;
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : body.trim().split("\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            for (String piece : cut(word)) {
                if (current.isEmpty()) {
                    current.append(piece);
                } else if (visibleLength(current + " " + piece) <= LINE_WIDTH) {
                    current.append(' ').append(piece);
                } else {
                    lines.add(prefix + current);
                    current.setLength(0);
                    current.append(piece);
                }
            }
        }
        if (!current.isEmpty() || lines.isEmpty()) {
            lines.add(prefix + current);
        }
        return lines;
    }

    /** The number of characters a line shows: the text without its colour codes. */
    public static int visibleLength(String text) {
        return CODE.matcher(text).replaceAll("").length();
    }

    private static List<String> cut(String word) {
        if (word.length() <= LINE_WIDTH) {
            return List.of(word);
        }
        List<String> pieces = new ArrayList<>();
        for (int start = 0; start < word.length(); start += LINE_WIDTH) {
            pieces.add(word.substring(start, Math.min(word.length(), start + LINE_WIDTH)));
        }
        return pieces;
    }
}
