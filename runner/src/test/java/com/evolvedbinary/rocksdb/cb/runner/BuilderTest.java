package com.evolvedbinary.rocksdb.cb.runner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class BuilderTest {

    @Test
    public void nullRepoDir(@TempDir final Path tempDir) {
        assertThrows(NullPointerException.class, ()  ->
                new Builder().build(null, tempDir, Arrays.asList("target1"))
        );
    }

    @Test
    public void invalidRepoDir(@TempDir final Path tempDir) {
        final Path repoDir = Paths.get("/no/such/path");
        assertThrows(IllegalArgumentException.class, ()  ->
                new Builder().build(repoDir, tempDir, Arrays.asList("target1"))
        );
    }

    @Test
    public void nullLogDir(@TempDir final Path tempDir) {
        assertThrows(NullPointerException.class, ()  ->
                new Builder().build(tempDir, null, Arrays.asList("target1"))
        );
    }

    @Test
    public void invalidLogDir(@TempDir final Path tempDir) {
        final Path logDir = Paths.get("/no/such/path");
        assertThrows(IllegalArgumentException.class, ()  ->
                new Builder().build(tempDir, logDir, Arrays.asList("target1"))
        );
    }
}
