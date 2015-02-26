package ru.ifmo.ctddev.shah.arrayset;

import java.util.Random;
import java.util.TreeSet;

/**
 * Created by sultan on 25.02.15.
 */
public class Main {

    public static void main(String[] args) {

        TreeSet<Integer> treeSet = new TreeSet<>();

        Random random = new Random();
        for (int i = 0; i < 50; i++) {
            int chisl = random.nextInt();
            treeSet.add(chisl);
        }
        ArraySet<Integer> arraySet = new ArraySet<>(treeSet);

        for (Integer item : treeSet) {
            assert  treeSet.descendingSet().first().equals(arraySet.descendingSet().first()) : "test first";
            assert  treeSet.descendingSet().last().equals(arraySet.descendingSet().last()) : "test last";
            assert  treeSet.descendingSet().tailSet(item).last().equals(
                    arraySet.descendingSet().tailSet(item).last()) : "test tailSet";
            assert  treeSet.tailSet(item).last().equals(
                    arraySet.descendingSet().descendingSet().tailSet(item).last()) : "double test tailSet";
            if (item != treeSet.last()) {
                assert treeSet.descendingSet().headSet(item).first().equals(
                        arraySet.descendingSet().headSet(item).first());
            }
            if (item != treeSet.first()) {
                assert treeSet.headSet(item).first().equals(
                        arraySet.descendingSet().descendingSet().headSet(item).first() ) : "double test headSet";
            }

            assert treeSet.descendingSet().higher(item) == arraySet.descendingSet().higher(item);
        }
    }
}
