package com.evolvedbinary.rocksdb.cb.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListUtilTest {

    @Test
    public void asString() {
        final String elem1 = "elem1";
        final String elem2 = "elem2";
        final String elem3 = "elem3";

        final List<String> list = Arrays.asList(elem1, elem2, elem3);
        assertEquals("elem1, elem2, elem3", ListUtil.asString(list));
    }
}
