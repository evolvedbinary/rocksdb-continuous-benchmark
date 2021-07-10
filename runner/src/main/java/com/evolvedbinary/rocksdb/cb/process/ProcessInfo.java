package com.evolvedbinary.rocksdb.cb.process;

import java.nio.file.Path;

public class ProcessInfo {
    public final Path stdOutputLogFile;
    public final Path stdErrorLogFile;
    public final Process process;

    public ProcessInfo(final Path stdOutputLogFile, final Path stdErrorLogFile, final Process process) {
        this.stdOutputLogFile = stdOutputLogFile;
        this.stdErrorLogFile = stdErrorLogFile;
        this.process = process;
    }
}
