package io.corefactory.spigot.survey;

import io.corefactory.core.survey.ChunkSurvey;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Paints a chunk survey onto a map. The pixels are laid out once, when the renderer is made, and each render only writes
 * them to the canvas. The renderer is not contextual, so every viewer of the map sees the same picture.
 */
public final class SurveyRenderer extends MapRenderer {

    private final byte[] colours;

    /**
     * @param survey the survey of the chunk
     * @param oreRgb the base colour of each ore, indexed by ore index
     */
    public SurveyRenderer(ChunkSurvey.Survey survey, int[] oreRgb) {
        super(false);
        int[] rgb = SurveyMap.paint(survey, oreRgb);
        Map<Integer, Byte> palette = new HashMap<>();
        colours = new byte[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            colours[i] = palette.computeIfAbsent(rgb[i], SurveyRenderer::matchColour);
        }
    }

    /** Writes the matched palette bytes. The byte form is used on purpose: the colours are matched once, above. */
    @Override
    @SuppressWarnings("deprecation")
    public void render(MapView map, MapCanvas canvas, Player player) {
        for (int y = 0; y < SurveyMap.SIZE; y++) {
            for (int x = 0; x < SurveyMap.SIZE; x++) {
                canvas.setPixel(x, y, colours[y * SurveyMap.SIZE + x]);
            }
        }
    }

    private static byte matchColour(int rgb) {
        return MapPalette.matchColor(new Color(rgb));
    }
}
