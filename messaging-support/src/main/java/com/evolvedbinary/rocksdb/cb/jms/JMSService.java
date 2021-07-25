package com.evolvedbinary.rocksdb.cb.jms;

public interface JMSService {

    void runSync() throws InterruptedException;

    JMSServiceInstance runAsync();
}
