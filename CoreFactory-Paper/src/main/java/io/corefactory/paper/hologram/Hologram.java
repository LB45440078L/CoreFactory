package io.corefactory.paper.hologram;

import io.corefactory.core.hologram.HologramLayout;
import io.corefactory.core.hologram.Tone;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One hologram: a single text display that shows every line, one above the other, anchored by its bottom line. Its text is pushed to the
 * display only when it changes, and so is its tone, the colour of its background. The game shows it to the players in range. Create
 * holograms through {@link HologramService#create}, which owns the display.
 */
public final class Hologram {

    private final Location bottom;
    private final int lineCount;
    private final boolean pylon;
    private final Runnable onChange;
    private List<String> lines;
    private String shown;
    private Tone tone = Tone.NEUTRAL;
    private Tone shownTone;
    private UUID display;

    /**
     * @param bottom   the point the bottom line stands at
     * @param lines    the text of each line, top first
     * @param pylon    true for a pylon's hologram, which is shown to everyone at all times; false for a device's, which is hidden until
     *                 the holograms of devices are turned on
     * @param onChange told when the text or the tone changes, so the display is updated
     */
    Hologram(Location bottom, List<String> lines, boolean pylon, Runnable onChange) {
        this.bottom = bottom;
        this.lineCount = lines.size();
        this.pylon = pylon;
        this.onChange = onChange;
        this.lines = fit(lines);
    }

    /** The number of lines, fixed when the hologram is made. */
    public int lineCount() {
        return lineCount;
    }

    /** True for a pylon's hologram, which is shown at all times; false for a device's. Fixed when the hologram is made. */
    public boolean pylon() {
        return pylon;
    }

    /** A copy of the point the bottom line stands at. */
    public Location position() {
        return bottom.clone();
    }

    /** A copy of the point the display's centre stands at: the bottom line raised by half the stack. */
    Location centre() {
        return bottom.clone().add(0, HologramLayout.centreOffset(lineCount), 0);
    }

    /** The text of a line, with its colour codes or MiniMessage tags. */
    public String text(int line) {
        return lines.get(line);
    }

    /** The text of every line, top first. */
    public List<String> lines() {
        return lines;
    }

    /**
     * Sets the text of the lines. The number of lines is fixed when the hologram is made: a longer list is cut to it, and a shorter one is
     * padded with empty lines. The display is updated only when the text differs from what it shows.
     */
    public void setLines(List<String> text) {
        List<String> next = fit(text);
        if (!HologramLayout.joined(next).equals(shown)) {
            this.lines = next;
            onChange.run();
        }
    }

    /** The tone the background shows now. */
    public Tone tone() {
        return tone;
    }

    /** Sets the tone the background shows. The display is updated only when it differs from the tone shown. */
    public void setTone(Tone tone) {
        if (tone != this.tone) {
            this.tone = tone;
            onChange.run();
        }
    }

    /** The text the display shows now, joined into one string, or null before the display is shown. */
    String shown() {
        return shown;
    }

    /** Records the text the display now shows. */
    void setShown(String text) {
        this.shown = text;
    }

    /** The tone the display shows now, or null before the display is shown. */
    Tone shownTone() {
        return shownTone;
    }

    /** Records the tone the display now shows. */
    void setShownTone(Tone tone) {
        this.shownTone = tone;
    }

    /** The text as the display should show it: every line, joined. */
    String joined() {
        return HologramLayout.joined(lines);
    }

    World world() {
        return bottom.getWorld();
    }

    /** The unique id of the text display, or null while none is shown. */
    UUID display() {
        return display;
    }

    void setDisplay(UUID display) {
        this.display = display;
    }

    private List<String> fit(List<String> text) {
        List<String> fitted = new ArrayList<>();
        for (int line = 0; line < lineCount; line++) {
            String value = text != null && line < text.size() ? text.get(line) : null;
            fitted.add(value == null ? "" : value);
        }
        return List.copyOf(fitted);
    }
}
