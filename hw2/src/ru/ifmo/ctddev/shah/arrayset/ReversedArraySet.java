package ru.ifmo.ctddev.shah.arrayset;

import java.util.*;

/**
 * Created by sultan on 25.02.15.
 */
public class ReversedArraySet<T> extends ArraySet<T> {

    protected ReversedArraySet(List<T> array, Comparator<? super T> comparator1) {
        super(array, comparator1);
    }

    @Override
    public T lower(T t) {
        return super.higher(t);
    }

    @Override
    public T floor(T t) {
        return super.ceiling(t);
    }

    @Override
    public T ceiling(T t) {
        return super.floor(t);
    }

    @Override
    public T higher(T t) {
        return super.lower(t);
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<>(array, comparator);
    }

    @Override
    public Comparator<? super T> comparator() {
        return Collections.reverseOrder(comparator);
    }

    @Override
    public Iterator<T> descendingIterator() {
        return super.iterator();
    }

    @Override
    public Iterator<T> iterator() {
        return super.descendingIterator();
    }

    @Override
    protected NavigableSet<T> getSetFromSubList(int left, int right) {
        return new ReversedArraySet<>(
                left >= 0 && left <=size() && right >= 0 && right <= size() ?
                        array.subList(left, right) : null,
                comparator
        );
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        return super.subSet(toElement, toInclusive, fromElement, fromInclusive);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        return super.tailSet(toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        return super.headSet(fromElement, inclusive);
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
        return super.last();
    }

    @Override
    public T last() {
        return super.first();
    }
}
