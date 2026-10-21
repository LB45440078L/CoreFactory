package io.corefactory.spigot.hologram;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the ampersand-coded text of a hologram into the JSON component its name is sent as. Colour codes 0 to 9 and a to f
 * set the colour of the text after them, and {@code &r} clears it. Any other {@code &} is kept as written.
 */
final class HologramJson {

    private static final String CODES = "0123456789abcdef";
    private static final String[] COLOURS = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};

    private HologramJson() {
    }

    /** The JSON component for the text: a root with one part per run of text that has a colour of its own. */
    static String of(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        String colour = null;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                int index = CODES.indexOf(code);
                if (index >= 0 || code == 'r') {
                    flush(parts, run, colour);
                    colour = index >= 0 ? COLOURS[index] : null;
                    i += 2;
                    continue;
                }
            }
            run.append(c);
            i++;
        }
        flush(parts, run, colour);
        return "{\"text\":\"\",\"extra\":[" + String.join(",", parts) + "]}";
    }

    private static void flush(List<String> parts, StringBuilder run, String colour) {
        if (run.isEmpty()) {
            return;
        }
        StringBuilder part = new StringBuilder("{\"text\":\"").append(escape(run.toString())).append('"');
        if (colour != null) {
            part.append(",\"color\":\"").append(colour).append('"');
        }
        parts.add(part.append('}').toString());
        run.setLength(0);
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
