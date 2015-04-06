package ru.ifmo.ctddev.shah.concurrent;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;

/**
 * A {@link java.util.Queue} that additionaly supports operations
 * that wait if necessary for the queue to add element.
 * @author Egor Shah
 */
public class TaskQueue {
    private final Queue<Task<?, ?>> queue = new LinkedList<>();

    /**
     * Retrieves and removes the head of this queue.
     *
     * @return the head of this queue.
     */
    public synchronized Task<?, ?> poll() {
        return queue.poll();
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
            queue.add(t);
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
     */
    public synchronized <T, R> Task<T, R> push(final Function<? super T, ? extends  R> func, T arg) {
        Task<T, R> task = new Task<>(func, arg);
        queue.add(task);
        notify();
        return task;
    }

    /**
     * Return true if collection contains no element.
     * @return true if collection contains no element.
     */
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

}
