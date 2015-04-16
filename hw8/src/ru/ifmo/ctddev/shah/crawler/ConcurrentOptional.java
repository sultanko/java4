package ru.ifmo.ctddev.shah.crawler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 16.04.15.
 *
 * @author sultan
 */
class ConcurrentOptional<T> {
    private final AtomicInteger countExceptions;
    private volatile T exceptionThread;

    ConcurrentOptional() {
        this.countExceptions = new AtomicInteger();
        exceptionThread = null;
    }

    public synchronized void set(T obj) {
        countExceptions.incrementAndGet();
        exceptionThread = obj;
    }

    public boolean isPresent() {
        return countExceptions.get() != 0;
    }

    public T get() {
        return exceptionThread;
    }
}
