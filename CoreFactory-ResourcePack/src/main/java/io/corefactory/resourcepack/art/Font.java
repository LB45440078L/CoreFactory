package io.corefactory.resourcepack.art;

import java.util.Locale;
import java.util.Map;

/** A 3 x 5 pixel font for short labels: letters, digits and a few signs. */
public final class Font {

    /** Each glyph is 15 characters, row by row, three per row: '1' is ink. */
    private static final Map<Character, String> GLYPHS = Map.ofEntries(
            Map.entry('A', "010101111101101"), Map.entry('B', "110101110101110"),
            Map.entry('C', "011100100100011"), Map.entry('D', "110101101101110"),
            Map.entry('E', "111100110100111"), Map.entry('F', "111100110100100"),
            Map.entry('G', "011100101101011"), Map.entry('H', "101101111101101"),
            Map.entry('I', "111010010010111"), Map.entry('J', "001001001101010"),
            Map.entry('K', "101101110101101"), Map.entry('L', "100100100100111"),
            Map.entry('M', "101111111101101"), Map.entry('N', "101111111111101"),
            Map.entry('O', "010101101101010"), Map.entry('P', "110101110100100"),
            Map.entry('Q', "010101101110011"), Map.entry('R', "110101110101101"),
            Map.entry('S', "011100010001110"), Map.entry('T', "111010010010010"),
            Map.entry('U', "101101101101111"), Map.entry('V', "101101101101010"),
            Map.entry('W', "101101111111101"), Map.entry('X', "101101010101101"),
            Map.entry('Y', "101101010010010"), Map.entry('Z', "111001010100111"),
            Map.entry('0', "111101101101111"), Map.entry('1', "010110010010111"),
            Map.entry('2', "110001010100111"), Map.entry('3', "110001010001110"),
            Map.entry('4', "101101111001001"), Map.entry('5', "111100110001110"),
            Map.entry('6', "011100110101010"), Map.entry('7', "111001010010010"),
            Map.entry('8', "010101010101010"), Map.entry('9', "010101011001110"),
            Map.entry('-', "000000111000000"), Map.entry('/', "001001010100100"),
            Map.entry('.', "000000000000010"));

    private Font() {
    }

    /** The width of {@code text} in pixels, with one pixel between letters. */
    public static int width(String text) {
        return text.isEmpty() ? 0 : text.length() * 4 - 1;
    }

    /** Draws {@code text} with its top-left corner at ({@code x}, {@code y}). Unknown characters draw as a dash. */
    public static void draw(Canvas c, String text, int x, int y, int argb) {
        String upper = text.toUpperCase(Locale.ROOT);
        for (int i = 0; i < upper.length(); i++) {
            String glyph = GLYPHS.getOrDefault(upper.charAt(i), GLYPHS.get('-'));
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    if (glyph.charAt(row * 3 + col) == '1') {
                        c.set(x + i * 4 + col, y + row, argb);
                    }
                }
            }
        }
    }
}
