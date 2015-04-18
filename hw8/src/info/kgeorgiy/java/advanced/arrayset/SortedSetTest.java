package info.kgeorgiy.java.advanced.arrayset;

import net.java.quickcheck.Generator;
import net.java.quickcheck.collection.Pair;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import java.util.*;

import static net.java.quickcheck.generator.CombinedGenerators.excludeValues;
import static net.java.quickcheck.generator.CombinedGenerators.lists;
import static net.java.quickcheck.generator.CombinedGeneratorsIterables.*;
import static net.java.quickcheck.generator.PrimitiveGenerators.fixedValues;
import static net.java.quickcheck.generator.PrimitiveGenerators.integers;

/**
 * @author Georgiy Korneev (kgeorgiy@kgeorgiy.info)
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SortedSetTest {

    public static final int PERFORMANCE_SIZE = 10_000;

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(final Description description) {
            System.out.println("== Running " + description.getMethodName());
        }
    };

    @Test
    public void test01_constructors() throws ClassNotFoundException, NoSuchMethodException {
        final Class<?> token = loadClass();
        Assert.assertTrue(token.getName() + " should implement SortedSet interface", SortedSet.class.isAssignableFrom(token));

        checkConstructor("default constructor", token);
        checkConstructor("constructor out of Collection", token, Collection.class);
        checkConstructor("constructor out of Collection and Comparator", token, Collection.class, Comparator.class);
    }

    @Test
    public void test02_empty() {
        final SortedSet<Integer> set = create(new Object[]{});
        Assert.assertEquals("Empty set size should be zero", 0, (Object) set.size());
        Assert.assertTrue("Empty set should be empty", set.isEmpty());
        Assert.assertEquals("toArray for empty set should return empty array", 0, (Object) set.toArray().length);
    }

    @Test
    public void test03_naturalOrder() {
        for (final List<Integer> elements : someLists(integers())) {
            final SortedSet<Integer> set = set(elements);
            final SortedSet<Integer> treeSet = treeSet(elements);
            assertEq(set, treeSet, "elements = " + elements);
        }
    }

    @Test
    public void test04_externalOrder() {
        for (final Pair<NamedComparator, List<Integer>> pair : withComparator()) {
            final List<Integer> elements = pair.getSecond();
            final Comparator<Integer> comparator = pair.getFirst();

            assertEq(
                    set(elements, comparator),
                    treeSet(elements, comparator),
                    "(comparator = " + comparator + ", elements = " + elements + ")"
            );
        }
    }

    protected Iterable<Pair<NamedComparator, List<Integer>>> withComparator() {
        return somePairs(comparators, lists(integers()));
    }

    @Test
    public void test05_constructorPerformance() {
        performance("constructor", () -> performanceSet(PERFORMANCE_SIZE));
    }

    @Test
    public void test06_immutable() {
        final SortedSet<Integer> set = set(Arrays.asList(1));
        try {
            set.add(1);
            Assert.fail("add should throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
        }

        try {
            set.addAll(Arrays.asList(1));
            Assert.fail("addAll should throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
        }

        try {
            set.clear();
            Assert.fail("clear should throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
        }

        try {
            final Iterator<Integer> iterator = set.iterator();
            iterator.next();
            iterator.remove();
            Assert.fail("iterator.remove should throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
        }

        try {
            set.remove(1);
            Assert.fail("remove should throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
        }

        try {
            set.removeAll(Arrays.asList(1));
            Assert.fail("removeAll should throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
        }

        try {
            set.retainAll(Arrays.asList(0));
            Assert.fail("retainAll should throw UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
        }
    }

    @Test
    public void test07_contains() {
        for (final Pair<NamedComparator, List<Integer>> pair : withComparator()) {
            final List<Integer> elements = pair.getSecond();
            final Comparator<Integer> comparator = pair.getFirst();

            final SortedSet<Integer> set = set(elements, comparator);
            final String context = "(comparator = " + comparator + ", elements = " + elements + ")";
            for (final Integer element : elements) {
                Assert.assertTrue("set should contains() element " + element + " " + context, set.contains(element));
            }

            final SortedSet<Integer> treeSet = set(elements, comparator);
            for (final Integer element : someOneOf(excludeValues(integers(), elements))) {
                Assert.assertEquals("contains(" + element + ") " + context, treeSet.contains(element), set.contains(element));
            }
        }
    }

    @Test
    public void test08_containsPerformance() {
        performance("contains", new Runnable() {
            @Override
            public void run() {
                final SortedSet<Integer> set = performanceSet(10_000);
                for (final Integer element : set) {
                    Assert.assertTrue(null, set.contains(element));
                }
            }
        });
    }

    @Test
    public void test09_containsAll() {
        for (final Pair<NamedComparator, List<Integer>> pair : withComparator()) {
            final List<Integer> elements = pair.getSecond();
            final Comparator<Integer> comparator = pair.getFirst();

            final SortedSet<Integer> set = set(elements, comparator);
            final String context = "(comparator = " + comparator + ", elements = " + elements + ")";
            Assert.assertTrue("set should contains() all elements " + " " + context, set.containsAll(elements));

            final SortedSet<Integer> treeSet = set(elements, comparator);
            for (final Integer element : someOneOf(excludeValues(integers(), elements))) {
                final List<Integer> l = new ArrayList<>(elements);
                elements.add(element);
                Assert.assertEquals("containsAll(" + l + ") " + context, treeSet.containsAll(l), set.containsAll(l));
            }
        }
    }

    @Test
    public void test10_containsAllPerformance() {
        performance("contains", new Runnable() {
            @Override
            public void run() {
                final SortedSet<Integer> set = performanceSet(10_000);
                Assert.assertTrue(null, set.containsAll(new ArrayList<>(set)));
            }
        });
    }

    private void performance(final String description, final Runnable runnable) {
        runnable.run();

        final long start = System.currentTimeMillis();
        runnable.run();
        final long time = System.currentTimeMillis() - start;
        System.out.println("    " + description + " done in " + time + "ms");
        Assert.assertTrue(description + " works too slow", time < 100);
    }

    private SortedSet<Integer> performanceSet(final int size) {
        final Random random = new Random();
        final List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(random.nextInt());
        }
        return set(list);
    }

    private List<Integer> toList(final SortedSet<Integer> set) {
        return new ArrayList<>(set);
    }

    private List<Number> toArray(final SortedSet<Integer> set) {
        return Arrays.asList(set.toArray(new Number[0]));
    }

    private TreeSet<Integer> treeSet(final List<Integer> elements) {
        return new TreeSet<>(elements);
    }

    private SortedSet<Integer> set(final List<Integer> elements) {
//        List<Integer> z = new LlinkedList<>(treeSet(elements));
        return create(new Object[]{elements}, Collection.class);
    }

    protected SortedSet<Integer> set(final List<Integer> elements, final Comparator<Integer> comparator) {
        return create(new Object[]{elements, comparator}, Collection.class, Comparator.class);
    }

    protected void assertEq(final SortedSet<Integer> set, final SortedSet<Integer> treeSet, final String context) {
        Assert.assertEquals("invalid element order " + context, toList(treeSet), toList(set));
        Assert.assertEquals("invalid toArray " + context, toArray(set), toArray(set));
        Assert.assertEquals("invalid set size " + context, treeSet.size(), (Object) set.size());
        Assert.assertEquals("invalid isEmpty " + context, treeSet.isEmpty(), set.isEmpty());
    }

    protected SortedSet<Integer> treeSet(final List<Integer> elements, final Comparator<Integer> comparator) {
        final SortedSet<Integer> set = new TreeSet<>(comparator);
        set.addAll(elements);
        return set;
    }
    
    protected abstract class NamedComparator implements Comparator<Integer> {
        private final String name;

        private NamedComparator(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Generator<NamedComparator> comparators = fixedValues(Arrays.asList(
            new NamedComparator("Natural order") {
                @Override
                public int compare(final Integer i1, final Integer i2) {
                    return Integer.compare(i1, i2);
                }
            },
            new NamedComparator("Reverse order") {
                @Override
                public int compare(final Integer i1, final Integer i2) {
                    return Integer.compare(i2, i1);
                }
            },
            new NamedComparator("Div 100") {
                @Override
                public int compare(final Integer i1, final Integer i2) {
                    return Integer.compare(i1 / 100, i2 / 100);
                }
            },
            new NamedComparator("Even first") {
                @Override
                public int compare(final Integer i1, final Integer i2) {
                    final int c = Integer.compare(i1 % 2, i2 % 2);
                    return c != 0 ? c : Integer.compare(i1, i2);
                }
            },
            new NamedComparator("All equal") {
                @SuppressWarnings("ComparatorMethodParameterNotUsed")
                @Override
                public int compare(final Integer i1, final Integer i2) {
                    return 0;
                }
            }
    ));

    private Class<?> loadClass() throws ClassNotFoundException {
        final String className = System.getProperty("cut");
        Assert.assertTrue("Class name not specified", className != null);

        return Class.forName(className);
    }

    private SortedSet<Integer> create(final Object[] params, final Class<?>... types) {
        try {
            @SuppressWarnings("unchecked") final
            SortedSet<Integer> set = (SortedSet<Integer>) loadClass().getConstructor(types).newInstance(params);
            return set;
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.fail("Instantiation error");
            return null;
        }
    }

    private void checkConstructor(final String description, final Class<?> token, final Class<?>... params) {
        try {
            token.getConstructor(params);
        } catch (final NoSuchMethodException e) {
            Assert.fail(token.getName() + " should have " + description);
        }
    }

    @Test
    public void test11_comparator() {
        for (final Pair<NamedComparator, List<Integer>> pair : withComparator()) {
            final List<Integer> elements = pair.getSecond();
            final Comparator<Integer> comparator = pair.getFirst();

            Assert.assertSame("comparator() should return provided comparator", comparator, set(elements, comparator).comparator());
        }
        for (final List<Integer> elements : someLists(integers())) {
            Assert.assertNull("comparator() should return null for default order", set(elements).comparator());
        }
    }

    @Test
    public void test12_headSet() {
        for (final Pair<NamedComparator, List<Integer>> pair : withComparator()) {
            final List<Integer> elements = pair.getSecond();
            final Comparator<Integer> comparator = pair.getFirst();
            final SortedSet<Integer> set = set(elements, comparator);
            final SortedSet<Integer> treeSet = treeSet(elements, comparator);

            for (final Integer element : inAndOut(elements)) {
                assertEq(
                        set.headSet(element),
                        treeSet.headSet(element),
                        "in headSet(" + element + ") (comparator = " + comparator + ", elements = " + elements + ")"
                );
            }
        }
    }

    @Test
    public void test13_tailSet() {
        for (final Pair<NamedComparator, List<Integer>> pair : withComparator()) {
            final List<Integer> elements = pair.getSecond();
            final Comparator<Integer> comparator = pair.getFirst();
            final SortedSet<Integer> set = set(elements, comparator);
            final SortedSet<Integer> treeSet = treeSet(elements, comparator);

            for (final Integer element : inAndOut(elements)) {
                assertEq(
                        set.tailSet(element),
                        treeSet.tailSet(element),
                        "in tailSet(" + element + ") (comparator = " + comparator + ", elements = " + elements + ")"
                );
            }
        }
    }

    protected Collection<Integer> inAndOut(final List<Integer> elements) {
        return concat(elements, someOneOf(excludeValues(integers(), elements)));
    }

    private Collection<Integer> concat(final Iterable<Integer> items1, final Iterable<Integer> items2) {
        final List<Integer> list = new ArrayList<>();
        for (final Integer integer : items1) {
            list.add(integer);
        }
        for (final Integer integer : items2) {
            list.add(integer);
        }
        return list;
    }

    @Test
    public void test14_subSet() {
        for (final Pair<NamedComparator, List<Integer>> pair : withComparator()) {
            final List<Integer> elements = pair.getSecond();
            final Comparator<Integer> comparator = pair.getFirst();
            final SortedSet<Integer> set = set(elements, comparator);
            final SortedSet<Integer> treeSet = treeSet(elements, comparator);

            final Collection<Integer> all = values(elements);
            for (final Pair<Integer, Integer> p : somePairs(fixedValues(all), fixedValues(all))) {
                final Integer from = p.getFirst();
                final Integer to = p.getSecond();
                if (comparator.compare(from, to) <= 0) {
                    assertEq(
                            set.subSet(from, to),
                            treeSet.subSet(from, to),
                            "in subSet(" + from + ", " + to + ") (comparator = " + comparator + ", elements = " + elements + ")"
                    );
                }
            }
        }
    }

    protected Collection<Integer> values(final List<Integer> elements) {
        return concat(inAndOut(elements), Arrays.asList(0, Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    @Test
    public void test15_tailSetPerformance() {
        performance("tailSet", new Runnable() {
            @Override
            public void run() {
                final SortedSet<Integer> set = performanceSet(10_000);
                for (final Integer element : set) {
                    Assert.assertTrue(null, set.tailSet(element).contains(element));
                }
            }
        });
    }

    @Test
    public void test16_first() {
        for (final Pair<NamedComparator, List<Integer>> pair : withComparator()) {
            final List<Integer> elements = pair.getSecond();
            final Comparator<Integer> comparator = pair.getFirst();

            final SortedSet<Integer> set = set(elements, comparator);
            if (set.isEmpty()) {
                try {
                    set.first();
                    Assert.fail("first() should throw NoSuchElementException for empty set");
                } catch (final NoSuchElementException e) {
                }
            } else {
                Assert.assertEquals("first() " + "(comparator = " + comparator + ", elements = " + elements + ")", set(elements, comparator).first(), set.first());
            }
        }
    }

    @Test
    public void test17_last() {
        for (final Pair<NamedComparator, List<Integer>> pair : withComparator()) {
            final List<Integer> elements = pair.getSecond();
            final Comparator<Integer> comparator = pair.getFirst();

            final SortedSet<Integer> set = set(elements, comparator);
            if (set.isEmpty()) {
                try {
                    set.last();
                    Assert.fail("last() should throw NoSuchElementException for empty set");
                } catch (final NoSuchElementException e) {
                    // ok
                }
            } else {
                Assert.assertEquals("last() " + "(comparator = " + comparator + ", elements = " + elements + ")", set(elements, comparator).last(), set.last());
            }
        }
    }
}
