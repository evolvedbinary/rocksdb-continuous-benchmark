package com.evolvedbinary.rocksdb.cb.jms;

public enum JMSServiceState {
    IDLE,
    RUNNING,
    AWAITING_SHUTDOWN,
    SHUTTING_DOWN
}
