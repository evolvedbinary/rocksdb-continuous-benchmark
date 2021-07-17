package com.evolvedbinary.rocksdb.cb.runner.benchmarker;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Benchmarker {

    /**
     * Run a Benchmark.
     *
     * @param benchmarkId a unique identifier for the benchmark
     * @param projectRepoDir the location of the source code
     * @param projectLogDir the location to write benchmark log files to
     * @param projectDbDir the location to write db files to
     * @param projectWalDir the location to write db WAL files to
     * @param benchmarkEnvironmentVariables the environment variables for the benchmark
     * @param benchmarkArgs the arguments to the benchmark
     *
     * @return the result of the benchmark
     *
     * @throws IllegalArgumentException if the {@code projectRepoDir} does not exist,
     *     or if the {@code projectLogDir} does not exist or cannot be created,
     *     or if the {@code projectDbDir} does not exist or cannot be created,
     *     or if the {@code projectWalDir} if not null, and does not exist or cannot be created,
     *
     * @throws IOException if the benchmark cannot be started
     */
    BenchmarkResult benchmark(final UUID benchmarkId, final Path projectRepoDir, final Path projectLogDir, final Path projectDbDir, @Nullable final Path projectWalDir, final Map<String, String> benchmarkEnvironmentVariables, final List<String> benchmarkArgs) throws IOException;
}
