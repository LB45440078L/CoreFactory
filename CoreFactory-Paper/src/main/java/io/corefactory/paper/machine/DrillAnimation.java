package io.corefactory.paper.machine;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Moves the display of a running drill up and down into the ground. The drill's own item display is the only entity used:
 * its transformation is set to one of {@link #FRAMES} keyframes of a stroke, and the display interpolates to each keyframe
 * over the {@link #INTERVAL_TICKS} ticks before the next, so the motion is smooth at a low update rate. A drill that is not
 * running goes back to its rest height and stays still. The display is written only on every {@link #INTERVAL_TICKS}th
 * tick, and only when its height changes.
 */
public final class DrillAnimation {

    /** Ticks between keyframes. The display interpolates over the same time. */
    public static final int INTERVAL_TICKS = 5;

    /** Keyframes in one stroke: rest, half down, fully down and half down again. */
    public static final int FRAMES = 4;

    /** How far the display sinks at the bottom of the stroke, in blocks. */
    public static final double STROKE = 0.125;

    private static final double EPSILON = 1e-6;

    private final Map<PlacedMachine, Boolean> running = new WeakHashMap<>();

    /** Records whether a drill is running, as its status reads this tick. Only a running drill strokes. */
    public void report(PlacedMachine placed, boolean isRunning) {
        running.put(placed, isRunning);
    }

    /** Animates the display of a drill for this tick. Call once per tick for every drill whose chunk is loaded. */
    public void tick(PlacedMachine placed, long tick) {
        if (placed.display() != null) {
            animate(placed.display(), running.getOrDefault(placed, false), tick);
        }
    }

    /**
     * Sets the height of one display on a keyframe tick: the next keyframe of the stroke while the drill runs, or the rest
     * height when it does not. Does nothing on the other ticks, or when the display is not loaded.
     *
     * @param display   the display's entity id
     * @param isRunning true while the drill is running
     * @param tick      the server tick
     */
    public void animate(UUID display, boolean isRunning, long tick) {
        if (tick % INTERVAL_TICKS != 0) {
            return;
        }
        Entity entity = Bukkit.getEntity(display);
        if (!(entity instanceof ItemDisplay item)) {
            return;
        }
        double target = isRunning ? offset((int) ((tick / INTERVAL_TICKS) % FRAMES)) : 0.0;
        Transformation current = item.getTransformation();
        if (Math.abs(current.getTranslation().y - target) < EPSILON) {
            return;
        }
        item.setInterpolationDelay(0);
        item.setInterpolationDuration(INTERVAL_TICKS);
        item.setTransformation(new Transformation(new Vector3f(0, (float) target, 0), current.getLeftRotation(),
                current.getScale(), current.getRightRotation()));
    }

    /**
     * The height of a keyframe, in blocks, from 0 at rest down to {@code -STROKE}: a half cosine over the stroke, so the
     * display rests at frame 0, sinks to its lowest at frame {@code FRAMES / 2}, and comes back up.
     */
    public static double offset(int frame) {
        return -STROKE * (1 - Math.cos(2 * Math.PI * frame / FRAMES)) / 2;
    }
}
