package ru.ifmo.ctddev.shah.udp;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 28.04.15.
 *
 * @author sultan
 */
public class ThreadId {
    private static final AtomicInteger nextId = new AtomicInteger(0);

    private static final ThreadLocal<Integer> threadId =
            new ThreadLocal<Integer>() {
                @Override protected Integer initialValue() {
                    return nextId.getAndIncrement();
                }
            };

    public static int get() {
        return threadId.get();
    }
}
