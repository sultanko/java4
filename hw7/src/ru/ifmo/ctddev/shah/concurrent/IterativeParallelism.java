package ru.ifmo.ctddev.shah.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.concurrent.ParallelMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementation of {@link info.kgeorgiy.java.advanced.concurrent.ListIP}
 * provides methods to work with List
 *
 * @see java.util.stream.Stream
 * @see java.util.function.Function
 * @see Thread
 */
public class IterativeParallelism implements ListIP {

    private ParallelMapper mapper;

    /**
     * Returns maximum element in <code>values</code>.
     *
     * @param threads count of threads to be used
     * @param values list of values
     * @param comparator comprator for values
     * @param <T> the element type of <code>values</code>
     * @return maximum element in values
     * @throws InterruptedException when thread is interrupted
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<List<? extends T>, T> function =
                list -> list.stream().max(comparator).get();
        return getResult(threads, values, function, function);
    }


    /**
     * Returns minimum element in <code>values</code>.
     *
     * @param threads count of threads to be used
     * @param values list of values
     * @param comparator comprator for values
     * @param <T> the element type of <code>values</code>
     * @return minimum element in values
     * @throws InterruptedException when thread is interrupted
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * Check that all values matches given <code>predicate</code>
     *
     * @param threads count of threads to be used
     * @param values list of values
     * @param <T> the element type of <code>values</code>
     * @return true if all elements in <code>values</code> matches given <code>predicate</code>; otherwise false
     * @throws InterruptedException when thread is interrupted
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return getResult(threads, values,
                list -> list.stream().allMatch(predicate::test),
                list -> list.stream().allMatch(Predicate.isEqual(true))
        );
    }

    /**
     * Check that at least one value matches given <code>predicate</code>
     *
     * @param threads count of threads to be used
     * @param values list of values
     * @param <T> the element type of <code>values</code>
     * @return true if al least one element in <code>values</code> matches given <code>predicate</code>; otherwise false
     * @throws InterruptedException when thread is interrupted
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return getResult(threads, values,
            list -> list.stream().anyMatch(predicate::test),
            list -> list.stream().anyMatch(Predicate.isEqual(true))
        );
    }

    /**
     * Returns string representation of <code>values</code>
     *
     * @param threads count of using threads
     * @param values elements
     * @return string of all string repsentation of elements in <code>values</code>
     * @throws InterruptedException when thread is interrupted
     */
    @Override
    public String concat(int threads, List<?> values) throws InterruptedException {
        return getResult(threads, values,
                list -> list.stream().map(Object::toString).collect(Collectors.joining()),
                list -> String.join("", list));
    }

    /**
     * Returns a list consisting of elements matching this predicate
     *
     * @param threads count of using threads
     * @param values elements
     * @param predicate given statement on filter
     * @param <T> the element type of <code>values</code>
     * @return the new list
     * @throws InterruptedException when thread is interrupted
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return getResult(threads, values,
                list -> list.stream().filter(predicate::test).collect(Collectors.toList()),
                list -> list.stream().collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll)
        );
    }

    /**
     * Returns a list consisting of the results of applying the given function to the elements of
     * <code>values</code>.
     *
     * @param threads count of using threads
     * @param values elements
     * @param f given function to apply to each element
     * @return the new list
     * @param <T> the element type of <code>values</code>
     * @param <U> the element type of new list
     * @throws InterruptedException when thread is interrupted
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return getResult(threads, values,
                list -> list.stream().map(f).collect(Collectors.toList()),
                list -> list.stream().collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll)
        );
    }

    private  <T, R> List<R> getPartitionsResult(final int threadsCount, final List<? extends T> values, final Function<List<? extends T>, R> func) throws InterruptedException {
        final List<Thread> threads = new ArrayList<>();
        final int partSize = threadsCount <= values.size() ?
                values.size() / threadsCount : 1;
        final int resultSize = threadsCount <= values.size() ? threadsCount : values.size();
        final R[] threadResult = (R[]) new Object[resultSize];
        List<List<? extends T>> args = new ArrayList<>();
        for (int i = 0; i < resultSize; i++)
        {
            final int left = partSize * i;
            final int right = (i == threadsCount - 1) ? values.size() : (partSize * (i + 1));
            final int insertedNum = threads.size();
            args.add(values.subList(left, right));
//            threads.add(new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    threadResult[insertedNum] = func.apply(values.subList(left, right));
//                }
//            }));
//            threads.get(insertedNum).start();
        }

//        for (Thread thread : threads) {
//            thread.join();
//        }

        mapper = new ParralelMapperImpl(threadsCount);

//        return Arrays.asList(threadResult);
        return mapper.run(func, args);
    }

    private  <T, R> R getResult(int threadsCount, final List<? extends T> values, Function<List<? extends T>, R> func, Function<List<? extends R>, R> funcConcat) throws InterruptedException {
        return funcConcat.apply(getPartitionsResult(threadsCount, values, func));
    }
}
