package io.corefactory.spigot.ledger;

import io.corefactory.core.ledger.ChunkBudgets;
import io.corefactory.core.ledger.MemoryChunkBudgets;
import io.corefactory.core.ledger.MemoryMiningLedger;
import io.corefactory.core.ledger.MiningLedger;
import io.corefactory.core.ledger.OreBudget;
import io.corefactory.storage.Database;
import io.corefactory.storage.JdbcChunkBudgets;
import io.corefactory.storage.JdbcMiningLedger;

import java.util.Map;
import java.util.Optional;

/**
 * The drill ledger and the chunk budgets, on the research database when it is open, otherwise in memory for this start.
 * The storage classes return their own types, so these adapters give the core its interfaces.
 */
public final class DrillLedgers {

    private DrillLedgers() {
    }

    /** The mining ledger on the database, or in memory when the database is not open. */
    public static MiningLedger miningLedger(Optional<Database> database) {
        return database.<MiningLedger>map(JdbcLedger::new).orElseGet(MemoryMiningLedger::new);
    }

    /** The chunk budgets on the database, or in memory when the database is not open. */
    public static ChunkBudgets chunkBudgets(Optional<Database> database) {
        return database.<ChunkBudgets>map(JdbcBudgets::new).orElseGet(MemoryChunkBudgets::new);
    }

    /** The mining ledger on the database. */
    private static final class JdbcLedger implements MiningLedger {

        private final JdbcMiningLedger delegate;

        JdbcLedger(Database database) {
            this.delegate = new JdbcMiningLedger(database);
        }

        @Override
        public void add(String drillKey, String oreId, long amount) {
            delegate.add(drillKey, oreId, amount);
        }

        @Override
        public long mined(String drillKey, String oreId) {
            return delegate.mined(drillKey, oreId);
        }

        @Override
        public Map<String, Long> minedBy(String drillKey) {
            return delegate.minedBy(drillKey);
        }
    }

    /** The chunk budgets on the database. */
    private static final class JdbcBudgets implements ChunkBudgets {

        private final JdbcChunkBudgets delegate;

        JdbcBudgets(Database database) {
            this.delegate = new JdbcChunkBudgets(database);
        }

        @Override
        public boolean isBudgeted(String world, int chunkX, int chunkZ) {
            return delegate.isBudgeted(world, chunkX, chunkZ);
        }

        @Override
        public void plant(String world, int chunkX, int chunkZ, Map<String, Long> planned) {
            delegate.plant(world, chunkX, chunkZ, planned);
        }

        @Override
        public Optional<OreBudget> find(String world, int chunkX, int chunkZ, String oreId) {
            return delegate.find(world, chunkX, chunkZ, oreId)
                    .map(budget -> new OreBudget(budget.planned(), budget.mined()));
        }

        @Override
        public boolean recordMined(String world, int chunkX, int chunkZ, String oreId, long amount) {
            return delegate.recordMined(world, chunkX, chunkZ, oreId, amount);
        }
    }
}
