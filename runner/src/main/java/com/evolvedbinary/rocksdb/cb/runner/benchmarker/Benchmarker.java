package com.evolvedbinary.rocksdb.cb.runner.benchmarker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Benchmarker {

    /**
     * Run a Benchmark.
     *
     * @param benchmarkId a unique identifier for the benchmark
     * @param projectRepoDir the location of the source code
     * @param projectLogDir the location to write benchmark log files to
     * @param benchmarkArgs the make build targets
     *
     * @return the result of the build
     *
     * @throws IllegalArgumentException if the projectRepoDir does not exist,
     *     if the projectLog dir does not exist or is not writable
     *
     * @throws IllegalArgumentException if the {@code projectRepoDir} does not exist,
     *     or if the {@projectLogDir} does not exist or cannot be created,
     *     or if the targets list is empty
     *
     * @throws IOException if the benchmark cannot be started
     */
    BenchmarkResult benchmark(final UUID benchmarkId, final Path projectRepoDir, final Path projectLogDir, final List<String> benchmarkArgs) throws IOException;
}
