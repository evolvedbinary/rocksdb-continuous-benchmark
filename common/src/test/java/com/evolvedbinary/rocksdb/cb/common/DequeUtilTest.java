package com.evolvedbinary.rocksdb.cb.common;

import org.junit.jupiter.api.Test;

import java.util.Deque;

import static com.evolvedbinary.rocksdb.cb.common.DequeUtil.Deque;
import static org.junit.jupiter.api.Assertions.*;

public class DequeUtilTest {

    @Test
    public void emptyDequeFromNullEntries() {
        final Deque<String> deque = Deque(null);
        assertNotNull(deque);
        assertEquals(0, deque.size());
    }

    @Test
    public void emptyDeque() {
        final Deque<String> deque = Deque();
        assertNotNull(deque);
        assertEquals(0, deque.size());
    }

    @Test
    public void map() {
        final String elem1 = "elem1";
        final String elem2 = "elem2";
        final String elem3 = "elem3";

        final Deque<String> deque = Deque(elem1, elem2, elem3);
        assertNotNull(deque);
        assertEquals(3, deque.size());
        assertEquals(elem1, deque.pop());
        assertEquals(elem2, deque.pop());
        assertEquals(elem3, deque.pop());
        assertNull(deque.poll());
    }
}
