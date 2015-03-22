package bla.blabla;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by sultan on 01.03.15.
 */
public abstract class BaseOfBase {

    public abstract void test3() throws IllegalArgumentException, FileNotFoundException;

    public abstract Socket setNet(InputStream is);

    public int rewrited() {
        return 0;
    }

    public abstract Double getCalced();
}
