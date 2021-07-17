package com.evolvedbinary.rocksdb.cb.common;

import javax.annotation.Nullable;
import java.util.List;

public interface ListUtil {

    /**
     * Write out a String representation of the elements in a List.
     *
     * @param list the list
     *
     * @return the string representation
     */
    static <T> String asString(@Nullable final List<T> list) {
        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (final T element : list) {
            if (!first) {
                builder.append(", ");
            } else {
                first = false;
            }
            builder.append(element);
        }
        return builder.toString();
    }
}
