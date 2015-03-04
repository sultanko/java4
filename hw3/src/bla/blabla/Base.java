package bla.blabla;

import java.io.File;
import java.sql.Time;
import java.util.TreeSet;

/**
 * Created by sultan on 01.03.15.
 */
public abstract class Base extends BaseOfBase implements Inter, Inter2 {
    int hole;
    protected Base(int i1) {
        hole = i1;
    }
    public abstract boolean openFile(File file);
    protected abstract Thread getThread(Thread thread, int modifiers, String dsfds);
    public abstract Integer sysConst(TreeSet<?> coll, Time t);
    public abstract char getOpenFile(File file);
    public abstract double calc(int i1, int i2);
    public abstract Float manyArgs(Integer[]... integers);

    public Double getCalced() {
        return 0.0;
    }

    public abstract class MMMM {
        public abstract void gets();
    }

}
