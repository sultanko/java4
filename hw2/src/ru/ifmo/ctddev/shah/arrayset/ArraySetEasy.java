package ru.ifmo.ctddev.shah.arrayset;

import com.sun.org.apache.xml.internal.security.algorithms.JCEMapper;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by sultan on 24.02.15.
 */
public abstract class ArraySetEasy<T> implements SortedSet<T> {

    T[] array;
    int size;
    Comparator<? super T> comparator;

    public ArraySetEasy(Collection<? extends T> array) {
        this.array = (T[]) array.toArray();
        this.comparator = (Comparator<T>) Comparator.naturalOrder();
        this.size = this.array.length;
        Arrays.sort(this.array, this.comparator);
    }

    public ArraySetEasy(Collection<? extends T> array, Comparator<? super T> comparator) {
        this.array = (T[]) array.toArray();
        this.comparator = comparator;
        this.size = this.array.length;
        Arrays.sort(this.array, this.comparator);
    }

    protected ArraySetEasy(T[] array, Comparator<? super T> comparator) {
        if (array == null) {
            this.array = (T[]) new Object[0];
        } else {
            this.array = array;
        }
        this.comparator =  comparator;
    }

    @Override
    public Comparator<? super T> comparator() {
        return this.comparator;
    }

    @Override
    public T first() {
        return array[0];
    }

    @Override
    public T last() {
        return array[size() - 1];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        if (o != null) {
            int index = Arrays.binarySearch(array, (T)o, comparator);
            if (index >= 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return array;
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return null;
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        Iterator<?> iterator = c.iterator();
        boolean result = true;
        while (result && iterator.hasNext()) {
            result = result && contains(iterator.next());
        }
        return result;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
