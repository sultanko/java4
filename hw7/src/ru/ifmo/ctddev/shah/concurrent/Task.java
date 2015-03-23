package ru.ifmo.ctddev.shah.concurrent;

import java.util.function.Function;

/**
 * Created by sultan on 22.03.15.
 */
public class Task<T, R> {

    private final Function<? super T, ? extends R> function;
    private final T arg;
    private volatile R result = null;

    public Task(Function<? super T, ? extends R> function, T arg) {
        this.function = function;
        this.arg = arg;
    }

    public synchronized R getResult() {
        return result;
    }

    public synchronized void calculateResult() {
        result = function.apply(arg);
        notify();
    }

}
