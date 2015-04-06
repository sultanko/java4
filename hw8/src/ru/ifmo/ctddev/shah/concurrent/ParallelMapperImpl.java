package ru.ifmo.ctddev.shah.concurrent;

import info.kgeorgiy.java.advanced.mapper.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
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
        queue = new TaskQueue(2 * countThreads);
        TaskExecutor taskExecutor = new TaskExecutor(queue);
        for (int i = 0; i < countThreads; i++) {
            threads.add(new Thread(taskExecutor));
            threads.get(i).start();
        }
//        service = Executors.newFixedThreadPool(countThreads);
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
        List<Task<T, R>> curTasks = new ArrayList<>();
        for (T arg : args) {
           curTasks.add(queue.push(f, arg));
        }
//        List<Future<R>> curTasks = new ArrayList<>();
//        for (T arg : args) {
//            curTasks.add(service.submit(new Callable<R>() {
//                @Override
//                public R call() throws Exception {
//                    return f.apply(arg);
//                }
//            }));
//        }

        List<R> results = new ArrayList<>(curTasks.size());
        for (Task<T,R> curTask : curTasks) {
            results.add(curTask.getResult());
        }
//        for (Future<R> res : curTasks) {
//            try {
//                results.add(res.get());
//            } catch (ExecutionException e) {
//            }
//        }
        return results;
    }

    public <T, R> void addTask(Function<? super T, ? extends R> f, T arg) throws  InterruptedException {
        queue.push(f, arg);
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
