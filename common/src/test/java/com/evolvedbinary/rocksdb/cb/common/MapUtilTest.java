package com.evolvedbinary.rocksdb.cb.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.evolvedbinary.rocksdb.cb.common.MapUtil.Entry;
import static com.evolvedbinary.rocksdb.cb.common.MapUtil.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MapUtilTest {

    @Test
    public void entry() {
        final String key = "key1", value = "value1";
        final Entry entry = Entry(key, value);
        assertEquals(key, entry.key);
        assertEquals(value, entry.value);
    }

    @Test
    public void emptyMapFromNullEntries() {
        final Map<String, String> map = Map(null);
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @Test
    public void emptyMap() {
        final Map<String, String> map = Map();
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @Test
    public void map() {
        final String key1 = "key1", value1 = "value1";
        final String key2 = "key2", value2 = "value2";
        final String key3 = "key3", value3 = "value3";

        final Map<String, String> map = Map(Entry(key1, value1), Entry(key2, value2), Entry(key3, value3));
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals(value1, map.get(key1));
        assertEquals(value2, map.get(key2));
        assertEquals(value3, map.get(key3));
    }
}
