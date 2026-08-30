package net.skykings.core.storage;

/** Unchecked Wrapper um Persistenzfehler, damit Services nicht mit {@code SQLException} arbeiten muessen. */
public class DataStoreException extends RuntimeException {

    public DataStoreException(String message) {
        super(message);
    }

    public DataStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
