package io.corefactory.spigot.ledger;

import io.corefactory.core.deposit.DepositStore;
import io.corefactory.core.deposit.MemoryDepositStore;
import io.corefactory.storage.Database;
import io.corefactory.storage.JdbcDepositStore;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * The store of the planned deposits and their remaining amounts. It lives in the research database when that database is open,
 * so the amounts survive a restart; otherwise it is in memory and lasts until the server stops.
 */
public final class DepositLedger {

    private DepositLedger() {
    }

    /**
     * Opens the deposit store for this start, and logs which one it is.
     *
     * @param database the research database, empty when it did not open
     * @param log      receives the choice of store
     */
    public static DepositStore open(Optional<Database> database, Logger log) {
        if (database.isPresent()) {
            log.info("Deposits are kept in the database, so their remaining amounts survive a restart.");
            return new JdbcDepositStore(database.get());
        }
        log.warning("The database is not open, so deposits and their remaining amounts are kept in memory and last only until the "
                + "server stops.");
        return new MemoryDepositStore();
    }
}
