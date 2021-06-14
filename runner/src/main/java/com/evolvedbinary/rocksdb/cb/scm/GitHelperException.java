package com.evolvedbinary.rocksdb.cb.scm;

public class GitHelperException extends Exception {
    public GitHelperException(final String message) {
        super(message);
    }

    public GitHelperException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
