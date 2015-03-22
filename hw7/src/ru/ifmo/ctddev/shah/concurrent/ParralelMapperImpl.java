package ru.ifmo.ctddev.shah.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ParallelMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Created by sultan on 19.03.15.
 */
public class ParralelMapperImpl implements ParallelMapper{
    private List<Thread> threads;
    private TaskQueue queue;

    public ParralelMapperImpl(int countThreads) {
        threads = new ArrayList<>();
        for (int i = 0; i < countThreads; i++) {
            threads.add(new Thread());
        }
    }

    @Override
    public <T, R> List<R> run(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        for (int i = 0; i < args.size(); i++) {
            queue.set(new Task<T, R>(f, args.get(i)));
        }
        R[] res = (R[]) new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            Task<T, R> t = (Task<T, R>) queue.get();
            res[i] = t.getFunction().apply(t.getArg());
        }
        return Arrays.asList(res);
    }

    @Override
    public void close() throws InterruptedException {
        for (Thread thread : threads) {
            thread.interrupt();
        }
    }
}
