package com.evolvedbinary.rocksdb.cb.common;

public interface ExitCodes {

    int NORMAL_EXIT_CODE = 0;

    int INVALID_ARGUMENT = 1;

    int UNABLE_TO_CONNECT_TO_JMS_BROKER = 8;

    int NO_SUCH_KEYSTORE = 16;
    int INVALID_PATH = 17;

    int INTERRUPTED_EXIT_CODE = 25;
}
