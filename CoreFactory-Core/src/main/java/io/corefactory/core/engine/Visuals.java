package io.corefactory.core.engine;

import org.bukkit.Location;

/**
 * Shows a named visual effect at a place: the sprite effects of blasts, machines and research. The core asks for an
 * effect by name and the platform decides how to draw it. The effects are listed in {@link VisualCatalog}.
 */
@FunctionalInterface
public interface Visuals {

    /** Shows nothing. The default until the platform supplies its effects. */
    Visuals NONE = (effect, at, scale) -> {
    };

    /**
     * Plays a named effect.
     *
     * @param effect the effect's name, as {@link VisualCatalog} lists it
     * @param at     where the effect is centred
     * @param scale  the effect's size in blocks: a radius, a height or a spread, depending on the effect
     */
    void play(String effect, Location at, double scale);
}
