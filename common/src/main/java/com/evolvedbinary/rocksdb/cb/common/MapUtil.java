package com.evolvedbinary.rocksdb.cb.common;

import java.util.HashMap;
import java.util.Map;
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
    static <K, V> Map<K, V> Map(final Entry... entries) {
        return Map(HashMap::new, entries);
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
    static <K, V> Map<K, V> Map(final Supplier<Map<K, V>> mapConstructor, final Entry... entries) {
        final Map<K, V> map = mapConstructor.get();
        if (entries != null) {
            for (final Entry<K, V> entry : entries) {
                map.put(entry.key, entry.value);
            }
        }
        return map;
    }
}
