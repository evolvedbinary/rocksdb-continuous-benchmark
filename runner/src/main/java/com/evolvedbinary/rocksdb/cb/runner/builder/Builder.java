package com.evolvedbinary.rocksdb.cb.runner.builder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Builder {

    /**
     * Build the Source Code.
     *
     * @param buildId a unique identifier for the build
     * @param projectRepoDir the location of the source code
     * @param projectLogDir the location to write build log files to
     * @param targets the make build targets
     *
     * @return the result of the build
     *
     * @throws IllegalArgumentException if the {@code projectRepoDir} does not exist,
     *     or if the {@code projectLogDir} does not exist or cannot be created,
     *     or if the targets list is empty
     *
     * @throws IOException if the build cannot be started
     */
    BuildResult build(final UUID buildId, final Path projectRepoDir, final Path projectLogDir, final List<String> targets) throws IOException;
}
