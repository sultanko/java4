package ru.ifmo.ctddev.shah.concurrent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by sultan on 22.03.15.
 */
public class TaskQueue {
    private Queue<Task<?, ?>> q2 = new LinkedList<>();

    public synchronized Task<?, ?> get() {
        return q2.poll();
    }

    public synchronized void set(Task<?, ?> t) {
        q2.add(t);
    }

    public synchronized boolean isEmpty() {
        return q2.isEmpty();
    }

}
