package com.evolvedbinary.rocksdb.cb.common;

import com.evolvedbinary.j8fu.function.SupplierE;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public interface CloseUtil {

    /**
     * Invokes the closer and if an exception
     * is thrown then it is passed to logger.
     *
     * @param closer the closer function
     * @param logger the logger function
     */
    static void closeAndLogIfException(final SupplierE<AutoCloseable, Throwable> closer, final Consumer<Throwable> logger) {
        try {
            final AutoCloseable closeable = closer.get();
            closeable.close();
        } catch (final Throwable t) {
            logger.accept(t);
        }
    }

    /**
     * Invokes {@link AutoCloseable#close()} on the closeable,
     * and if an exception is thrown then it is passed to logger.
     *
     * If the closeable is null, then the function returns immediately.
     *
     * @param closeable the closeable object
     * @param logger the logger function
     */
    static void closeAndLogIfException(@Nullable final AutoCloseable closeable, final Consumer<Throwable> logger) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (final Throwable t) {
            logger.accept(t);
        }
    }

    /**
     * Invokes {@link AutoCloseable#close()} on the closeable,
     * and if an exception is thrown then it is passed to logger.
     *
     * If the closeable is null, then the function returns immediately.
     *
     * @param closeable the closeable object
     * @param logger the logger function
     */
    static void closeAndLogIfException(@Nullable final AutoCloseable closeable, final Logger logger) {
        closeAndLogIfException(closeable, t -> logger.error(t.getMessage(), t));
    }
}
