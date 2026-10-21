package io.corefactory.spigot.research;

import io.corefactory.core.research.ResearchService;
import io.corefactory.core.research.ResearchStore;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.storage.Database;
import io.corefactory.storage.JdbcResearchStore;
import io.corefactory.storage.StorageSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Opens research storage from config.yml and owns the research service. When the database cannot open, research
 * falls back to memory for this start and the log says so. Gated items stay gated either way, so a broken database
 * never unlocks bombs.
 */
public final class ResearchRuntime implements AutoCloseable {

    private final ResearchService service;
    private final Database database;
    private final ExecutorService writes;

    private ResearchRuntime(ResearchService service, Database database, ExecutorService writes) {
        this.service = service;
        this.database = database;
        this.writes = writes;
    }

    /**
     * Opens the store named by the {@code storage} section of config.yml.
     */
    public static ResearchRuntime open(Plugin plugin, ConfigurationSection config, ResearchTree tree) {
        Logger log = plugin.getLogger();
        ExecutorService writes = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "CoreFactory research");
            thread.setDaemon(true);
            return thread;
        });
        Database database = null;
        ResearchStore store;
        try {
            database = Database.open(settings(plugin, config), log);
            store = new ResearchAdapter(new JdbcResearchStore(database));
            log.info("Research progress is stored with " + settings(plugin, config).type() + ".");
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "Research storage could not open. Research lasts only until the server stops.", e);
            store = new MemoryStore();
        }
        return new ResearchRuntime(new ResearchService(tree, store, writes, log), database, writes);
    }

    public ResearchService service() {
        return service;
    }

    /**
     * The database the research store uses, which also keeps the drill ledger and the chunk budgets. Empty when it could not
     * open this start.
     */
    public Optional<Database> database() {
        return Optional.ofNullable(database);
    }

    /** Waits for pending writes, then closes the database. */
    @Override
    public void close() {
        writes.shutdown();
        try {
            writes.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (database != null) {
            database.close();
        }
    }

    private static StorageSettings settings(Plugin plugin, ConfigurationSection config) {
        boolean postgres = "postgresql".equalsIgnoreCase(config.getString("storage.type", "sqlite"));
        File file = new File(plugin.getDataFolder(), config.getString("storage.sqlite-file", "research.db"));
        return new StorageSettings(
                postgres ? StorageSettings.Type.POSTGRESQL : StorageSettings.Type.SQLITE,
                file.getPath(),
                config.getString("storage.postgresql.host", "localhost"),
                config.getInt("storage.postgresql.port", 5432),
                config.getString("storage.postgresql.database", "corefactory"),
                config.getString("storage.postgresql.user", "corefactory"),
                config.getString("storage.postgresql.password", ""),
                config.getInt("storage.postgresql.pool-size", 6),
                plugin.getDataFolder().toPath().resolve("libraries"));
    }

    /** Keeps research in memory only. Used when the database cannot open. */
    private static final class MemoryStore implements ResearchStore {

        private final Map<UUID, Set<String>> unlocked = new ConcurrentHashMap<>();
        private final Map<String, Integer> progress = new ConcurrentHashMap<>();

        @Override
        public Set<String> unlocked(UUID player) {
            return Set.copyOf(unlocked.getOrDefault(player, Set.of()));
        }

        @Override
        public boolean unlock(UUID player, String node, long epochMillis) {
            return unlocked.computeIfAbsent(player, key -> ConcurrentHashMap.newKeySet()).add(node);
        }

        @Override
        public int progress(UUID player, String node) {
            return progress.getOrDefault(player + "/" + node, 0);
        }

        @Override
        public void setProgress(UUID player, String node, int units) {
            progress.put(player + "/" + node, units);
        }
    }
}
