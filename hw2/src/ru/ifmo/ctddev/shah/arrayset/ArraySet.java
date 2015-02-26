package ru.ifmo.ctddev.shah.arrayset;

import java.util.*;

/**
 * Created by sultan on 24.02.15.
 */
public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {

    protected final List<T> array;
    protected final Comparator<? super T> comparator;


    private List<T> initialize(Collection<? extends T> collection) {
        List<T> result = new ArrayList<T>(collection);

        Collections.sort(result, comparator);
        Iterator<T> iterator = result.iterator();
        T last = null;
        while (iterator.hasNext()) {
            if (last != null) {
                T now = iterator.next();
                if (comparator != null) {
                    if (comparator.compare(last, now) == 0) {
                        iterator.remove();
                    }
                } else if (((Comparable<? super T>)(last)).compareTo(now) == 0) {
                    iterator.remove();
                }
            } else {
                last = iterator.next();
            }
        }
        return result;
    }

    public ArraySet() {
        this.comparator = null;
        this.array = new ArrayList<>();
    }

    public ArraySet(Collection<? extends T> array) {
        this.comparator = null;
        this.array = initialize(array);
    }

    public ArraySet(Collection<? extends T> array, Comparator<? super T> comparator1) {
        this.comparator = comparator1;
        this.array = initialize(array);
    }

    protected ArraySet(List<T> arrayList, Comparator<? super T> comparator1) {
        this.comparator = comparator1;
        if (arrayList != null) {
            this.array = arrayList;
        } else {
            this.array = new ArrayList<>();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o != null) {
            int index = Collections.binarySearch(array, (T)o, comparator);
            if (index >= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return array.size();
    }


    @Override
    public Iterator<T> iterator() {
        return new ArraySetIterator<>(array);
    }

    private T getLess(T t, boolean inclusive) {
        int index = Collections.binarySearch(array, t, comparator);
        if ((index == 0 && !inclusive) || index == -1) {
            return null;
        } else if (index >= 0) {
            return array.get(index - (inclusive ? 0 : 1));
        } else {
            return array.get(-(index + 1) - 1);
        }
    }

    @Override
    public T lower(T t) {
        return getLess(t, false);
    }

    @Override
    public T floor(T t) {
        return getLess(t, true);
    }

    private T getGreater(T t, boolean inclusive) {
        int index = Collections.binarySearch(array, t, comparator);
        if (index == -size() - 1 || (index == size() - 1 && !inclusive)) {
            return null;
        } else if (index >= 0) {
            return array.get(index + (inclusive ? 0 : 1));
        } else {
            return array.get(-(index + 1));
        }
    }

    @Override
    public T ceiling(T t) {
        return getGreater(t, true);
    }

    @Override
    public T higher(T t) {
        return getGreater(t, false);
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
        return new ReversedArraySet<>(array, comparator);
    }


    @Override
    public Iterator<T> descendingIterator() {
        return new ReverseArraySetIterator<>(array);
    }

    protected NavigableSet<T> getSetFromSubList(int left, int right) {
        return new ArraySet<>(
                left >= 0 && left <=size() && right >= 0 && right <= size() ?
                        array.subList(left, right) : null,
                comparator
        );
    }
    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        int index = Collections.binarySearch(array, toElement, comparator);
        if (index < 0) {
            index = -(index + 1);
        } else {
            index = index + (inclusive ? 1 : 0);
        }

        return getSetFromSubList(0, index);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        int index = Collections.binarySearch(array, fromElement, comparator);

        if (index < 0) {
            index = -(index + 1);
        } else {
            index = index + (inclusive ? 0 : 1);
        }
        return getSetFromSubList(index, size());
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
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

    @Override
    public T first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return array.get(0);
    }

    @Override
    public T last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return array.get(size() - 1);
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    protected class ArraySetIterator<E> implements Iterator<E> {

        Iterator<E> iterator;

        public ArraySetIterator(List<E> array) {
            iterator = array.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public E next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    protected class ReverseArraySetIterator<E> implements Iterator<E> {

        private ListIterator<E> iter;

        public ReverseArraySetIterator(List<E> array) {
            iter = array.listIterator(array.size() - 1);
        }

        @Override
        public boolean hasNext() {
            return iter.hasPrevious();
        }

        @Override
        public E next() {
            return iter.previous();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
