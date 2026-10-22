package io.corefactory.storage;

import io.corefactory.core.deposit.DepositStore;
import io.corefactory.core.deposit.MemoryDepositStore;
import org.junit.jupiter.api.BeforeEach;

class MemoryDepositStoreTest extends DepositStoreContract {

    private DepositStore store;

    @BeforeEach
    void open() {
        store = new MemoryDepositStore();
    }

    @Override
    protected DepositStore store() {
        return store;
    }
}
