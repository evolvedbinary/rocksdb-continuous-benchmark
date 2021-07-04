package com.evolvedbinary.rocksdb.cb.scm;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GitHelperJGitImpl implements GitHelper {

    private final Git git;

    private GitHelperJGitImpl(final Git git) {
        this.git = git;
    }

    public static GitHelper clone(final String uri, final Path repoDir, final String checkoutBranch) throws GitHelperException {
        try {
            Files.createDirectories(repoDir);
        } catch (final IOException e) {
            throw new GitHelperException("Unable to create directory for repository: " + repoDir.toAbsolutePath(), e);
        }

        final CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(uri)
                .setDirectory(repoDir.toFile())
                .setRemote(Constants.DEFAULT_REMOTE_NAME)
                .setCloneAllBranches(false)  //TODO(AR) do we need this to be true?
                .setBranch(checkoutBranch);

        final Git git;
        try {
            git = cloneCommand.call();
        } catch (final GitAPIException e) {
            throw new GitHelperException("Unable to clone: " + uri + ". " + e.getMessage(), e);
        }

        return new GitHelperJGitImpl(git);
    }

    public static GitHelper open(final Path repoDir) throws GitHelperException {
        final Git git;
        try {
            git = Git.open(repoDir.toFile());
        } catch (final IOException e) {
            throw new GitHelperException("Unable to open repo: " + repoDir.toAbsolutePath() + ". " + e.getMessage(), e);
        }
        return new GitHelperJGitImpl(git);
    }

    @Override
    public String getBranch() throws GitHelperException {
        try {
            final Repository repository = git.getRepository();
            return repository.getBranch();
        } catch (final IOException e) {
            throw new GitHelperException("Unable to get branch", e);
        }
    }

    @Override
    public String getHead() throws GitHelperException {
        try {
            final Repository repository = git.getRepository();
            final ObjectId lastCommitId = repository.resolve(Constants.HEAD);
            return lastCommitId.getName();
        } catch (final IOException e) {
            throw new GitHelperException("Unable to get HEAD", e);
        }
    }

    @Override
    public List<String> listRefs() throws GitHelperException {
        RefDatabase refDatabase = null;
        try {
            final Repository repository = git.getRepository();
            refDatabase = repository.getRefDatabase();
            final List<Ref> refs = refDatabase.getRefs();
            final List<String> strRefs = new ArrayList<>(refs.size());
            for (final Ref ref : refs) {
                strRefs.add(ref.getName());
            }
            return strRefs;
        } catch (final IOException e) {
            throw new GitHelperException("Unable to list refs", e);
        } finally {
            if (refDatabase != null) {
                refDatabase.close();
            }
        }
    }

    @Override
    public List<String> listCommits() throws GitHelperException {

        try {
            final LogCommand logCommand = git.log().all();
            final Iterable<RevCommit> itLog = logCommand.call();

            final List<String> commits = new ArrayList<>();
            for (final RevCommit revCommit : itLog) {
                commits.add(revCommit.getName());
            }

            return commits;
        } catch (final GitAPIException | IOException e) {
            throw new GitHelperException("Unable to list commits. " + e.getMessage(), e);
        }
    }

    @Override
    public GitHelper fetch() throws GitHelperException {
        final FetchCommand fetchCommand = git.fetch()
                .setRemote(Constants.DEFAULT_REMOTE_NAME)
                .setForceUpdate(true);
        try {
            fetchCommand.call();
        } catch (final GitAPIException e) {
            throw new GitHelperException("Unable to fetch. " + e.getMessage(), e);
        }

        return this;
    }

    @Override
    public GitHelper cleanAll() throws GitHelperException {
        final CleanCommand cleanCommand = git.clean()
                .setForce(true)
                .setCleanDirectories(true)
                .setIgnore(true);
        try {
            cleanCommand.call();
        } catch (final GitAPIException e) {
            throw new GitHelperException("Unable to clean. " + e.getMessage(), e);
        }

        return this;
    }

    @Override
    public GitHelper checkout(final String nameOrCommit) throws GitHelperException {
        final CheckoutCommand checkoutCommand = git.checkout().setName(nameOrCommit);

        try {
            checkoutCommand.call();
        } catch (final GitAPIException e) {
            throw new GitHelperException("Unable to checkout: "  + nameOrCommit + ". " + e.getMessage(), e);
        }

        return this;
    }

    @Override
    public void close() {
        git.close();

        // TODO(AR) should we ever call this?
//        git.shutdown();
    }
}
