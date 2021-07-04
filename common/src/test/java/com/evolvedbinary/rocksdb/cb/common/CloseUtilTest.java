package com.evolvedbinary.rocksdb.cb.common;

import com.evolvedbinary.j8fu.function.SupplierE;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.function.Consumer;

import static org.easymock.EasyMock.*;

public class CloseUtilTest {

    @Test
    public void closeAndLogIfException_supplier_exception() throws Throwable {
        final SupplierE<AutoCloseable, Throwable> mockCloser = mock(SupplierE.class);
        final Consumer<Throwable> mockLogger = mock(Consumer.class);

        // behaviour
        final Throwable t = new Throwable("test error");
        expect(mockCloser.get()).andThrow(t);
        mockLogger.accept(t);

        replay(mockCloser, mockLogger);

        // method under test
        CloseUtil.closeAndLogIfException(mockCloser, mockLogger);

        verify(mockCloser, mockLogger);
    }

    @Test
    public void closeAndLogIfException_supplier_close_exception() throws Throwable {
        final SupplierE<AutoCloseable, Throwable> mockCloser = mock(SupplierE.class);
        final AutoCloseable mockCloseable = mock(AutoCloseable.class);
        final Consumer<Throwable> mockLogger = mock(Consumer.class);

        // behaviour
        final Exception e = new Exception("test error");
        expect(mockCloser.get()).andReturn(mockCloseable);
        mockCloseable.close();
        expectLastCall().andThrow(e);
        mockLogger.accept(e);

        replay(mockCloser, mockCloseable, mockLogger);

        // method under test
        CloseUtil.closeAndLogIfException(mockCloser, mockLogger);

        verify(mockCloser, mockCloseable, mockLogger);
    }

    @Test
    public void closeAndLogIfException_supplier_noException() throws Throwable {
        final SupplierE<AutoCloseable, Throwable> mockCloser = mock(SupplierE.class);
        final AutoCloseable mockCloseable = mock(AutoCloseable.class);
        final Consumer<Throwable> mockLogger = mock(Consumer.class);

        // behaviour
        expect(mockCloser.get()).andReturn(mockCloseable);
        mockCloseable.close();

        replay(mockCloser, mockCloseable, mockLogger);

        // method under test
        CloseUtil.closeAndLogIfException(mockCloser, mockLogger);

        verify(mockCloser, mockCloseable, mockLogger);
    }

    @Test
    public void closeAndLogIfException_autoCloseable_null() {
        final Consumer<Throwable> mockLogger = mock(Consumer.class);

        replay(mockLogger);

        CloseUtil.closeAndLogIfException((AutoCloseable) null, mockLogger);

        verify(mockLogger);
    }

    @Test
    public void closeAndLogIfException_autoCloseable_exception() throws Exception {
        final AutoCloseable mockCloseable = mock(AutoCloseable.class);
        final Consumer<Throwable> mockLogger = mock(Consumer.class);

        // behaviour
        final Exception e = new Exception("test error");
        mockCloseable.close();
        expectLastCall().andThrow(e);
        mockLogger.accept(e);

        replay(mockCloseable, mockLogger);

        // method under test
        CloseUtil.closeAndLogIfException(mockCloseable, mockLogger);

        verify(mockCloseable, mockLogger);
    }

    @Test
    public void closeAndLogIfException_autoCloseable_noException() throws Throwable {
        final AutoCloseable mockCloseable = mock(AutoCloseable.class);
        final Consumer<Throwable> mockLogger = mock(Consumer.class);

        // behaviour
        mockCloseable.close();

        replay(mockCloseable, mockLogger);

        // method under test
        CloseUtil.closeAndLogIfException(mockCloseable, mockLogger);

        verify(mockCloseable, mockLogger);
    }

    @Test
    public void closeAndLogIfException_autoCloseableLogger_null() {
        final Logger mockLogger = mock(Logger.class);

        replay(mockLogger);

        CloseUtil.closeAndLogIfException((AutoCloseable) null, mockLogger);

        verify(mockLogger);
    }

    @Test
    public void closeAndLogIfException_autoCloseableLogger_exception() throws Exception {
        final AutoCloseable mockCloseable = mock(AutoCloseable.class);
        final Logger mockLogger = mock(Logger.class);

        // behaviour
        final Exception e = new Exception("test error");
        mockCloseable.close();
        expectLastCall().andThrow(e);
        mockLogger.error(e.getMessage(), e);

        replay(mockCloseable, mockLogger);

        // method under test
        CloseUtil.closeAndLogIfException(mockCloseable, mockLogger);

        verify(mockCloseable, mockLogger);
    }

    @Test
    public void closeAndLogIfException_autoCloseableLogger_noException() throws Throwable {
        final AutoCloseable mockCloseable = mock(AutoCloseable.class);
        final Logger mockLogger = mock(Logger.class);

        // behaviour
        mockCloseable.close();

        replay(mockCloseable, mockLogger);

        // method under test
        CloseUtil.closeAndLogIfException(mockCloseable, mockLogger);

        verify(mockCloseable, mockLogger);
    }
}
