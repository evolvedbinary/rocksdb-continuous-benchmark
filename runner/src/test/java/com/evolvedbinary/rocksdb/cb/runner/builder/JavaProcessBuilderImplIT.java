package com.evolvedbinary.rocksdb.cb.runner.builder;

import com.evolvedbinary.rocksdb.cb.process.ProcessHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class JavaProcessBuilderImplIT {

    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win");

    @Test
    public void arguments(@TempDir final Path tempDir) throws IOException {
        assumeFalse(IS_WINDOWS);

        final Path projectRepoDir = Files.createDirectories(tempDir.resolve("repo"));
        final Path projectLogDir = Files.createDirectories(tempDir.resolve("log"));

        final Builder builder = new JavaProcessBuilderImpl(Collections.emptyMap(), "echo", Arrays.asList("'hello'"));
        final BuildResult buildResult = builder.build(UUID.randomUUID(), projectRepoDir, projectLogDir, Arrays.asList("target1"));

        assertNotNull(buildResult);
        assertTrue(buildResult.ok);
        assertEquals(ProcessHelper.NORMAL_EXIT_CODE, buildResult.exitCode);
        assertTrue(buildResult.duration > 0);

        assertTrue(Files.exists(buildResult.stdOutputLogFile));
        assertTrue(Files.exists(buildResult.stdErrorLogFile));

        final String outputLog = new String(Files.readAllBytes(buildResult.stdOutputLogFile), UTF_8);
        final String errorLog = new String(Files.readAllBytes(buildResult.stdErrorLogFile), UTF_8);

        assertNotNull(outputLog);
        assertEquals("'hello' target1\n", outputLog);
        assertNotNull(errorLog);
        assertEquals(0, errorLog.length());
    }
}
