package com.evolvedbinary.rocksdb.cb.dataobject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * A simple object holding data
 * which can be serialized/deserialized to/from
 * a data stream.
 */
public interface DataObject {

    /**
     * Serialize the Data Object to an OutputStream.
     *
     * @param os the output stream
     *
     * @throws IOException if an error occurs during serialization
     */
    void serialize(final OutputStream os) throws IOException;

    /**
     * Serialize the Data Object to a String.
     *
     * @return the string representation
     *
     * @throws IOException if an error occurs during serialization
     */
    String serialize() throws IOException;

    /**
     * Deserialize the InputStream into the DataObject.
     *
     * @param is the input stream
     * @param <T> the type of the data object that is deserialized
     *
     * @return this
     *
     * @throws IOException if an error occurs during deserialization
     */
    <T extends DataObject> T deserialize(final InputStream is) throws IOException;

    /**
     * Deserialize the String into the DataObject.
     *
     * @param data the input data
     * @param <T> the type of the data object that is deserialized
     *
     * @return this
     *
     * @throws IOException if an error occurs during deserialization
     */
    <T extends DataObject> T deserialize(final String data) throws IOException;
}
