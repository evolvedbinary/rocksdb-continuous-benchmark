package com.evolvedbinary.rocksdb.cb.common;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

public interface DequeUtil {

    /**
     * Construct an Array Deque.
     *
     * @param <T> the element type
     *
     * @param elements the entries for the deque,
     *     element[0] is considered the head of the deque
     *
     * @return the deque
     */
    static <T> Deque<T> Deque(final T... elements) {
        return Deque(len -> new ArrayDeque<>(len), elements);
    }

    /**
     * Construct a Deque.
     *
     * @param <T> the element type
     *
     * @param dequeConstructor the type of deque constructor
     * @param elements the entries for the deque,
     *     element[0] is considered the head of the deque
     *
     * @return the deque
     */
    static <T> Deque<T> Deque(final Function<Integer, Deque<T>> dequeConstructor, @Nullable final T... elements) {
        final Deque<T> deque = dequeConstructor.apply(elements != null ? elements.length : 0);
        if (elements != null) {
            for (final T element : elements) {
                deque.addLast(element);
            }
        }
        return deque;
    }
}
