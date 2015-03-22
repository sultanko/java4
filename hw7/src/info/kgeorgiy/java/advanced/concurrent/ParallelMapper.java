package info.kgeorgiy.java.advanced.concurrent;

import java.util.List;
import java.util.function.Function;

/**
 * Created by sultan on 19.03.15.
 */
public interface ParallelMapper extends AutoCloseable {
    <T, R> List<R> run(
            Function<? super T, ? extends R> f,
            List<? extends T> args
    ) throws InterruptedException;

    @Override
    void close() throws InterruptedException;
}
