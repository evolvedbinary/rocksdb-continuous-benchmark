package com.evolvedbinary.rocksdb.cb.github;

import com.evolvedbinary.rocksdb.cb.dataobject.WebHookPayloadSummary;

import java.io.IOException;

interface WebHookPayloadSummaryProcessor {
    void process(final WebHookPayloadSummary webHookPayloadSummary) throws IOException;
}
