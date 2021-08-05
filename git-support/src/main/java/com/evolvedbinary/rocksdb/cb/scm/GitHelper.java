package com.evolvedbinary.rocksdb.cb.scm;

import org.eclipse.jgit.annotations.Nullable;

import java.util.List;

public interface GitHelper extends AutoCloseable {

    String getBranch() throws GitHelperException;

    String getHead() throws GitHelperException;

    List<String> listRefs() throws GitHelperException;

    List<String> listCommits() throws GitHelperException;

    GitHelper fetch() throws GitHelperException;

    GitHelper cleanAll() throws GitHelperException;

    GitHelper checkout(final String nameOrCommit) throws GitHelperException;

    GitHelper reset(final boolean hard, @Nullable final String remote, final String nameOrCommit) throws GitHelperException;

    GitHelper add(final String path) throws GitHelperException;

    GitHelper commit(final String message) throws GitHelperException;

    GitHelper push(@Nullable final String username, @Nullable final String password) throws GitHelperException;

    @Override
    void close();
}
