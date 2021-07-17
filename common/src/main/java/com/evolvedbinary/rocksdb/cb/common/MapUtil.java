package com.evolvedbinary.rocksdb.cb.common;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Simple static utility methods to simplify
 * constructing and adding items to maps.
 */
public interface MapUtil {

    /**
     * An Entry in a Map.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    class Entry<K, V> {
        final K key;
        final V value;

        public Entry(final K key, final V value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Construct a Map Entry.
     *
     * @param <K> the key type
     * @param <V> the value type
     *
     * @param key the key
     * @param key the value
     *
     * @return the map entry
     */
    static <K, V> Entry<K, V> Entry(final K key, final V value) {
        return new Entry<>(key, value);
    }

    /**
     * Construct a Hash Map.
     *
     * @param <K> the key type
     * @param <V> the value type
     *
     * @param entries the entries for the map
     *
     * @return the map
     */
    static <K, V> Map<K, V> Map(@Nullable final Entry... entries) {
        return Map(len -> new HashMap(len), entries);
    }

    /**
     * Construct a Map.
     *
     * @param <K> the key type
     * @param <V> the value type
     *
     * @param mapConstructor the type of map constructor
     * @param entries the entries for the map
     *
     * @return the map
     */
    static <K, V> Map<K, V> Map(final Function<Integer, Map<K, V>> mapConstructor, @Nullable final Entry... entries) {
        final Map<K, V> map = mapConstructor.apply(entries != null ? entries.length : 0);
        if (entries != null) {
            for (final Entry<K, V> entry : entries) {
                map.put(entry.key, entry.value);
            }
        }
        return map;
    }

    /**
     * Write out a String representation of the entries in a Map.
     *
     * @param map the map
     *
     * @return the string representation
     */
    static <K, V> String asString(@Nullable final Map<K, V> map) {
        final StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (!first) {
                builder.append(", ");
            } else {
                first = false;
            }
            builder.append(entry.getKey());
            builder.append(" -> ");
            builder.append(entry.getValue());
        }
        return builder.toString();
    }
}
