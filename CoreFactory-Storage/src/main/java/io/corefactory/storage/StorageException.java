package io.corefactory.storage;

/** Unchecked wrapper for failures from the database or the file system. */
public final class StorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what failed
     * @param cause the underlying failure
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
