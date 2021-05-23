package com.evolvedbinary.rocksdb.cb.junit;

import org.junit.jupiter.api.extension.*;
import org.junit.rules.ExternalResource;

import java.lang.reflect.Method;

public class JUnit5ExternalResourceAdapter<T extends ExternalResource> implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback {

    private final Method beforeMethod;
    private final Method afterMethod;
    /**
     * Ensures that before/after is only
     * called once if beforeAll and afterAll
     * are in play, e.g. this resource
     * is static resource.
     */
    private int referenceCount;

    private final T externalResource;

    public JUnit5ExternalResourceAdapter(final T externalResource) {
        this.externalResource = externalResource;
        try {
            this.beforeMethod = ExternalResource.class.getDeclaredMethod("before");
            this.beforeMethod.setAccessible(true);
            this.afterMethod = ExternalResource.class.getDeclaredMethod("after");
            this.afterMethod.setAccessible(true);
        } catch (final Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public T getExternalResource() {
        return externalResource;
    }

    @Override
    public void beforeAll(final ExtensionContext extensionContext) throws Exception {
        before(extensionContext);
    }

    @Override
    public void beforeEach(final ExtensionContext extensionContext) throws Exception {
        before(extensionContext);
    }

    @Override
    public void afterEach(final ExtensionContext extensionContext) throws Exception {
        after(extensionContext);
    }

    @Override
    public void afterAll(final ExtensionContext extensionContext) throws Exception {
        after(extensionContext);
    }

    private void before(final ExtensionContext extensionContext) throws Exception {
        if (++referenceCount != 1) {
            return;
        }

        beforeMethod.invoke(externalResource);
    }

    private void after(final ExtensionContext extensionContext) throws Exception {
        if (--referenceCount != 0) {
            return;
        }

        afterMethod.invoke(externalResource);
    }
}
