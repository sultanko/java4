package ru.ifmo.ctddev.shah.concurrent;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * A {@link java.util.Queue} that additionaly supports operations
 * that wait if necessary for the queue to add element.
 * @author Egor Shah
 */
public class TaskQueue {
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final int MAX_SIZE;
    private final List<Task<?, ?>> queue;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public TaskQueue(int MAX_SIZE) {
        this.MAX_SIZE = MAX_SIZE;
        queue = new ArrayList<>(MAX_SIZE);
        for (int i = 0; i < MAX_SIZE; i++) {
            queue.add(null);
        }
    }

    /**
     * Retrieves and removes the head of this queue.
     *
     * @return the head of this queue.
     * @throws InterruptedException  when thread is interrupted
     */
    public Task<?, ?> poll() throws InterruptedException {
        lock.lock();
        try {
            while (isEmpty()) {
                notEmpty.await();
            }
            Task<?, ?> res = queue.get(queueSize.decrementAndGet());
            notFull.signal();
            return res;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Push all arguments of function to queue.
     *
     * @param func function
     * @param args arguments of function
     * @param <T> type of arguments
     * @param <R> type of result
     * @return new Tasks added to TaskQueue
     */
    public synchronized <T, R> List<Task<T, R>> addAll(final Function<? super T, ? extends R> func, final Collection<? extends T> args) {
        List<Task<T, R>> tasks = new ArrayList<>(args.size());
        for (T arg : args) {
            Task<T, R> t = new Task<>(func, arg);
            tasks.add(t);
        }
        notifyAll();
        return tasks;
    }

    /**
     * Returns a Task from queue where will be result.
     *
     * @param func function
     * @param arg argument of function
     * @param <T> type of argument
     * @param <R> type of result
     * @return new Task added to TaskQueue
     * @throws InterruptedException  when thread is interrupted
     */
    public <T, R> Task<T, R> push(final Function<? super T, ? extends  R> func, T arg) throws InterruptedException {
        lock.lock();
        try {
            while (queueSize.get() == MAX_SIZE) {
                notFull.await();
            }
            Task<T, R> task = new Task<>(func, arg);
            queue.set(queueSize.getAndIncrement(), task);
            notEmpty.signal();
            return task;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return true if collection contains no element.
     * @return true if collection contains no element.
     */
    public boolean isEmpty() {
        return queueSize.get() == 0;
    }

}
