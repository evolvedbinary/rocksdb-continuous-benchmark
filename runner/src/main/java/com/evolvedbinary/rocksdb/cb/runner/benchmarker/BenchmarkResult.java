package com.evolvedbinary.rocksdb.cb.runner.benchmarker;

import java.nio.file.Path;

import static com.evolvedbinary.rocksdb.cb.process.ProcessHelper.NORMAL_EXIT_CODE;

public class BenchmarkResult {
    public final boolean ok;
    public final int exitCode;
    public final long duration;
    public final Path stdOutputLogFile;
    public final Path stdErrorLogFile;

    static BenchmarkResult ok(final long duration, final Path stdOutputLogFile, final Path stdErrorLogFile) {
        return new BenchmarkResult(true, NORMAL_EXIT_CODE, duration, stdOutputLogFile, stdErrorLogFile);
    }

    static BenchmarkResult failure(final int exitCode, final long duration, final Path stdOutputLogFile, final Path stdErrorLogFile) {
        return new BenchmarkResult(false, exitCode, duration, stdOutputLogFile, stdErrorLogFile);
    }

    private BenchmarkResult(final boolean ok, final int exitCode, final long duration, final Path stdOutputLogFile, final Path stdErrorLogFile) {
        this.ok = ok;
        this.exitCode = exitCode;
        this.duration = duration;
        this.stdOutputLogFile = stdOutputLogFile;
        this.stdErrorLogFile = stdErrorLogFile;
    }
}
