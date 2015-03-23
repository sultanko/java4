package ru.ifmo.ctddev.shah.concurrent;

import java.util.*;
import java.util.function.Function;

/**
 * Created by sultan on 22.03.15.
 */
public class TaskQueue {
    private volatile Queue<Task<?, ?>> q2 = new LinkedList<>();

    public synchronized Task<?, ?> get() {
        return q2.poll();
    }

    public synchronized <T, R> List<Task<T, R>> addAll(Function<? super T, ? extends R> func, Collection<? extends T> args) {
        List<Task<T, R>> res = new ArrayList<>();
        for (T arg : args) {
            Task<T, R> t = new Task<>(func, arg);
            q2.add(t);
            res.add(t);
        }
        notifyAll();
        return res;
    }

    public synchronized void set(Task<?, ?> t)  {
        q2.add(t);
        notifyAll();
    }

    public synchronized boolean isEmpty() {
        return q2.isEmpty();
    }

}
