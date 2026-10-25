package io.corefactory.paper.research;

import io.corefactory.core.research.ResearchStore;
import io.corefactory.storage.JdbcResearchStore;

import java.util.Set;
import java.util.UUID;

/** Gives the core's research service the database-backed store. */
public final class ResearchAdapter implements ResearchStore {

    private final JdbcResearchStore delegate;

    public ResearchAdapter(JdbcResearchStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<String> unlocked(UUID player) {
        return delegate.unlocked(player);
    }

    @Override
    public boolean unlock(UUID player, String node, long epochMillis) {
        return delegate.unlock(player, node, epochMillis);
    }

    @Override
    public int progress(UUID player, String node) {
        return delegate.progress(player, node);
    }

    @Override
    public void setProgress(UUID player, String node, int units) {
        delegate.setProgress(player, node, units);
    }
}
