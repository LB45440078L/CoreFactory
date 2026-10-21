package io.corefactory.core.mechanics;

import io.corefactory.core.config.Messages;
import io.corefactory.core.engine.Blast;
import io.corefactory.core.engine.Effects;
import io.corefactory.core.engine.Explosions;
import io.corefactory.core.radiation.Radiation;

import java.util.function.Supplier;

/**
 * The shared services mechanics are built from. One instance is created by the core and handed to every mechanic.
 *
 * @param effects    sounds and particles
 * @param explosions entity damage and knockback
 * @param blast      the standard blast and its effect modifiers
 * @param messages   player-facing text, read lazily so reloads take effect
 * @param radiation  fallout, the radioactive suit and the Geiger counter
 */
public record Toolkit(Effects effects, Explosions explosions, Blast blast, Supplier<Messages> messages,
                      Radiation radiation) {
}
