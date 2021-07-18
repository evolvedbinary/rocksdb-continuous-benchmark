package com.evolvedbinary.rocksdb.cb.scm;

import java.util.List;

public interface GitHelper extends AutoCloseable {

    String getBranch() throws GitHelperException;

    String getHead() throws GitHelperException;

    List<String> listRefs() throws GitHelperException;

    List<String> listCommits() throws GitHelperException;

    GitHelper fetch() throws GitHelperException;

    GitHelper cleanAll() throws GitHelperException;

    GitHelper checkout(final String nameOrCommit) throws GitHelperException;

    @Override
    void close();
}
