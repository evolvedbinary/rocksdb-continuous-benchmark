package com.evolvedbinary.rocksdb.cb.runner.builder;

import java.nio.file.Path;

import static com.evolvedbinary.rocksdb.cb.process.ProcessHelper.NORMAL_EXIT_CODE;

public class BuildResult {
    public final boolean ok;
    public final int exitCode;
    public final long duration;
    public final Path stdOutputLogFile;
    public final Path stdErrorLogFile;

    static BuildResult ok(final long duration, final Path stdOutputLogFile, final Path stdErrorLogFile) {
        return new BuildResult(true, NORMAL_EXIT_CODE, duration, stdOutputLogFile, stdErrorLogFile);
    }

    static BuildResult failure(final int exitCode, final long duration, final Path stdOutputLogFile, final Path stdErrorLogFile) {
        return new BuildResult(false, exitCode, duration, stdOutputLogFile, stdErrorLogFile);
    }

    private BuildResult(final boolean ok, final int exitCode, final long duration, final Path stdOutputLogFile, final Path stdErrorLogFile) {
        this.ok = ok;
        this.exitCode = exitCode;
        this.duration = duration;
        this.stdOutputLogFile = stdOutputLogFile;
        this.stdErrorLogFile = stdErrorLogFile;
    }
}
