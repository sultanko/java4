package ru.ifmo.ctddev.shah.concurrent;

import info.kgeorgiy.java.advanced.mapper.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Class implements interface {@link info.kgeorgiy.java.advanced.mapper.ParallelMapper}.
 * Provides method to parallel mapping function.
 * @author Egor Shah
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads;
    private final TaskQueue queue;

    /**
     * Construst new ParallelMapperImpl with given count of Threads.
     * @param countThreads count of using Threads
     */
    public ParallelMapperImpl(int countThreads) {
        threads = new ArrayList<>();
        queue = new TaskQueue();
        TaskExecutor taskExecutor = new TaskExecutor(queue);
        for (int i = 0; i < countThreads; i++) {
            threads.add(new Thread(taskExecutor));
            threads.get(i).start();
        }
    }

    /**
     * Returns a list consisting of the results of applying the given
     * function to the elements in <code>args</code>. Applying of function
     * execute parallel.
     *
     * @param f  function to apply to each element
     * @param args list of arguments
     * @param <T> The type to arguments
     * @param <R> The element type of result
     * @return the new list of results
     * @throws InterruptedException when threads are interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<Task<T, R>> curTasks = queue.addAll(f, args);

        List<R> results = new ArrayList<>(curTasks.size());
        for (Task<T,R> curTask : curTasks) {
            results.add(curTask.getResult());
        }
        return results;
    }

    /**
     * Close all threads creating by this ParallelMapperImpl.
     *
     * @throws InterruptedException when threads are interrupted
     */
    @Override
    public void close() throws InterruptedException {
        threads.forEach(java.lang.Thread::interrupt);
    }
}
