package com.evolvedbinary.rocksdb.cb.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathUtilTest {

    @Test
    public void deleteFile(@TempDir final Path scratch) throws IOException {
        final Path tempFile = Files.createTempFile(scratch, "deleteFileTest", ".tmp");
        assertTrue(Files.exists(tempFile));

        PathUtil.delete(tempFile);
        assertFalse(Files.exists(tempFile));
    }

    @Test
    public void deleteNonExistentFile(@TempDir final Path scratch) throws IOException {
        final Path tempFile = scratch.resolve("no-such-file");
        assertFalse(Files.exists(tempFile));

        PathUtil.delete(tempFile);
        assertFalse(Files.exists(tempFile));
    }

    @Test
    public void deleteDir(@TempDir final Path scratch) throws IOException {
        final Path tempDir = Files.createTempDirectory(scratch, "deleteDirTest");
        assertTrue(Files.exists(tempDir));

        PathUtil.delete(tempDir);
        assertFalse(Files.exists(tempDir));
    }

    @Test
    public void deleteDirRecursive(@TempDir final Path scratch) throws IOException {
        final Path tempDir = Files.createTempDirectory(scratch, "deleteDirTest");
        assertTrue(Files.exists(tempDir));

        final Path subFile = Files.createTempFile(tempDir, "subFile", ".tmp");
        assertTrue(Files.exists(subFile));
        final Path subDir = Files.createTempDirectory(tempDir, "subDir");
        assertTrue(Files.exists(subDir));
        final Path subSubFile = Files.createTempFile(subDir, "subSubFile", ".tmp");
        assertTrue(Files.exists(subSubFile));

        PathUtil.delete(tempDir);
        assertFalse(Files.exists(tempDir));
        assertFalse(Files.exists(subFile));
        assertFalse(Files.exists(subDir));
        assertFalse(Files.exists(subSubFile));
    }
}
