package io.corefactory.paper.screen;

import io.corefactory.core.hologram.Tone;

import io.corefactory.core.text.Palette;

import java.util.ArrayList;
import java.util.List;

/**
 * The rows of a device's screen, as MiniMessage. The first row is blank, so the text has a little padding above it. The second is the
 * device's name in a gradient of the accent colours, the third a thin separator, and the fourth the status word, tinted by the device's
 * tone: green while it works, red while it is stuck, and yellow when it is idle or has nothing to say. The rows after them are the lines
 * of the device's hologram. Each of those lines is split on the double spaces between its segments, so every segment takes a row of its
 * own. Pure, so the rows are tested without a server.
 */
public final class ScreenText {

    private static final String NAME_OPEN = "<gradient:" + Palette.ACCENT.asHexString() + ":" + Palette.BAD.asHexString() + ">";
    private static final String NAME_CLOSE = "</gradient>";
    private static final String SEPARATOR = "<hint>" + "─".repeat(12) + "</hint>";
    private static final String SEGMENT_GAP = "  ";
    private static final String DOT_GAP = "  ·  ";
    private static final String DOT = " · ";

    private ScreenText() {
    }

    /**
     * The rows of a screen. The rows past the most a screen shows are left out.
     *
     * @param name   the device's name, shown in the gradient
     * @param status the status word, such as {@code Working}, without a colour
     * @param tone   the tone of the device, which tints the status word
     * @param lines  the lines of the device's hologram, top first, with their ampersand colour codes
     */
    public static List<String> rows(String name, String status, Tone tone, List<String> lines) {
        List<String> rows = new ArrayList<>();
        rows.add("");
        rows.add(NAME_OPEN + name + NAME_CLOSE);
        rows.add(SEPARATOR);
        rows.add(tinted(tone, status));
        for (String line : lines) {
            for (String segment : line.replace(DOT_GAP, DOT).split(SEGMENT_GAP)) {
                if (!segment.isBlank()) {
                    rows.add(segment);
                }
            }
        }
        return List.copyOf(rows.subList(0, Math.min(rows.size(), DeviceScreen.MAX_ROWS)));
    }

    /**
     * A text in the colour of a tone, as a MiniMessage tag around it: {@code <good>} for a working device, {@code <bad>} for a stuck one,
     * and {@code <warn>} otherwise.
     */
    static String tinted(Tone tone, String text) {
        String tag = switch (tone) {
            case WORKING -> "good";
            case STUCK -> "bad";
            case NEUTRAL -> "warn";
        };
        return "<" + tag + ">" + text + "</" + tag + ">";
    }
}
