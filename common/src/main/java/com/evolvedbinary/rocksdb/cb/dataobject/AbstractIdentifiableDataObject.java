package com.evolvedbinary.rocksdb.cb.dataobject;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

public abstract class AbstractIdentifiableDataObject extends AbstractDataObject {

    protected UUID id;
    protected ZonedDateTime timeStamp;

    protected AbstractIdentifiableDataObject() {
        this(UUID.randomUUID(), ZonedDateTime.now(ZoneId.of("UTC")));
    }

    protected AbstractIdentifiableDataObject(final UUID id, final ZonedDateTime timeStamp) {
        this.id = id;
        this.timeStamp = timeStamp;
    }

    public UUID getId() {
        return id;
    }

    public ZonedDateTime getTimeStamp() {
        return timeStamp;
    }
}
