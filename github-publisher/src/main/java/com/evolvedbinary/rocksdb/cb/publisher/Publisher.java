package com.evolvedbinary.rocksdb.cb.publisher;

public class Publisher {

    private final Settings settings;

    public Publisher(final Settings settings) {
        this.settings = settings;
    }

    static class Settings {
        final String outputQueueName;
        final String repo;
        final String repoBranch;

        public Settings(final String outputQueueName, final String repo, final String repoBranch) {
            this.outputQueueName = outputQueueName;
            this.repo = repo;
            this.repoBranch = repoBranch;
        }
    }
}
