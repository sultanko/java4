package ru.ifmo.ctddev.shah.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Created by sultan on 19.03.15.
 */
public class ParralelMapperImpl implements ParallelMapper{
    private final List<Thread> threads;
    private final TaskQueue queue;

    public ParralelMapperImpl(int countThreads) {
        threads = new ArrayList<>();
        queue = new TaskQueue();
        for (int i = 0; i < countThreads; i++) {
            threads.add(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!Thread.interrupted()) {
                            Task t;
                            synchronized (queue) {
                                while (queue.isEmpty()) {
                                    queue.wait();
                                }
                                t = queue.get();
                            }
                            t.calculateResult();
                        }
                    } catch (InterruptedException ignored) {
                    }
                    Thread.currentThread().interrupt();
                }
            }));
        }
        for (Thread thread : threads) {
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, R> List<R> run(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<Task<T, R>> res = queue.addAll(f, args);
        R[] ans = (R[]) new Object[args.size()];
        for (int i = 0; i < args.size(); i++) {
            if (res.get(i).getResult() == null) {
                synchronized (res.get(i)) {
                    while (res.get(i).getResult() == null) {
                        res.get(i).wait();
                    }
                }
            }
            ans[i] = res.get(i).getResult();
        }
        return Arrays.asList(ans);
    }

    @Override
    public void close() throws InterruptedException {
        threads.forEach(java.lang.Thread::interrupt);
    }
}
