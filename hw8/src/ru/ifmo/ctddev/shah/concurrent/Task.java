package ru.ifmo.ctddev.shah.concurrent;

import java.util.function.Function;

/**
 * Task using by {@link ru.ifmo.ctddev.shah.concurrent.TaskExecutor}.
 * Save function and arg.
 * Calculate result and save it.
 * @author Egor Shah
 */
public class Task<T, R> {

    private final Function<? super T, ? extends R> function;
    private final T arg;
    private volatile R result = null;
    private boolean calculated = false;

    public Task(Function<? super T, ? extends R> function, T arg) {
        this.function = function;
        this.arg = arg;
    }

    /**
     * Return the result of calculating. Wait if necessary for getting result.
     *
     * @return result of applying function on argument.
     * @throws InterruptedException when thread is interrupted
     */
    public synchronized R getResult() throws InterruptedException {
        while (!calculated) {
            wait();
        }
        return result;
    }

    /**
     * Apply <code>function</code> to <code>arg</code> and save result.
     * Notify other thread about calculating.
     */
    public synchronized void calculateResult() {
        result = function.apply(arg);
        calculated = true;
        notify();
    }

}
