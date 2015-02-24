package ru.ifmo.ctddev.shah.arrayset;

import java.util.*;

/**
 * Created by sultan on 24.02.15.
 */
public class ArraySet<T> extends ArraySetEasy<T> implements NavigableSet<T> {

    public ArraySet(Collection<? extends T> array) {
        super(array);
    }

    protected ArraySet(T[] array, Comparator<? super T> comparator1) {
        super(array, comparator1);
    }

    @Override
    public T lower(T t) {
        int index = Arrays.binarySearch(array, t, comparator);
        if (index == 0 || index == -1) {
            return null;
        } else if (index > 0) {
            return array[index - 1];
        } else {
            return array[-(index + 1) - 1];
        }
    }

    @Override
    public T floor(T t) {
        int index = Arrays.binarySearch(array, t, comparator);
        if (index == -1) {
            return null;
        } else if (index >= 0) {
            return array[index];
        } else {
            return array[-(index + 1) - 1];
        }
    }

    @Override
    public T ceiling(T t) {
        int index = Arrays.binarySearch(array, t, comparator);
        if (index == -size() - 1) {
            return null;
        } else if (index >= 0) {
            return array[index];
        } else {
            return array[-(index + 1)];
        }
    }

    @Override
    public T higher(T t) {
        int index = Arrays.binarySearch(array, t, comparator);
        if (index == -size() - 1 || index == size() - 1) {
            return null;
        } else if (index >= 0) {
            return array[index + 1];
        } else {
            return array[-(index + 1)];
        }
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return null;
    }

    @Override
    public Iterator<T> descendingIterator() {
        return null;
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        int leftIndex = Arrays.binarySearch(array, fromElement, comparator);
        int rightIndex = Arrays.binarySearch(array, toElement, comparator);
        if (leftIndex > rightIndex || leftIndex < 0 || rightIndex < 0) {
            throw new IllegalArgumentException();
        }

        return new ArraySet<>(
                Arrays.copyOfRange(array,
                        leftIndex + (fromInclusive ? 1 : 0),
                        rightIndex - (toInclusive ? 1 : 0)),
                comparator);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        int index = Arrays.binarySearch(array, toElement, comparator);
        if (index < 0) {
            throw new IllegalArgumentException();
        }

        return new ArraySet<T>(Arrays.copyOfRange(array, 0, index - (inclusive ? 1 : 0)),
                comparator);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        int index = Arrays.binarySearch(array, fromElement, comparator);
        if (index < 0) {
            throw new IllegalArgumentException();
        }


        return new ArraySet<>(Arrays.copyOfRange(array, index + (inclusive ? 1 : 0), size()),
                comparator());
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }
}
