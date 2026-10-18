package io.moretntplus.core.mechanics;

import io.moretntplus.core.config.Messages;
import io.moretntplus.core.engine.Blast;
import io.moretntplus.core.engine.Effects;
import io.moretntplus.core.engine.Explosions;

import java.util.function.Supplier;

/**
 * The shared services mechanics are built from. One instance is created by the core and handed to every mechanic.
 *
 * @param effects    sounds and particles
 * @param explosions entity damage and knockback
 * @param blast      the standard blast and its effect modifiers
 * @param messages   player-facing text, read lazily so reloads take effect
 */
public record Toolkit(Effects effects, Explosions explosions, Blast blast, Supplier<Messages> messages) {
}
