package com.evolvedbinary.rocksdb.cb.common;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class PathUtil {

    private static class DeleteDirVisitor extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.deleteIfExists(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            if (exc != null) {
                throw exc;
            }

            Files.deleteIfExists(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    private final static SimpleFileVisitor<Path> DELETE_DIR_VISITOR = new DeleteDirVisitor();

    /**
     * Deletes a path from the filesystem
     *
     * If the path is a directory its contents
     * will be recursively deleted before it itself
     * is deleted.
     *
     * Note that removal of a directory is not an atomic-operation
     * and so if an error occurs during removal, some of the directories
     * descendants may have already been removed
     *
     * @param path the path to delete.
     *
     * @throws IOException if an error occurs whilst removing a file or directory
     */
    public static void delete(final Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            Files.deleteIfExists(path);
        } else {
            Files.walkFileTree(path, DELETE_DIR_VISITOR);
        }
    }
}
