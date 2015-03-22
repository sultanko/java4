package ru.ifmo.ctddev.shah.concurrent;

import java.util.function.Function;

/**
 * Created by sultan on 22.03.15.
 */
public class Task<T, R> {

    private Function<? super T, ? extends R> function;
    private T arg;

    public Task(Function<? super T, ? extends R> function, T arg) {
        this.function = function;
        this.arg = arg;
    }

    public void setFunction(Function<? super T, ? extends R> function) {
        this.function = function;
    }

    public T getArg() {
        return arg;
    }

    public void setArg(T arg) {
        this.arg = arg;
    }

    public Function<? super T, ? extends R> getFunction() {

        return function;
    }
}
