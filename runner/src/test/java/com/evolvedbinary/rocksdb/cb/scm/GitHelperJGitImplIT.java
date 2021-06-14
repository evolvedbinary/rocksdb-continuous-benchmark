package com.evolvedbinary.rocksdb.cb.scm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class GitHelperJGitImplIT {

    private static final String REPO_URI = "https://github.com/evolvedbinary/docker-rocksjava";
    private static final String BRANCH = "master";

    private static final String INVALID_REPO_URI = "https://github.com/evolvedbinary/no-such-repo";
    private static final String INVALID_BRANCH = "no-such-branch";

    @Test
    public void cloneRepo(@TempDir final Path tempDir) throws GitHelperException, IOException {
        final Path repoDir = Files.createTempDirectory(tempDir, "cloneRepoTest");
        try (final GitHelper gitHelper = GitHelperJGitImpl.clone(REPO_URI, repoDir, BRANCH)) {
            assertTrue(Files.exists(repoDir));
            try (final Stream<Path> stream = Files.list(repoDir)) {
                assertTrue(stream.count() > 1);
            }
            assertNotNull(gitHelper.getHead());
            assertEquals(BRANCH, gitHelper.getBranch());
        }
    }

    @Test
    public void cloneRepoInvalidBranch(@TempDir final Path tempDir) throws IOException {
        final Path repoDir = Files.createTempDirectory(tempDir, "cloneRepoInvalidBranchTest");
        assertThrows(GitHelperException.class, () -> {
            try (final GitHelper gitHelper = GitHelperJGitImpl.clone(REPO_URI, repoDir, INVALID_BRANCH)) {
                assertNotNull(gitHelper);
            }
        });
    }

    @Test
    public void cloneRepoNoSuchRepo(@TempDir final Path tempDir) throws IOException {
        final Path repoDir = Files.createTempDirectory(tempDir, "cloneRepoNoSuchRepoTest");
        assertThrows(GitHelperException.class, () -> {
            try (final GitHelper gitHelper = GitHelperJGitImpl.clone(INVALID_REPO_URI, repoDir, BRANCH)) {
                assertNotNull(gitHelper);
            }
        });
    }

    @Test
    public void openExistingRepo(@TempDir final Path tempDir) throws GitHelperException, IOException {
        final Path repoDir = Files.createTempDirectory(tempDir, "openExistingRepoTest");

        // clone first
        try (final GitHelper gitHelper = GitHelperJGitImpl.clone(REPO_URI, repoDir, BRANCH)) {
            assertTrue(Files.exists(repoDir));
            try (final Stream<Path> stream = Files.list(repoDir)) {
                assertTrue(stream.count() > 1);
            }
            assertNotNull(gitHelper.getHead());
            assertEquals(BRANCH, gitHelper.getBranch());
        }

        // then try and open the clone
        try (final GitHelper gitHelper = GitHelperJGitImpl.open(repoDir)) {
            assertNotNull(gitHelper);
            assertNotNull(gitHelper.getHead());
            assertEquals(BRANCH, gitHelper.getBranch());
        }
    }

    @Test
    public void openNonExistentRepo(@TempDir final Path tempDir) {
        final Path repoDir = tempDir.resolve("openNonExistentRepoTest");
        assertThrows(GitHelperException.class, () -> {
            try (final GitHelper gitHelper = GitHelperJGitImpl.open(repoDir)) {
                assertNotNull(gitHelper);
            }
        });
    }

    @Test
    public void openAndFetch(@TempDir final Path tempDir) throws GitHelperException, IOException {
        final Path repoDir = Files.createTempDirectory(tempDir, "openAndFetchTest");

        // clone first
        try (final GitHelper gitHelper = GitHelperJGitImpl.clone(REPO_URI, repoDir, BRANCH)) {
            assertTrue(Files.exists(repoDir));
            try (final Stream<Path> stream = Files.list(repoDir)) {
                assertTrue(stream.count() > 1);
            }
            assertNotNull(gitHelper.getHead());
            assertEquals(BRANCH, gitHelper.getBranch());
        }

        // then open
        try (final GitHelper gitHelper = GitHelperJGitImpl.open(repoDir)) {
            assertNotNull(gitHelper);
            assertNotNull(gitHelper.getHead());
            assertEquals(BRANCH, gitHelper.getBranch());

            // then try and fetch
            gitHelper.fetch();

            assertNotNull(gitHelper.getHead());
            assertEquals(BRANCH, gitHelper.getBranch());
        }
    }

    @Test
    public void cleanAll(@TempDir final Path tempDir) throws IOException, GitHelperException {
        final Path repoDir = Files.createTempDirectory(tempDir, "cleanAllTest");

        // clone first
        try (final GitHelper gitHelper = GitHelperJGitImpl.clone(REPO_URI, repoDir, BRANCH)) {
            assertTrue(Files.exists(repoDir));
            try (final Stream<Path> stream = Files.list(repoDir)) {
                assertTrue(stream.count() > 1);
            }
            assertNotNull(gitHelper.getHead());
            assertEquals(BRANCH, gitHelper.getBranch());

            // then call clean all
            gitHelper.cleanAll();
        }
    }

    @Test
    public void checkoutRevision(@TempDir final Path tempDir) throws IOException, GitHelperException {
        final Path repoDir = Files.createTempDirectory(tempDir, "checkoutRevisionTest");

        // clone first
        try (final GitHelper gitHelper = GitHelperJGitImpl.clone(REPO_URI, repoDir, BRANCH)) {
            assertTrue(Files.exists(repoDir));
            try (final Stream<Path> stream = Files.list(repoDir)) {
                assertTrue(stream.count() > 1);
            }
            assertNotNull(gitHelper.getHead());
            assertEquals(Arrays.asList("HEAD", "refs/heads/master", "refs/remotes/origin/master"), gitHelper.listRefs());
            assertEquals(BRANCH, gitHelper.getBranch());

            final List<String> commits = gitHelper.listCommits();
            assertTrue(commits.size() > 3);

            // then call checkout on a revision
            final String prevCommit = commits.get(2);
            gitHelper.checkout(prevCommit);
            assertEquals(prevCommit, gitHelper.getHead());
            assertEquals(prevCommit, gitHelper.getBranch());
        }
    }
}
