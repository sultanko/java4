package bla.blabla;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

/**
 * Created by sultan on 02.03.15.
 */
public interface Inter extends Inter2 {
    default public void smthDefault(int i1) {
        return;
    }

    public Set<String> dsf(Set<Integer> e1);

    public Integer[] arrayTest(String[] arr);

    public void exceptionTest() throws IOException;

    public abstract void one();

}
