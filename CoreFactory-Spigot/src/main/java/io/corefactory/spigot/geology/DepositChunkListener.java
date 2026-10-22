package io.corefactory.spigot.geology;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Objects;

/**
 * Lays the deposits of a chunk when it loads. A new chunk gets them as it generates; an older chunk gets them the first
 * time it loads after the deposits came in, and the chunk's marker keeps every later load from laying it again.
 */
public final class DepositChunkListener implements Listener {

    private final DepositGenerator generator;

    /** @param generator the generator that lays each chunk */
    public DepositChunkListener(DepositGenerator generator) {
        this.generator = Objects.requireNonNull(generator, "generator");
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        generator.populate(event.getChunk());
    }
}
